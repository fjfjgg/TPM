/*
    This file is part of Tool Provider Manager - Manager of LTI Tool Providers
    for learning platforms.
    Copyright (C) 2022  Francisco José Fernández Jiménez.

    Tool Provider Manager is free software: you can redistribute it and/or
    modify it under the terms of the GNU General Public License as published
    by the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Tool Provider Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
    Public License for more details.

    You should have received a copy of the GNU General Public License along
    with Tool Provider Manager. If not, see <https://www.gnu.org/licenses/>.
*/

package es.us.dit.lti;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Consumer;
import es.us.dit.lti.entity.Context;
import es.us.dit.lti.entity.LtiUser;
import es.us.dit.lti.entity.Nonce;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.ResourceUser;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.entity.ToolKey;
import es.us.dit.lti.persistence.ToolConsumerDao;
import es.us.dit.lti.persistence.ToolConsumerUserDao;
import es.us.dit.lti.persistence.ToolContextDao;
import es.us.dit.lti.persistence.ToolKeyDao;
import es.us.dit.lti.persistence.ToolNonceDao;
import es.us.dit.lti.persistence.ToolResourceLinkDao;
import es.us.dit.lti.persistence.ToolResourceUserDao;
import jakarta.servlet.http.HttpServletRequest;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;

/**
 * LTI tool session initiated by tool consumer.
 *
 * <p>Only support LTI 1.1 <a href= "https://www.imsglobal.org/spec/lti-bo/v1p1/">
 * https://www.imsglobal.org/spec/lti-bo/v1p1/</a>.
 *
 * <p>Validates tool launch (keys, sign, authorization) and save information about
 * consumer, context, resource link, LTI user, etc.
 *
 * @author Francisco José Fernández Jiménez
 *
 */
public final class ToolSession implements Serializable {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private final transient Logger logger = LoggerFactory.getLogger(getClass());
	/**
	 * Recognized LTI role prefixes.
	 */
	private static final String[] ROLE_PREFIXES = { "urn:lti:role:ims/lis/", "urn:lti:sysrole:ims/lis/",
			"urn:lti:instrole:ims/lis/" };
	/**
	 * Roles equivalent to learner.
	 */
	private static final String[] LEARNER_ALIASES = { "Learner" };
	/**
	 * Roles equivalent to instructor.
	 */
	private static final String[] INSTRUCTOR_ALIASES = { "Instructor", "ContentDeveloper", "TeachingAssistant" };
	/**
	 * Roles equivalet to administrator. Not used for now.
	 */
	private static final String[] ADMINISTRATOR_ALIASES = { "Administrator", "SysAdmin", "TeachingAssistant" };
	/**
	 * Default LTI nonce duration in minutes.
	 */
	private static final int DEFAULT_NONCE_DURATION = 10;
	/**
	 * Supported LTI version (send in request).
	 */
	private static final String LTI_VERSION = "LTI-1p0";
	/**
	 * URL for a redirect tool.
	 */
	private static final String REDIRECT_MODE_URL = "learner/redirect.jsp";
	/**
	 * URL for a learner.
	 */
	private static final String LEARNER_URL = "learner/tool.jsp";
	/**
	 * URL for a instructor.
	 */
	private static final String INSTRUCTOR_URL = "instructor/tool.jsp";

	/**
	 * Request parameters to save in resource link custom properties. This can be used to
	 * customize the tool for some resource links.
	 *
	 * <p>They are the following:
	 * <ul>
	 * <li><code>custom_args</code>: extra arguments to pass to corrector.
	 * <li><code>custom_debug</code>: show debug information on error.
	 * <li><code>custom_nocal</code>: disable saving score/outcome/califications.
	 * <li><code>custom_filepattern</code>: allowed file pattern for the resource
	 * link.
	 * </ul>
	 *
	 */
	private static String[] customPropertyNames = { 
			"custom_args", "custom_debug", 
			"custom_nocal", "custom_filepattern" };

	// Session attributes
	/**
	 * The LTI user is a learner.
	 */
	private boolean isLearner = false;
	/**
	 * The LTI user is a instructor.
	 */
	private boolean isInstructor = false;
	/**
	 * The LTI user is a administrator.
	 */
	private boolean isAdministrator = false;
	/**
	 * Roles of the LTI user.
	 */
	private Set<String> roles = new HashSet<>();
	/**
	 * URL to go back to consumer.
	 *
	 * <p>From LTI specification: launch_presentation_return_url "Fully qualified URL
	 * where the TP can redirect the user back to the TC interface. This URL can be
	 * used once the TP is finished or if the TP cannot start or has some technical
	 * difficulty. In the case of an error, the TP may add a parameter called
	 * lti_errormsg that includes some detail as to the nature of the error. The
	 * lti_errormsg value should make sense if displayed to the user. If the tool
	 * has displayed a message to the end user and only wants to give the TC a
	 * message to log, use the parameter lti_errorlog instead of lti_errormsg. If
	 * the tool is terminating normally, and wants a message displayed to the user
	 * it can include a text message as the lti_msg parameter to the return URL. If
	 * the tool is terminating normally and wants to give the TC a message to log,
	 * use the parameter lti_log. This data should be sent on the URL as a GET – so
	 * the TP should take care to keep the overall length of the parameters small
	 * enough to fit within the limitations of a GET request. This parameter is
	 * recommended."
	 */
	private String ltiReturnUrl;
	/**
	 * Locale requested.
	 */
	private String presentationLocale;
	/**
	 * Where the tool will be shown: full window/tab, iframe.
	 */
	private String presentationDocumentTarget;
	/**
	 * True if {@link #presentationDocumentTarget} ends with "frame".
	 */
	private boolean frameMode = false;
	/**
	 * Username of LTI user. The value of "sourceId" is used.
	 *
	 * @see LtiUser
	 */
	private String sessionUserId; // ltiResourceUser.getUser().getSourceId()
	/**
	 * ID used to avoid CSRF attacks. Must be used in forms and POST request.
	 *
	 * <p>Random ID, different to web session id. Not stored in cookies.
	 */
	private String launchId;
	/**
	 * Determine if score/outcome must be sent to consumer.
	 */
	private boolean outcomeAllowed;
	/**
	 * The request is valid and can continue.
	 */
	private boolean valid = false;
	/**
	 * Error message in case of error, to return to consumer.
	 */
	private String error = null;

	// Objects
	/**
	 * Tool key of this session. Influences the learners an instructor can see.
	 */
	private ToolKey toolKey;
	/**
	 * The tool.
	 */
	private Tool tool;
	/**
	 * Tool consumer.
	 */
	private Consumer consumer;
	/**
	 * Context in tool consumer. Course.
	 */
	private Context context;
	/**
	 * Resource link origin.
	 */
	private ResourceLink resourceLink;
	/**
	 * LTI user and information to write/read outcomes.
	 */
	private ResourceUser ltiResourceUser;

	/**
	 * Gets de URL to redirect after first request.
	 *
	 * @return the appropriate URL
	 */
	public String getContinueUrl() {

		String url = ltiReturnUrl;
		if (valid) {
			if (tool.getToolUiConfig().isRedirectMode()) {
				// Redirect twice
				url = REDIRECT_MODE_URL;
			} else if (isLearner()) {
				// Redirect to learner tool
				url = LEARNER_URL;
			} else { // instructor
				// Redirect to instructor tool
				url = INSTRUCTOR_URL;
			}

		}
		return url;
	}

	/**
	 * Gets the LTI return URL received in request.
	 *
	 * @return the ltiReturnUrl
	 */
	public String getLtiReturnUrl() {
		return ltiReturnUrl;
	}

	/**
	 * Sets the LTI return URL.
	 *
	 * @param ltiReturnUrl new URL
	 */
	public void setLtiReturnUrl(String ltiReturnUrl) {
		this.ltiReturnUrl = ltiReturnUrl;
	}

	/**
	 * Gets the presentation locale.
	 *
	 * @return the presentation locale.
	 */
	public String getPresentationLocale() {
		return presentationLocale;
	}

	/**
	 * Sets the presentation locale.
	 *
	 * @param presentationLocale new value
	 */
	public void setPresentationLocale(String presentationLocale) {
		this.presentationLocale = presentationLocale;
	}

	/**
	 * Gets the presentation document target (window, frame,...).
	 *
	 * @return the presentation document target
	 */
	public String getPresentationDocumentTarget() {
		return presentationDocumentTarget;
	}

	/**
	 * Sets the presentation document target (window, frame,...).
	 *
	 * @param presentationDocumentTarget new value
	 */
	public void setPresentationDocumentTarget(String presentationDocumentTarget) {
		this.presentationDocumentTarget = presentationDocumentTarget;
	}

	/**
	 * Gets if the presentation document target is "frame" or "iframe".
	 *
	 * @return true if frame mode
	 */
	public boolean isFrameMode() {
		return frameMode;
	}

	/**
	 * Sets the frame mode.
	 *
	 * @param frameMode new value
	 */
	public void setFrameMode(boolean frameMode) {
		this.frameMode = frameMode;
	}

	/**
	 * Gets the tool key associated to this tool session.
	 *
	 * @return the tool key
	 */
	public ToolKey getToolKey() {
		return toolKey;
	}

	/**
	 * Sets the tool key associated to this tool session.
	 *
	 * @param toolKey new value
	 */
	public void setToolKey(ToolKey toolKey) {
		this.toolKey = toolKey;
	}

	/**
	 * Gets the tool associated to this tool session.
	 *
	 * @return the tool
	 */
	public Tool getTool() {
		return tool;
	}

	/**
	 * Sets the tool associated to this tool session.
	 *
	 * @param tool new value
	 */
	public void setTool(Tool tool) {
		this.tool = tool;
	}

	/**
	 * Gets the tool consumer associated to this tool session.
	 *
	 * @return the tool consumer
	 */
	public Consumer getConsumer() {
		return consumer;
	}

	/**
	 * Sets the tool consumer associated to this tool session.
	 *
	 * @param consumer new value
	 */
	public void setConsumer(Consumer consumer) {
		this.consumer = consumer;
	}

	/**
	 * Gets the tool context associated to this tool session.
	 *
	 * @return the tool context
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * Sets the tool context associated to this tool session.
	 *
	 * @param context new value
	 */
	public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * Gets the resource link associated to this tool session.
	 *
	 * @return the resource link
	 */
	public ResourceLink getResourceLink() {
		return resourceLink;
	}

	/**
	 * Sets the resource link associated to this tool session.
	 *
	 * @param resourceLink new value
	 */
	public void setResourceLink(ResourceLink resourceLink) {
		this.resourceLink = resourceLink;
	}

	/**
	 * Gets the resource user associated to this tool session.
	 *
	 * @return the tool context
	 */
	public ResourceUser getLtiResourceUser() {
		return ltiResourceUser;
	}

	/**
	 * Sets the resource user associated to this tool session.
	 *
	 * @param ltiResourceUser new value
	 */
	public void setLtiResourceUser(ResourceUser ltiResourceUser) {
		this.ltiResourceUser = ltiResourceUser;
	}

	/**
	 * Gets the LTI username associated to this tool session.
	 *
	 * @return the tool context
	 */
	public String getSessionUserId() {
		return sessionUserId;
	}

	/**
	 * Sets the LTI username associated to this tool session.
	 *
	 * @param sessionUserId new value
	 */
	public void setSessionUserId(String sessionUserId) {
		this.sessionUserId = sessionUserId;
	}

	/**
	 * Gets the random ID associated to this tool session.
	 *
	 * @return the random ID
	 */
	public String getLaunchId() {
		return launchId;
	}

	/**
	 * Sets the random ID associated to this tool session.
	 *
	 * @param launchId new value
	 */
	public void setLaunchId(String launchId) {
		this.launchId = launchId;
	}

	/**
	 * Gets if this tool session is valid.
	 *
	 * <p>A tool session is valid if it is correctly initiated without errors.
	 *
	 * @return the tool context
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Sets the valid state of this tool session.
	 *
	 * @param valid new value
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * Gets the error message associated to this tool session.
	 *
	 * @return the error message
	 */
	public String getError() {
		return error;
	}

	/**
	 * Sets the error message.
	 *
	 * @param error new value
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * Gets if this tool session can write the score/outcome in tool consumer.
	 *
	 * @return if it is allowed write the outcome.
	 */
	public boolean isOutcomeAllowed() {
		return outcomeAllowed;
	}

	/**
	 * Sets if writing outcome are allowed.
	 *
	 * @param outcomeAllowed new value
	 */
	public void setOutcomeAllowed(boolean outcomeAllowed) {
		this.outcomeAllowed = outcomeAllowed;
	}

	/**
	 * Sets the roles of the LTI user.
	 *
	 * @param roles set of roles
	 */
	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	/**
	 * Sets user roles from comma separated string.
	 *
	 * @param rolesString string to analyze
	 */
	public void setRoles(String rolesString) {
		final List<String> rolesList = Arrays.asList(rolesString.split(","));
		roles.clear();
		for (String role : rolesList) {
			role = role.trim();
			// Remove prefixes
			for (final String prefix : ROLE_PREFIXES) {
				if (role.startsWith(prefix)) {
					role = role.substring(prefix.length());
					break;
				}
			}
			if (!role.isEmpty() && roles.add(role)) {
				// Detect common roles
				if (ArrayUtils.contains(LEARNER_ALIASES, role)) {
					isLearner = true;
				} else if (ArrayUtils.contains(INSTRUCTOR_ALIASES, role)) {
					isInstructor = true;
				} else if (ArrayUtils.contains(ADMINISTRATOR_ALIASES, role)) {
					isAdministrator = true;
				}
			}
		}
	}

	/**
	 * Gets if the user is a learner.
	 *
	 * @return true if user is a learner
	 */
	public boolean isLearner() {
		return isLearner;
	}

	/**
	 * Sets if user is a learner.
	 *
	 * @param isLearner new value
	 */
	public void setLearner(boolean isLearner) {
		this.isLearner = isLearner;
	}

	/**
	 * Gets if the user is a instructor.
	 *
	 * @return true if user is a instructor
	 */
	public boolean isInstructor() {
		return isInstructor;
	}

	/**
	 * Sets if user is a instructor.
	 *
	 * @param isInstructor new value
	 */
	public void setInstructor(boolean isInstructor) {
		this.isInstructor = isInstructor;
	}

	/**
	 * Gets if the user is a administrator.
	 *
	 * @return true if user is a administrator
	 */
	public boolean isAdministrator() {
		return isAdministrator;
	}

	/**
	 * Sets if user is a administrator.
	 *
	 * @param isAdministrator new value
	 */
	public void setAdministrator(boolean isAdministrator) {
		this.isAdministrator = isAdministrator;
	}

	/**
	 * Gets the set of roles of the user.
	 *
	 * @return set of roles
	 */
	public Set<String> getRoles() {
		return roles;
	}

	/**
	 * Gets if this tool session can not write the score/outcome.
	 *
	 * @return true if no score/outcome can be written
	 */
	private Boolean isWithoutOutcome() {
		final String customNocal = resourceLink.getCustomProperty("custom_nocal");
		Boolean nocal;
		if (tool.isOutcome()) {
			if (resourceLink.getOutcomeServiceUrl() != null) {
				if (customNocal != null) {
					nocal = Boolean.valueOf(customNocal);
				} else {
					nocal = false;
				}
			} else if (customNocal != null) {
				nocal = Boolean.valueOf(customNocal);
			} else {
				nocal = true;
			}
		} else {
			nocal = true;
		}
		return nocal;
	}

	/**
	 * Initializes the tool session analyzing the consumer request.
	 *
	 * <p>Verifies minimum requirements and that the tool key is valid for the
	 * consumer, context and resource link.
	 *
	 * <p>Create objects in db if they do not exist.
	 *
	 * @param request HTTP request of tool consumer
	 * @return if it is valid
	 */
	public boolean init(HttpServletRequest request) {
		valid = false;

		// Send errors in requested locale
		presentationLocale = request.getParameter("launch_presentation_locale");

		if (!checkMinimumRequirements(request.getParameterMap())) {
			logger.error("The request does not meet the minimum requirements.");
			return valid;
		}

		// Get key information
		final String consumerGuid = request.getParameter("tool_consumer_instance_guid").trim();
		final String contextId = request.getParameter("context_id").trim();
		final String resourceLinkId = request.getParameter("resource_link_id").trim();
		final String ltiKey = request.getParameter("oauth_consumer_key").trim();
		if (!loadToolKey(ltiKey, consumerGuid, contextId, resourceLinkId, request.getRemoteAddr())) {
			error = "T_LTI_PROHIBIDO";
			logger.error("Key does not exist, is not allowed or is not enabled: {}. Remote address: {}",
					ltiKey, request.getRemoteAddr());
			return valid;
		}

		// Check signature
		if (!checkSignature(request)) {
			error = "T_LTI_ERROR_FIRMA";
			return valid;
		}

		// Return URL (not set before to avoid reflected DoS)
		ltiReturnUrl = request.getParameter("launch_presentation_return_url");

		// Create/update consumer data
		final Consumer auxConsumer = new Consumer();
		auxConsumer.setGuid(consumerGuid);
		auxConsumer.setLtiVersion(request.getParameter("lti_version"));
		auxConsumer.setName(request.getParameter("tool_consumer_instance_name"));
		String consumerVersion = null;
		if (request.getParameter("tool_consumer_info_product_family_code") != null) {
			consumerVersion = request.getParameter("tool_consumer_info_product_family_code");
			if (request.getParameter("tool_consumer_info_version") != null) {
				consumerVersion += "-" + request.getParameter("tool_consumer_info_version");
			}
		} else if (request.getParameter("ext_lms") != null) {
			consumerVersion = request.getParameter("ext_lms");
		}
		auxConsumer.setVersion(consumerVersion);
		if (request.getParameter("launch_presentation_css_url") != null) {
			auxConsumer.setCssPath(request.getParameter("launch_presentation_css_url"));
		} else if (request.getParameter("ext_launch_presentation_css_url") != null) {
			auxConsumer.setCssPath(request.getParameter("ext_launch_presentation_css_url"));
		}
		// Check if exist
		if (toolKey.getConsumer() != null) {
			consumer = toolKey.getConsumer();
		} else {
			consumer = ToolConsumerDao.getByGuid(consumerGuid);
		}
		if (consumer != null) {
			// compare with auxConsumer and update
			if (!consumer.equals(auxConsumer)) {
				logger.info("Consumer has changed");
				auxConsumer.setSid(consumer.getSid());
				ToolConsumerDao.update(auxConsumer);
				consumer = auxConsumer;
				logger.info("Consumer updated: {}", auxConsumer.getName());
			}
		} else // if not exist, create
		if (!ToolConsumerDao.create(auxConsumer)) {
			logger.error("I can't continue because I can't create the consumer");
			return valid;
		} else {
			consumer = auxConsumer;
			logger.info("New Consumer created: {}", auxConsumer.getName());
		}

		// Check nonce
		// "The LTI parameter oauth_nonce in combination with the oauth_timestamp is
		// used by the OAuth authentication
		// protocol as a defense against man-in-the-middle attacks."
		// Unique for a tool consumer
		int ts = 0;
		try {
			ts = Integer.parseInt(request.getParameter("oauth_timestamp"));
		} catch (final NumberFormatException e) {
			logger.error("Error getting oauth_timestamp: {}", request.getParameter("oauth_timestamp"));
		}
		final Nonce nonce = new Nonce(toolKey.getSid(), consumer.getSid(), request.getParameter("oauth_nonce"), ts,
				DEFAULT_NONCE_DURATION);
		if (!ToolNonceDao.exist(nonce)) {
			ToolNonceDao.create(nonce);
		} else {
			logger.error("Nonce exists");
			error = "T_LTI_ERROR_NONCE_DUPLICADO";
			return valid;
		}
		// Delete expired nonces
		ToolNonceDao.deleteExpired();

		// Create/update objects

		// Consumer data (before)

		// Context data
		final Context auxContext = new Context();
		auxContext.setConsumer(consumer);
		auxContext.setContextId(contextId);
		auxContext.setLabel(request.getParameter("context_label"));
		auxContext.setTitle(request.getParameter("context_title"));
		// Check if exist
		if (toolKey.getContext() != null) {
			context = toolKey.getContext();
		} else {
			context = ToolContextDao.getById(consumer, contextId);
		}
		if (context != null) {
			// compare with auxConsumer and update
			if (!context.equals(auxContext)) {
				logger.info("Context has changed");
				auxContext.setSid(context.getSid());
				ToolContextDao.update(auxContext);
				context = auxContext;
				logger.info("Context updated: {}", auxContext.getContextId());
			}
		} else // if not exist, create
		if (!ToolContextDao.create(auxContext)) {
			logger.error("I can't continue because I can't create the context");
			return valid;
		} else {
			context = auxContext;
			logger.info("New Context created: {}", auxContext.getContextId());
		}

		// Resource link data
		final ResourceLink auxResourceLink = new ResourceLink();
		auxResourceLink.setTool(tool);
		auxResourceLink.setContext(context);
		auxResourceLink.setResourceId(resourceLinkId);
		auxResourceLink.setTitle(request.getParameter("resource_link_title"));
		auxResourceLink.setOutcomeServiceUrl(request.getParameter("lis_outcome_service_url"));
		auxResourceLink.setToolKey(toolKey);
		// Custom properties
		for (final String name : customPropertyNames) {
			if (request.getParameter(name) != null) {
				auxResourceLink.setCustomProperty(name, request.getParameter(name));
			}
		}
		// Check if exist
		if (toolKey.getResourceLink() != null) {
			resourceLink = toolKey.getResourceLink();
		} else {
			final Integer toolSid = auxResourceLink.getTool() == null ? null : auxResourceLink.getTool().getSid();
			final Integer contextSid = auxResourceLink.getContext() == null ? null
					: auxResourceLink.getContext().getSid();
			resourceLink = ToolResourceLinkDao.getById(toolSid, contextSid, resourceLinkId);
		}
		if (resourceLink != null) {
			auxResourceLink.setSid(resourceLink.getSid());
			// compare with aux and update
			if (!resourceLink.equals(auxResourceLink)) {
				logger.info("Resource Link has changed");
				ToolResourceLinkDao.update(auxResourceLink);
				logger.info("Resource Link updated : {}", auxResourceLink.getResourceId());
			}
			resourceLink = auxResourceLink;
		} else if (!ToolResourceLinkDao.create(auxResourceLink)) { // if not exist, create
			logger.error("I can't continue because I can't create the resource link");
			return valid;
		} else {
			resourceLink = auxResourceLink;
			logger.info("New Resource Link created: {}", auxResourceLink.getResourceId());
		}

		// user data
		LtiUser user = new LtiUser();
		user.setConsumer(consumer);
		user.setUserId(request.getParameter("user_id").trim());
		user.setNameGiven(request.getParameter("lis_person_name_given"));
		user.setNameFamily(request.getParameter("lis_person_name_family"));
		user.setNameFull(request.getParameter("lis_person_name_full"));
		user.setEmail(request.getParameter("lis_person_contact_email_primary"));
		// Sanitize sourceId
		String sourceId = request.getParameter("lis_person_sourcedid");
		if (sourceId != null) {
			final String sanitizedId = Settings.sanitizeString(sourceId);
			if (!sanitizedId.equals(sourceId)) {
				logger.info("SourceId sanitized {} -> {}", sourceId, sanitizedId);
				sourceId = sanitizedId;
			}
		}
		user.setSourceId(sourceId);

		// Check if exist
		final LtiUser auxUser = ToolConsumerUserDao.getById(consumer.getSid(), user.getUserId());

		if (auxUser != null) {
			// compare with auxConsumer and update
			if (!auxUser.equals(user)) {
				logger.info("LTI User has changed");
				user.setSid(auxUser.getSid());
				ToolConsumerUserDao.update(user);
				logger.info("LTI user updated: {}", user.getSourceId());
			} else {
				user = auxUser;
			}
		} else // if not exist, create
		if (!ToolConsumerUserDao.create(user)) {
			logger.error("I can't continue because I can't create the LTI user");
			return valid;
		} else {
			logger.info("New LTI user created: {}", user.getSourceId());
		}

		// resource_user data
		final ResourceUser auxResourceUser = new ResourceUser();
		auxResourceUser.setResourceLink(resourceLink);
		auxResourceUser.setUser(user);
		auxResourceUser.setResultSourceId(request.getParameter("lis_result_sourcedid"));
		// Check if exist
		ltiResourceUser = ToolResourceUserDao.getById(resourceLink.getSid(), user.getSid());
		if (ltiResourceUser != null) {
			// compare with aux and update
			if (!ltiResourceUser.equals(auxResourceUser)) {
				logger.info("Resource User has changed");
				auxResourceUser.setSid(ltiResourceUser.getSid());
				ToolResourceUserDao.update(auxResourceUser);
				logger.info("Resource User updated.");
				ltiResourceUser = auxResourceUser;
			} else {
				// equals, complete fields
				ltiResourceUser.setResourceLink(resourceLink);
				ltiResourceUser.setUser(user);
			}
		} else // if not exist, create
		if (!ToolResourceUserDao.create(auxResourceUser)) {
			logger.error("I can't continue because I can't create the resource user");
			return valid;
		} else {
			logger.info("New Resource User created.");
			ltiResourceUser = auxResourceUser;
		}

		// Session data
		// - roles, outcome, presentation
		setRoles(request.getParameter("roles"));
		outcomeAllowed = !isWithoutOutcome();
		presentationDocumentTarget = request.getParameter("launch_presentation_document_target");
		frameMode = presentationDocumentTarget != null && presentationDocumentTarget.endsWith("frame");
		sessionUserId = getUserId(request.getParameter("custom_username"));
		// Learner and Instructor at the same time is not permitted
		if (isLearner && isInstructor) {
			if (logger.isWarnEnabled()) {
				logger.warn("User [{}] is learner and instructor: [{}]. The instructor role is eliminated",
					sessionUserId, request.getParameter("roles"));
			}
			isInstructor = false;
		}
		generateLaunchId();
		valid = true;

		return valid;
	}

	/**
	 * Loads tool key from db and verifies if it is enabled for a consumer, context
	 * and resource link.
	 *
	 * @param key            key of tool key
	 * @param consumerGuid   consumer GUID of tool session
	 * @param contextId      context ID of tool session
	 * @param resourceLinkId resource link ID of tool session
	 * @param remoteAddress	 remote address of tool session
	 * @return true if the took key is found and valid for the tool session
	 */
	private boolean loadToolKey(String key, String consumerGuid, String contextId, String resourceLinkId,
			String remoteAddress) {
		// Search key with associate objects
		final ToolKey tk = ToolKeyDao.get(key, false); // lazy = false

		try {
			// Check if key and tool are enabled and Check if origin is allowed
			if (tk != null && tk.isEnabled() && tk.getTool().isEnabled()
					&& (tk.getConsumer() == null || consumerGuid.equals(tk.getConsumer().getGuid()))
					&& (tk.getContext() == null || contextId.equals(tk.getContext().getContextId()))
					&& (tk.getResourceLink() == null || resourceLinkId.equals(tk.getResourceLink().getResourceId()))
					&& (tk.getAddress() == null || tk.getAddress().isBlank()
							|| Pattern.matches(tk.getAddress(), remoteAddress))) {
				toolKey = tk;
				tool = tk.getTool();
			}
		} catch (final PatternSyntaxException e) {
			// Not allowed
			logger.error("Invalid address pattern: {}", tk.getAddress());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return toolKey != null;
	}

	/**
	 * Check OAuth signature using tool key.
	 *
	 * @param request HTTP request
	 * @return true if signature is valid
	 */
	private boolean checkSignature(HttpServletRequest request) {
		boolean res;

		// calbackURL is in parameter "oauth_callback"
		final String oauthCallback = request.getParameter("oauth_callback");
		final OAuthConsumer oAuthConsumer = new OAuthConsumer(oauthCallback, toolKey.getKey(), toolKey.getSecret(),
				null);
		final OAuthAccessor oAuthAccessor = new OAuthAccessor(oAuthConsumer);
		final OAuthValidator oAuthValidator = new SimpleOAuthValidator();
		final OAuthMessage oAuthMessage = new JakartaHttpRequestMessage(request);
		
		try {
			oAuthValidator.validateMessage(oAuthMessage, oAuthAccessor);
			res = true;
		} catch (final Exception e) {
			logger.error("Check signature: {}", e.getMessage());
			logger.error("oauth_timestamp: {}", request.getParameter("oauth_timestamp"));
			res = false;
		}

		return res;
	}

	/**
	 * Check that the minimum requirements are met.
	 *
	 * @param params parameters of HTTP request
	 * @return true if minimum requirements are met
	 */
	private boolean checkMinimumRequirements(Map<String, String[]> params) {
		boolean res = true;

		final String[] requiredParams = { "oauth_consumer_key", "resource_link_id", "user_id", "roles",
				"lti_message_type", "lti_version", "tool_consumer_instance_guid", "context_id", "oauth_callback" };
		// Required parameters
		for (final String rp : requiredParams) {
			if (!params.containsKey(rp)) {
				res = false;
				logger.error("Parameter does not exist: {}", rp);
			} else {
				final int length = params.get(rp)[0].length();
				if (length == 0 || length > 255) {
					res = false;
					logger.error("Parameter with wrong length: {}", rp);
				}
			}
			if (!res) {
				break;
			}
		}
		// Required values
		if (res && (!"basic-lti-launch-request".equals(params.get("lti_message_type")[0])
				|| !LTI_VERSION.equals(params.get("lti_version")[0]))) {
			if (logger.isErrorEnabled()) {
				logger.error("Incorrect version or message type values: {} {}", params.get("lti_message_type")[0],
						params.get("lti_version")[0]);
			}
			res = false;
		}

		return res;
	}

	/**
	 * Generates a random ID.
	 */
	private void generateLaunchId() {
		try {
			launchId = SecurityUtil.getRandomId();
		} catch (final Exception e) {
			// This should never happen.
			e.printStackTrace();
		}
	}

	/**
	 * URL encode a text.
	 *
	 * <p>Wrapper function to avoid exceptions due to charset.
	 *
	 * @param text text to encode
	 * @return the encoded text
	 */
	public static String urlEncode(String text) {
		String result;
		try {
			result = URLEncoder.encode(text, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			// This should never happen.
			result = text;
		}
		return result;
	}

	/**
	 * Get a sanitized username.
	 *
	 * @param customUserId custom username or null
	 * @return the username
	 */
	private String getUserId(String customUserId) {
		String userId = "";
		if (customUserId != null) {
			userId = customUserId;
		} else if (ltiResourceUser.getUser().getSourceId() != null) {
			userId = ltiResourceUser.getUser().getSourceId();
		} else if (ltiResourceUser.getUser().getUserId() != null) {
			userId = ltiResourceUser.getUser().getUserId();
		}
		return Settings.sanitizeString(userId);
	}

	/**
	 * Initializes a bogus tool session for a management user.
	 *
	 * @param tool          target tool
	 * @param launchId      random ID to use
	 * @param sessionUser   username
	 * @param title         title of context
	 * @param learner       if it is a learner session
	 * @param instructor    if it is a instructor session
	 * @param remoteAddress remote address of tool session
	 * @param outcomeSrvUrl URL of test outcome service
	 * @return if it is valid
	 */
	public boolean initTest(Tool tool, String launchId, String sessionUser, String title, boolean learner,
			boolean instructor, String remoteAddress, String outcomeSrvUrl) {

		final String consumerGuid = "test";
		final String contextId = "test";
		final String resourceLinkId = "test";
		final String ltiKey = tool.getName() + "_test";
		this.tool = tool;

		toolKey = ToolKeyDao.get(ltiKey, false);
		if (toolKey == null) {
			toolKey = ToolKeyDao.getDefault(tool);
			if (toolKey == null) {
				toolKey = new ToolKey();
			}
		}
		// Test address restriction
		try {
			if (toolKey.getAddress() != null && !toolKey.getAddress().isBlank()
					&& !Pattern.matches(toolKey.getAddress(), remoteAddress)) {
				logger.error("The remote address is not allowed: {}", remoteAddress);
				return valid;
			}
		} catch (final PatternSyntaxException e) {
			// Not allowed
			logger.error("Invalid address pattern: {}", toolKey.getAddress());
		}
		if (toolKey.getTool() != null) {
			this.tool = toolKey.getTool(); // Simulate real session
		}
		toolKey.setEnabled(true);
		
		// Return URL (not set before to avoid reflected DoS)
		ltiReturnUrl = null;

		// Create/update consumer data
		// Check if exist
		if (toolKey.getConsumer() != null) {
			consumer = toolKey.getConsumer();
		} else {
			consumer = ToolConsumerDao.getByGuid(consumerGuid);
		}
		if (consumer == null) {
			final Consumer auxConsumer = new Consumer();
			auxConsumer.setGuid(consumerGuid);
			auxConsumer.setLtiVersion("");
			auxConsumer.setName("test");
			auxConsumer.setVersion("1.0");
			// if not exist, create
			if (!ToolConsumerDao.create(auxConsumer)) {
				logger.error("I can't continue because I can't create the consumer");
				return valid;
			} else {
				consumer = auxConsumer;
				logger.info("New Consumer created: {}", auxConsumer.getName());
			}
		}

		// Create/update objects
		// Context data
		// Check if exist
		if (toolKey.getContext() != null) {
			context = toolKey.getContext();
		} else {
			context = ToolContextDao.getById(consumer, contextId);
		}
		if (context == null) {
			final Context auxContext = new Context();
			auxContext.setConsumer(consumer);
			auxContext.setContextId(contextId);
			auxContext.setLabel("");
			auxContext.setTitle(title);
			// if not exist, create
			if (!ToolContextDao.create(auxContext)) {
				logger.error("I can't continue because I can't create the context");
				return valid;
			} else {
				context = auxContext;
				logger.info("New Context created: {}", auxContext.getContextId());
			}
		}

		// Resource link data
		// Check if exist
		if (toolKey.getResourceLink() != null) {
			resourceLink = toolKey.getResourceLink();
		} else {
			final Integer toolSid = this.tool.getSid();
			final Integer contextSid = context.getSid();
			resourceLink = ToolResourceLinkDao.getById(toolSid, contextSid, resourceLinkId);
		}
		if (toolKey.getSid() == 0 && (resourceLink == null || resourceLink.getToolKey() == null)) {
			// create tool key
			toolKey.setConsumer(consumer);
			toolKey.setContext(context);
			toolKey.setEnabled(false);
			toolKey.setResourceLink(resourceLink);
			toolKey.setTool(tool);
			toolKey.setSecret(SecurityUtil.getRandomId());
			toolKey.setKey(ltiKey);
			try {
				if (ToolKeyDao.create(toolKey)) {
					// Get with Sid
					ToolKey aux = ToolKeyDao.get(ltiKey, false);
					if (aux != null) {
						toolKey = aux;
					}
				}
			} catch (final FileAlreadyExistsException e) {
				// ignore
				logger.trace("Test key exists");
			}
			toolKey.setEnabled(true);
		}

		if (resourceLink == null) {
			final ResourceLink auxResourceLink = new ResourceLink();
			auxResourceLink.setTool(this.tool);
			auxResourceLink.setContext(context);
			auxResourceLink.setResourceId(resourceLinkId);
			auxResourceLink.setTitle("test");
			auxResourceLink.setOutcomeServiceUrl(outcomeSrvUrl);
			if (toolKey.getSid() != 0) {
				auxResourceLink.setToolKey(toolKey);
			}
			// if not exist, create
			if (!ToolResourceLinkDao.create(auxResourceLink)) {
				logger.error("I can't continue because I can't create the resource link");
				return valid;
			} else {
				resourceLink = auxResourceLink;
				logger.info("New Resource Link created: {}", auxResourceLink.getResourceId());
			}
		} else {
			resourceLink.setTool(tool);
			resourceLink.setOutcomeServiceUrl(outcomeSrvUrl);
			if (resourceLink.getToolKey() == null
					|| resourceLink.getToolKey() != null && toolKey.getSid() != resourceLink.getToolKey().getSid()) {
				// Update
				resourceLink.setToolKey(toolKey);
				ToolResourceLinkDao.update(resourceLink);
				logger.info("Resource Link updated: {}", resourceLink.getResourceId());
			} else {
				resourceLink.setToolKey(toolKey);
			}
		}

		// user data
		LtiUser user = new LtiUser();
		user.setConsumer(consumer);
		user.setUserId(sessionUser);
		user.setNameGiven(sessionUser);
		user.setNameFamily(sessionUser);
		user.setNameFull(sessionUser);
		user.setEmail("");
		// Sanitize sourceId
		user.setSourceId(sessionUser);

		// Check if exist
		final LtiUser auxUser = ToolConsumerUserDao.getById(consumer.getSid(), user.getUserId());
		if (auxUser != null) {
			user = auxUser;
		} else // if not exist, create
		if (!ToolConsumerUserDao.create(user)) {
			logger.error("I can't continue because I can't create the LTI user");
			return valid;
		} else {
			logger.info("New LTI user created: {}", user.getSourceId());
		}

		// resource_user data
		// Check if exist
		ltiResourceUser = ToolResourceUserDao.getById(resourceLink.getSid(), user.getSid());
		if (ltiResourceUser != null) {
			// complete fields
			ltiResourceUser.setResourceLink(resourceLink);
			ltiResourceUser.setUser(user);
		} else {
			final ResourceUser auxResourceUser = new ResourceUser();
			auxResourceUser.setResourceLink(resourceLink);
			auxResourceUser.setUser(user);
			if (outcomeSrvUrl != null && learner) {
				auxResourceUser.setResultSourceId("0");
			} else {
				auxResourceUser.setResultSourceId(null);
			}
			// if not exist, create
			if (!ToolResourceUserDao.create(auxResourceUser)) {
				logger.error("I can't continue because I can't create the resource user");
				return valid;
			} else {
				logger.info("New resource user created.");
				ltiResourceUser = auxResourceUser;
			}
		}

		if (tool.isOutcome()) {
			//Test outcome
			resourceLink.setCustomProperty("custom_nocal", "false");
		}
		// Session data
		// - roles, outcome, presentation
		outcomeAllowed = !isWithoutOutcome();
		presentationDocumentTarget = "top";
		frameMode = presentationDocumentTarget != null && presentationDocumentTarget.endsWith("frame");
		presentationLocale = null;
		sessionUserId = sessionUser;
		this.launchId = launchId;
		isLearner = learner;
		isInstructor = instructor;

		valid = true;
		return valid;
	}

}
