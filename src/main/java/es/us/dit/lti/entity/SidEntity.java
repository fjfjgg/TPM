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

import java.io.Serializable;

/**
 * Database entity with a serial identifier.
 *
 * @author Francisco José Fernández Jiménez
 */
public class SidEntity implements Serializable {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Serial identifier.
	 */
	protected int sid;

	/**
	 * Gets the serial identifier.
	 *
	 * @return the serial identifier.
	 */
	public int getSid() {
		return sid;
	}

	/**
	 * Sets the serial identifier.
	 *
	 * @param sid the serial identifier
	 */
	public void setSid(int sid) {
		this.sid = sid;
	}

}
