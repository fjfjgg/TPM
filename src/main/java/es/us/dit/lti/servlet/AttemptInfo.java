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

import es.us.dit.lti.runner.ToolRunner;

/**
 * Information of attempt of delivery to display to users.
 *
 * @author Francisco José Fernández Jiménez
 */
public class AttemptInfo {
	/**
	 * The serial ID used in database (ciphered).
	 */
	private String sid;
	/**
	 * A unique identifier for this attempt. It will be displayed to users.
	 */
	private String id;
	/**
	 * The user/owner of this attempt.
	 */
	private String userId;
	/**
	 * Name of the delivery file.
	 */
	private String fileName;
	/**
	 * Creation instant.
	 */
	private String timestamp;
	/**
	 * Flag that indicates if the delivery file was saved.
	 */
	private boolean withFile;
	/**
	 * Flag that indicates if the output of assessment was saved.
	 */
	private boolean withOutput;
	/**
	 * Score/outcome of assessment.
	 */
	private int score;
	/**
	 * Error code of assessment.
	 */
	private int errorCode;

	/**
	 * Gets the serial ID used in database (ciphered).
	 *
	 * @return the sid
	 */
	public String getSid() {
		return sid;
	}

	/**
	 * Sets the serial ID used in database (ciphered).
	 *
	 * @param sid the sid to set
	 */
	public void setSid(String sid) {
		this.sid = sid;
	}

	/**
	 * Gets a unique identifier for this attempt. It will be displayed to users.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the attempt ID.
	 *
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Gets the user/owner of this attempt (username).
	 *
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Sets the username of the user/owner of this attempt.
	 *
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Gets the name of the delivery file.
	 *
	 * @return the filename
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the name of the delivery file.
	 *
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Gets the creation instant.
	 *
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets the creation timestamp.
	 *
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Gets if the delivery file was saved.
	 *
	 * @return true if the delivery file was saved
	 */
	public boolean isWithFile() {
		return withFile;
	}

	/**
	 * Sets if the delivery file was saved.
	 *
	 * @param withFile new value
	 */
	public void setWithFile(boolean withFile) {
		this.withFile = withFile;
	}

	/**
	 * Gets if the output of assessment was saved.
	 *
	 * @return true if the output of assessment was saved
	 */
	public boolean isWithOutput() {
		return withOutput;
	}

	/**
	 * Sets if the output of assessment was saved.
	 *
	 * @param withOutput new value
	 */
	public void setWithOutput(boolean withOutput) {
		this.withOutput = withOutput;
	}

	/**
	 * Gets the score/outcome of assessment.
	 *
	 * <p>Integer value between 0 and 100.
	 *
	 * @return the score
	 */
	public int getScore() {
		return score;
	}

	/**
	 * Sets the score/outcome of assessment.
	 *
	 * @param score the score to set [0-100]
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

}