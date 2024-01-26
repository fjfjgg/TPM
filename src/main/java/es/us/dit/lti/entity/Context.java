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
 * Tool context data.
 *
 * <p>Specified in <a href="https://www.imsglobal.org/specs/ltiv1p1">
 * https://www.imsglobal.org/specs/ltiv1p1</a>
 *
 * @author Francisco José Fernández Jiménez
 */
public class Context extends UpdateRecordEntity {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 643093883086335781L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * This is an opaque identifier that uniquely identifies the context that
	 * contains the link being launched.
	 */
	private String contextId;
	/**
	 * A plain text label for the context, intended to fit in a column.
	 */
	private String label;
	/**
	 * A plain text title of the context. It should be about the length of a line.
	 */
	private String title;
	/**
	 * Parent tool consumer.
	 */
	private Consumer consumer;

	/**
	 * Gets an opaque identifier that uniquely identifies the context that contains
	 * the link being launched.
	 *
	 * @return the context ID
	 */
	public String getContextId() {
		return contextId;
	}

	/**
	 * Sets the context ID.
	 *
	 * @param contextId the context ID
	 */
	public void setContextId(String contextId) {
		this.contextId = contextId;
	}

	/**
	 * Gets a plain text label for the context, intended to fit in a column.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the label.
	 *
	 * @param label the label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Gets a plain text title of the context.
	 *
	 * <p>It should be about the length of a line.
	 *
	 * @return the context title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 *
	 * @param title the title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the parent tool consumer.
	 *
	 * @return the parent tool consumer
	 */
	public Consumer getConsumer() {
		return consumer;
	}

	/**
	 * Sets the tool consumer.
	 *
	 * @param consumer the tool consumer
	 */
	public void setConsumer(Consumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public int hashCode() {
		return Objects.hash(consumer.getSid(), contextId, label, title);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Context other = (Context) obj;
		return Objects.equals(consumer.getSid(), other.consumer.getSid()) && Objects.equals(label, other.label)
				&& Objects.equals(title, other.title);
	}

}
