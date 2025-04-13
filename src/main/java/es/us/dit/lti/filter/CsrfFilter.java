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

package es.us.dit.lti.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.ToolSession;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet Filter to prevent CSRF attacks.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebFilter(dispatcherTypes = { DispatcherType.REQUEST }, filterName = "CsrfFilter", urlPatterns = { "/*" })
public class CsrfFilter extends HttpFilter implements Filter {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private final transient Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Filters requests.
	 *
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		final HttpServletRequest req = (HttpServletRequest) request;
		try {
			req.setCharacterEncoding("UTF-8");
		} catch (final UnsupportedEncodingException e1) {
			// never
			logger.error("UTF-8", e1);
		}
		boolean accept = true;
		final String method = req.getMethod();
		if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
			final HttpSession session = req.getSession(false); // do not create new sessions
			if (session != null) {
				final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
				String launchId = null;
				if (ts != null) {
					launchId = ts.getLaunchId();
					if (session.getAttribute("launchId") != null) {
						// copy to avoid problems
						session.setAttribute("launchId", launchId);
					}
				} else {
					launchId = (String) session.getAttribute("launchId");
				}
				if (launchId != null) {
					String receivedId = null;
					if ("DELETE".equals(method)) {
						receivedId = req.getHeader("X-launch-id");
					} else {
						receivedId = req.getParameter("launchId");
					}
					if (!Objects.equals(launchId, receivedId) && !isMultipart(req)
							&& !"/tools".equals(req.getServletPath())) {
						// if request is multipart, verification is done in servlet
						// LTI launch do not use this
						accept = false;
					}
				}
			}
		}

		if (accept) {
			// pass the request along the filter chain
			chain.doFilter(request, response);
		} else {
			logger.error("launchId incorrect: {}", req.getServletPath());
		}

	}

	/**
	 * Determines if request is multipart.
	 * 
	 * @param req the request
	 * @return if request is multipart
	 */
	private boolean isMultipart(HttpServletRequest req) {
		final String ct = req.getContentType();
		return ct != null && ct.toLowerCase(Locale.ENGLISH).startsWith("multipart/");
	}

}
