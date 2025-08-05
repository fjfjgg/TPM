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

package es.us.dit.lti.entity;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Global application settings.
 *
 * @author Francisco José Fernández Jiménez.
 * @version 1.0
 */
public final class Settings {

	/**
	 * Date and time formatter for instants.
	 *
	 * <p>It is used to download files and generate attempts ID.
	 */
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyyMMdd'T'HHmmss'Z'nnnnnnnnn").withZone(ZoneId.from(ZoneOffset.UTC));
	/**
	 * Extension of the file that saves the results of the assessment.
	 */
	public static final String RESULT_EXT = ".output";
	/**
	 * Extension of the file that saves the errors produced during the assessment.
	 */
	public static final String OUTPUT_ERROR_EXT = ".error";
	/**
	 * Prohibited characters in user and file names.
	 */
	private static final char[] CHAR_INVALIDS = { '\\', '/', '*', '?', '"', '\'', '<', '>', '|', '[', ']', '\'', ';',
			'=', ',', ' ', '$' };
	/**
	 * Character that replaces prohibited characters.
	 */
	private static final char CHAR_VALID = '_';

	/**
	 * Pending message session attribute name.
	 */
	public static final String PENDING_MSG_ATTRIB = "pendingMessage";

	/**
	 * Application name.
	 *
	 * <p>If null or empty, the default value is the message text "T_NOMBRE_LTI".
	 *
	 */
	private static String appName = "";
	/**
	 * Use DbUtilDatasource if true or DbUtilSingleConnection if false.
	 */
	private static boolean datasourceMode = false;
	/**
	 * Folder where tool data is stored.
	 */
	private static String toolsFolder = "";
	/**
	 * Maximum size of files that can be uploaded in kB.
	 *
	 * <p>Each tool can set limits lower than this.
	 */
	private static int maxUploadSize = 10;
	/**
	 * Name of the executable or configuration file of the corrector.
	 *
	 * <p>This file will be stored in the directory of each tool with the data uploaded
	 * by the user.
	 *
	 * <p>"corrector" by default.
	 */
	private static String correctorFilename = "corrector";
	/**
	 * Maximum number of concurrent LTI users/assessments.
	 */
	private static int concurrentUsers = 10;
	/**
	 * This is a string with generic CSS URLs (comma separated list of paths) used
	 * by default in tool web pages.
	 */
	private static String defaultCssPath = "";
	/**
	 * Global notice to show to users.
	 *
	 * <p>For example, to warn that the system will be shut down in the future for
	 * maintenance.
	 */
	private static String notice = null;

	/**
	 * Can not create objects.
	 */
	private Settings() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Initialize settings.
	 *
	 * @param appName           application name
	 * @param datasourceMode    datasource mode
	 * @param toolsFolder       tools folder
	 * @param maxUploadSize     maximum upload size in kB.
	 * @param concurrentUsers   maximum number of concurrent users
	 * @param correctorFilename corrector file name
	 * @param defaultCssPath    default CSS path
	 * @param notice            global notice
	 */
	public static void init(String appName, boolean datasourceMode, String toolsFolder, int maxUploadSize,
			int concurrentUsers, String correctorFilename, String defaultCssPath, String notice) {
		Settings.appName = appName;
		Settings.datasourceMode = datasourceMode;
		Settings.toolsFolder = toolsFolder;
		Settings.maxUploadSize = maxUploadSize;
		Settings.concurrentUsers = concurrentUsers;
		Settings.correctorFilename = correctorFilename;
		Settings.defaultCssPath = defaultCssPath;
		Settings.notice = notice;
	}

	/**
	 * Creates string with settings values.
	 *
	 * @return string with settings values
	 */
	public static String printString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("\nAPP_NAME: [" + appName + "]\n");
		sb.append("DATASOURCE_MODE: [" + datasourceMode + "]\n");
		sb.append("TOOLS_FOLDER: [" + toolsFolder + "]\n");
		sb.append("MAX_UPLOAD_SIZE: [" + maxUploadSize + "]\n");
		sb.append("CONCURRENT_USERS: [" + concurrentUsers + "]\n");
		sb.append("CORRECTOR_FILENAME: [" + correctorFilename + "]\n");
		sb.append("DEFAULT_CSS_PATH: [" + defaultCssPath + "]\n");
		sb.append("NOTICE: [" + notice + "]\n");
		return sb.toString();
	}

	/**
	 * Sanitizes a string replacing invalid characters.
	 *
	 * @param original original string
	 * @return sanitized string
	 */
	public static String sanitizeString(String original) {

		for (final char element : CHAR_INVALIDS) {
			if (-1 != original.indexOf(element)) {
				original = original.replace(element, CHAR_VALID);
			}
		}
		return original;
	}

	/**
	 * Gets the application name.
	 *
	 * @return the application name
	 */
	public static String getAppName() {
		return appName;
	}

	/**
	 * Sets the application name.
	 *
	 * <p>If null or empty, the default value is the message text "T_NOMBRE_LTI".
	 *
	 * @param appName the application name to set
	 */
	public static void setAppName(String appName) {
		Settings.appName = appName;
	}

	/**
	 * Gets the tools folder.
	 *
	 * <p>Folder where tool data is stored.
	 *
	 * @return the tools folder
	 */
	public static String getToolsFolder() {
		return toolsFolder;
	}

	/**
	 * Sets the tools folder.
	 *
	 * @param toolsFolder the tools folder to set
	 */
	public static void setToolsFolder(String toolsFolder) {
		Settings.toolsFolder = toolsFolder;
	}

	/**
	 * Gets the maximum size of files that can be uploaded in kB.
	 *
	 * <p>Each tool can set limits lower than this.
	 *
	 * @return the maxUploadSize
	 */
	public static int getMaxUploadSize() {
		return maxUploadSize;
	}

	/**
	 * Sets the maximum size of files that can be uploaded in kB.
	 *
	 * @param maxUploadSize the maxUploadSize to set
	 */
	public static void setMaxUploadSize(int maxUploadSize) {
		Settings.maxUploadSize = maxUploadSize;
	}

	/**
	 * Gets the name of the executable or configuration file of the corrector.
	 *
	 * @return the corrector filename
	 */
	public static String getCorrectorFilename() {
		return correctorFilename;
	}

	/**
	 * Sets the name of the executable or configuration file of the corrector.
	 *
	 * <p>This file will be stored in the directory of each tool with the data uploaded
	 * by the user.
	 *
	 * @param correctorFilename the corrector filename to set
	 */
	public static void setCorrectorFilename(String correctorFilename) {
		Settings.correctorFilename = correctorFilename;
	}

	/**
	 * Gets the maximum number of concurrent LTI users/assessments.
	 *
	 * @return the maximum concurrent users
	 */
	public static int getConcurrentUsers() {
		return concurrentUsers;
	}

	/**
	 * Sets the maximum number of concurrent LTI users/assessments.
	 *
	 * @param concurrentUsers the maximum concurrent users to set
	 */
	public static void setConcurrentUsers(int concurrentUsers) {
		Settings.concurrentUsers = concurrentUsers;
	}

	/**
	 * Gets a string with generic CSS URLs (comma separated list of paths) used by
	 * default in tool web pages.
	 *
	 * @return the default CSS path
	 */
	public static String getDefaultCssPath() {
		return defaultCssPath;
	}

	/**
	 * Sets a string with generic CSS URLs (comma separated list of paths) used by
	 * default in tool web pages.
	 *
	 * @param defaultCssPath the default CSS path to set
	 */
	public static void setDefaultCssPath(String defaultCssPath) {
		Settings.defaultCssPath = defaultCssPath;
	}

	/**
	 * Gets the global notice to show to users.
	 *
	 * @return the notice
	 */
	public static String getNotice() {
		return notice;
	}

	/**
	 * Sets the global notice to show to users.
	 *
	 * @param notice the notice to set
	 */
	public static void setNotice(String notice) {
		Settings.notice = notice;
	}

	/**
	 * Gets the datasource mode.
	 *
	 * <p>Use DbUtilDatasource if true or DbUtilSingleConnection if false.
	 *
	 * @return the datasourceMode
	 */
	public static boolean isDatasourceMode() {
		return datasourceMode;
	}

}
