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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
 * Servlet implementation class to download all tool files.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/editor/downloadtool")
public class DownloadToolServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DownloadToolServlet.class);
	/**
	 * Buffer size to send response.
	 */
	private static final int DEFAULT_BUFFER_SIZE = 10240;

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

		try {
			Tool tool;
			final String toolname = request.getParameter("toolname");
			if (toolname != null) {
				tool = ToolDao.get(toolname);
			} else {
				tool = null;
			}
			if (tool != null) {
				final String path = tool.getToolPath();

				if (ToolDao.getToolUserType(sessionUser, tool) <= MgmtUserType.EDITOR.getCode()) {
					session.setAttribute("lasttool", toolname);
					final String filename = URLEncoder.encode(toolname, StandardCharsets.UTF_8) + ".zip";
					response.reset();
					response.setBufferSize(DEFAULT_BUFFER_SIZE);
					response.setContentType("application/zip");
					response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
					final File file = new File(path);
					if (!file.exists() || !file.isDirectory()) {
						logger.error("Folder does not exists: {}", file.getAbsolutePath());
					} else {
						try (ZipOutputStream output = new ZipOutputStream(response.getOutputStream());) {
							zipDir(path, output, path.length());
						}
					}
				} else {
					forwardError(request, response);
				}
			}

		} catch (final IOException e) {
			logger.error("Error downloading.", e);
		}

	}

	/**
	 * Forwards a request to error page, capturing exceptions.
	 *
	 * @param request  the HTTP request
	 * @param response the HTTP response
	 */
	private void forwardError(HttpServletRequest request, HttpServletResponse response) {
		try {
			request.getRequestDispatcher("../errorlogin.html").forward(request, response);
		} catch (final Exception e) {
			logger.error("Error forwarding", e);
		}
	}

	/**
	 * ZIP a directory.
	 *
	 * @param dir2zip        directory name
	 * @param zos            output stream
	 * @param prefixToRemove prefix to remove from file names
	 */
	private void zipDir(String dir2zip, ZipOutputStream zos, int prefixToRemove) {
		try {
			final File zipDir = new File(dir2zip);
			// get a listing of the directory content
			final String[] dirList = zipDir.list();
			if (dirList != null) {
				final byte[] readBuffer = new byte[DEFAULT_BUFFER_SIZE];
				int bytesIn = 0;
				// loop through dirList, and zip the files
				for (final String element : dirList) {
					final File f = new File(zipDir, element);
					if (f.isDirectory()) {
						// if the File object is a directory, call this
						// function again to add its content recursively
						final String filePath = f.getPath();
						zipDir(filePath, zos, prefixToRemove);
					} else {
						// if we reached here, the File object f was not a directory
						// create a FileInputStream on top of f
						try (FileInputStream fis = new FileInputStream(f);) {
							// create a new zip entry
							final ZipEntry anEntry = new ZipEntry(f.getPath().substring(prefixToRemove + 1));
							// place the zip entry in the ZipOutputStream object
							zos.putNextEntry(anEntry);
							// now write the content of the file to the ZipOutputStream
							while ((bytesIn = fis.read(readBuffer)) != -1) {
								zos.write(readBuffer, 0, bytesIn);
							}
						}
					}
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
