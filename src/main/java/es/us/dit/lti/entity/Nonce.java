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
 * Request Nonce.
 *
 * <p>Specified in <a href="https://www.imsglobal.org/specs/ltiv1p1">
 * https://www.imsglobal.org/specs/ltiv1p1</a>
 *
 * @author Francisco José Fernández Jiménez
 */
public class Nonce {

	/**
	 * The tool key serial ID of this nonce.
	 */
	private final int keyId;
	/**
	 * The consumer serial ID of this nonce.
	 */
	private final int consumerId;
	/**
	 * The value (random).
	 */
	private final String value;
	/**
	 * The timestamp.
	 */
	private final int ts;
	/**
	 * Expiration date/time of the nonce.
	 */
	private final Calendar expires;

	/**
	 * Constructor.
	 *
	 * @param keyId      the tool key serial ID
	 * @param consumerId the consumer serial ID
	 * @param value      the value
	 * @param ts         the timestamp
	 * @param duration   the expiration date/time
	 */
	public Nonce(int keyId, int consumerId, String value, int ts, int duration) {
		this.keyId = keyId;
		this.consumerId = consumerId;
		this.value = value;
		expires = Calendar.getInstance();
		this.ts = ts;
		expires.add(Calendar.MINUTE, duration);
	}

	/**
	 * Gets the took key serial ID.
	 *
	 * @return the took key serial ID
	 */
	public int getKeyId() {
		return keyId;
	}

	/**
	 * Gets the consumer serial ID.
	 *
	 * @return the consumer serial ID
	 */
	public int getConsumerId() {
		return consumerId;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return the timestamp
	 */
	public int getTs() {
		return ts;
	}

	/**
	 * Gets the expiration date/time.
	 *
	 * @return the experiation date/time
	 */
	public Calendar getExpires() {
		Calendar calendar = null;
		if (expires != null) {
			calendar = (Calendar) expires.clone();
		}
		return calendar;
	}

}
