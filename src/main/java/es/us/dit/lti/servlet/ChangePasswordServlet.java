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
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.persistence.MgmtUserDao;

/**
 * Servlet implementation class for changing management user passwords.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/user/change")
public class ChangePasswordServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ChangePasswordServlet.class);


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
		final String oldpassword = request.getParameter("oldpassword");
		final String newpassword = request.getParameter("password");
		if (sessionUser != null && oldpassword != null && newpassword != null) {
			String res;
			// require a decent password, not any.
			if (MgmtUser.isValidPassword(newpassword)) {
				// Check current password
				sessionUser.setPassword(oldpassword);
				if (!MgmtUserDao.login(sessionUser)) {
					res = "T_ERROR_CLAVE_ACTUAL";
				} else if (MgmtUserDao.changePassword(sessionUser, newpassword)) {
					res = "T_CLAVE_CAMBIADA";
				} else {
					res = "T_ERROR_CAMBIO_CLAVE";
				}
			} else {
				res = "T_ERROR_REQUISITOS_CLAVE";
			}
			session.setAttribute(Settings.PENDING_MSG_ATTRIB, res);
		}
		try {
			request.getRequestDispatcher("menu.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			// ignore
			logger.error("IO error.", e);
		}
	}

}
