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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import es.us.dit.lti.ToolSession;

/**
 * Servlet Filter that filters learner pages.
 *
 * @author Francisco José Fernández Jiménez
 * @version 1.0
 */
@WebFilter(dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD }, urlPatterns = { "/learner/*" })
public class LearnerFilter implements Filter {

	/**
	 * Filters requests.
	 *
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpSession session = req.getSession(false);
		if (session != null) {
			final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
			if (ts != null && (ts.isLearner() || ts.isInstructor())) {
				// pass the request along the filter chain
				chain.doFilter(request, response);
			} else {
				final HttpServletResponse res = (HttpServletResponse) response;
				res.setStatus(HttpServletResponse.SC_FORBIDDEN);
				request.getRequestDispatcher("../errorlogin.html").forward(request, response);
			}
		}
	}

}
