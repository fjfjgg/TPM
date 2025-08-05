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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.MessageMap;
import es.us.dit.lti.OutcomeService;
import es.us.dit.lti.SecurityUtil;
import es.us.dit.lti.ToolSession;
import es.us.dit.lti.config.ToolUiConfig;
import es.us.dit.lti.entity.Attempt;
import es.us.dit.lti.entity.LtiUser;
import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.entity.ToolKey;
import es.us.dit.lti.persistence.MgmtUserDao;
import es.us.dit.lti.persistence.ToolAttemptDao;
import es.us.dit.lti.persistence.ToolDao;
import es.us.dit.lti.runner.ToolRunner;
import es.us.dit.lti.runner.ToolRunnerFactory;
import es.us.dit.lti.runner.ToolRunnerType;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import jakarta.servlet.jsp.JspFactory;
import jakarta.servlet.jsp.PageContext;

/**
 * Servlet that processes an assessment or redirect attempt.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet({ "/learner/assess" })
@MultipartConfig(fileSizeThreshold = 1024 * 1024,
	maxFileSize = 1024 * 1024 * 5, 
	maxRequestSize = 1024 * 1024 * 5 * 5)
public class AssessServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(AssessServlet.class);
	/**
	 * Maximum file name length.
	 */
	private static final int MAX_FILENAME_LENGTH = 255;
	/**
	 * Error code if assessment was successful and no outcome must be written.
	 */
	private static final int OK_WITHOUT_OUTCOME = 0;
	/**
	 * Error code if assessment was successful and outcome was written in consumer.
	 */
	private static final int OK_WITH_OUTCOME = 1;
	/**
	 * Default grace period to add to delivery deadline.
	 */
	private static final int DEFAULT_GRACE_TIME = 5;

	/**
	 * Number of concurrent users being assessed.
	 */
	private final SortedSet<String> concurrentUsers = Collections.synchronizedSortedSet(new TreeSet<String>());
	
	
	/**
	 * Processes an assessment or redirect attempt.
	 *
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		PrintWriter out;
		try {
			out = response.getWriter();
		} catch (final IOException e2) {
			logger.error("Error getting writer");
			return;
		}
		final HttpSession session = request.getSession();
		final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
		final String userId = ts.getSessionUserId();
		final Tool tool = ts.getTool();
		final boolean isInstructor = ts.isInstructor();
		final MessageMap text = (MessageMap) session.getAttribute("text");
		boolean isReassessment = false;
		boolean maxConcurrencyOnlyStoreMode = false;
		
		if (tool != null && userId != null && (ts.isLearner() || isInstructor)) {

			// Use of streaming API
			final ToolUiConfig tui = tool.getToolUiConfig();
			boolean error = false;
			boolean validDeliveryPassword = false;
			if (tool.getDeliveryPassword() == null || tool.getDeliveryPassword().isEmpty()) {
				// Do not verify
				validDeliveryPassword = true;
			}
			int maxUploadSize = Settings.getMaxUploadSize();
			if (tui.getInputFileSize() == null || tui.getInputFileSize() == 0
					|| tui.getInputFileSize() > Settings.getMaxUploadSize()) {
				tui.setInputFileSize(maxUploadSize);
			} else {
				maxUploadSize = tui.getInputFileSize();
			}

			Iterator<Part> iter = null;
			if (tool.isEnabled()
					&& (isInstructor || tool.isEnabledByDate(Calendar.getInstance(), DEFAULT_GRACE_TIME))) {
				try {
					// Parse the request
					iter = request.getParts().iterator();
				} catch (final Exception e) {
					error = true;
					out.println(formatError(text.get("T_ERROR_TAM_SUBIDA") + ": " + maxUploadSize + "kB"));
				}
			} else {
				error = true;
				logger.warn("Tool {} User {}: The tool has been disabled.", tool.getName(), userId);
				out.println(formatError(text.get("T_AVISO_DESHABILITADA")));
			}

			// Init values and previous checks
			if (!error) {
				// Check concurrent users
				synchronized (concurrentUsers) {
					try {
						if (addConcurrentUser(userId, tool)) {
							request.setAttribute("concurrence", concurrentUsers.size());
						} else {
							out.println(formatError(text.get("T_ERROR_CONCURRENCIA_MAXIMA")));
							logger.error("Max. concurrent users: {}", concurrentUsers.size());
							if (tui.isKeepFiles()
									&& tool.getEnabledUntil() != null
									&& tool.getToolUiConfig().getMaxConcurrentUsers() != 0 
									&& !tool.isEnabledByDate(Calendar.getInstance(), -DEFAULT_GRACE_TIME * 2)) {
								// If the remaining time is short and the files must be saved, we activate this
								// mode (if global limit is not reached)
								if (addConcurrentUserIgnoringTool(userId, tool.getName())) {
									maxConcurrencyOnlyStoreMode = true;
									logger.info("Max Concurrency Only Store Mode ON: {}", userId);
								}
							} else {
								error = true;
							}
						}
					} catch (final ConcurrentModificationException e) {
						out.println(formatError(text.get("T_ERROR_CORRECCION_SIMULTANEA")));
						logger.error("Concurrent delivery: {}", userId);
						error = true;
					}
				}
			}

			if (!error) {
				Part item = null;

				// First item must be launchId
				boolean validated = false;
				try {
					if (iter.hasNext()) {
						item = iter.next();
						if (item.getName().equals("launchId")) {
							//Not file
							final String receivedLaunchId = new String(item.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
							final String launchId = ts.getLaunchId();
							if (receivedLaunchId.equals(launchId)) {
								validated = true;
							}
						}
					}
				} catch (final Exception e) {
					validated = false;
				}
				if (!validated) {
					error = true;
					out.println(formatError(text.get("T_ERROR_CORRECCION_SIMULTANEA")));
				}

				// Save attempt in db
				final Attempt attempt = new Attempt();
				attempt.setInstant(Instant.now());
				attempt.setResourceUser(ts.getLtiResourceUser());
				attempt.setOriginalResourceUser(ts.getLtiResourceUser());
				String userFilePath = null;
				String outputPath = null;
				String filename = null;
				// TODO storageType for now the default is used
				if (!error) {

					try {
						while (iter.hasNext() && filename == null) {
							item = iter.next();

							// File or field
							if (item.getContentType() == null) {
								//not file
								if (item.getName().equals("password")) {
									final ByteArrayOutputStream baos = new ByteArrayOutputStream();
									item.getInputStream().transferTo(baos);
									baos.close();
									if (tool.getDeliveryPassword().equals(baos.toString(StandardCharsets.UTF_8))) {
										validDeliveryPassword = true;
									} else if (!validDeliveryPassword) {
										logger.warn("Tool {} User {}: incorrect delivery password", tool.getName(), userId);
									}
								} else if (item.getName().equals("sid") && isInstructor
										&& tui.isManageAttempts()) {
									// A assessment is made to a previously delivered file.
									final ByteArrayOutputStream baos = new ByteArrayOutputStream();
									item.getInputStream().transferTo(baos);
									baos.close();
									final String cipheredSid = URLDecoder.decode(baos.toString(StandardCharsets.UTF_8),
											StandardCharsets.UTF_8);
									// Get original data
									final Attempt originalAttempt = ToolAttemptDao.getBySecuredSid(cipheredSid,
											ts.getToolKey());
									if (originalAttempt != null) {
										// The input is taken from the original file
										originalAttempt.getResourceUser().getResourceLink().setTool(tool);
										// Copy info from original
										attempt.setOriginalResourceUser(originalAttempt.getResourceUser());
										attempt.setFileName(originalAttempt.getFileName());
										attempt.setInstant(originalAttempt.getInstant());
										userFilePath = originalAttempt.getUserFilePath(); // original
										final File archivoServer = new File(userFilePath);
										if (archivoServer.exists() && archivoServer.isFile()) {
											// We create the directory structure of the user
											final File folder = new File(attempt.getUserFolderPath());
											if (!folder.exists() && !folder.mkdirs()) {
												logger.error("mkdir {}", folder.getAbsolutePath());
											}
											// The output is saved in the instructor's directory
											outputPath = attempt.getCorrectorResultPath(attempt.getId());
											filename = originalAttempt.getFileName();
											// Copy sid in case user is the original user, because no new attempt will
											// be created
											attempt.setSid(originalAttempt.getSid());
											isReassessment = true;
										} else {
											userFilePath = null;
										}
									} else {
										userFilePath = null;
									}
								}
							} else if (validDeliveryPassword) {
								filename = item.getSubmittedFileName();
								filename = Settings.sanitizeString(filename);
								attempt.setFileName(filename);
								// Create user folders
								final File folder = new File(attempt.getUserFolderPath());
								if (!folder.exists() && !folder.mkdirs()) {
									logger.error("mkdir {}", folder.getAbsolutePath());
								}

								if (tui.getMaxAttempts() >= 0
										&& tui.getMaxAttempts() <= getCurrentAttempts(ts.getLtiResourceUser().getUser(),
												ts.getToolKey(), filename, tui.isMaxAttemptsDependsOnFilenames())) {
									out.println(formatError(text.get("T_ERROR_MAX_INTENTOS")));
								} else if (item.getSize() / 1024 > maxUploadSize) {
									out.println(
											formatError(text.get("T_ERROR_TAM_SUBIDA") + ": " + maxUploadSize + "kB"));
								} else if (checkFilename(tui, filename, isInstructor)) {
									userFilePath = copyReceivedFile(attempt, item);
									attempt.setFileSaved(true);
									if (userFilePath == null) {
										session.setAttribute("errorMessage", text.get("T_ERROR_IO"));
										request.getRequestDispatcher("./error.jsp").forward(request, response);
									} else {
										outputPath = attempt.getCorrectorResultPathFromFile(userFilePath);
									}
								} else {
									out.println(formatError(text.get("T_ERROR_NOMBRE_FICHERO")));
								}
							}

						}
					} catch (final Exception e1) {
						e1.printStackTrace();
						session.setAttribute("errorMessage", text.get("T_ERROR_PETICION_INCORRECTA"));
						forward(request, response, "./error.jsp");
					}
				}
				if (!validDeliveryPassword) {
					out.println(formatError(text.get("T_ERROR_AUTORIZACION")));
				} else if (userFilePath != null && filename != null) {
					int scoreInt = 1000;
					final boolean nocal = !ts.isOutcomeAllowed();
					final File resultFile = new File(outputPath);
					if (maxConcurrencyOnlyStoreMode) {
						scoreInt = ToolRunner.ERROR_CONCURRENT_EXCEPTION;
						// this modo is only enabled if keep files is enabled.
						attempt.setOutputSaved(false);
					} else {
						final ToolRunner executer = getToolRunner(tool);
						if (executer != null) {
							// Assess
							final int counter = ToolDao.incrementCounter(tool);
							// Add extra arguments
							final List<String> extraArgs = generateExtraArguments(request, ts);
	
							logger.info("{}:{} > {} > concurrence={}", tool.getName(), counter, userId,
									concurrentUsers.size());
							// This can also be done asynchronously. Keep it in mind in the future.
							scoreInt = executer.exec(userFilePath, outputPath, userId, filename, counter, isInstructor,
									extraArgs, 60); // 1 minute max correction
							logger.info("{}:{} > {} > result={}", tool.getName(), counter, userId, scoreInt);
							
							// Output of execution
							if (resultFile.exists() && resultFile.length() > 0) {
								attempt.setOutputSaved(true);
							}
							// Delete unnecessary files
							try {
								if (!tui.isKeepFiles() && !tui.isKeepOutput()) {
									clean(outputPath); // Clean error output
									if (!new File(userFilePath).delete()) {
										logger.error("Error deleting files");
									}
									attempt.setFileSaved(false);
									attempt.setOutputSaved(false);
								} else if (tui.isEnableInstructorCommand() && filename.equals(tui.getCommandFilename())
										&& isInstructor || scoreInt > 100) {
									// command or execution error, delete files and output
									// if isReassessment do not delete original file
									if (!isReassessment && !new File(userFilePath).delete()) {
										logger.error("Error deleting delivery file");
									} else {
										attempt.setFileSaved(false);
									}
									clean(outputPath); // Clean error output
									attempt.setOutputSaved(false);
								} else if (!tui.isKeepFiles() || !tui.isKeepOutput()) {
									// not keep all
									if (tui.isKeepFiles()) {
										// keep files, delete error output
										clean(outputPath); // Clean error output
										attempt.setOutputSaved(false);
									} else if (tui.isKeepOutput() && !new File(userFilePath).delete()) {
										// keep output, delete files
										logger.error("Error deleting files");
									} else {
										attempt.setFileSaved(false);
									}
								}
							} catch (final Exception e) {
								logger.info("Some file could not be deleted.");
							}
						}
					}

					if (scoreInt == ToolRunner.ERROR_CORRECTOR_EXCEPTION) {
						// Error in corrector (settings, params, servers...)
						out.println(formatError(text.get("T_ERROR_CORRECTOR_EXCEPTION")));
						attempt.setErrorCode(scoreInt);
					} else if (scoreInt == ToolRunner.ERROR_RUNNER_EXCEPTION) {
						// Exception in ToolRunner
						out.println(formatError(text.get("T_ERROR_RUNNER_EXCEPTION")));
						attempt.setErrorCode(scoreInt);
					} else if (scoreInt == ToolRunner.ERROR_TIMEOUT) {
						// Timeout
						out.println(formatError(text.get("T_ERROR_TIMEOUT")));
						attempt.setErrorCode(scoreInt);
					} else if (maxConcurrencyOnlyStoreMode) {
						out.println(formatError(text.get("T_ERROR_CONCURRENT_EXCEPTION")));
						out.println(formatError("ID=" + attempt.getId()));
						attempt.setErrorCode(scoreInt);
					} else if (scoreInt >= ToolRunner.ERROR_GENERIC) {
						// Unknown error
						out.println(formatError(text.get("T_ERROR_GENERIC") + " " + scoreInt));
						attempt.setErrorCode(scoreInt);
					} else if (!nocal && ts.getLtiResourceUser() != null) {
						attempt.setScore(scoreInt);
						if (isInstructor) {
							// Instructor is only testing
							out.println("<p id='score'><strong>(TEST) " + text.get("T_NOTA") + ":</strong> " 
									+ String.format("%.1f", scoreInt * 0.1)
									+ "</p>");
							attempt.setErrorCode(OK_WITHOUT_OUTCOME);
						} else if (OutcomeService.writeOutcome(ts.getLtiResourceUser(), ts.getToolKey(),
								String.valueOf(scoreInt * 0.01))) {
							attempt.setErrorCode(OK_WITH_OUTCOME);
							out.println("<p id='score'><strong>" + text.get("T_NOTA") + ":</strong> "
									+ String.format("%.1f", scoreInt * 0.1)
									+ "</p>");
						} else {
							out.println(formatError(text.get("T_ERROR_WRITE_OUTCOME")));
							attempt.setErrorCode(ToolRunner.ERROR_WRITE_OUTCOME);
						}
					} else {
						attempt.setScore(scoreInt);
						attempt.setErrorCode(OK_WITHOUT_OUTCOME);
					}
					// Create attempt if not exist
					if (!isReassessment) {
						ToolAttemptDao.create(attempt);
					} else if (attempt.getResourceUser().getSid() != attempt.getOriginalResourceUser().getSid()) {
						// Check if exist
						final Attempt aux = ToolAttemptDao.getById(attempt.getResourceUser().getSid(),
								attempt.getInstant());
						if (aux == null) {
							ToolAttemptDao.create(attempt);
						}
					}
					// Send output
					if (resultFile.exists() && resultFile.length() > 0) {
						if (!tui.isRedirectMode()) {
							// Generate iframe
							String secureId = SecurityUtil.getSecureSid(attempt);
							out.println(
									"<div class='resizer'><iframe class='resized outputframe' src='attempt/"
											+ URLDecoder.decode(attempt.getResourceUser().getUser().getSourceId(),
													StandardCharsets.UTF_8)
											+ "/output/" + secureId + "'></iframe></div>");
						} else {
							// Copy to response
							try (BufferedInputStream br = new BufferedInputStream(new FileInputStream(resultFile));) {
								WriterOutputStream wos = WriterOutputStream.builder().setWriter(out)
										.setCharset(StandardCharsets.UTF_8).get();
								br.transferTo(wos);
								wos.flush();
							} catch (final IOException e) {
								out.println(formatError(text.get("T_ERROR_IO")));
							}

							if (!attempt.isOutputSaved() && !resultFile.delete()) {
								logger.error("Error deleting file");
							}
						}
					}
				}

				removeConcurrentUser(userId, concurrentUsers, tool);
			}
		}
	}

	/**
	 * Forwards request capturing exceptions.
	 *
	 * @param request    HTTP request
	 * @param response   HTTP response
	 * @param forwardUrl forward URL
	 */
	private void forward(HttpServletRequest request, HttpServletResponse response, String forwardUrl) {
		try {
			request.getRequestDispatcher(forwardUrl).forward(request, response);
		} catch (ServletException | IOException e) {
			logger.error("Error al reenviar");
		}
	}

	/**
	 * Adds user to concurrent users set.
	 *
	 * @param userId          user name
	 * @param concurrentUsers set of concurrent users
	 * @param tool            the used tool
	 * @return true if successful
	 * @throws ConcurrentModificationException if could not add to the set
	 */
	private boolean addConcurrentUser(String userId, Tool tool)
			throws ConcurrentModificationException {
		final int numAlumnos = concurrentUsers.size();
		boolean result = false;
		// Check global limits
		if (numAlumnos < Settings.getConcurrentUsers()) {
			// Check tool limits
			final int max = tool.getToolUiConfig().getMaxConcurrentUsers();
			if (max < 0 || tool.getConcurrentUsers().size() < max) {
				// add to set
				if (!concurrentUsers.add(tool.getName() + ":" + userId)) {
					throw new ConcurrentModificationException(); // This shouldn't happen
				} else {
					tool.getConcurrentUsers().add(userId);
					result = true;
				}
			}
		}
		return result;
	}
	
	/**
	 * Adds user to concurrent users set (ignoring tool).
	 *
	 * @param userId          user name
	 * @param concurrentUsers set of concurrent users
	 * @param toolName        the used tool name
	 * @return true if successful
	 * @throws ConcurrentModificationException if could not add to the set
	 */
	private boolean addConcurrentUserIgnoringTool(String userId, String toolName)
			throws ConcurrentModificationException {
		final int numAlumnos = concurrentUsers.size();
		boolean result = false;
		// Check global limits
		if (numAlumnos < Settings.getConcurrentUsers()) {
			// Check tool limits
			// add to set
			if (!concurrentUsers.add(toolName + ":" + userId)) {
				throw new ConcurrentModificationException(); // This shouldn't happen
			} else {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Removes user from concurrent user set.
	 *
	 * @param userId              user name
	 * @param alumnosConcurrentes set of concurrent users
	 * @param tool                the used tool
	 */
	private void removeConcurrentUser(String userId, Set<String> alumnosConcurrentes, Tool tool) {
		alumnosConcurrentes.remove(tool.getName() + ":" + userId);
		tool.getConcurrentUsers().remove(userId);
	}

	/**
	 * Copies received file to final destination.
	 *
	 * @param attempt attempt data
	 * @param item    data stream
	 * @return the saved file path
	 */
	private String copyReceivedFile(Attempt attempt, Part item) {
		String userFilePath;
		userFilePath = attempt.getUserFilePath();
		final File archivoServer = new File(userFilePath);
		// Copy
		try (BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(archivoServer));
				BufferedInputStream bin = new BufferedInputStream(item.getInputStream());) {
			bin.transferTo(bout);
		} catch (final IOException e) {
			userFilePath = null;
		}
		return userFilePath;
	}

	/**
	 * Gets the tool runner that is associated with the tool.
	 *
	 * @param tool tool whose tool runner is requested
	 * @return the tool runner or null if not possible
	 */
	public static ToolRunner getToolRunner(Tool tool) {
		ToolRunner executer = tool.getToolRunner();
		if (executer == null) {
			final MgmtUser admin = MgmtUserDao.get(ToolDao.getAdmin(tool));
			if (admin != null) {
				if (!admin.isLocal() && tool.getToolType() == ToolRunnerType.TR_LOCAL) {
					// Not allowed
					logger.info("ToolRunner: Local execute permission not allowed: user={}", admin.getUsername());
				} else {
					// Create
					executer = ToolRunnerFactory.fromType(tool.getToolType());
					if (executer != null) {

						executer.init(tool.getCorrectorPath(), admin.getExecutionRestrictions());
						tool.setToolRunner(executer);
					}
				}
			}
		}
		return executer;
	}

	/**
	 * Checks the file name (length, authorized pattern).
	 *
	 * @param tui          the tool extra configuration
	 * @param filename     the file name
	 * @param isInstructor if user is an instructor
	 * @return true if successful
	 */
	private boolean checkFilename(ToolUiConfig tui, String filename, boolean isInstructor) {
		boolean filenameOk = false;
		// Check file name based on patterns
		if (tui.isEnableInstructorCommand() && filename.equals(tui.getCommandFilename()) && isInstructor) {
			// command file
			filenameOk = true;
		} else if (tui.isEnableSendText() && filename.equals(tui.getTextFilename())) {
			// file with form text
			filenameOk = true;
		} else if (filename.length() <= MAX_FILENAME_LENGTH) {
			final String pattern = tui.getInputFilePattern();
			if (pattern == null || Pattern.matches(pattern, filename)) {
				// any name accepted or matches pattern
				filenameOk = true;
			}
		}
		return filenameOk;
	}

	/**
	 * Print info about possible extra arguments.
	 *
	 * @param request HTTP request
	 */
	private void printDebug(HttpServletRequest request) {
		final StringBuilder sb = new StringBuilder();
		// List all accessible parameters
		sb.append("Tool properties (ts.tool): name, description, " + "deliveryPassword, enabled, enabledFrom, enabledUntil, "
				+ "outcome, extraArgs, counter\n");
		sb.append("Request attributes: ");
		Enumeration<String> names = request.getAttributeNames();
		while (names.hasMoreElements()) {
			sb.append(names.nextElement() + ", ");
		}
		sb.append("\n");
		sb.append("Session attributes: ");
		names = request.getSession().getAttributeNames();
		while (names.hasMoreElements()) {
			sb.append(names.nextElement() + ", ");
		}
		sb.append("\n");
		sb.append("Application attributes: ");
		names = request.getServletContext().getAttributeNames();
		while (names.hasMoreElements()) {
			sb.append(names.nextElement() + ", ");
		}
		sb.append("\n");
		logger.info("{}", sb);
	}

	/**
	 * Generates the extra arguments using Expression Language.
	 *
	 * @param request HTTP request
	 * @param ts      the tool session
	 * @return list of extra arguments
	 */
	private List<String> generateExtraArguments(HttpServletRequest request, ToolSession ts) {
		final ArrayList<String> extraArgs = new ArrayList<>();
		final Tool tool = ts.getTool();
		if (tool.getExtraArgs() != null && !tool.getExtraArgs().isEmpty()) {
			final JspFactory jspFactory = JspFactory.getDefaultFactory();
			final PageContext pageContext = jspFactory.getPageContext(this, request, null, null, true, 0, true);
			final ExpressionFactory ef = jspFactory
					.getJspApplicationContext(pageContext.getRequest().getServletContext()).getExpressionFactory();
			final ELContext elContext = pageContext.getELContext();
			request.setAttribute("ts", ts);

			if (tool.getExtraArgs().equals("debug")) {
				printDebug(request);
			}
			// Separate by commas
			for (String token : tool.getExtraArgs().split(",")) {
				// Cleaning spaces before and after
				token = token.trim();
				// Replace tokens
				if (token.equals("${custom_args}")) {
					// Add custom_args
					String customArgs = null;
					if (ts.getResourceLink() != null) {
						customArgs = ts.getResourceLink().getCustomProperty("custom_args");
					}
					if (customArgs != null && !customArgs.isEmpty()) {
						// Separate by commas
						for (final String token2 : customArgs.split(",")) {
							// Cleaning spaces before and after
							extraArgs.add(token2.trim());
						}
					}
				} else {
					try {
						extraArgs.add(
								(String) ef.createValueExpression(elContext, token, String.class).getValue(elContext));
					} catch (final Exception e) {
						logger.error("Error processing extraArgs {}: {}", token, e.getMessage());
						extraArgs.add("");
					}
				}
			}
		}
		return extraArgs;
	}

	/**
	 * Gets the number of attempts of a user.
	 *
	 * @param user              the LTI user
	 * @param tk                the tool key
	 * @param filename          the file name
	 * @param dependsOnFilename if attempts depend on file names
	 * @return the current number of attempts
	 */
	private int getCurrentAttempts(LtiUser user, ToolKey tk, String filename, boolean dependsOnFilename) {
		int attempts = 0;
		if (dependsOnFilename) {
			attempts = ToolAttemptDao.countUserAttempts(user, tk, filename);
		} else {
			attempts = ToolAttemptDao.countUserAttempts(user, tk);
		}
		return attempts;
	}

	/**
	 * Format a error string in HTML.
	 *
	 * @param error the error string
	 * @return HTML formatted string
	 */
	private String formatError(String error) {
		return "<p class='error'>" + error + "</p>";
	}
	
	/**
	 * Clean output error file but do not delete output file yet.
	 *
	 * @param outputPath output file path
	 */
	private void clean(String outputPath) {
		final File outputErr = new File(outputPath + Settings.OUTPUT_ERROR_EXT);
		if (outputErr.exists() && !outputErr.delete()) {
			logger.error("Error deleting output.error");
		}
	}
}
