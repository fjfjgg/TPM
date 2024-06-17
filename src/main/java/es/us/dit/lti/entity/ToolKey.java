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

package es.us.dit.lti.entity;

import java.util.Objects;

/**
 * Tool key (LTI consumer key).
 *
 * <p>Specified in <a href="https://www.imsglobal.org/specs/ltiv1p1">
 * https://www.imsglobal.org/specs/ltiv1p1</a>
 *
 * @author Francisco José Fernández Jiménez
 */
public class ToolKey extends UpdateRecordEntity {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 3918103925278787156L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * Associated tool.
	 */
	private Tool tool;
	/**
	 * Consumer to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 */
	private Consumer consumer;
	/**
	 * Context to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 */
	private Context context;
	/**
	 * Resource link to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 */
	private ResourceLink resourceLink;
	/**
	 * Consumer key.
	 */
	private String key;
	/**
	 * Secret to authenticate request.
	 */
	private String secret;
	/**
	 * Regex of remote IP address to which the tool key is restricted.
	 * 
	 * <p>If it is null, there is no constraint. 
	 */
	private String address;
	/**
	 * If enabled, requests with this tool key are allowed.
	 */
	private boolean enabled;

	/**
	 * Compares the passed consumer, context, and resource link serial identifiers
	 * with those of the key (in that order).
	 *
	 * <p>A null value is considered 0.
	 *
	 * @param c  other consumer
	 * @param ct other context
	 * @param rl other resource link
	 * @return 0, less than 0, or greater than 0 depending on whether the tool key
	 *         values are equal to, less than, or greater than those passed as
	 *         parameters, respectively.
	 */
	public int compareResource(Consumer c, Context ct, ResourceLink rl) {
		final int cSid = consumer == null ? 0 : consumer.getSid();
		final int ctSid = context == null ? 0 : context.getSid();
		final int rlSid = resourceLink == null ? 0 : resourceLink.getSid();
		final int ocSid = c == null ? 0 : c.getSid();
		final int octSid = ct == null ? 0 : ct.getSid();
		final int orlSid = rl == null ? 0 : rl.getSid();
		int res;
		if (cSid == ocSid && ctSid == octSid && rlSid == orlSid) {
			res = 0;
		} else {
			res = cSid - ocSid;
			if (res == 0) { // same consumer
				res = ctSid - octSid;
				if (res == 0) { // same context
					res = rlSid - orlSid;
				}
			}
		}
		return res;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ToolKey other = (ToolKey) obj;
		return Objects.equals(sid, other.sid);
	}

	/**
	 * Gets the associated tool.
	 *
	 * @return the tool
	 */
	public Tool getTool() {
		return tool;
	}

	/**
	 * Sets the associated tool.
	 *
	 * @param tool the tool to set
	 */
	public void setTool(Tool tool) {
		this.tool = tool;
	}

	/**
	 * Gets the consumer to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 *
	 * @return the consumer
	 */
	public Consumer getConsumer() {
		return consumer;
	}

	/**
	 * Sets the consumer to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 *
	 * @param consumer the consumer to set
	 */
	public void setConsumer(Consumer consumer) {
		this.consumer = consumer;
	}

	/**
	 * Gets the context to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 *
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * Sets the context to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 *
	 * @param context the context to set
	 */
	public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * Gets the resource link to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 *
	 * @return the resourceLink
	 */
	public ResourceLink getResourceLink() {
		return resourceLink;
	}

	/**
	 * Sets the consumer to which the tool key is restricted.
	 *
	 * <p>If it is null, there is no constraint.
	 *
	 * @param resourceLink the resourceLink to set
	 */
	public void setResourceLink(ResourceLink resourceLink) {
		this.resourceLink = resourceLink;
	}

	/**
	 * Gets the consumer key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets the consumer key.
	 *
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Gets the secret.
	 *
	 * @return the secret
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * Sets the secret.
	 *
	 * @param secret the secret to set
	 */
	public void setSecret(String secret) {
		this.secret = secret;
	}

	/**
	 * Gets the address regex.
	 * 
	 * @return the address regex
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Sets the address regex.
	 * 
	 * @param address the address regex to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * Gets if it is enabled.
	 *
	 * <p>If enabled, requests with this tool key are allowed.
	 *
	 * @return true if it is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Sets if it is enabled.
	 *
	 * <p>If enabled, requests with this tool key are allowed.
	 *
	 * @param enabled true if it is enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
