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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.ToolSession;
import es.us.dit.lti.entity.Attempt;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.persistence.ToolAttemptDao;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet implementation class to manage attempts.
 * 
 * <p>It provides the following functions:
 * <ul>
 * <li>Get the delivery file of an attempt.
 * <li>Get the result/output of an attempt.
 * <li>Delete an attempt.
 * <li>Download a ZIP with a list of attempts.
 * </ul>
 * 
 * @author Francisco José Fernández Jiménez
 */
@WebServlet({ "/learner/attempt/*", "/instructor/attempt/*", "/learner/downloadattempts",
		"/instructor/downloadattempts" })
public class AttemptManagerServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(AttemptManagerServlet.class);
	/**
	 * Path segment that indicates that the request references a result/output file.
	 */
	private static final String OUTPUT_SEGMENT = "output";
	/**
	 * Buffer size to send response.
	 */
	private static final int DEFAULT_BUFFER_SIZE = 8192;

	
	private final Map<String, String> mimeToExt = new HashMap<>();
	
	/**
	 * Init mimeToExtensionMap.
	 * 
	 * @see HttpServlet#init(ServletConfig config)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		mimeToExt.put("text/html", "html");
		mimeToExt.put("text/plain", "txt");
		mimeToExt.put("image/jpeg", "jpg");
		mimeToExt.put("text/csv", "csv");
		mimeToExt.put("application/pdf", "pdf");
		mimeToExt.put("image/svg+xml", "svg");
		mimeToExt.put("image/png", "png");
		mimeToExt.put("image/gif", "gif");
		mimeToExt.put("image/webp", "webp");
		mimeToExt.put("application/xml", "xml");
		mimeToExt.put("application/json", "json");
	}
	
	
	/**
	 * Gets the file or result/output of an attempt.
	 * 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final HttpSession session = request.getSession();
		final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
		final Tool tool = ts.getTool();
		final String userId = ts.getSessionUserId();
		// Get ciphered serial ID and file/output from pathinfo
		final String[] parts = request.getPathInfo().split("/");
		boolean output = false;
		String uid = null;
		String cipheredSid = null;
		if (parts.length == 3) {
			uid = parts[1];
			cipheredSid = parts[2];
		} else if (parts.length == 4 && parts[2].equals(OUTPUT_SEGMENT)) {
			uid = parts[1];
			output = true;
			cipheredSid = parts[3];
		}
		if (uid != null && cipheredSid != null && tool != null) {
			uid = URLDecoder.decode(uid, StandardCharsets.UTF_8);
			final Attempt attempt = ToolAttemptDao.getBySecuredSid(cipheredSid, ts.getToolKey());
			if (attempt != null && (uid.equals(userId)
					&& ts.getLtiResourceUser().getUser().getSid() == attempt.getResourceUser().getUser().getSid()
					|| request.getServletPath().equals("/instructor/attempt")
							&& tool.getToolUiConfig().isManageAttempts())) {
				attempt.getResourceUser().getResourceLink().setTool(tool);

				final File f = getFile(attempt, output);
				if (f != null) {
					if (output) {
						response.setContentType(tool.getToolUiConfig().getOutputMimeType()); 
					} else {
						response.setContentType(request.getServletContext().getMimeType(f.getName()));
						response.setHeader("Content-Disposition", "inline; filename=\"" + attempt.getFileName() + "\"");
					}
					try (FileInputStream fis = new FileInputStream(f)) {
						fis.transferTo(response.getOutputStream());
					} catch (final IOException e) {
						logger.error("Error sending file", e);
					}
					// If file should be deleted, do it
					if (output && !attempt.isOutputSaved() && !f.delete()) {
						logger.error("Error deleting file");
					}
				}
			} else {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			try {
				request.getRequestDispatcher("/error404.html").include(request, response);
			} catch (ServletException | IOException e) {
				logger.error("Error including page.");
			}
		}
	}

	/**
	 * Gets the file or result/output file of an attempt.
	 *
	 * @param attempt the attempt
	 * @param output true if result/output if requested
	 * @return the file if successfull or null
	 */
	private File getFile(Attempt attempt, boolean output) {
		File f;
		try {
			File aux = new File(attempt.getUserFilePath());
			if (output) {
				aux = new File(attempt.getCorrectorResultPathFromFile(aux.getCanonicalPath()));
			}
			if (aux.exists() && aux.isFile()) {
				f = aux;
			} else {
				f = null;
			}
		} catch (final IOException e) {
			// ignore
			f = null;
		}
		return f;
	}

	/**
	 * Deletes an attempt.
	 *
	 * @see HttpServlet#doDelete(HttpServletRequest, HttpServletResponse)
	 */
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
		try {
			final HttpSession session = request.getSession();
			final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
			final Tool tool = ts.getTool();
			if (Objects.equals(request.getHeader("X-launch-id"), ts.getLaunchId())
					&& request.getServletPath().equals("/instructor/attempt") && tool != null
					&& tool.getToolUiConfig().isManageAttempts()) {
				// Get ciphered serial ID from pathinfo
				final String[] parts = request.getPathInfo().split("/");
				String uid = null;
				String cipheredSid = null;
				if (parts.length == 3) {
					uid = parts[1];
					cipheredSid = parts[2];
				}

				if (uid != null && cipheredSid != null) {
					final Attempt attempt = ToolAttemptDao.getBySecuredSid(cipheredSid, ts.getToolKey());
					// check and delete attempt from DB
					if (attempt != null && ToolAttemptDao.delete(attempt)) {
						attempt.getResourceUser().getResourceLink().setTool(tool);

						// user files
						File f = getFile(attempt, false);
						if (f != null && !f.delete()) {
							logger.error("Error deleting file: {}", f.getPath());
						}
						// result/output
						f = getFile(attempt, true);
						if (f != null) {
							clean(f.getPath());
						}
						response.sendError(HttpServletResponse.SC_NO_CONTENT);
					} else {
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						response.setContentType("text/plain");
						response.getWriter().append("Error id");
					}
				} else {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.setContentType("text/plain");
					response.getWriter().append("Error");
				}
			} else {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				request.getRequestDispatcher("/errorlogin.html").include(request, response);
			}
		} catch (IOException | ServletException e) {
			logger.error("Servlet exception", e);
		}
	}

	/**
	 * Gets a ZIP with a list of attempts.
	 *
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (final UnsupportedEncodingException e1) {
			// never
			logger.error("UTF-8", e1);
		}
		try {
			final HttpSession session = request.getSession();
			final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
			final Tool tool = ts.getTool();
			final String userId = ts.getSessionUserId();
			final String servletPath = request.getServletPath();
			final boolean allowed = tool != null && userId != null && servletPath.endsWith("downloadattempts")
					&& (tool.getToolUiConfig().isShowAttempts()
							|| request.getServletPath().equals("/instructor/downloadattempts")
									&& tool.getToolUiConfig().isManageAttempts());
			if (allowed) {
				// Extract all the parameters and generate a list of files to add
				final List<File> files = new ArrayList<>();
				final String[] params = request.getParameterValues("attempt");
				if (params != null) {
					for (final String p : params) {
						// We don't support .output in the list
						// Get ciphered serial IDs from parameters
						final String[] parts = p.split("/");
						String uid = null;
						String cipheredSid = null;
						if (parts.length == 2) {
							uid = parts[0];
							cipheredSid = parts[1];
						}
						if (uid != null && cipheredSid != null) {
							uid = URLDecoder.decode(uid, StandardCharsets.UTF_8);
							final Attempt attempt = ToolAttemptDao.getBySecuredSid(cipheredSid, ts.getToolKey());
							final boolean forceUserId = request.getServletPath().equals("/learner/downloadattempts")
									|| !tool.getToolUiConfig().isManageAttempts();
							if (attempt != null && (uid.equals(userId) && ts.getLtiResourceUser().getUser()
									.getSid() == attempt.getResourceUser().getUser().getSid() || !forceUserId)) {
								attempt.getResourceUser().getResourceLink().setTool(tool);
								// user files
								File f = getFile(attempt, false);
								if (f != null) {
									files.add(f);
								}
								// include result/output file if exists
								f = getFile(attempt, true);
								if (f != null) {
									files.add(f);
								}
							}
						}
					}
				}
				// Compress
				if (!files.isEmpty()) {
					final String filename = Settings.DATE_TIME_FORMATTER.format(Instant.now()) + "_download.zip";
					response.reset();
					response.setContentType("application/zip");
					response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
					try (ZipOutputStream output = new ZipOutputStream(response.getOutputStream());) {
						// Find extension for output files
						final String ext = mimeToExt.get(tool.getToolUiConfig().getOutputMimeType());
						zipFiles(files, output, tool.getToolDataPath().length(), ext != null ? "." + ext : "");
					}
				} else {
					response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				}
			} else {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				request.getRequestDispatcher("/errorlogin.html").include(request, response);
			}
		} catch (final IOException | ServletException e) {
			logger.error("IO Error.", e);
		}
	}

	/**
	 * ZIP a list of files.
	 *
	 * @param files list of files
	 * @param zos output stream
	 * @param prefixToRemove prefix to remove from file names
	 * @param outputExt extension to add to the file name of output files
	 */
	private void zipFiles(List<File> files, ZipOutputStream zos, int prefixToRemove, String outputExt) {
		try {
			final byte[] readBuffer = new byte[DEFAULT_BUFFER_SIZE];
			int bytesIn = 0;
			// loop through dirList, and zip the files
			for (final File f : files) {
				// if we reached here, the File object f was not a directory
				// create a FileInputStream on top of f
				try (FileInputStream fis = new FileInputStream(f);) {
					// create a new zip entry
					final String path = f.getPath();
					final boolean isOutput = path.endsWith(Settings.RESULT_EXT);
					final ZipEntry anEntry = new ZipEntry(
							path.substring(prefixToRemove + 1) + (isOutput ? outputExt : ""));
					// place the zip entry in the ZipOutputStream object
					zos.putNextEntry(anEntry);
					// now write the content of the file to the ZipOutputStream
					while ((bytesIn = fis.read(readBuffer)) != -1) {
						zos.write(readBuffer, 0, bytesIn);
					}
				}
			}
		} catch (final Exception e) {
			logger.error("Error creating ZIP: ", e);
		}
	}
	
	/**
	 * Clean output files.
	 *
	 * @param outputPath output file path
	 */
	public static void clean(String outputPath) {
		final File output = new File(outputPath);
		final File outputErr = new File(outputPath + Settings.OUTPUT_ERROR_EXT);
		if (output.exists() && !output.delete()) {
			logger.error("Error deleting output");
		}
		if (outputErr.exists() && !outputErr.delete()) {
			logger.error("Error deleting output.error");
		}
	}
}
