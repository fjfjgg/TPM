/*
    This file is part of Tool Provider Manager - Manager of LTI Tool Providers
    for learning platforms.
    Copyright (C) 2025  Francisco José Fernández Jiménez.

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet to test outcome service (write).
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet("/testoutcome")
public class TestOutcomeServiceServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(TestOutcomeServiceServlet.class);

	/**
	 * Value of "imsx_codeMayor" when it simulates a success.
	 */
	private static final String SUCCESS_VALUE = "success";
	
	/**
	 * Value of "imsx_codeMayor" when it simulates a failure.
	 */
	private static final String FAILURE_VALUE = "failure";
	
	/**
	 * If it simulates a success.
	 */
	private String codeMayor;
	
	
	/**
	 * Processes GET request. Switch "imsx_codeMajor" between "success"/"failure".
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			synchronized (this) {
				codeMayor = SUCCESS_VALUE.equals(codeMayor) ? FAILURE_VALUE : SUCCESS_VALUE;
				response.getWriter().append("Simulando codeMayor = " + codeMayor);
			}
		} catch (final IOException e) {
			logger.error("IO Error.", e);
		}
	}
	
	/**
	 * Init servlet.
	 * 
	 * @see HttpServlet#init(ServletConfig config)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		codeMayor = SUCCESS_VALUE;
	}

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
		
		response.setContentType("application/xml");
		
		final StringBuilder xmlResponse = new StringBuilder();
		xmlResponse.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xmlResponse.append("<imsx_POXEnvelopeResponse xmlns=\"http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0\">\n");
		xmlResponse.append("<imsx_POXHeader>\n");
		xmlResponse.append("  <imsx_POXResponseHeaderInfo>\n");
		xmlResponse.append("    <imsx_version>V1.0</imsx_version>\n");
		xmlResponse.append("    <imsx_messageIdentifier>test-messageIdentifier</imsx_messageIdentifier>\n");
		xmlResponse.append("    <imsx_statusInfo>\n");
		synchronized (this) {
			xmlResponse.append("      <imsx_codeMajor>" + codeMayor + "</imsx_codeMajor>\n");
		}
		xmlResponse.append("      <imsx_severity>status</imsx_severity>\n");
		xmlResponse.append("      <imsx_description>OK</imsx_description>\n");
		xmlResponse.append("      <imsx_messageRefIdentifier>messageRefIdentifier</imsx_messageRefIdentifier>\n");
		xmlResponse.append("      <imsx_operationRefIdentifier>replaceResult</imsx_operationRefIdentifier>\n");
		xmlResponse.append("    </imsx_statusInfo>\n");
		xmlResponse.append("  </imsx_POXResponseHeaderInfo>\n");
		xmlResponse.append("</imsx_POXHeader>\n");
		xmlResponse.append("<imsx_POXBody>\n");
		xmlResponse.append("  <replaceResultResponse />\n");
		xmlResponse.append("</imsx_POXBody>\n");
		xmlResponse.append("</imsx_POXEnvelopeResponse>\n");
		try {
			response.getWriter().write(xmlResponse.toString());
		} catch (IOException e) {
			//Ignored
			logger.error("POST IO Error.", e);
		}
		logger.info("Test Service Outcome");
	}

}
