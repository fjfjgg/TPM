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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import es.us.dit.lti.persistence.SettingsDao;
import es.us.dit.lti.persistence.ToolConsumerDao;
import es.us.dit.lti.persistence.ToolConsumerUserDao;
import es.us.dit.lti.persistence.ToolContextDao;
import es.us.dit.lti.persistence.ToolNonceDao;
import es.us.dit.lti.persistence.ToolResourceLinkDao;
import es.us.dit.lti.persistence.ToolResourceUserDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class to do maintenance actions attempts, with same
 * tool key.
 */
@WebServlet({ "/super/optimize", "/super/getunused", "/super/deleteunusedusers", "/super/deleteunusedresourceusers",
		"/super/deleteunusedresourcelinks", "/super/deleteunusedcontexts", "/super/deleteunusedconsumers" })
public class MaintenanceServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(MaintenanceServlet.class);

	/**
	 * Processes GET request for get current unused element statistics.
	 * 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		PrintWriter out;
		try {
			out = response.getWriter();
		} catch (final IOException e) {
			logger.error("IO Error.", e);
			return;
		}
		if (request.getServletPath().equals("/super/getunused")) {
			response.setContentType("application/json");
			UnusedInfo info = new UnusedInfo();
			info.setConsumers(ToolConsumerDao.getUnused().size());
			info.setContexts(ToolContextDao.getUnused().size());
			info.setResourceLinks(ToolResourceLinkDao.getUnused().size());
			info.setResourceUsers(ToolResourceUserDao.getUnused().size());
			info.setUsers(ToolConsumerUserDao.getUnused().size());
			out.append(new Gson().toJson(info));
		} else {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			try {
				request.getRequestDispatcher("/errorlogin.html").include(request, response);
			} catch (ServletException | IOException e) {
				logger.error("IO Error.", e);
			}
		}
	}

	/**
	 * Processes POST request for perform maintenace actions.
	 *
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (final UnsupportedEncodingException e1) {
			// never
			logger.error("UTF-8", e1);
			return;
		}
		PrintWriter out;
		try {
			out = response.getWriter();
		} catch (final IOException e) {
			logger.error("IO Error.", e);
			return;
		}
		// Only for instructors
		switch (request.getServletPath()) {
		case "/super/optimize":
			out.print(SettingsDao.optimizeDb());
			break;
		case "/super/deleteunusedusers":
			out.print(ToolConsumerUserDao.deleteUnused());
			break;
		case "/super/deleteunusedresourceusers":
			out.print(ToolResourceUserDao.deleteUnused());
			break;
		case "/super/deleteunusedresourcelinks":
			out.print(ToolResourceLinkDao.deleteUnused());
			break;
		case "/super/deleteunusedcontexts":
			out.print(ToolContextDao.deleteUnused());
			break;
		case "/super/deleteunusedconsumers":
			ToolNonceDao.deleteAll(); //Nonce depends on consumer
			out.print(ToolConsumerDao.deleteUnused());
			break;
		default:
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			try {
				request.getRequestDispatcher("/errorlogin.html").include(request, response);
			} catch (ServletException | IOException e) {
				logger.error("IO Error.", e);
			}
			break;
		}
	}
}
