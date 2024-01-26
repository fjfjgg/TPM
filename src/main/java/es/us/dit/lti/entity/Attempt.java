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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import es.us.dit.lti.runner.ToolRunner;
import es.us.dit.lti.servlet.AssessServlet;
import es.us.dit.lti.storage.StorageType;

/**
 * Attempt of delivery.
 *
 * @author Francisco José Fernández Jiménez
 */
public class Attempt extends SidEntity implements IUpdateRecordEntity {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 5793223280881242947L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * Resource User of this attempt.
	 */
	private ResourceUser resourceUser;
	/**
	 * Original Resource User of this attempt (only in a reassess).
	 */
	private ResourceUser originalResourceUser;
	/**
	 * Creation instant.
	 *
	 * <p>It is saved in database as 2 integers, epoch seconds and nanoseconds.
	 */
	private Instant instant;
	/**
	 * Flag that indicates if the delivery file was saved.
	 */
	private boolean fileSaved;
	/**
	 * Flag that indicates if the output of assessment was saved.
	 */
	private boolean outputSaved;
	/**
	 * Name of the delivery file.
	 */
	private String fileName;
	/**
	 * Type of storage, to locate files.
	 */
	private StorageType storageType = StorageType.LOCAL;
	/**
	 * Score/outcome of assessment.
	 */
	private int score;
	/**
	 * Error code of assessment.
	 */
	private int errorCode;

	/**
	 * Gets the Resource User of this attempt.
	 *
	 * @return the resource user
	 */
	public ResourceUser getResourceUser() {
		return resourceUser;
	}

	/**
	 * Sets the Resource User of this attempt.
	 *
	 * @param resourceUser new value
	 */
	public void setResourceUser(ResourceUser resourceUser) {
		this.resourceUser = resourceUser;
	}

	/**
	 * Gets the Original Resource User of this attempt (only in a reassess).
	 *
	 * @return the original resource user
	 */
	public ResourceUser getOriginalResourceUser() {
		return originalResourceUser;
	}

	/**
	 * Sets the Original Resource User of this attempt (only in a reassess).
	 *
	 * @param resourceUser new value
	 */
	public void setOriginalResourceUser(ResourceUser resourceUser) {
		originalResourceUser = resourceUser;
	}

	/**
	 * Gets the creation instant.
	 *
	 * <p>It is saved in database as 2 integers, epoch seconds and nanoseconds.
	 *
	 * @return the creation instant
	 */
	public Instant getInstant() {
		return instant;
	}

	/**
	 * Sets the creation instant.
	 *
	 * <p>It is saved in database as 2 integers, epoch seconds and nanoseconds.
	 *
	 * @param instant new value
	 */
	public void setInstant(Instant instant) {
		this.instant = instant;
	}

	/**
	 * Sets the creation instant.
	 *
	 * <p>It is saved in database as 2 integers, epoch seconds and nanoseconds.
	 *
	 * @param cal   date and time with precision of seconds
	 * @param nanos nanoseconds
	 */
	public void setInstant(Calendar cal, int nanos) {
		instant = cal.toInstant().truncatedTo(ChronoUnit.SECONDS).plusNanos(nanos);
	}

	/**
	 * Gets if the delivery file was saved.
	 *
	 * @return true if the delivery file was saved
	 */
	public boolean isFileSaved() {
		return fileSaved;
	}

	/**
	 * Sets if the delivery file was saved.
	 *
	 * @param fileSaved new value
	 */
	public void setFileSaved(boolean fileSaved) {
		this.fileSaved = fileSaved;
	}

	/**
	 * Gets if the output of assessment was saved.
	 *
	 * @return true if the output of assessment was saved
	 */
	public boolean isOutputSaved() {
		return outputSaved;
	}

	/**
	 * Sets if the output of assessment was saved.
	 *
	 * @param outputSaved new value
	 */
	public void setOutputSaved(boolean outputSaved) {
		this.outputSaved = outputSaved;
	}

	/**
	 * Gets the name of the delivery file.
	 *
	 * @return the name of the delivery file
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the name of the delivery file.
	 *
	 * @param fileName new value
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Gets the score/outcome of assessment.
	 *
	 * @return the core/outcome of assessment
	 */
	public int getScore() {
		return score;
	}

	/**
	 * Sets the score/outcome of assessment.
	 *
	 * @param score new score/outcome
	 */
	public void setScore(int score) {
		this.score = score;
	}

	/**
	 * Gets the error code of assessment.
	 *
	 * <p>The possible values are defined in {@link AssessServlet} and
	 * {@link ToolRunner}.
	 *
	 * @return the error code of assessment
	 * @see AssessServlet
	 * @see ToolRunner
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * Sets the error code of assessment.
	 *
	 * <p>The possible values are defined in {@link AssessServlet} and
	 * {@link ToolRunner}.
	 *
	 * @param errorCode new value
	 * @see AssessServlet
	 * @see ToolRunner
	 */
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Gets the type of storage, to locate files.
	 *
	 * @return the type of storage
	 */
	public StorageType getStorageType() {
		return storageType;
	}

	/**
	 * Sets the type of storage, to locate files.
	 *
	 * @param storageType new value
	 */
	public void setStorageType(StorageType storageType) {
		this.storageType = storageType;
	}

	/**
	 * Sets the type of storage, to locate files.
	 *
	 * @param storageType storage type code
	 */
	public void setStorageType(int storageType) {
		this.storageType = StorageType.fromInt(storageType);
	}

	/**
	 * Gets the folder path where files of the attempt user are saved.
	 *
	 * <p>Needs that tool of resource link of resource user to be not null.
	 *
	 * @return the attempt user folder path
	 */
	public String getUserFolderPath() {
		String value;
		if (storageType == StorageType.LOCAL) {
			value = resourceUser.getResourceLink().getTool().getToolDataPath() + File.separator
					+ URLEncoder.encode(resourceUser.getUser().getSourceId(), StandardCharsets.UTF_8);
		} else {
			value = "UNKNOWN";
		}
		return value;
	}

	/**
	 * Gets the path to the delivery file.
	 *
	 * @return the path of the delivery file
	 */
	public String getUserFilePath() {
		String value;
		if (storageType == StorageType.LOCAL) {
			value = getUserFolderPath() + File.separator + getId();
		} else {
			value = "UNKNOWN";
		}
		return value;
	}

	/**
	 * Gets a unique identifier for this attempt. It will be displayed to users.
	 *
	 * <p>You are not supposed to create two intents in the same nanosecond, with the
	 * same username and filename.
	 *
	 * @return a unique identifier
	 */
	public String getId() {
		return Settings.DATE_TIME_FORMATTER.format(instant) + "["
				+ URLEncoder.encode(originalResourceUser.getUser().getSourceId(), StandardCharsets.UTF_8) + "]"
				+ URLEncoder.encode(fileName, StandardCharsets.UTF_8);
	}

	/**
	 * Gets the path to the file with the output of assessment, using the delivery
	 * filename (relative) as base.
	 *
	 * @param relFilePath the delivery filename (relative)
	 * @return the path to the file with the output of assessment
	 */
	public String getCorrectorResultPath(String relFilePath) {
		String value;
		if (storageType == StorageType.LOCAL) {
			value = getUserFolderPath() + File.separator + relFilePath + Settings.RESULT_EXT;
		} else {
			value = "UNKNOWN";
		}
		return value;
	}

	/**
	 * Gets the path to the file with the output of assessment, using the delivery
	 * file path (absolute) as base.
	 *
	 * @param absfilePath the delivery file path (absolute)
	 * @return the path to the file with the output of assessment
	 */
	public String getCorrectorResultPathFromFile(String absfilePath) {
		return absfilePath + Settings.RESULT_EXT;
	}

	@Override
	public Calendar getCreated() {
		return new Calendar.Builder().setInstant(instant.toEpochMilli()).build();
	}

	/**
	 * Empty method. Parameter is ignored.
	 */
	@Override
	public void setCreated(Calendar created) {
		// ignored
	}

	@Override
	public Calendar getUpdated() {
		return new Calendar.Builder().setInstant(instant.toEpochMilli()).build();
	}

	/**
	 * Empty method. Parameter is ignored.
	 */
	@Override
	public void setUpdated(Calendar updated) {
		// ignored
	}

}
