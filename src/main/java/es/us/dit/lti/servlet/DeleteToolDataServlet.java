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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.MgmtUserType;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.persistence.ToolDao;

/**
 * Servlet to delete tool data (all attempts).
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/admin/deletetooldata")
public class DeleteToolDataServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DeleteToolDataServlet.class);

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
		final String toolname = request.getParameter("toolname");

		boolean del;
		final Tool tool = ToolDao.get(toolname);
		final int userType = ToolDao.getToolUserType(sessionUser, tool);
		if (tool != null && (userType == MgmtUserType.EDITOR.getCode() || userType == MgmtUserType.ADMIN.getCode())) {
			// delete attempts from db
			if (ToolDao.deleteToolData(tool)) {
				final File toolDataDir = new File(tool.getToolDataPath());
				try {
					FileUtils.deleteDirectory(toolDataDir);
					del = true;
				} catch (final IOException e) {
					del = false;
				}
				if (!toolDataDir.exists() && !toolDataDir.mkdir()) {
					logger.error("mkdir {}", toolDataDir.getAbsolutePath());
				}
				if (del) {
					session.setAttribute(Settings.PENDING_MSG_ATTRIB, "T_DATOS_BORRADOS");
				} else {
					session.setAttribute(Settings.PENDING_MSG_ATTRIB, "T_ERROR_BORRAR_DATOS");
				}
			} else {
				session.setAttribute(Settings.PENDING_MSG_ATTRIB, "T_ERROR_BORRAR_DATOS");
			}
			session.setAttribute("lasttool", toolname);
		}
		try {
			request.getRequestDispatcher("../user/tools.jsp").forward(request, response);
		} catch (ServletException | IOException e) {
			// ignore
			logger.error("IO error.", e);
		}

	}

}
