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

import java.util.Calendar;

/**
 * Database entity with a serial identifier, a creation date, and an update
 * date.
 *
 * @author Francisco José Fernández Jiménez
 */
public interface IUpdateRecordEntity {

	/**
	 * Gets the serial identifier.
	 *
	 * @return the serial identifier.
	 */
	int getSid();

	/**
	 * Gets the creation date/time as a Calendar object.
	 *
	 * @return the creation date/time
	 */
	Calendar getCreated();

	/**
	 * Sets the creation date/time.
	 *
	 * @param created the creation date/time
	 */
	void setCreated(Calendar created);

	/**
	 * Gets the update date/time as a Calendar object.
	 *
	 * @return the update date/time
	 */
	Calendar getUpdated();

	/**
	 * Sets the update date/time.
	 *
	 * @param updated the update date/time
	 */
	void setUpdated(Calendar updated);

	/**
	 * Gets the serial version UID of this class.
	 *
	 * @return the serial version UID
	 */
	long getSerialVersionUid();

}