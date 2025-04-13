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

package es.us.dit.lti.runner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import es.us.dit.lti.config.ExecutionRestrictionsConfig;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.runner.HttpToolConfig.Entry;

/**
 * Tool Runner with remote corrector via HTTP.
 *
 * @author Francisco José Fernández Jiménez
 */
public class HttpToolRunner implements ToolRunner {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(HttpToolRunner.class);
	/**
	 * Regular expression to detect elements to replace.
	 */
	private static final Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
	/**
	 * Maximum response time.
	 */
	private static final int DEFAULT_TIMEOUT = 30000;
	/**
	 * Configuration of tool.
	 */
	private HttpToolConfig tc = null;
	/**
	 * Indicates whether to make substitutions in the request.
	 */
	private boolean replaceRequest = false;
	/**
	 * Indicates whether to make substitutions in the response.
	 */
	private boolean replaceResponse = false;
	/**
	 * Client who will make the requests.
	 */
	private HttpClient client = null;

	/**
	 * Initializes tool runner.
	 * 
	 * <p><code>executionRestrictions</code> is ignored. <code>exeData</code> is the
	 * file name of JSON serialized {@link HttpToolConfig}.
	 */
	@Override
	public void init(String exeData, String executionRestrictions) {
		// get working directory.
		final File ed = new File(exeData);

		if (ed.exists() && ed.isFile() && ed.canRead()) {
			try {
				tc = HttpToolConfig.fromString(new FileReader(ed, StandardCharsets.UTF_8));
			} catch (final IOException e) {
				// Ignore
				tc = null;
			}
		}
		if (tc != null) {
			// debug
			logger.debug("{}", tc);

			// check values
			if (tc.getUrl() == null) {
				logger.error("HttpToolRunner: URL can not be null: {}", exeData);
				tc = null;
			} else {
				// set default values
				if (tc.getRequestMethod() == null) {
					tc.setRequestMethod("POST");
				} else { // Always put in capital letters
					tc.setRequestMethod(tc.getRequestMethod().toUpperCase());
				}
				if (tc.getHeaders() == null) {
					tc.setHeaders(new ArrayList<>());
				}
				if (tc.getParameters() == null) {
					tc.setParameters(new ArrayList<>());
				}
				// precalculate whether to make replacements in response or request and save the
				// value
				if (pattern.matcher(tc.getUrl()).find()
						|| tc.getFileParameter() != null && pattern.matcher(tc.getFileParameter()).find()
						|| tc.getRequestBody() != null && pattern.matcher(tc.getRequestBody()).find()) {
					replaceRequest = true;
				} else {
					for (final Entry e : tc.getHeaders()) {
						if (!e.literal || replaceRequest) {
							replaceRequest = true;
							break;
						}
					}
					for (final Entry e : tc.getParameters()) {
						if (!e.literal || replaceRequest) {
							replaceRequest = true;
							break;
						}
					}
				}
				// response
				if (tc.getScoreTemplate() != null && pattern.matcher(tc.getScoreTemplate()).find()) {
					replaceResponse = true;
				} else if (tc.getResponseTemplate() != null) {
					for (final String line : tc.getResponseTemplate()) {
						if (pattern.matcher(line).find()) {
							replaceResponse = true;
							break;
						}
					}
				}
				// 30 seconds by default
				final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(DEFAULT_TIMEOUT)
						.setRedirectsEnabled(false).setContentCompressionEnabled(false).build();

				if (tc.isNoVerifyCertificate()) {
					try {
						client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
								.setSSLContext(new SSLContextBuilder()
										.loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
								.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
					} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
						// ignore
						client = null;
					}
				}
				if (client == null) {
					client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
				}
			}

		} else {
			logger.error("HttpToolRunner: Error reading configuration: {}", exeData);
		}

		if (executionRestrictions != null && !executionRestrictions.isEmpty()) {
			final ExecutionRestrictionsConfig er = ExecutionRestrictionsConfig.fromString(executionRestrictions);
			if (er == null) {
				logger.error("HttpToolRunner: Error reading ExecutionRestrictions");
			}
		}
	}

	/**
	 * Execute the tool.
	 */
	@Override
	public int exec(String filePath, String outputPath, String userId, String originalFilename, int counter,
			boolean isInstructor, List<String> extraArgs, long maxSecondsWait) {
		int result = 0;
		if (tc != null && tc.getUrl() != null) {
			final File output = new File(outputPath);
			final File outputErr = new File(outputPath + Settings.OUTPUT_ERROR_EXT);
			final ArrayList<String> args = new ArrayList<>();
			// do not use preArgs
			args.add(userId);
			args.add(originalFilename);
			args.add(String.valueOf(counter));
			args.add(String.valueOf(isInstructor));
			args.addAll(extraArgs);

			try (PrintWriter errorLog = new PrintWriter(outputErr, StandardCharsets.UTF_8)) {
				// request
				final HttpUriRequest request = createRequest(filePath, originalFilename, args, errorLog);
				// request timer
				final TimerTask task = new TimerTask() {
					@Override
					public void run() {
						if (request != null) {
							request.abort();
						}
					}
				};
				final Timer t = new Timer(true);
				t.schedule(task, maxSecondsWait * 1000);
				// send HTTP request
				try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request);) {
					// process response
					result = processResponse(output, errorLog, response);
				}
				t.cancel();
			} catch (final IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * Process an HTTP response.
	 * 
	 * @param output   file to write results
	 * @param errorLog output to write errors
	 * @param response the HTTP response
	 * @return score/outcome of the attempt
	 */
	private int processResponse(File output, PrintWriter errorLog, CloseableHttpResponse response) {
		int result;
		Map<String, String> replacements = null;
		if (replaceResponse) {
			// replace what can be replaced
			replacements = createResponseReplacements(response);
		}

		if (tc.getScoreTemplate() == null) {
			// values according to the status of the response
			if (response.getStatusLine().getStatusCode() < 300) {
				result = tc.getDefaultScoreOnSuccess();
			} else {
				result = tc.getDefaultScoreOnError();
			}
		} else {
			// fixed score.
			try {
				if (replaceResponse) {
					result = Integer.parseInt(replaceResponseTokens(tc.getScoreTemplate(), replacements));
				} else {
					result = Integer.parseInt(tc.getScoreTemplate());
				}
			} catch (final NumberFormatException e1) {
				result = ERROR_CORRECTOR_EXCEPTION;
			}
		}

		if (replaceResponse || tc.getResponseTemplate() != null) {
			// copy body
			try (FileWriter fw = new FileWriter(output, StandardCharsets.UTF_8)) {
				if (tc.getResponseTemplate() != null) {
					for (final String l : tc.getResponseTemplate()) {
						fw.write(replaceResponse ? replaceResponseTokens(l, replacements) : l);
						fw.append('\n');
					}
				} else if (replaceResponse) {
					fw.write(replacements.get("body"));
				}
			} catch (final FileNotFoundException e) {
				errorLog.println("The output file does not exist.");
			} catch (final IOException e1) {
				e1.printStackTrace();
			}
		} else {
			// simpler case
			// copy received body as is
			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				final long len = entity.getContentLength();
				// set a limit to the maximum size of the received file
				if (len != -1 && len < Settings.getMaxUploadSize() * 1024) {
					try (FileOutputStream fos = new FileOutputStream(output)) {
						entity.getContent().transferTo(fos);
					} catch (final FileNotFoundException e) {
						errorLog.println("The output file does not exist.");
					} catch (final IOException e1) {
						e1.printStackTrace();
					}
				} else {
					errorLog.println("Response too big.");
					EntityUtils.consumeQuietly(entity);
				}
			}
		}
		return result;
	}

	/**
	 * Create the HTTP request.
	 * 
	 * @param filePath         delivery file path
	 * @param originalFilename original file name
	 * @param args             arguments, to make replacements
	 * @param errorLog         output to write errors
	 * @return an HTTP request
	 */
	private HttpUriRequest createRequest(String filePath, String originalFilename, ArrayList<String> args,
			PrintWriter errorLog) {
		List<Entry> parameters = tc.getParameters();
		List<Entry> headers = tc.getHeaders();
		String url = tc.getUrl();
		String requestBody = tc.getRequestBody();
		String fileParameter = tc.getFileParameter();
		// request method, content type do not need replace
		if (replaceRequest) {
			url = replaceRequestTokens(url, args);
			if (fileParameter != null) {
				fileParameter = replaceRequestTokens(fileParameter, args);
			}
			if (requestBody != null) {
				requestBody = replaceRequestTokens(requestBody, args);
			}
			parameters = new ArrayList<Entry>(); //clone 
			for (final Entry e : tc.getParameters()) {
				if (!e.literal) {
					Entry m = new Entry();
					m.key = replaceRequestTokens(e.key, args);
					m.value = replaceRequestTokens(e.value, args);
					parameters.add(m);
				} else {
					parameters.add(e);
				}
			}
			headers = new ArrayList<Entry>(); //clone
			for (final Entry e : tc.getHeaders()) {
				if (!e.literal) {
					Entry m = new Entry();
					m.key = replaceRequestTokens(e.key, args);
					m.value = replaceRequestTokens(e.value, args);
					parameters.add(m);
				} else {
					headers.add(e);
				}
			}
		}
		final HttpUriRequest request = getRequestFromMethod(errorLog, url);

		// add headers
		for (final Entry e : headers) {
			request.addHeader(e.key, e.value);
		}
		if (tc.isJsonResponse()) {
			request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());
		}
		// body
		if (request instanceof HttpEntityEnclosingRequestBase) {
			final HttpEntityEnclosingRequestBase rp = (HttpEntityEnclosingRequestBase) request;
			HttpEntity entity = null;
			ContentType ct;
			if (tc.getContentType() != null) {
				ct = ContentType.parse(tc.getContentType());
			} else if (request.containsHeader(HTTP.CONTENT_TYPE)) {
				ct = ContentType.parse(request.getFirstHeader(HTTP.CONTENT_TYPE).getValue());
			} else {
				// Default value
				ct = ContentType.DEFAULT_TEXT;
			}
			if (fileParameter == null) {
				if (requestBody != null) {
					// use text
					if (!parameters.isEmpty()) {
						createUriParameters(errorLog, parameters, request);
					}
					entity = EntityBuilder.create().setText(requestBody).setContentType(ct).build();
				} else if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(ct.getMimeType())) {
					final List<NameValuePair> nameValuePairs = new ArrayList<>();
					for (final Entry e : parameters) {
						nameValuePairs.add(new BasicNameValuePair(e.key, e.value));
					}
					entity = new UrlEncodedFormEntity(nameValuePairs, ct.getCharset());
				}
			} else if (ContentType.MULTIPART_FORM_DATA.getMimeType().equals(ct.getMimeType())) {
				final MultipartEntityBuilder builder = MultipartEntityBuilder.create()
						.setMode(HttpMultipartMode.BROWSER_COMPATIBLE).setCharset(StandardCharsets.UTF_8);
				for (final Entry e : parameters) {
					builder.addTextBody(e.key, e.value);
				}
				// delivery file
				final File file = new File(filePath);
				String mimeType = URLConnection.guessContentTypeFromName(originalFilename);
				if (mimeType == null) {
					// Try custom mimetype
					if (originalFilename.endsWith(".docx")) {
						mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
					} else if (originalFilename.endsWith(".xlsx")) {
						mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
					}
				}
				if (mimeType == null) {
					builder.addBinaryBody(fileParameter, file, ContentType.APPLICATION_OCTET_STREAM, originalFilename);
				} else {
					builder.addBinaryBody(fileParameter, file, ContentType.create(mimeType), originalFilename);
				}
				entity = builder.build();
			} else {
				// copy file as is
				entity = EntityBuilder.create().setFile(new File(filePath)).setContentType(ct).build();
			}

			rp.setEntity(entity);
		} else if (!parameters.isEmpty()) {
			createUriParameters(errorLog, parameters, request);
		}
		return request;
	}

	/**
	 * Create URI parameters in an HTTP request.
	 * 
	 * @param errorLog   output to write errors
	 * @param parameters list of parameters/values
	 * @param request    HTTP request
	 */
	private void createUriParameters(PrintWriter errorLog, List<Entry> parameters, HttpUriRequest request) {
		final List<NameValuePair> nameValuePairs = new ArrayList<>();
		for (final Entry e : parameters) {
			nameValuePairs.add(new BasicNameValuePair(e.key, e.value));
		}
		try {
			final URI uri = new URIBuilder(request.getURI()).addParameters(nameValuePairs).build();
			((HttpRequestBase) request).setURI(uri);
		} catch (final URISyntaxException e1) {
			errorLog.println("Error updating URI");
		}
	}

	/**
	 * Factory method to get the proper object according to a URL.
	 *
	 * <p>Only GET, POST and PUT are supported
	 *
	 * @param errorLog output to write errors
	 * @param url      an HTTP URL
	 * @return the proper HttpUriRequest object according to a URL
	 */
	private HttpUriRequest getRequestFromMethod(PrintWriter errorLog, String url) {
		// Build request, only GET, POST and PUT are supported
		HttpUriRequest request;
		switch (tc.getRequestMethod()) {
		case "GET":
			request = new HttpGet(url);
			break;
		case "POST":
			request = new HttpPost(url);
			break;
		case "PUT":
			request = new HttpPut(url);
			break;
		default:
			errorLog.println("Method not supported. Using POST by default.");
			request = new HttpPost(url);
			break;
		}
		return request;
	}

	/**
	 * Replaces a text with a map of replacements.
	 * 
	 * <p>Argument expansion:
	 * <ul>
	 * <li><code>${n}</code> where n is a number it is replaced by the argument n
	 * <li><code>${%n}</code> same as before but the value is URLencoded
	 * </ul>
	 *
	 * @param text         text to replace
	 * @param replacements map of possible substitutions
	 * @return text with replacements made
	 */
	public static String replaceResponseTokens(String text, Map<String, String> replacements) {
		final Matcher matcher = pattern.matcher(text);
		final StringBuilder buffer = new StringBuilder();
		while (matcher.find()) {
			// it would also be worth appendReplacement(buffer, replacement), but
			// has problems with certain characters.
			matcher.appendReplacement(buffer, "");
			final String expression = matcher.group(1).trim();
			if (expression.charAt(0) == '%') {
				try {
					buffer.append(URLDecoder.decode(replacements.getOrDefault(expression.substring(1), ""),
							StandardCharsets.UTF_8));
				} catch (final Exception e) {
					buffer.append("[ERROR: ").append(e.getMessage()).append("]");
				}
			} else {
				buffer.append(replacements.getOrDefault(expression, ""));
			}
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	/**
	 * Replaces a text with a list of arguments.
	 *
	 * <p>Argument expansion:
	 * <ul>
	 * <li><code>${n}</code> where n is a number it is replaced by the argument n
	 * <li><code>${%n}</code> same as before but the value is URLencoded
	 * </ul>
	 *
	 * @param text         text to replace
	 * @param replacements arguments
	 * @return text with replacements made
	 */
	public static String replaceRequestTokens(String text, List<String> replacements) {
		final Matcher matcher = pattern.matcher(text);
		final StringBuilder buffer = new StringBuilder();
		while (matcher.find()) {
			// it would also be worth appendReplacement(buffer, replacement), but
			// has problems with certain characters.
			matcher.appendReplacement(buffer, "");
			String expression = matcher.group(1).trim();
			int index = -1;
			boolean encode = false;
			if (expression.charAt(0) == '%') {
				encode = true;
				expression = expression.substring(1);
			}
			try {
				index = Integer.parseInt(expression);
			} catch (final NumberFormatException e1) {
				index = -1;
			}

			if (index >= 0 && index < replacements.size()) {
				String r = replacements.get(index);
				if (encode) {
					try {
						r = URLEncoder.encode(replacements.get(index), StandardCharsets.UTF_8);
					} catch (final Exception e) {
						// ignore
						logger.error("Encode.", e);
					}
				}
				buffer.append(r);
			}
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	/**
	 * Generates the replacement texts with data from the response.
	 *
	 * <p>The following substitutions are allowed:
	 * <ul>
	 * <li><code>${body}</code> full body received in the request.
	 * <li><code>${h.header}</code> header value.
	 * <li><code>${j.property}</code> property of the JSON object (only if
	 * jsonResponse is true).
	 * </ul>
	 *
	 * @param response HTTP response
	 * @return map of replacements
	 */
	private Map<String, String> createResponseReplacements(HttpResponse response) {
		final Map<String, String> replacements = new HashMap<>();
		final Header[] respHeaders = response.getAllHeaders();
		for (final Header h : respHeaders) {
			// If exists, add comma separated to previous value
			final String key = "h." + h.getName();
			final String value = replacements.get(key);
			if (value == null) {
				replacements.put(key, h.getValue());
			} else {
				replacements.replace(key, value + "," + h.getValue());
			}
		}
		String body = "";
		// copy body
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			if (entity.isChunked()) {
				try {
					int max = Settings.getMaxUploadSize() * 1024;
					InputStream instream = entity.getContent();
					ByteArrayOutputStream outstream = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					int len;
					int total = 0;
					while ((len = instream.read(buffer)) > 0 && total < max + 1) {
						outstream.write(buffer, 0, len);
						total += len;
					}
					if (entity.getContentEncoding() == null) {
						body = new String(outstream.toByteArray(), StandardCharsets.UTF_8);
					} else {
						body = new String(outstream.toByteArray(),
								Charset.forName(entity.getContentEncoding().getValue()));
					}
					outstream.close();
					instream.close();
					if (body.length() > Settings.getMaxUploadSize() * 1024) {
						body = "Excessive length";
					}
				} catch (ParseException | IOException e1) {
					// ignore
					logger.error("Body.", e1);
				}
			} else {
				final long len = entity.getContentLength();
				if (len != -1 && len < Settings.getMaxUploadSize() * 1024) {
					try {
						body = EntityUtils.toString(entity);
					} catch (ParseException | IOException e1) {
						// ignore
						logger.error("Body.", e1);
					}
				} else {
					body = "Excessive length";
					EntityUtils.consumeQuietly(entity);
					entity = null;
				}
			}
		}
		replacements.put("body", body);
		if (tc.isJsonResponse() && entity != null
				&& entity.getContentType().getValue().startsWith(ContentType.APPLICATION_JSON.getMimeType())) {
			// extract JSON properties
			createJsonReplacements(replacements, body);
		}
		return replacements;
	}

	/**
	 * Generates the replacement texts with data from the JSON response.
	 *
	 * @param replacements map to which new replacements are added
	 * @param body         body of HTTP response
	 */
	private void createJsonReplacements(Map<String, String> replacements, String body) {
		final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		try {
			final JsonElement json = JsonParser.parseString(body);
			if (json.isJsonObject()) {
				final JsonObject jsonObject = json.getAsJsonObject();
				for (final Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
					if (e.getValue().isJsonPrimitive()) {
						replacements.put("j." + e.getKey(), e.getValue().getAsString());
					} else {
						replacements.put("j." + e.getKey(), gson.toJson(e.getValue()));
					}
				}
			} else if (json.isJsonArray()) {
				final JsonArray jsonArray = json.getAsJsonArray();
				final int size = jsonArray.size();
				for (int i = 0; i < size; i++) {
					replacements.put("j." + i, gson.toJson(jsonArray.get(i)));
				}
			} else if (json.isJsonPrimitive()) {
				replacements.put("j.0", gson.toJson(json));
			}
		} catch (JsonSyntaxException e) {
			logger.error("JsonSyntaxException: {}\nBody:\n|{}|", e.getMessage(), body);
		}
	}
}