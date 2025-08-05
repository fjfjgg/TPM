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

package es.us.dit.lti.servlet;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import es.us.dit.lti.ToolSession;
import es.us.dit.lti.entity.Attempt;
import es.us.dit.lti.entity.Consumer;
import es.us.dit.lti.entity.Context;
import es.us.dit.lti.entity.LtiUser;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.ResourceUser;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.persistence.ToolAttemptDao;
import es.us.dit.lti.persistence.ToolConsumerDao;
import es.us.dit.lti.persistence.ToolConsumerUserDao;
import es.us.dit.lti.persistence.ToolContextDao;
import es.us.dit.lti.persistence.ToolResourceLinkDao;
import es.us.dit.lti.persistence.ToolResourceUserDao;

/**
 * Servlet for getting details of an assessment attempt from a secured serial
 * ID.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/instructor/attemptdetail/*")
public class AttemptDetailServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(AttemptDetailServlet.class);

	/**
	 * List of field names that must not be serialized.
	 */
	private static final Set<String> HIDDEN_FIELDS = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList("sid", "updated", "created", "instant", "cssPath",
					"tool", "toolKey", "customProperties", "enabled", "storageType", "outcomeServiceUrl")));

	/**
	 * GSON strategy for excluding details.
	 */
	private static final ExclusionStrategy strategyAttempt = new ExclusionStrategy() {
		/**
		 * Excludes HIDDEN_FIELDS fields.
		 */
		@Override
		public boolean shouldSkipField(FieldAttributes field) {
			boolean res = false;
			if (HIDDEN_FIELDS.contains(field.getName())) {
				res = true;
			}
			return res;
		}

		/**
		 * Excludes classes: Instant.
		 */
		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return clazz == Instant.class;
		}
	};

	/**
	 * Processes request and returns attempt details in JSON.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Get tool session
		final HttpSession session = request.getSession();
		final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
		final Tool tool = ts.getTool();
		if (tool != null) {
			String cipheredSid = null;
			// Get secured serial ID from pathinfo
			final String[] parts = request.getPathInfo().split("/");
			if (parts.length == 2) {
				cipheredSid = parts[1];
			}
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			try {
				// Attempt info
				final Attempt attempt = ToolAttemptDao.getBySecuredSid(cipheredSid, ts.getToolKey());
				if (attempt != null) {
					// Complete all fields
					int auxSid = attempt.getResourceUser().getSid();
					ResourceUser ru = ToolResourceUserDao.getBySid(auxSid);
					attempt.setResourceUser(ru);
					// Mask resultSourceId
					String resultSourceId = ru.getResultSourceId();
					if (resultSourceId == null || resultSourceId.isEmpty()) {
						ru.setResultSourceId(null);
					} else {
						ru.setResultSourceId("");
					}
					auxSid = ru.getResourceLink().getSid();
					if (ts.getResourceLink().getSid() != auxSid) {
						final ResourceLink rl = ToolResourceLinkDao.getBySid(auxSid);
						ru.setResourceLink(rl);
						auxSid = rl.getContext().getSid();
						if (ts.getContext().getSid() != auxSid) {
							final Context ct = ToolContextDao.getBySid(auxSid);
							rl.setContext(ct);
						}
					} else {
						ru.setResourceLink(null);
					}
					auxSid = ru.getUser().getSid();
					LtiUser u = ToolConsumerUserDao.getBySid(auxSid);
					ru.setUser(u);
					auxSid = u.getConsumer().getSid();
					Consumer c = ToolConsumerDao.getBySid(auxSid);
					u.setConsumer(c);

					if (attempt.getOriginalResourceUser().getSid() == attempt.getResourceUser().getSid()) {
						attempt.setOriginalResourceUser(null);
					} else {
						auxSid = attempt.getOriginalResourceUser().getSid();
						ru = ToolResourceUserDao.getBySid(auxSid);
						attempt.setOriginalResourceUser(ru);
						// Mask resultSourceId
						resultSourceId = ru.getResultSourceId();
						if (resultSourceId == null || resultSourceId.isEmpty()) {
							ru.setResultSourceId(null);
						} else {
							ru.setResultSourceId("");
						}
						auxSid = ru.getResourceLink().getSid();
						if (ts.getResourceLink().getSid() != auxSid) {
							final ResourceLink rl = ToolResourceLinkDao.getBySid(auxSid);
							ru.setResourceLink(rl);
							auxSid = rl.getContext().getSid();
							if (ts.getContext().getSid() != auxSid) {
								final Context ct = ToolContextDao.getBySid(auxSid);
								rl.setContext(ct);
							}
						} else {
							ru.setResourceLink(null);
						}
						auxSid = ru.getUser().getSid();
						u = ToolConsumerUserDao.getBySid(auxSid);
						ru.setUser(u);
						auxSid = u.getConsumer().getSid();
						c = ToolConsumerDao.getBySid(auxSid);
						u.setConsumer(c);
					}

					// Serialize
					final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(strategyAttempt).create();
					response.getWriter().append(gson.toJson(attempt));
				} else {
					response.getWriter().append("[]");
				}

			} catch (final IOException e) {
				// ignore
				logger.error("IO error.", e);
			}
		}
	}

}
