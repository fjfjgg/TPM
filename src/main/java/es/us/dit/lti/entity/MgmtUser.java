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

/**
 * Management user data.
 *
 * @author Francisco José Fernández Jiménez
 */
public class MgmtUser extends UpdateRecordEntity {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 5927263836354863769L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * User name, for sign in.
	 */
	private String username;
	/**
	 * Password for sign in.
	 */
	private String password;
	/**
	 * Type of mgmt user.
	 */
	private MgmtUserType type;
	/**
	 * if user can execute local executables.
	 */
	private boolean isLocal;
	/**
	 * JSON or string with restrictions to apply to tools created by this user,
	 * depends on the type of execution.
	 */
	private String executionRestrictions;
	/**
	 * Full name.
	 */
	private String nameFull;
	/**
	 * Email, for notifications.
	 */
	private String email;

	/**
	 * Gets if user is administrator.
	 *
	 * @return true if user is administrator
	 */
	public boolean getAdmin() {
		return type == MgmtUserType.ADMIN || type == MgmtUserType.SUPER;
	}

	/**
	 * Gets the user name, for sign in.
	 *
	 * @return the user name
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the user name.
	 *
	 * @param username the user name to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password.
	 *
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Gets the management user type.
	 *
	 * @return the type
	 */
	public MgmtUserType getType() {
		return type;
	}

	/**
	 * Sets the management user type.
	 *
	 * @param type the type to set
	 */
	public void setType(MgmtUserType type) {
		this.type = type;
	}

	/**
	 * Gets if this user is a user of the local system and can execute local tools.
	 *
	 * @return true if is local
	 */
	public boolean isLocal() {
		return isLocal;
	}

	/**
	 * Sets if this user is a local system user.
	 *
	 * @param isLocal the value to set
	 */
	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	/**
	 * Restrictions as a JSON string.
	 *
	 * @return the executionRestrictions
	 */
	public String getExecutionRestrictions() {
		return executionRestrictions;
	}

	/**
	 * Sets the restrictions as a JSON string.
	 *
	 * @param executionRestrictions the executionRestrictions to set
	 */
	public void setExecutionRestrictions(String executionRestrictions) {
		this.executionRestrictions = executionRestrictions;
	}

	/**
	 * Gets the full name.
	 *
	 * @return the full name
	 */
	public String getNameFull() {
		return nameFull;
	}

	/**
	 * Sets the full name.
	 *
	 * @param nameFull the full name to set
	 */
	public void setNameFull(String nameFull) {
		this.nameFull = nameFull;
	}

	/**
	 * Gets the email.
	 *
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the email.
	 *
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	
	/**
	 * Checks if an email is valid.
	 * 
	 * @param email the email
	 * @return true if valid
	 */
	public static boolean isValidEmail(String email) {
		return email == null || email.isEmpty() || email.matches("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
	}
	
	/**
	 * Checks if a password is secure.
	 * 
	 * @param password the password
	 * @return true if valid
	 */
	public static boolean isValidPassword(String password) {
		// for now we are going to require 12 characters.
		return password != null && password.length() >= 12;
	}

}
