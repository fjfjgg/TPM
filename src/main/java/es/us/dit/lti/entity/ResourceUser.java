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
 * Tool resource user data.
 *
 * <p>Specified in <a href="https://www.imsglobal.org/specs/ltiv1p1">
 * https://www.imsglobal.org/specs/ltiv1p1</a>
 *
 * @author Francisco José Fernández Jiménez
 */
public class ResourceUser extends UpdateRecordEntity {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = -7101697796884592144L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * Resource link.
	 */
	private ResourceLink resourceLink;
	/**
	 * LTI user.
	 */
	private LtiUser user;
	/**
	 * This field contains an identifier that indicates the LIS Result Identifier
	 * (if any) associated with this launch.
	 * 
	 * <p>This field identifies a unique row and column within the TC gradebook.
	 */
	private String resultSourceId;

	@Override
	public int hashCode() {
		return Objects.hash(resourceLink.getSid(), resultSourceId, user.getSid());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ResourceUser other = (ResourceUser) obj;
		return Objects.equals(resourceLink.getSid(), other.resourceLink.getSid())
				&& Objects.equals(resultSourceId, other.resultSourceId)
				&& Objects.equals(user.getSid(), other.user.getSid());
	}

	/**
	 * Gets the resource link.
	 *
	 * @return the resourceLink
	 */
	public ResourceLink getResourceLink() {
		return resourceLink;
	}

	/**
	 * Sets the resource link.
	 *
	 * @param resourceLink the resourceLink to set
	 */
	public void setResourceLink(ResourceLink resourceLink) {
		this.resourceLink = resourceLink;
	}

	/**
	 * Gets the LTI user.
	 *
	 * @return the user
	 */
	public LtiUser getUser() {
		return user;
	}

	/**
	 * Sets the LTI user.
	 *
	 * @param user the user to set
	 */
	public void setUser(LtiUser user) {
		this.user = user;
	}

	/**
	 * Gets the result source ID.
	 *
	 * @return the resultSourceId
	 */
	public String getResultSourceId() {
		return resultSourceId;
	}

	/**
	 * Sets the result source ID.
	 *
	 * @param resultSourceId the resultSourceId to set
	 */
	public void setResultSourceId(String resultSourceId) {
		this.resultSourceId = resultSourceId;
	}

}
