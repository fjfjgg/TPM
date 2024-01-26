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
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import es.us.dit.lti.ToolSession;
import es.us.dit.lti.entity.Consumer;
import es.us.dit.lti.entity.LtiUser;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.persistence.ToolDao;

/**
 * Servlet to list LTI user that had attempts with same tool key and same username. 
 * 
 * <p>Not used.
 */
//Disabled @WebServlet("/instructor/user/*")
@WebServlet("/super/instructor/user/*")
public class InfoLtiUserServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(InfoLtiUserServlet.class);

	/**
	 * GSON strategy for excluding details.
	 */
	private static final ExclusionStrategy strategyUsers = new ExclusionStrategy() {
		/**
		 * Excludes serial ID, updated, created and cssPath fields.
		 */
		@Override
		public boolean shouldSkipField(FieldAttributes field) {
			boolean res = false;
			if (field.getName().equals("sid") || field.getName().equals("updated")
					|| field.getName().equals("created")) {
				res = true;
			}
			if (field.getDeclaringClass() == Consumer.class && field.getName().equals("cssPath")) {
				res = true;
			}
			return res;
		}

		/**
		 * Do not exclude full classes.
		 */
		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
	};

	/**
	 * Processes GET request.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final ToolSession ts = (ToolSession) request.getSession().getAttribute(ToolSession.class.getName());
		final Tool tool = ts.getTool();
		if (tool != null) {
			String userId = ts.getSessionUserId();
			// Get userId from pathinfo
			final String[] parts = request.getPathInfo().split("/");
			if (parts.length == 2) {
				userId = parts[1];
			}
			final List<LtiUser> ltiUsers = ToolDao.getLtiUserBySourceId(tool, userId);
			response.setContentType("application/json");
			try {
				if (ltiUsers.isEmpty()) {
					response.getWriter().append("[]");
				} else {
					// Serialize
					final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(strategyUsers).create();
					response.getWriter().append(gson.toJson(ltiUsers));
				}
			} catch (final IOException e) {
				logger.error("IO Error.", e);
			}
		}
	}

}
