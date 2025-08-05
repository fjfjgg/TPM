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

package es.us.dit.lti;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import net.oauth.OAuth;
import net.oauth.OAuthMessage;

/**
 * OAuthMessage compatible with Jakarta EE.
 * 
 * <p>Necessary to continue using the same oauth library.
 *
 * @author Francisco José Fernández Jiménez
 */
public class JakartaHttpRequestMessage extends OAuthMessage {

	/**
	 * HTTP request.
	 */
	private final HttpServletRequest request;

	/**
	 * Default constructor.
	 * 
	 * @param request HTTP request
	 */
	public JakartaHttpRequestMessage(HttpServletRequest request) {
		super(request.getMethod(), null, null);
		this.request = request;
		URL = request.getRequestURL().toString();
		final int q = URL.indexOf('?');
		if (q >= 0) {
			URL = URL.substring(0, q);
		}
		// Parameters
		for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			final String name = entry.getKey();
			if (name.equals("Authorization")) {
				for (final String value : entry.getValue()) {
					for (final OAuth.Parameter parameter : decodeAuthorization(value)) {
						if (!"realm".equalsIgnoreCase(parameter.getKey())) {
							addParameter(parameter);
						}
					}
				}
			} else {
				for (final String value : entry.getValue()) {
					addParameter(new OAuth.Parameter(name, value));
				}
			}
		}
		// Headers
		final List<Map.Entry<String, String>> headers = getHeaders();
		final Enumeration<String> names = request.getHeaderNames();
		if (names != null) {
			while (names.hasMoreElements()) {
				final String name = names.nextElement();
				final Enumeration<String> values = request.getHeaders(name);
				while (values.hasMoreElements()) {
					headers.add(new OAuth.Parameter(name, values.nextElement()));
				}
			}
		}
	}

	@Override
	public InputStream getBodyAsStream() throws IOException {
		return request.getInputStream();
	}

	@Override
	public String getBodyEncoding() {
		return request.getCharacterEncoding();
	}

}
