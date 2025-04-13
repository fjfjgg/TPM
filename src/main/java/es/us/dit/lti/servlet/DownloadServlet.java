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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.persistence.ToolDao;

/**
 * Servlet to download tool files.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet({ "/editor/download" })
public class DownloadServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DownloadServlet.class);
	/**
	 * Buffer size to send response.
	 */
	private static final int DEFAULT_BUFFER_SIZE = 10240;

	/**
	 * Processes GET request.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (final UnsupportedEncodingException e1) {
			// never
			logger.error("UTF-8", e1);
		}
		final HttpSession session = request.getSession(true);
		final MgmtUser sessionUser = (MgmtUser) session.getAttribute("mgmtUser");

		if (sessionUser != null) {
			try {
				final String toolname = request.getParameter("toolname");
				final String type = request.getParameter("type");
				final Tool tool = ToolDao.get(toolname);
				if (ToolDao.getToolUserType(sessionUser, tool) <= MgmtUserType.EDITOR.getCode()) {
					String filename;
					String path;
					if (tool != null && type != null) {
						if (type.equals("corrector")) {
							path = tool.getCorrectorPath();
							filename = toolname + ".run";
						} else if (type.equals("description")) {
							path = tool.getDescriptionPath();
							filename = toolname + ".html";
						} else {
							path = tool.getExtraZipPath();
							filename = toolname + "_extra.zip";
						}
						doDownloadFile(filename, path, response);
					}
				} else {
					request.getRequestDispatcher("../errorlogin.html").forward(request, response);
				}
			} catch (final Exception e) {
				logger.error("Error downloading.", e);
			}
		}
	}

	/**
	 * Sends the file.
	 *
	 * @param fileName file name
	 * @param filePath file path (folder)
	 * @param response HTTP response
	 */
	private void doDownloadFile(String fileName, String filePath, HttpServletResponse response) {
		final File file = new File(filePath);
		if (!file.exists()) {
			logger.error("Folder does not exists: {}", file.getAbsolutePath());
		} else {
			response.reset();
			response.setBufferSize(DEFAULT_BUFFER_SIZE);

			final String fileType = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
			if (fileType.trim().equalsIgnoreCase("txt")) {
				response.setContentType("text/plain");
			} else if (fileType.trim().equalsIgnoreCase("pdf")) {
				response.setContentType("application/pdf");
			} else {
				response.setContentType("application/octet-stream");
			}

			response.setHeader("Content-Length", String.valueOf(file.length()));
			response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");

			try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
					BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream(),
							DEFAULT_BUFFER_SIZE)) {
				final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				int length;
				while ((length = input.read(buffer)) > 0) {
					output.write(buffer, 0, length);
				}
			} catch (final IOException e) {
				logger.error("Error sending file {}: {}", file.getPath(), e);
			}
		}
	}

}
