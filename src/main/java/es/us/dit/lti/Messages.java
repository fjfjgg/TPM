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

package es.us.dit.lti;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class to ease access to ResourceBundle and avoid exceptions.
 *
 * @author Francisco José Fernández Jiménez
 *
 */
public final class Messages {

	/**
	 * Can not create objects.
	 */
	private Messages() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Gets the message associated to a <code>key</code> in specified
	 * <code>locale</code>. If <code>locale</code> is null, uses the default locale.
	 *
	 * @param messageKey key of the message
	 * @param locale     locale or null (default locale)
	 * @return messages if found or <code>"???messageKey???"</code>
	 */
	public static String getMessageForLocale(String messageKey, Locale locale) {
		if (locale == null) {
			locale = Locale.getDefault();
		}
		try {
			return ResourceBundle.getBundle("messages", locale).getString(messageKey);
		} catch (final MissingResourceException e) {
			return "???" + messageKey + "???";
		}
	}

}