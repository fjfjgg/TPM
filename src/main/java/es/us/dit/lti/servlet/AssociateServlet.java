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
import es.us.dit.lti.persistence.MgmtUserDao;
import es.us.dit.lti.persistence.ToolDao;

/**
 * Servlet for associating a user to a tool.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/admin/associate")
public class AssociateServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(AssociateServlet.class);

	/**
	 * Processes POST request.
	 *
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final HttpSession session = request.getSession(true);
		final MgmtUser sessionUser = (MgmtUser) session.getAttribute("mgmtUser");
		final String[] usernames = request.getParameterValues("username");
		final String toolname = (String) session.getAttribute("lasttool");
		final String typeString = request.getParameter("type");
		MgmtUserType type;
		try {
			type = MgmtUserType.valueOf(typeString);
		} catch (final Exception e) {
			type = MgmtUserType.UNKNOWN;
		}

		if (type == MgmtUserType.UNKNOWN || type.getCode() <= sessionUser.getType().getCode()) {
			session.setAttribute(Settings.PENDING_MSG_ATTRIB, "T_ERROR_ASOCIAR");
		} else if (usernames != null && toolname != null) {
			final Tool tool = ToolDao.get(toolname);
			if (ToolDao.getToolUserType(sessionUser, tool) == MgmtUserType.ADMIN.getCode()) {
				boolean result = true;
				for (final String username : usernames) {
					final MgmtUser user = MgmtUserDao.get(username);
					result = ToolDao.associateUser(user, tool, type);
					if (!result) {
						break;
					}
				}
				if (result) {
					session.setAttribute(Settings.PENDING_MSG_ATTRIB, "T_USUARIO_ASOCIADO");
				} else {
					session.setAttribute(Settings.PENDING_MSG_ATTRIB, "T_ERROR_ASOCIAR");
				}
			} else {
				logger.error("Invalid admin");
			}
		}
		try {
			request.getRequestDispatcher("../user/tools.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			logger.error("Error forwarding", e);
		}
	}
}
