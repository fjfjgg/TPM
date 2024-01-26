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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import es.us.dit.lti.entity.Settings;

/**
 * Map interface to ResourceBundle for use in JSP.
 *
 * @author Francisco José Fernández Jiménez
 */
public class MessageMap extends AbstractMap<String, String> implements Serializable {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default language.
	 */
	private static final String DEFAULT_LANGUAGE = "es";
	/**
	 * Supported languages in alphabetical order.
	 */
	private static final List<String> SUPPORTED_LANGUAGES = Collections.unmodifiableList(Arrays.asList("en", "es"));

	/**
	 * Locate of returned messages.
	 */
	private final Locale locale;

	/**
	 * Construct MessageMap with default language.
	 *
	 * @see Settings
	 */
	public MessageMap() {
		locale = Locale.forLanguageTag(DEFAULT_LANGUAGE);
	}

	/**
	 * Construct MessageMap with specified locale if supported or default language
	 * if not supported.
	 *
	 * @param locale specified locale
	 */
	public MessageMap(Locale locale) {
		if (Collections.binarySearch(SUPPORTED_LANGUAGES, locale.getLanguage()) < 0) {
			locale = Locale.forLanguageTag(DEFAULT_LANGUAGE);
		}

		this.locale = locale;
	}

	/**
	 * Gets the locale of messages.
	 *
	 * @return the locale of messages
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Gets the messages of a <code>key</code> using locale.
	 *
	 * @param key key of message (<code>messages.properties</code>)
	 * @return the message using locale or <code>"???key???"</code>
	 */
	@Override
	public String get(Object key) {
		try {
			return ResourceBundle.getBundle("messages", locale).getString((String) key);
		} catch (final MissingResourceException e) {
			return "???" + key + "???";
		}

	}

	/**
	 * Do not use.
	 *
	 * @return empty set
	 */
	@Override
	public Set<Map.Entry<String, String>> entrySet() {
		// no needed
		return Collections.emptySet();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		return prime * result + Objects.hash(locale);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj) || getClass() != obj.getClass()) {
			return false;
		}
		final MessageMap other = (MessageMap) obj;
		return Objects.equals(locale, other.locale);
	}

}
