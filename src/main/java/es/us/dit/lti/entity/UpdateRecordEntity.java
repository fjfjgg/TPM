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
 * Base class for entities that track creation and update.
 *
 * @author Francisco José Fernández Jiménez
 *
 */
public abstract class UpdateRecordEntity extends SidEntity implements IUpdateRecordEntity {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Date of creation.
	 */
	private Calendar created = null;
	/**
	 * Last update.
	 */
	private Calendar updated = null;

	@Override
	public Calendar getCreated() {
		Calendar calendar = null;
		if (created != null) {
			calendar = (Calendar) created.clone();
		}
		return calendar;
	}

	@Override
	public void setCreated(Calendar created) {
		if (created != null) {
			this.created = (Calendar) created.clone();
		} else {
			this.created = null;
		}
	}

	@Override
	public Calendar getUpdated() {
		Calendar calendar = null;
		if (updated != null) {
			calendar = (Calendar) updated.clone();
		}
		return calendar;
	}

	@Override
	public void setUpdated(Calendar updated) {
		if (updated != null) {
			this.updated = (Calendar) updated.clone();
		} else {
			this.updated = null;
		}
	}

}
