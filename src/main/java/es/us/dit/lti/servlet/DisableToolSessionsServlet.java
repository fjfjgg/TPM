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
import java.io.UnsupportedEncodingException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.MgmtUserType;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.persistence.ToolDao;
import es.us.dit.lti.persistence.ToolKeyDao;

/**
 * Servlet implementation to disable all current sessions of a specific tool.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/editor/disabletoolsessions")
public class DisableToolSessionsServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DisableToolSessionsServlet.class);

	/**
	 * Processes POST request.
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
		}
		final HttpSession session = request.getSession(true);
		final MgmtUser sessionUser = (MgmtUser) session.getAttribute("mgmtUser");
		final String toolTitle = request.getParameter("toolname");

		final Tool tool = ToolDao.get(toolTitle);
		if (toolTitle != null && tool != null
				&& ToolDao.getToolUserType(sessionUser, tool) <= MgmtUserType.EDITOR.getCode()) {
			for (final String key : ToolDao.getAllKeys(tool)) {
				final Tool cached = ToolKeyDao.getCache(key);
				if (cached != null) {
					cached.setEnabled(false);
					// Delete the cache so that the following sessions are correct.
					ToolKeyDao.deleteCache(key);
				}
			}
			session.setAttribute(Settings.PENDING_MSG_ATTRIB, "T_HERRAMIENTA_DESHABILITADA_SESIONES");
		}

		try {
			request.getRequestDispatcher("../user/tools.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			// ignore
			logger.error("IO error.", e);
		}

	}

}
