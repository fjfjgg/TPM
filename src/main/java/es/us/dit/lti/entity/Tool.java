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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.config.ToolUiConfig;
import es.us.dit.lti.runner.ToolRunner;
import es.us.dit.lti.runner.ToolRunnerType;
import es.us.dit.lti.servlet.UploadedFile;

/**
 * Tool data.
 *
 * @author Francisco José Fernández Jiménez
 * @version 1.0
 */
public class Tool extends UpdateRecordEntity {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 6218906299492469589L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * Logger.
	 */
	private final transient Logger logger = LoggerFactory.getLogger(getClass());
	/**
	 * Added suffix to file backups.
	 */
	private static final String FILE_BACKUP_SUFFIX = "~";

	/**
	 * Name.
	 */
	private String name;
	/**
	 * Internal description, used in management tasks.
	 */
	private String description;
	/**
	 * Password that users must provide in order to make a delivery.
	 */
	private String deliveryPassword;
	/**
	 * Whether the tool is enabled or not.
	 */
	private volatile boolean enabled;
	/**
	 * If the tool is enabled, enable start date.
	 */
	private Calendar enabledFrom = null;
	/**
	 * If the tool is enabled, enable end date.
	 */
	private Calendar enabledUntil = null;
	/**
	 * If the writing of scores/outcomes on the consumer is allowed.
	 */
	private volatile boolean outcome;
	/**
	 * Extra arguments to pass to the corrector.
	 */
	private String extraArgs;
	/**
	 * Assessment counter.
	 */
	private volatile int counter;
	/**
	 * MgmtUserType code, used in a user's tool listings.
	 */
	private volatile int userTypeCode;
	/**
	 * Tool runner userTypeCode.
	 */
	private ToolRunnerType toolType;
	/**
	 * Serialized toolUiConfig.
	 */
	private String jsonConfig;
	/**
	 * Properties of a tool that you do not want to store separately in the
	 * database.
	 */
	private ToolUiConfig toolUiConfig;
	/**
	 * Tool runner to assess.
	 */
	private transient ToolRunner toolRunner;
	/**
	 * LTI usernames that are concurrently performing assessments.
	 */
	private transient SortedSet<String> concurrentUsers;

	// Paths
	/**
	 * Tool folder path.
	 */
	private String path;
	/**
	 * Tool data folder path.
	 */
	private String dataPath;
	/**
	 * Corrector executable/configuration file path.
	 */
	private String correctorPath;
	/**
	 * Tool description HTML file for users.
	 */
	private String descriptionPath;
	/**
	 * Extra ZIP file path with the files referenced by the description file.
	 */
	private String extraZipPath;
	/**
	 * Tool extra folder path with the files referenced by the description file.
	 */
	private String extraPath;

	/**
	 * Gets the MgmtUserType code, used in a user's tool listings.
	 *
	 * @return the MgmtUserType code of current user
	 */
	public int getUserTypeCode() {
		return userTypeCode;
	}

	/**
	 * Sets the MgmtUserType code.
	 *
	 * @param userTypeCode new value
	 */
	public void setUserTypeCode(int userTypeCode) {
		this.userTypeCode = userTypeCode;
	}

	/**
	 * Gets the tool name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the tool name.
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the delivery password.
	 *
	 * <p>Password that users must provide in order to make a delivery.
	 *
	 * @return the delivery password
	 */
	public String getDeliveryPassword() {
		return deliveryPassword;
	}

	/**
	 * Sets the delivery password.
	 *
	 * @param deliveryPassword the delivery password to set
	 */
	public void setDeliveryPassword(String deliveryPassword) {
		this.deliveryPassword = deliveryPassword;
	}

	/**
	 * Gets whether the tool is enabled or not.
	 *
	 * @return true if tool is enabled
	 */
	public boolean isEnabled() {
		return enabled;

	}
	
	/**
	 * Gets whether the tool is enabled or not by date.
	 *
	 * <p>This takes into account the start and end dates.
	 *
	 * @return true if tool is enabled
	 */
	public boolean isEnabledByDate() {
		final Calendar now = Calendar.getInstance();
		return enabled && (enabledFrom == null || !enabledFrom.after(now))
				&& (enabledUntil == null || enabledUntil.after(now));

	}
	
	/**
	 * Gets whether the tool is enabled or not by date using the passed calendar and
	 * grace period.
	 *
	 * <p>This takes into account the start and end dates.
	 *
	 * @param requestDate        date of request as a calendar object
	 * @param gracePeriodSeconds number of extra seconds allowed
	 * @return true if tool is enabled
	 */
	public boolean isEnabledByDate(Calendar requestDate, int gracePeriodSeconds) {
		boolean res = enabled && (enabledFrom == null || !enabledFrom.after(requestDate));
		if (enabledUntil != null) {
			final Calendar enabledUtilWithGracePeriod = (Calendar) enabledUntil.clone();
			enabledUtilWithGracePeriod.add(Calendar.SECOND, gracePeriodSeconds);
			res = res && enabledUtilWithGracePeriod.after(requestDate);
		}
		return res;
	}

	/**
	 * Gets whether the tool can be enabled as long as the dates are appropriate.
	 *
	 * @return true if enableable
	 */
	public boolean isEnableable() {
		return enabled;
	}

	/**
	 * Sets whether the tool can be enabled.
	 *
	 * @param enable the value to set
	 */
	public void setEnabled(boolean enable) {
		enabled = enable;
	}

	/**
	 * Gets the enable start date, if the tool is enabled.
	 *
	 * <p>A null value indicates that there is no limitation.
	 *
	 * @return the enable start date
	 */
	public Calendar getEnabledFrom() {
		Calendar calendar = null;
		if (enabledFrom != null) {
			calendar = (Calendar) enabledFrom.clone();
		}
		return calendar;
	}

	/**
	 * Sets the enable start date.
	 *
	 * @param enabledFrom the enable start date to set
	 */
	public void setEnabledFrom(Calendar enabledFrom) {
		if (enabledFrom != null) {
			this.enabledFrom = (Calendar) enabledFrom.clone();
		} else {
			this.enabledFrom = null;
		}
	}

	/**
	 * Gets the enable end date, if the tool is enabled.
	 *
	 * <p>A null value indicates that there is no limitation.
	 *
	 * @return the enable end date
	 */
	public Calendar getEnabledUntil() {
		Calendar calendar = null;
		if (enabledUntil != null) {
			calendar = (Calendar) enabledUntil.clone();
		}
		return calendar;
	}
	
	/**
	 * Gets the remaining time to disable the tool.
	 * 
	 * @return the time left
	 */
	public Long getTimeLeft() {
		if (enabledUntil != null) {
			final Calendar now = Calendar.getInstance();
			return enabledUntil.getTimeInMillis() - now.getTimeInMillis();
		} else {
			return null;
		}
	}

	/**
	 * Sets the enable end date.
	 *
	 * @param enabledUntil the enable end date to set
	 */
	public void setEnabledUntil(Calendar enabledUntil) {
		if (enabledUntil != null) {
			this.enabledUntil = (Calendar) enabledUntil.clone();
		} else {
			this.enabledUntil = null;
		}
	}

	/**
	 * Gets the internal description, used in management tasks.
	 *
	 * @return the internal description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the internal description.
	 *
	 * @param description the internal description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets if the writing of scores/outcomes on the consumer is allowed.
	 *
	 * @return true if it is allowed
	 */
	public boolean isOutcome() {
		return outcome;
	}

	/**
	 * Sets if the writing of scores/outcomes on the consumer is allowed.
	 *
	 * @param outcome new value
	 */
	public void setOutcome(boolean outcome) {
		this.outcome = outcome;
	}

	/**
	 * Gets the extra arguments to pass to the corrector.
	 *
	 * @return extra arguments to pass to the corrector
	 */
	public String getExtraArgs() {
		return extraArgs;
	}

	/**
	 * Sets the extra arguments to pass to the corrector.
	 *
	 * @param extraArgs extra arguments to pass to the corrector
	 */
	public void setExtraArgs(String extraArgs) {
		this.extraArgs = extraArgs;
	}

	/**
	 * Gets the assessment counter.
	 *
	 * @return assessment counter
	 */
	public int getCounter() {
		return counter;
	}

	/**
	 * Sets the assessment counter.
	 *
	 * @param counter the counter to set
	 */
	public void setCounter(int counter) {
		this.counter = counter;
	}

	/**
	 * Gets the tool runner type.
	 *
	 * @return the tool runner type
	 */
	public ToolRunnerType getToolType() {
		return toolType;
	}

	/**
	 * Sets the tool runner type.
	 *
	 * @param toolType the tool runner type to set
	 */
	public void setToolType(ToolRunnerType toolType) {
		this.toolType = toolType;
	}

	/**
	 * Gets the tool runner to assess.
	 *
	 * @return the tool runner to assess.
	 */
	public ToolRunner getToolRunner() {
		return toolRunner;
	}

	/**
	 * Sets the ool runner to assess.
	 *
	 * @param toolRunner the tool runner to set
	 */
	public void setToolRunner(ToolRunner toolRunner) {
		this.toolRunner = toolRunner;
	}

	/**
	 * Gets the LTI usernames that are concurrently performing assessments.
	 *
	 * @return set of usernames
	 */
	public SortedSet<String> getConcurrentUsers() {
		if (concurrentUsers == null) {
			concurrentUsers = Collections.synchronizedSortedSet(new TreeSet<String>());
		}
		return concurrentUsers;
	}

	/**
	 * Sets the LTI usernames that are concurrently performing assessments.
	 *
	 * @param concurrentUsers set of usernames
	 */
	public void setConcurrentUsers(SortedSet<String> concurrentUsers) {
		this.concurrentUsers = concurrentUsers;
	}

	/**
	 * Gets a JSON string with properties of a tool that you do not want to store
	 * separately in the database.
	 *
	 * @return JSON extra configuration
	 */
	public String getJsonConfig() {
		if (jsonConfig == null) {
			toolUiConfig = new ToolUiConfig();
			jsonConfig = toolUiConfig.toString();
		}
		return jsonConfig;
	}

	/**
	 * Sets a JSON string with properties of a tool that you do not want to store
	 * separately in the database.
	 *
	 * <p>It also modifies the ToolUiConfig object.
	 *
	 * @param jsonConfig the JSON extra configuration to set
	 */
	public void setJsonConfig(String jsonConfig) {
		// Test if ok
		if (jsonConfig == null || jsonConfig.isEmpty()) {
			toolUiConfig = new ToolUiConfig();
			this.jsonConfig = toolUiConfig.toString();
		} else {
			toolUiConfig = ToolUiConfig.fromString(jsonConfig);
			if (toolUiConfig == null) {
				jsonConfig = "<JSON ERROR>" + jsonConfig;
			}
			this.jsonConfig = jsonConfig;
		}
	}

	/**
	 * Gets the object with properties of a tool that you do not want to store
	 * separately in the database.
	 *
	 * @return the ToolUiConfig object
	 */
	public ToolUiConfig getToolUiConfig() {
		if (jsonConfig == null) {
			getJsonConfig();
		} else if (toolUiConfig == null) {
			toolUiConfig = ToolUiConfig.fromString(jsonConfig);
			if (toolUiConfig == null) {
				// create default ToolUiConfig
				toolUiConfig = new ToolUiConfig();
			}
		}
		return toolUiConfig;
	}

	/**
	 * Sets the object with properties of a tool that you do not want to store
	 * separately in the database.
	 *
	 * @param toolUiConfig new value
	 */
	public void setToolUiConfig(ToolUiConfig toolUiConfig) {
		this.toolUiConfig = toolUiConfig;
	}

	/**
	 * Gets the tool folder path.
	 *
	 * @return the tool folder path.
	 */
	public String getToolPath() {
		if (path == null) {
			path = Settings.getToolsFolder() + File.separator + URLEncoder.encode(name, StandardCharsets.UTF_8);
		}
		return path;
	}

	/**
	 * Gets the tool data folder path.
	 *
	 * @return the tool data folder path
	 */
	public String getToolDataPath() {
		if (dataPath == null) {
			dataPath = getToolPath() + File.separator + "data";
		}
		return dataPath;
	}
	
	/**
	 * Gets the tool extra folder path.
	 *
	 * @return the tool extra folder path
	 */
	public String getToolExtraPath() {
		if (extraPath == null) {
			extraPath = getToolPath() + File.separator + "extra";
		}
		return extraPath;
	}
	
	/**
	 * Gets the extra zip path.
	 *
	 * @return the extra zip path
	 */
	public String getExtraZipPath() {
		if (extraZipPath == null) {
			extraZipPath = getToolPath() + File.separator + "extra.zip";
		}
		return extraZipPath;
	}

	/**
	 * Gets the corrector executable/configuration file path.
	 *
	 * @return the corrector executable/configuration file path
	 */
	public String getCorrectorPath() {
		if (correctorPath == null) {
			correctorPath = getToolPath() + File.separator + Settings.getCorrectorFilename();
		}
		return correctorPath;
	}

	/**
	 * Gets the tool description HTML file for users.
	 *
	 * @return the description file path
	 */
	public String getDescriptionPath() {
		if (descriptionPath == null) {
			descriptionPath = getToolPath() + File.separator + "description";
		}
		return descriptionPath;
	}

	/**
	 * Gets if they are the same except for the name.
	 *
	 * @param obj other tool
	 * @return true if they are the same except for the name
	 */
	public boolean equalsExceptName(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Tool other = (Tool) obj;
		return Objects.equals(deliveryPassword, other.deliveryPassword)
				&& Objects.equals(description, other.description) && enabled == other.enabled
				&& Objects.equals(enabledFrom, other.enabledFrom) && Objects.equals(enabledUntil, other.enabledUntil)
				&& Objects.equals(extraArgs, other.extraArgs) && Objects.equals(jsonConfig, other.jsonConfig)
				&& outcome == other.outcome && toolType == other.toolType && userTypeCode == other.userTypeCode;
	}

	/**
	 * Deletes backup files.
	 *
	 * @return true if all backup files could be deleted
	 */
	public boolean deleteToolFileBackups() {
		final File correctorBck = new File(getCorrectorPath() + FILE_BACKUP_SUFFIX);
		final File descriptionBck = new File(getDescriptionPath() + FILE_BACKUP_SUFFIX);
		final File extraZipBck = new File(getExtraZipPath() + FILE_BACKUP_SUFFIX);
		boolean bckCorrectorDeleted = false;
		boolean bckDescriptionDeleted = false;
		boolean bckExtraZipDeleted = false;
		if (correctorBck.exists()) {
			bckCorrectorDeleted = correctorBck.delete();
			if (!bckCorrectorDeleted) {
				logger.error("Error deleting backup {}", correctorBck.getPath());
			}
		} else {
			bckCorrectorDeleted = true;
		}
		if (descriptionBck.exists()) {
			bckDescriptionDeleted = descriptionBck.delete();
			if (!bckDescriptionDeleted) {
				logger.error("Error deleting backup {}", correctorBck.getPath());
			}
		} else {
			bckDescriptionDeleted = true;
		}
		if (extraZipBck.exists()) {
			bckExtraZipDeleted = extraZipBck.delete();
			if (!bckExtraZipDeleted) {
				logger.error("Error deleting backup {}", extraZipBck.getPath());
			}
		} else {
			bckExtraZipDeleted = true;
		}
		return bckCorrectorDeleted && bckDescriptionDeleted && bckExtraZipDeleted;
	}

	/**
	 * Restores existing backup files.
	 *
	 * @return true if successful
	 */
	public boolean restoreToolFileBackups() {
		final File correctorFile = new File(getCorrectorPath());
		final File correctorBck = new File(correctorFile.getPath() + FILE_BACKUP_SUFFIX);
		final File descriptionFile = new File(getDescriptionPath());
		final File descriptionBck = new File(descriptionFile.getPath() + FILE_BACKUP_SUFFIX);
		final File extraZipFile = new File(getExtraZipPath());
		final File extraZipBck = new File(extraZipFile.getPath() + FILE_BACKUP_SUFFIX);
		boolean correctorRestored = false;
		boolean descriptionRestored = false;
		boolean extraRestored = true;
		if (correctorBck.exists()) {
			try {
				Files.copy(correctorBck.toPath(), correctorFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
				correctorRestored = true;
				if (!correctorBck.delete()) {
					logger.error("Error deleting corrector backup.");
				}
			} catch (final IOException e) {
				logger.error("Error restoring corrector backup.");
			}

		}
		if (descriptionBck.exists()) {
			try {
				Files.copy(descriptionBck.toPath(), descriptionFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
				descriptionRestored = true;
				if (!descriptionBck.delete()) {
					logger.error("Error deleting description backup.");
				}
			} catch (final IOException e) {
				logger.error("Error restoring description backup.");
			}
		}
		if (extraZipBck.exists()) {
			extraRestored = false;
			try {
				Files.copy(extraZipBck.toPath(), extraZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
				if (!extraZipBck.delete()) {
					logger.error("Error deleting extra zip backup.");
				}
				// Try to delete extra folder
				final File dir = new File(getToolExtraPath());
				if (dir.exists() && dir.isDirectory()) {
					FileUtils.deleteDirectory(dir);
					// if something is deleted before giving an error,
					// we will not be able to recover it
				}
				// Create extra folder and unzip
				if (!dir.exists() && !dir.mkdirs()) {
					extraRestored = unzipFile(extraZipFile, dir);
				}
			} catch (final IOException e) {
				logger.error("Error restoring extra zip backup.");
			}
		}
		return correctorRestored && descriptionRestored && extraRestored;
	}

	/**
	 * Write file, if it already existed it creates a backup copy.
	 *
	 * @param filePath      file path
	 * @param fileItem      file item (uploaded file)
	 * @param setExecutable sets execution permissions
	 * @return true if successful
	 */
	private boolean writeToolFile(String filePath, UploadedFile fileItem, boolean setExecutable) {
		final File newFile = new File(filePath);
		boolean backupCreated = true; // if it does not exist, we do not make a backup
		boolean fileCreated = false;
		if (newFile.exists()) {
			final File backupFile = new File(newFile.getPath() + FILE_BACKUP_SUFFIX);
			backupCreated = false;
			boolean backupDeleted = true;
			if (backupFile.exists() && backupFile.isFile()) {
				// Delete previous copy
				backupDeleted = backupFile.delete();
			}
			if (backupDeleted) {
				// Create backup
				backupCreated = newFile.renameTo(backupFile);
			}
		}
		if (backupCreated) {
			try {
				fileItem.write(newFile.getAbsolutePath());
				// Give execution permissions
				if (setExecutable && !newFile.setExecutable(true)) {
					// With some concealers this is not necessary, so just warn.
					logger.warn("Failed to give execute permissions to file.");
				}
				fileCreated = true;
			} catch (final Exception e) {
				// fileCreated is false
				logger.error("IO", e);
			}
		}

		return fileCreated;
	}

	/**
	 * Create the files associated with this tool from FileItem.
	 *
	 * @param correctorFile   corrector file
	 * @param descriptionFile description file in HTML
	 * @param extraZipFile    additional user zip file with the files referenced by
	 *                        the description file
	 * @return true if they have been copied
	 */
	public boolean createToolFiles(UploadedFile correctorFile, UploadedFile descriptionFile,
			UploadedFile extraZipFile) {
		boolean result = true;

		// Create tool directory.
		File folder = new File(getToolPath());
		if (!folder.exists() && !folder.mkdirs()) {
			result = false;
		}

		// Create data folder.
		if (result) {
			folder = new File(getToolDataPath());
			if (!folder.exists() && !folder.mkdirs()) {
				result = false;
			}
		}
		
		// Create extra folder.
		if (result) {
			folder = new File(getToolExtraPath());
			if (!folder.exists() && !folder.mkdirs()) {
				result = false;
			}
		}

		// Copy corrector file.
		if (result && correctorFile != null && !writeToolFile(getCorrectorPath(), correctorFile, true)) {
			result = false;
		}

		// Copy description file.
		if (result && descriptionFile != null && !writeToolFile(getDescriptionPath(), descriptionFile, false)) {
			result = false;
		}
		
		// Copy extra zip file
		if (result && extraZipFile != null) {
			if (writeToolFile(getExtraZipPath(), extraZipFile, false)) {
				// Try to delete extra folder
				final File dir = new File(getToolExtraPath());
				if (dir.exists() && dir.isDirectory()) {
					try {
						FileUtils.deleteDirectory(dir);
					} catch (IOException e) {
						// Ignore because it is optional
						logger.warn("Error deleting : {}", dir.getPath());
					}
				}
				result = unzipFile(new File(getExtraZipPath()), new File(getToolExtraPath()));
			} else {
				result = false;
			}
		}

		if (!result) {
			// restore backups
			restoreToolFileBackups();
		}
		return result;
	}
	
	/**
	 * Unzip ZIP file into destination directory.
	 * 
	 * @param zip the ZIP file
	 * @param destination the destination directory
	 * @return true if no error
	 */
	public boolean unzipFile(File zip, File destination) {
		boolean res = false;
		final byte[] buffer = new byte[8192];
		try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zip))) {
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			while (zipEntry != null) {
				// @see https://snyk.io/research/zip-slip-vulnerability
				final File destFile = new File(destination, zipEntry.getName());
				String destDirPath = destination.getCanonicalPath();
				String destFilePath = destFile.getCanonicalPath();
				if (!destFilePath.startsWith(destDirPath + File.separator)) {
					throw new IOException("Security Error: path is outside of destination: " + zipEntry.getName());
				}
				if (zipEntry.isDirectory()) {
					if (!destFile.isDirectory() && !destFile.mkdirs()) {
						throw new IOException("Error creating directory " + destFile);
					}
				} else {
					File parent = destFile.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("Error creating directory " + parent);
					}
					try (FileOutputStream fos = new FileOutputStream(destFile)) {
						int len;
						while ((len = zipInputStream.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
				}
				zipEntry = zipInputStream.getNextEntry();
			}
			zipInputStream.closeEntry();
			res = true;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			res = false;
		}
		return res;
	}
	
}
