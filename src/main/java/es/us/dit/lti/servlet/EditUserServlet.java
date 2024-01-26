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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.MgmtUserType;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.persistence.MgmtUserDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet for updating a management user.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/super/edituser")
public class EditUserServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(EditUserServlet.class);

	/**
	 * Receives management user and updates it, then forwards to users.jsp.
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String typeString = request.getParameter("type");
		final String username = request.getParameter("username");
		final String email = request.getParameter("email");

		if (username != null && typeString != null) {
			final MgmtUser user = MgmtUserDao.get(username);
			String res;
			if (user != null) {
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
				boolean updated;
				if (MgmtUser.isValidEmail(email)) {
					updated = MgmtUserDao.update(user);
				} else {
					updated = false;
				}
				if (updated) {
					res = "T_USUARIO_MODIFICADO";
					final String newPassword = request.getParameter("password");
					if (newPassword != null && !newPassword.isBlank()) {
						// Update password
						if (MgmtUser.isValidPassword(newPassword)) {
							user.setPassword(newPassword);
							if (MgmtUserDao.changePassword(user, newPassword)) {
								res = "T_CLAVE_CAMBIADA";
							} else {
								res = "T_ERROR_CAMBIO_CLAVE";
							}
						} else {
							res = "T_ERROR_REQUISITOS_CLAVE";
						}
					}
				} else {
					res = "T_ERROR_MODIFICAR_USUARIO";
				}
			} else {
				res = "T_ERROR_MODIFICAR_USUARIO";
			}
			
			request.getSession().setAttribute(Settings.PENDING_MSG_ATTRIB, res);
		}
		try {
			request.getRequestDispatcher("users.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			// can not do anything
			logger.error("Error forwarding.", e);
		}

	}

}
