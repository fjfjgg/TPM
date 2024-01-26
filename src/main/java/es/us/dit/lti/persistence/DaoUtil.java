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

package es.us.dit.lti.persistence;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Utility class with date/time conversions for databases.
 *
 * @author Francisco José Fernández Jiménez
 *
 */
public final class DaoUtil {

	/**
	 * Can not create objects.
	 */
	private DaoUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Converts Calendar object to Timestamp object.
	 *
	 * @param cal Calendar object to convert
	 * @return a Timestamp object
	 */
	public static Timestamp toTimestamp(Calendar cal) {
		return new Timestamp(cal.getTimeInMillis());
	}

	/**
	 * Converts Timestamp object to Calendar object.
	 *
	 * @param ts Timestamp object to convert
	 * @return a Calendar object
	 */
	public static Calendar toCalendar(Timestamp ts) {
		if (ts == null) {
			return null;
		} else {
			return new Calendar.Builder().setInstant(ts).build();
		}
	}
}
