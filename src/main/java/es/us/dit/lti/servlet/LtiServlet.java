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
import java.util.Locale;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.MessageMap;
import es.us.dit.lti.ToolSession;
import es.us.dit.lti.entity.MgmtUser;

/**
 * Servlet for receiving initial tool request. LTI initial contact URL.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/tools")
public class LtiServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(LtiServlet.class);

	/**
	 * Processes LTI request.
	 *
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {
			request.setCharacterEncoding("UTF-8");
			final ToolSession ts = new ToolSession();
			ts.init(request);
			Locale locale = request.getLocale();
			if (ts.getPresentationLocale() != null) {
				locale = Locale.forLanguageTag(ts.getPresentationLocale());
			}
			final MessageMap text = new MessageMap(locale);
			if (ts.isValid()) {
				// Valid
				final HttpSession session = request.getSession(true);
				session.setAttribute(ToolSession.class.getName(), ts);
				session.setAttribute("text", text);
				response.sendRedirect(response.encodeRedirectURL(ts.getContinueUrl()));
			} else {
				// Invalid
				String url = ts.getLtiReturnUrl();
				String error = ts.getError();
				if (error == null) {
					error = "T_LTI_DEFAULT_ERROR";
				}
				if (url != null) {
					if (url.indexOf("?") >= 0) {
						url += '&';
					} else {
						url += '?';
					}
					url += "lti_errormsg=" + ToolSession.urlEncode(text.get(error));
					// we do not use lti_errorlog
					response.sendRedirect(url);
				} else if (ts.getError() != null) {
					response.sendRedirect("./error.jsp?errorMessage=" + ToolSession.urlEncode(text.get(error)));
				}
			}

		} catch (final IOException e) {
			// do nothing
			logger.error("IO Error.", e);
		}
	}

	/**
	 * Processes LTI logout.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		final HttpSession session = request.getSession(false);
		try {
			if (session != null) {
				//Test or not
				MgmtUser mgmtUser = (MgmtUser) session.getAttribute("mgmtUser");
				if (mgmtUser == null) {
					//LTI
					session.invalidate();
				} else {
					session.removeAttribute(ToolSession.class.getName());
				}
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (final IOException e) {
			// do nothing
			logger.error("IO Error.", e);
		}
	}
}
