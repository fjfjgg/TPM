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
 * Management user types.
 *
 * @author Francisco José Fernández Jiménez
 *
 */
public enum MgmtUserType {
	/**
	 * Superuser, can create new users.
	 */
	SUPER(-1),
	/**
	 * Administrator, can create new tools.
	 */
	ADMIN(0),
	/**
	 * Editor, can edit tools.
	 */
	EDITOR(1),
	/**
	 * Tester, can test tools.
	 */
	TESTER(2),
	/**
	 * Unknown, invalid user.
	 */
	UNKNOWN(100);

	/**
	 * Code associated to type, for saving in db.
	 */
	private int code = 0;

	/**
	 * Constructor.
	 *
	 * @param code code associated to type
	 */
	MgmtUserType(int code) {
		this.code = code;
	}

	/**
	 * Gets the type code.
	 *
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Gets the type from a code.
	 *
	 * @param type type code
	 * @return the type
	 */
	public static MgmtUserType fromInt(int type) {
		// Decode type
		for (final MgmtUserType t : values()) {
			if (type == t.getCode()) {
				return t;
			}
		}
		return UNKNOWN;
	}

}
