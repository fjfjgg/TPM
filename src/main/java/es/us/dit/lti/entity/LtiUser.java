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

import java.util.Objects;

/**
 * Tool LTI user data.
 *
 * <p>Specified in <a href="https://www.imsglobal.org/specs/ltiv1p1">
 * https://www.imsglobal.org/specs/ltiv1p1</a>
 *
 * @author Francisco José Fernández Jiménez
 */
public class LtiUser extends UpdateRecordEntity {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 8326645339981138920L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * Consumer from which it comes.
	 */
	private Consumer consumer;
	/**
	 * Uniquely identifies the user.
	 *
	 * <p>This should not contain any identifying information for the user. At a
	 * minimum, this value needs to be unique within a TC.
	 */
	private String userId;
	/**
	 * This field contains the LIS identifier for the user account that is
	 * performing this launch. It is simply a unique identifier. Optional.
	 */
	private String sourceId;
	/**
	 * First name.
	 */
	private String nameGiven;
	/**
	 * Family name, last name, surname.
	 */
	private String nameFamily;
	/**
	 * Full name.
	 */
	private String nameFull;
	/**
	 * Email.
	 */
	private String email;

	@Override
	public int hashCode() {
		return Objects.hash(consumer.getSid(), email, nameFamily, nameFull, nameGiven, sourceId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final LtiUser other = (LtiUser) obj;
		return Objects.equals(consumer.getSid(), other.consumer.getSid()) && Objects.equals(email, other.email)
				&& Objects.equals(nameFamily, other.nameFamily) && Objects.equals(nameFull, other.nameFull)
				&& Objects.equals(nameGiven, other.nameGiven) && Objects.equals(sourceId, other.sourceId)
				&& Objects.equals(userId, other.userId);
	}

	/**
	 * Gets the consumer.
	 *
	 * @return the consumer
	 */
	public Consumer getConsumer() {
		return consumer;
	}

	/**
	 * Sets the consumer.
	 *
	 * @param consumer the consumer to set
	 */
	public void setConsumer(Consumer consumer) {
		this.consumer = consumer;
	}

	/**
	 * Gets the user ID.
	 *
	 * <p>Uniquely identifies the user.
	 *
	 * <p>This should not contain any identifying information for the user. At a
	 * minimum, this value needs to be unique within a TC.
	 *
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Sets the user ID.
	 *
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Gets the source ID (sourced ID).
	 *
	 * <p>This field contains the LIS identifier for the user account that is
	 * performing this launch. It is simply a unique identifier. Optional.
	 *
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Sets the source ID.
	 *
	 * @param sourceId the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Gets the given (first) name.
	 *
	 * @return the given name
	 */
	public String getNameGiven() {
		return nameGiven;
	}

	/**
	 * Sets the given (first) name.
	 *
	 * @param nameGiven the given name to set
	 */
	public void setNameGiven(String nameGiven) {
		this.nameGiven = nameGiven;
	}

	/**
	 * Gets the family name.
	 *
	 * @return the family name
	 */
	public String getNameFamily() {
		return nameFamily;
	}

	/**
	 * Sets the family name.
	 *
	 * @param nameFamily the family name to set
	 */
	public void setNameFamily(String nameFamily) {
		this.nameFamily = nameFamily;
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

}
