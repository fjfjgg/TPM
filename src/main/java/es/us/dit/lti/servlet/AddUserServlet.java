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
import java.security.spec.InvalidKeySpecException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.MgmtUserType;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.persistence.MgmtUserDao;

/**
 * Servlet for creating a new management user.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/super/adduser")
public class AddUserServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(AddUserServlet.class);

	/**
	 * Receives new management user and creates it, then forwards to users.jsp.
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String email = request.getParameter("email");
		final String typeString = request.getParameter("type");
		if (request.getParameter("username") != null && request.getParameter("password") != null
				&& typeString != null) {
			final MgmtUser user = new MgmtUser();
			user.setUsername(Settings.sanitizeString(request.getParameter("username")));
			user.setPassword(request.getParameter("password"));
			if (typeString.equals("admin")) {
				user.setType(MgmtUserType.ADMIN);
			} else if (typeString.equals("editor")) {
				user.setType(MgmtUserType.EDITOR);
			} else {
				user.setType(MgmtUserType.TESTER);
			}
			user.setLocal("true".equals(request.getParameter("is_local")));
			user.setExecutionRestrictions(request.getParameter("exe_restrictions"));
			user.setNameFull(request.getParameter("fullname"));
			user.setEmail(email);
			
			boolean created;
			try {
				if (MgmtUser.isValidEmail(email)) {
					created = MgmtUserDao.add(user);
				} else {
					created = false;
				}
			} catch (final InvalidKeySpecException e) {
				created = false;
			}
			if (created) {
				request.getSession().setAttribute(Settings.PENDING_MSG_ATTRIB, "T_USUARIO_CREADO");
			} else {
				request.getSession().setAttribute(Settings.PENDING_MSG_ATTRIB, "T_ERROR_CREAR_USUARIO");
			}

		}
		try {
			request.getRequestDispatcher("users.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			// can not do anything
			logger.error("Error forwarding.", e);
		}

	}

}
