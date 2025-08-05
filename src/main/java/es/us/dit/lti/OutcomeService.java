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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.XMLConstants;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.VersionInfo;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.ResourceUser;
import es.us.dit.lti.entity.ToolKey;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;

/**
 * Only support LTI 1.1 and decimal values between 0.0 and 1.0 as defined in
 * <a href= "https://www.imsglobal.org/spec/lti-bo/v1p1/">
 * https://www.imsglobal.org/spec/lti-bo/v1p1/</a>.
 *
 * @author Francisco José Fernández Jiménez
 *
 */
public final class OutcomeService {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(OutcomeService.class);

	/**
	 * Timeout for receiving an HTTP response.
	 */
	private static final int TIMEOUT = 30000;

	/**
	 * Can not create objects.
	 */
	private OutcomeService() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Writes outcome/score in tool consumer (external).
	 *
	 * @param user    user data (<code>ResultSourceId)</code>
	 * @param toolKey tool key to authenticate request
	 * @param value   value of score/outcome
	 * @return true if successfull, false otherwise
	 */
	public static boolean writeOutcome(ResourceUser user, ToolKey toolKey, String value) {
		boolean result = false;
		final String url = user.getResourceLink().getOutcomeServiceUrl();
		if (url != null && !url.isEmpty()) {
			if (value == null) {
				value = "";
			}
			if (doServiceRequest(url, "replaceResultRequest", user.getResultSourceId(), value,
					toolKey.getKey(), toolKey.getSecret()) != null) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Deletes outcome/score stored in tool consumer (external).
	 *
	 * @param user    user data (<code>ResultSourceId)</code>
	 * @param toolKey tool key to authenticate request
	 * @return true if successfull, false otherwise
	 */
	public static boolean deleteOutcome(ResourceUser user, ToolKey toolKey) {
		boolean result = false;
		final String url = user.getResourceLink().getOutcomeServiceUrl();
		if (url != null && !url.isEmpty() && doServiceRequest(url, "deleteResultRequest", user.getResultSourceId(),
				null, toolKey.getKey(), toolKey.getSecret()) != null) {
			result = true;
		}
		return result;
	}

	/**
	 * Reads outcome/score from tool consumer (external).
	 *
	 * @param user    user data (<code>ResultSourceId)</code>
	 * @param toolKey tool key to authenticate request
	 * @return the score/outcome as a string
	 */
	public static String readOutcome(ResourceUser user, ToolKey toolKey) {
		String result = null;
		final String url = user.getResourceLink().getOutcomeServiceUrl();
		if (url != null && !url.isEmpty()) {
			final Document xmlDoc = doServiceRequest(url, "readResultRequest", user.getResultSourceId(), null,
					toolKey.getKey(), toolKey.getSecret());
			if (xmlDoc != null) {
				final Element element = getXmlChild(xmlDoc.getRootElement(), "textString");
				if (element != null) {
					result = element.getText();
				}
			}
		}
		return result;
	}

	/**
	 * Gest a child tag of a XML document.
	 *
	 * @param root root tag
	 * @param name name of child tag
	 * @return child tag or null
	 */
	private static Element getXmlChild(Element root, String name) {
		Element child = null;
		if (name != null) {
			final ElementFilter elementFilter = new ElementFilter(name);
			final Iterator<Element> iter = root.getDescendants(elementFilter);
			if (iter.hasNext()) {
				child = iter.next();
			}
		} else {
			final List<Element> elements = root.getChildren();
			if (!elements.isEmpty()) {
				child = elements.get(0);
			}
		}

		return child;

	}

	/**
	 * Converts a map of parameters in a list of {@link NameValuePair}.
	 *
	 * @param params map of parameters
	 * @return created list
	 */
	private static List<NameValuePair> getHttpParams(Map<String, String> params) {

		List<NameValuePair> nvPairs = null;
		if (params != null) {
			nvPairs = new ArrayList<>(params.size());
			for (final Entry<String, String> entry : params.entrySet()) {
				nvPairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
		}

		return nvPairs;

	}

	/**
	 * Makes a LTI 1.1 service request.
	 *
	 * @param url    LTI outcome service URL
	 * @param requestType request body type
	 * @param sourcedId   sourcedId of result record
	 * @param value       result value of resultRecord
	 * @param key    tool key
	 * @param secret tool secret to authenticate request
	 * @return response of the tool consumer as XML document
	 */
	private static Document doServiceRequest(String url, String requestType, String sourcedId, String value, String key,
			String secret) {
		final String messageId = UUID.randomUUID().toString();
		final StringBuilder xmlRequest = new StringBuilder();
		xmlRequest.append("<?xml version = \"1.0\" encoding = \"UTF-8\"?>\n");
		xmlRequest.append(
				"<imsx_POXEnvelopeRequest xmlns = \"http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0\">\n");
		xmlRequest.append("  <imsx_POXHeader>\n");
		xmlRequest.append("    <imsx_POXRequestHeaderInfo>\n");
		xmlRequest.append("      <imsx_version>V1.0</imsx_version>\n");
		xmlRequest.append("      <imsx_messageIdentifier>").append(messageId).append("</imsx_messageIdentifier>\n");
		xmlRequest.append("    </imsx_POXRequestHeaderInfo>\n");
		xmlRequest.append("  </imsx_POXHeader>\n");
		xmlRequest.append("  <imsx_POXBody>\n");
		xmlRequest.append("    <").append(requestType).append(">\n");
		xmlRequest.append("      <resultRecord>\n");
		xmlRequest.append("        <sourcedGUID>\n");
		xmlRequest.append("          <sourcedId>").append(sourcedId).append("</sourcedId>\n");
		xmlRequest.append("        </sourcedGUID>\n");
		if (value != null) {
			xmlRequest.append("        <result>\n");
			xmlRequest.append("          <resultScore>\n");
			xmlRequest.append("            <language>en-US</language>\n");
			xmlRequest.append("            <textString>").append(value).append("</textString>\n");
			xmlRequest.append("          </resultScore>\n");
			xmlRequest.append("        </result>\n");
		}
		xmlRequest.append("      </resultRecord>\n");
		xmlRequest.append("    </").append(requestType).append(">\n");
		xmlRequest.append("  </imsx_POXBody>\n");
		xmlRequest.append("</imsx_POXEnvelopeRequest>\n");

		// Body hash
		final String hash = Base64.encodeBase64String(DigestUtils.sha1(xmlRequest.toString()));
		final Map<String, String> params = new HashMap<>();
		params.put("oauth_body_hash", hash);

		String urlNoQuery = url;
		try {
			final URIBuilder uri = new URIBuilder(url);
			final List<NameValuePair> queryItems = uri.getQueryParams();
			if (queryItems != null) {
				urlNoQuery = uri.clearParameters().toString();
				for (final NameValuePair queryItem : queryItems) {
					params.put(queryItem.getName(), queryItem.getValue());
				}
			}
		} catch (final URISyntaxException e) {
			// ignore
			logger.error("URI", e);
		}

		// OAuth signature
		final Map<String, String> header = new HashMap<>();
		final OAuthMessage oAuthMessage = new OAuthMessage("POST", urlNoQuery, params.entrySet());
		final OAuthConsumer oAuthConsumer = new OAuthConsumer("about:blank", key, secret, null);
		final OAuthAccessor oAuthAccessor = new OAuthAccessor(oAuthConsumer);
		try {
			oAuthMessage.addRequiredParameters(oAuthAccessor);
			header.put("Authorization", oAuthMessage.getAuthorizationHeader(null));
			header.put("Content-Type", "application/xml");
		} catch (OAuthException | URISyntaxException | IOException e) {
			// ignore
			logger.error("OAuth", e);
		}
		final StringEntity entity = new StringEntity(xmlRequest.toString(),
				ContentType.create("application/xml", "UTF-8"));

		// Make request
		final String response = sendRequest(url, getHttpParams(params), header, entity);

		// Process response
		return response != null ? processResponse(response) : null;
	}

	/**
	 * Validates HTTP body response and converts it to XML document.
	 *
	 * @param response HTTP body response
	 * @return XML document if successful, null otherwise
	 */
	private static Document processResponse(String response) {
		// XML must start with <?xml
		Document xmlDoc = null;
		final int pos = response.indexOf("<?xml ");
		if (pos > 0) {
			response = response.substring(pos);
		}
		try {
			final SAXBuilder sb = new SAXBuilder();
			// XML parsers should not be vulnerable to XXE attacks
			sb.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			sb.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			xmlDoc = sb.build(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
		} catch (JDOMException | IOException e) {
			// ignore
			logger.error("XML", e);
		}
		if (xmlDoc != null) {
			String responseCode = null;
			final Element el = getXmlChild(xmlDoc.getRootElement(), "imsx_statusInfo");
			if (el != null) {
				final Element respCodeElement = getXmlChild(el, "imsx_codeMajor");
				if (respCodeElement != null) {
					responseCode = respCodeElement.getText();
				}
			}
			if (responseCode == null || !responseCode.equals("success")) {
				if (logger.isErrorEnabled()) {
					logger.error(xmlDoc.toString());
				}
				xmlDoc = null;
			}
		}

		return xmlDoc;
	}

	/**
	 * Send HTTP request.
	 *
	 * @param url    URL of the service
	 * @param params parameters of request if entity is null
	 * @param header map of headers/values
	 * @param entity body of request, if null params are used as body.
	 * @return body of response or null if error
	 */
	private static String sendRequest(String url, List<NameValuePair> params, Map<String, String> header,
			StringEntity entity) {

		String fileContent = null;

		// set the connection timeout
		final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(TIMEOUT).setRedirectsEnabled(false)
				.setContentCompressionEnabled(false).build();

		final HttpClient client = HttpClientBuilder.create().setHttpProcessor(HttpProcessorBuilder.create()
				.addAll(new RequestUserAgent(
						VersionInfo.getUserAgent("Apache-HttpClient", "org.apache.http.client", OutcomeService.class)),
						new RequestTargetHost(), new RequestContent())
				.build()).setDefaultRequestConfig(requestConfig).build();

		final HttpPost httpPost = new HttpPost();

		try {

			final URIBuilder ub = new URIBuilder(url);

			httpPost.setURI(ub.build());

			if (entity != null) {
				httpPost.setEntity(entity);
			} else {
				httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
			}
			if (header != null) {
				for (final Entry<String, String> entry : header.entrySet()) {
					httpPost.addHeader(entry.getKey(), entry.getValue());
				}
			}

			final HttpResponse response = client.execute(httpPost);
			fileContent = processHttpResponse(response);

		} catch (IOException | URISyntaxException e) {
			fileContent = null;
		}
		httpPost.releaseConnection();

		return fileContent;

	}

	/**
	 * Validates HTTP response and extracts body.
	 *
	 * <p>Response code must be less than 400 and body length less than 65535.
	 *
	 * @param response HTTP response
	 * @return body of response if successful
	 * @throws IOException If an input or output exception occurred
	 */
	private static String processHttpResponse(HttpResponse response) throws IOException {
		String fileContent = null;
		final int resp = response.getStatusLine().getStatusCode();
		if (resp < 400) {
			final HttpEntity httpEntity = response.getEntity();
			if (!httpEntity.isChunked()) {
				final long len = response.getEntity().getContentLength();
				if (len > 0 && len < 65535) {
					fileContent = EntityUtils.toString(response.getEntity());
				} else {
					// invalid response
					fileContent = null;
				}
			} else {
				fileContent = EntityUtils.toString(response.getEntity());
				if (fileContent.isEmpty()) {
					fileContent = null;
				}
			}
		}
		return fileContent;
	}

}
