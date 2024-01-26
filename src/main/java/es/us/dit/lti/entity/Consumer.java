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
 * Tool consumer (TC) data.
 *
 * <p>Specified in <a href="https://www.imsglobal.org/specs/ltiv1p1">
 * https://www.imsglobal.org/specs/ltiv1p1</a>
 *
 * @author Francisco José Fernández Jiménez
 */
public class Consumer extends UpdateRecordEntity {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 3693432949793785253L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * This is a unique identifier for the tool consumer.
	 */
	private String guid;
	/**
	 * This indicates which version of the LTI specification is being used.
	 */
	private String ltiVersion;
	/**
	 * This is a plain text user visible field.
	 */
	private String name;
	/**
	 * This field should have a major release number followed by a period. The
	 * format of the minor release is flexible.
	 */
	private String version;
	/**
	 * This is a list to LMS-specific CSS URLs (comma separated list of paths).
	 */
	private String cssPath;

	/**
	 * Gets the GUID.
	 *
	 * <p>This is a unique identifier for the tool consumer.
	 *
	 * @return the GUID.
	 */
	public String getGuid() {
		return guid;
	}

	/**
	 * Sets the GUID.
	 *
	 * @param guid the GUID
	 */
	public void setGuid(String guid) {
		this.guid = guid;
	}

	/**
	 * Gets which version of the LTI specification is being used.
	 *
	 * @return the LTI version
	 */
	public String getLtiVersion() {
		return ltiVersion;
	}

	/**
	 * Sets which version of the LTI specification is being used.
	 *
	 * @param ltiVersion the LTI version
	 */
	public void setLtiVersion(String ltiVersion) {
		this.ltiVersion = ltiVersion;
	}

	/**
	 * Gets the plain text user visible field.
	 *
	 * @return consumer name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the consumer name.
	 *
	 * @param name consumer name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the consumer version.
	 *
	 * <p>This field should have a major release number followed by a period. The
	 * format of the minor release is flexible.
	 *
	 * @return consumer version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the consumer version.
	 *
	 * @param version consumer version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Gets a comma separated list of URLs to LMS-specific CSS URLs.
	 *
	 * @return CSS URLs in a comma separated string
	 */
	public String getCssPath() {
		return cssPath;
	}

	/**
	 * Sets the list of URLs to LMS-specific CSS URLs, as a comma separated string.
	 *
	 * @param cssPath comma separated string of URLs
	 */
	public void setCssPath(String cssPath) {
		this.cssPath = cssPath;
	}

	@Override
	public int hashCode() {
		return Objects.hash(cssPath, guid, ltiVersion, name, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Consumer other = (Consumer) obj;
		return Objects.equals(cssPath, other.cssPath) && Objects.equals(guid, other.guid)
				&& Objects.equals(ltiVersion, other.ltiVersion) && Objects.equals(name, other.name)
				&& Objects.equals(version, other.version);
	}

}
