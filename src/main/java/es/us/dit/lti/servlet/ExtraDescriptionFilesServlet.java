/*
    This file is part of Tool Provider Manager - Manager of LTI Tool Providers
    for learning platforms.
    Copyright (C) 2024  Francisco José Fernández Jiménez.

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.ToolSession;
import es.us.dit.lti.entity.Tool;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet that serves static content from tool extra folder.
 * 
 * @author Francisco José Fernández Jiménez
 * @since 2024-11-04
 * @version 1.0
 * 
 */
@WebServlet({ "/learner/extra/*", "/instructor/extra/*" })
public class ExtraDescriptionFilesServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ExtraDescriptionFilesServlet.class);
	/**
	 * Buffer size to send response.
	 */
	private static final int DEFAULT_BUFFER_SIZE = 10240;
	
	/**
	 * Get an extra file for description.
	 * 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Check if user is authorized
		String extraFolderPath = getExtraFolderPath(request);
		if (extraFolderPath != null) {
			File file = new File(extraFolderPath + request.getPathInfo());
			try {
				if (!file.isFile() || !file.canRead()) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
				} else {

					// Start
					response.reset();
					response.setBufferSize(DEFAULT_BUFFER_SIZE);

					// Send content type and length
					String mimeType = getServletContext().getMimeType(file.getPath());
					if (mimeType == null) {
						mimeType = "application/octet-stream";
					}
					response.setContentType(mimeType);
					response.setHeader("Content-Length", String.valueOf(file.length()));

					// Send file
					try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file),
							DEFAULT_BUFFER_SIZE);
							BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream(),
									DEFAULT_BUFFER_SIZE);) {
						byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
						int length;
						while ((length = input.read(buffer)) > 0) {
							output.write(buffer, 0, length);
						}
					}
				}
			} catch (IOException e) {
				logger.error("Error sending file {}: {}", file.getPath(), e);
			}
		} else {
			try {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
			} catch (IOException e) {
				logger.error("Error sending error");
			}
		}
	}

	/**
	 * Get extra folder path.
	 * 
	 * @param request HTTP request
	 * @return extra folder path if authorized, or null if not.
	 */
	private String getExtraFolderPath(HttpServletRequest request) {
		String extraFolder = null;
		HttpSession session = request.getSession();
		ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
		Boolean extraFileAuthorized = (Boolean) session.getAttribute("extraFileAuthorized");
		if (ts != null && extraFileAuthorized != null && extraFileAuthorized) {
			Tool tool = ts.getTool();
			if (tool != null && (!ts.isLearner() || tool.isEnabled() && ts.getToolKey().isEnabled())) {
				extraFolder = tool.getToolExtraPath();
			} else {
				session.removeAttribute("extraFileAuthorized");
			}
		}

		return extraFolder;
	}

}
