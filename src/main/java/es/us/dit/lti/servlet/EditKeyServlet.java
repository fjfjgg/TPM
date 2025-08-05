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
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Consumer;
import es.us.dit.lti.entity.Context;
import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.MgmtUserType;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.entity.ToolKey;
import es.us.dit.lti.persistence.ToolConsumerDao;
import es.us.dit.lti.persistence.ToolContextDao;
import es.us.dit.lti.persistence.ToolDao;
import es.us.dit.lti.persistence.ToolKeyDao;
import es.us.dit.lti.persistence.ToolResourceLinkDao;

/**
 * Servlet to create, delete and edit tool keys.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/editor/editkey")
public class EditKeyServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(EditKeyServlet.class);

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
		response.setContentType("text/plain;charset=UTF-8");

		final MgmtUser sessionUser = (MgmtUser) request.getSession().getAttribute("mgmtUser");

		// Get old possible parameters
		final String toolName = request.getParameter("toolname");
		final String oldKey = request.getParameter("oldkey");
		String key = request.getParameter("key");
		final String secret = request.getParameter("secret");
		final String enabledString = request.getParameter("enabled");
		final boolean enabled = "true".equals(enabledString);
		final String consumerGuid = request.getParameter("consumer");
		final String contextId = request.getParameter("context");
		final String resourceLinkId = request.getParameter("link");
		final String address = request.getParameter("address");

		ToolKey old = null;
		if (oldKey != null) {
			old = ToolKeyDao.get(oldKey, false);
		}

		Tool tool;
		if (old != null) {
			tool = old.getTool();
		} else {
			tool = ToolDao.get(toolName);
		}

		try {
			if (tool == null || toolName == null
					|| ToolDao.getToolUserType(sessionUser, tool) > MgmtUserType.EDITOR.getCode()) {
				response.getWriter().append("No tiene acceso");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			} else if (oldKey != null && old == null) {
				// error, secret is required
				response.getWriter().append("Clave no existe");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			} else if (key != null && secret == null) {
				// error, secret is required
				response.getWriter().append("Parámetros incorrectos");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			} else if (key != null) {
				// edit or new
				// Add key prefix
				key = tool.getName() + "_" + key;
				if (old != null) {
					// edit
					editKey(response, key, secret, address, enabled, old);
				} else {
					createKey(response, key, secret, address, enabled, consumerGuid, contextId, resourceLinkId, tool);
				}

			} else {
				deleteKey(response, old);
			}
		} catch (final IOException e) {
			logger.error("IO Error.", e);
		}
	}

	/**
	 * Creates a tool key.
	 *
	 * @param response       the HTTP response
	 * @param key            the consumer key
	 * @param secret         the secret
	 * @param address        the address regex
	 * @param enabled        if key is enabled
	 * @param consumerGuid   the consumer GUID (constraint)
	 * @param contextId      the context ID (constraint)
	 * @param resourceLinkId the resource link ID (constraint)
	 * @param tool           the tool
	 * @throws IOException if HTTP response can not be written
	 */
	private void createKey(HttpServletResponse response, String key, String secret, String address, boolean enabled,
			String consumerGuid, String contextId, String resourceLinkId, Tool tool) throws IOException {
		// new
		final ToolKey tk = new ToolKey();
		tk.setKey(key);
		tk.setSecret(secret);
		tk.setAddress(address);
		tk.setEnabled(enabled);
		tk.setTool(tool);

		// check and set consumer, context and resource link
		boolean validated = true;
		if (consumerGuid != null) {
			final Consumer cs = ToolConsumerDao.getByGuid(consumerGuid);
			tk.setConsumer(cs);
			if (cs == null) {
				validated = false;
			} else if (contextId != null) {
				final Context ct = ToolContextDao.getById(cs, contextId);
				tk.setContext(ct);
				if (ct == null) {
					validated = false;
				} else if (resourceLinkId != null) {
					final ResourceLink rl = ToolResourceLinkDao.getById(tool.getSid(), ct.getSid(), resourceLinkId);
					tk.setResourceLink(rl);
					if (rl == null) {
						validated = false;
					}
				}
			}
		}
		if (validated) {
			// create
			try {
				// test pattern
				if (address != null && !address.isBlank()) {
					Pattern.compile(address); // ignore return value
				}
				if (ToolKeyDao.create(tk)) {
					response.getWriter().append("Creada");
				} else {
					response.getWriter().append("No se pudo crear");
					response.setStatus(HttpServletResponse.SC_CONFLICT);
				}
			} catch (final FileAlreadyExistsException e) {
				response.getWriter().append("La clave ya existe y no puede estar repetida");
				response.setStatus(HttpServletResponse.SC_CONFLICT);
			} catch (final PatternSyntaxException e) {
				response.getWriter().append("El patrón de dirección no es válido");
				response.setStatus(HttpServletResponse.SC_CONFLICT);
			}
		} else {
			response.getWriter().append("Parámetros incorrectos");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
	}

	/**
	 * Deletes a tool key.
	 *
	 * @param response the HTTP response
	 * @param old      tool key data
	 * @throws IOException if HTTP response can not be written
	 */
	private void deleteKey(HttpServletResponse response, ToolKey old) throws IOException {
		if (old != null) {
			// delete
			if (ToolKeyDao.delete(old)) {
				response.getWriter().append("Borrada");
			} else {
				response.getWriter().append("No se pudo borrar");
				response.setStatus(HttpServletResponse.SC_CONFLICT);
			}
		} else {
			// ignore
			response.getWriter().append("Ignorada");
		}
	}

	/**
	 * Updates a tool key (key, secret and enabled properties).
	 *
	 * @param response the HTTP response
	 * @param key      the consumer key
	 * @param secret   the secret
	 * @param address  the address regex
	 * @param enabled  it tool key is enabled
	 * @param old      old tool key data
	 * @throws IOException if HTTP response can not be written
	 */
	private void editKey(HttpServletResponse response, String key, String secret, String address, boolean enabled,
			ToolKey old) throws IOException {
		if (old.getKey().equals(key) && old.getSecret().equals(secret) && old.isEnabled() == enabled
				&& Objects.equals(old.getAddress(), address)) {
			// no changes
			response.getWriter().append("Ignorada");
		} else {
			old.setKey(key);
			old.setSecret(secret);
			old.setAddress(address);
			old.setEnabled(enabled);
			// restrictions are ignored, not allowed
			try {
				// test pattern
				if (address != null && !address.isBlank()) {
					Pattern.compile(address); // ignore return value
				}
				if (ToolKeyDao.update(old)) {
					response.getWriter().append("Editada");
				} else {
					response.getWriter().append("No se pudo editar");
					response.setStatus(HttpServletResponse.SC_CONFLICT);
				}
			} catch (final FileAlreadyExistsException e) {
				response.getWriter().append("La clave ya existe y no puede estar repetida");
				response.setStatus(HttpServletResponse.SC_CONFLICT);
			} catch (final PatternSyntaxException e) {
				response.getWriter().append("El patrón de dirección no es válido");
				response.setStatus(HttpServletResponse.SC_CONFLICT);
			}
		}
	}

}
