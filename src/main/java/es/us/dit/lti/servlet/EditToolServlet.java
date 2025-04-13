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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.MgmtUserType;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.persistence.ToolDao;
import es.us.dit.lti.runner.ToolRunnerType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

/**
 * Servlet to create and edit tools.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet({ "/editor/editTool", "/admin/createTool" })
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class EditToolServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(EditToolServlet.class);
	/**
	 * Charset.
	 */
	private static final String CHARSET = "UTF-8";
	/**
	 * Tool name parameter.
	 */
	private static final String TOOLNAME_PARAM = "toolname";
	/**
	 * Corrector file parameter.
	 */
	private static final String CORRECTOR_PARAM = "correctorfile";
	/**
	 * Description file parameter.
	 */
	private static final String DESCRIPTION_PARAM = "descriptionfile";
	/**
	 * Extra ZIP file parameter.
	 */
	private static final String EXTRAZIP_PARAM = "extrazipfile";

	/**
	 * Processes POST request.
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setCharacterEncoding(CHARSET);
		} catch (final UnsupportedEncodingException e1) {
			// never
			logger.error(CHARSET, e1);
		}
		try {
			boolean doCreate;
			if (request.getServletPath().equals("/admin/createTool")) {
				doCreate = true;
			} else {
				doCreate = false;
			}

			final HttpSession session = request.getSession();
			final MgmtUser sessionUser = (MgmtUser) session.getAttribute("mgmtUser");
			final Tool paramTool = new Tool();
			paramTool.setCounter(-1);
			// Prepare for file upload

			Collection<Part> fis = request.getParts();
			final Iterator<Part> iter = fis.iterator();

			String resMsg = null;
			if (doCreate && sessionUser.getAdmin()) {
				resMsg = createTool(sessionUser, paramTool, iter);
				if (resMsg.equals("T_HERRAMIENTA_CREADA")) {
					session.setAttribute("lasttool", paramTool.getName());
				}
			} else if (!doCreate) {
				resMsg = updateTool(sessionUser, paramTool, iter);
			}
			if (resMsg != null) {
				session.setAttribute(Settings.PENDING_MSG_ATTRIB, resMsg);
			}
			forward(request, response, "../user/tools.jsp");

		} catch (final ServletException e) {
			logger.error("Edit exception: ", e);
		}
	}

	/**
	 * Creates a tool.
	 *
	 * @param sessionUser owner administrator user
	 * @param paramTool   tool data
	 * @param iter        multipart request iterator
	 * @return message to display
	 */
	private String createTool(final MgmtUser sessionUser, final Tool paramTool, Iterator<Part> iter) {
		Part fi;
		UploadedFile descriptionFile = null;
		UploadedFile correctorFile = null;
		UploadedFile extraZipFile = null;
		while (iter.hasNext()) {
			fi = iter.next();
			if (fi.getContentType() != null) {
				final String fieldName = fi.getName();

				if (fi.getSubmittedFileName() != null && !fi.getSubmittedFileName().isEmpty()) {
					if (fieldName.equals(CORRECTOR_PARAM)) {
						correctorFile = new UploadedFile(fi);
					} else if (fieldName.equals(DESCRIPTION_PARAM)) {
						descriptionFile = new UploadedFile(fi);
					} else {
						extraZipFile = new UploadedFile(fi);
					}
				}
			} else {
				final String fieldName = fi.getName();
				setProperty(paramTool, fieldName, fi);
				if (fieldName.equals(DESCRIPTION_PARAM)) {
					if (descriptionFile == null) {
						descriptionFile = new UploadedFile(fi);
					}
				} else if (fieldName.equals(CORRECTOR_PARAM) && correctorFile == null) {
					// End of line conversion if necessary
					correctorFile = convertLineSeparator(fi);
				}
			}
		}

		String resMsg;
		if (correctorFile != null && descriptionFile != null) {
			try {
				if (ToolDao.create(sessionUser, paramTool, correctorFile, descriptionFile, extraZipFile)) {
					resMsg = "T_HERRAMIENTA_CREADA";
				} else {
					resMsg = "T_ERROR_CREAR_HERRAMIENTA";
				}
			} catch (final FileAlreadyExistsException e) {
				resMsg = "T_ERROR_HERRAMIENTA_CLAVE_DUPLICADA";
			} catch (final FileSystemException e) {
				resMsg = "T_ERROR_IO";
			}
		} else {
			resMsg = "T_ERROR_FALTAN_FICHEROS";
		}
		return resMsg;
	}

	/**
	 * Updates a tool.
	 *
	 * @param sessionUser owner administrator user
	 * @param paramTool   tool data
	 * @param iter        multipart request iterator
	 * @return message to display
	 */
	private String updateTool(final MgmtUser sessionUser, final Tool paramTool, Iterator<Part> iter) {
		Part fi;
		UploadedFile descriptionFile = null;
		UploadedFile correctorFile = null;
		UploadedFile extraZipFile = null;
		String oldname = "";

		boolean authorized = false;
		int toolTypeUser = 2; // 2 = unauthorized

		while (iter.hasNext()) {
			fi = iter.next();
			if (fi.getContentType() != null && authorized) {
				final String fieldName = fi.getName();

				if (fi.getSubmittedFileName() != null && !fi.getSubmittedFileName().isEmpty()) {
					if (fieldName.equals(CORRECTOR_PARAM)) {
						correctorFile = new UploadedFile(fi);
					} else if (fieldName.equals(DESCRIPTION_PARAM)) {
						descriptionFile = new UploadedFile(fi);
					} else if (fieldName.equals(EXTRAZIP_PARAM)) {
						extraZipFile = new UploadedFile(fi);
					}
				}
			} else {
				final String fieldName = fi.getName();
				setProperty(paramTool, fieldName, fi);

				if (fieldName.equals(TOOLNAME_PARAM)) {
					// It can only be processed if the user is an
					// administrator of the tool and must have been previously
					// authorized
					if (authorized && toolTypeUser == MgmtUserType.ADMIN.getCode()) {
						authorized = true;
					} else {
						authorized = false;
					}
				} else if (fieldName.equals("oldname")) {
					try {
						oldname = new String(fi.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
					} catch (final Exception e) {
						// never
						logger.error(CHARSET, e);
					}
					if (!oldname.isEmpty()) {
						final Tool oldTool = ToolDao.get(oldname);
						toolTypeUser = ToolDao.getToolUserType(sessionUser, oldTool);
						// oldToolTitle must appear first.
						// Name of the tool to update
						if (toolTypeUser < MgmtUserType.TESTER.getCode()) {
							authorized = true;
							if (toolTypeUser == MgmtUserType.EDITOR.getCode()) {
								setProperty(paramTool, TOOLNAME_PARAM, oldname);
								// copy so it can be updated
								paramTool.setToolType(oldTool.getToolType());
							}
						} else {
							authorized = false;
						}
					}
				} else if (fieldName.equals(DESCRIPTION_PARAM) && authorized) {
					if (descriptionFile == null) {
						descriptionFile = new UploadedFile(fi);
					}
				} else if (fieldName.equals(CORRECTOR_PARAM) && correctorFile == null && authorized) {
					// End of line conversion if necessary
					correctorFile = convertLineSeparator(fi);
				}
			}
		}
		String resMsg;
		// Verify that the user can actually edit
		if (authorized) {
			try {
				if (ToolDao.update(paramTool, oldname, correctorFile, descriptionFile, extraZipFile)) {
					resMsg = "T_HERRAMIENTA_EDITADA";
				} else {
					resMsg = "T_ERROR_EDITAR_HERRAMIENTA";
				}
			} catch (final FileAlreadyExistsException e) {
				resMsg = "T_ERROR_HERRAMIENTA_CLAVE_DUPLICADA";
			} catch (final FileSystemException e) {
				resMsg = "T_ERROR_IO";
			}
		} else {
			logger.error("The user {} can not edit the tool.", sessionUser.getUsername());
			resMsg = "T_ERROR_AUTORIZACION";
		}
		return resMsg;
	}

	/**
	 * Set a property of a tool from a request parameter (as a FileItem).
	 *
	 * @param tool     the tool
	 * @param property property name
	 * @param fi       request parameter as FileItem
	 */
	private void setProperty(Tool tool, String property, Part fi) {
		try {
			setProperty(tool, property, new String(fi.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
		} catch (final UnsupportedEncodingException e) {
			// never
			logger.error(CHARSET, e);
		} catch (final IOException e) {
			// never
			logger.error("IO param", e);
		}
	}

	/**
	 * Set the value of a property of a tool.
	 *
	 * @param tool     the tool
	 * @param property the property name
	 * @param value    the property new value
	 */
	private void setProperty(Tool tool, String property, String value) {
		switch (property) {
		case TOOLNAME_PARAM:
			tool.setName(value);
			break;
		case "description":
			tool.setDescription(value);
			break;
		case "deliveryPassword":
			tool.setDeliveryPassword(value);
			break;
		case "enabled":
			tool.setEnabled(Boolean.parseBoolean(value));
			break;
		case "enabledFrom":
			// Receive number timestamp
			if (!value.isEmpty()) {
				try {
					final long tsLong = Long.parseLong(value);
					if (tsLong > 0) {
						final Timestamp ts = new Timestamp(tsLong);
						tool.setEnabledFrom(new Calendar.Builder().setInstant(ts).build());
					}
				} catch (final NumberFormatException e2) {
					logger.error("enabledFrom invalid.", e2);
				}
			}
			break;
		case "enabledUntil":
			if (!value.isEmpty()) {
				try {
					final long tsLong = Long.parseLong(value);
					if (tsLong > 0) {
						final Timestamp ts = new Timestamp(tsLong);
						tool.setEnabledUntil(new Calendar.Builder().setInstant(ts).build());
					}
				} catch (final NumberFormatException e2) {
					logger.error("enabledUntil invalid.", e2);
				}
			}
			break;
		case "outcome":
			tool.setOutcome(Boolean.parseBoolean(value));
			break;
		case "jsonconfig":
			tool.setJsonConfig(value);
			break;
		case "tooltype":
			ToolRunnerType toolType = ToolRunnerType.UNKNOWN;
			try {
				toolType = ToolRunnerType.fromInt(Integer.parseInt(value));
			} catch (final NumberFormatException e1) {
				logger.error("toolType invalid.", e1);
			}
			tool.setToolType(toolType);
			break;
		case "extraArgs":
			tool.setExtraArgs(value);
			break;
		case "counter":
			int counter = -1;
			if (!value.isEmpty()) {
				try {
					counter = Integer.parseInt(value);
				} catch (final NumberFormatException e) {
					// Nada, seguirá siendo -1
					logger.error("counter invalid.", e);
				}
			}
			tool.setCounter(counter);
			break;
		default:
			// ignore
			break;
		}
	}

	/**
	 * Forwards HTTP request.
	 *
	 * @param request    the HTTP request
	 * @param response   the HTTP response
	 * @param forwardUrl the URL
	 */
	private void forward(HttpServletRequest request, HttpServletResponse response, String forwardUrl) {
		try {
			request.getRequestDispatcher(forwardUrl).forward(request, response);
		} catch (ServletException | IOException e) {
			logger.error("Error forwarding");
		}
	}

	/**
	 * Convert the line separator of a parameter from Windows to system default.
	 *
	 * @param fi request parameter as a FileItem
	 * @return converted parameter
	 */
	private UploadedFile convertLineSeparator(Part fi) {
		try {
			if (!"\r\n".equals(System.lineSeparator())) {
				final String textarea = new String(fi.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
						.replace("\r\n", System.lineSeparator());
				InputStream is = new ByteArrayInputStream(textarea.getBytes(StandardCharsets.UTF_8));
				return new UploadedFile(is);			
			} else {
				return new UploadedFile(fi);
			}
		} catch (final IOException e) {
			logger.info("Failed to change line ending");
		}
		return null;
	}
}
