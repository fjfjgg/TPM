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

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Tool resource link data.
 *
 * <p>Specified in <a href="https://www.imsglobal.org/specs/ltiv1p1">
 * https://www.imsglobal.org/specs/ltiv1p1</a>
 *
 * @author Francisco José Fernández Jiménez
 */
public class ResourceLink extends UpdateRecordEntity {

	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = -6262179525382224929L;

	@Override
	public long getSerialVersionUid() {
		return serialVersionUID;
	}

	/**
	 * This is an opaque unique identifier that the TC guarantees will be unique
	 * within the TC for every placement of the link.
	 */
	private String resourceId;
	/**
	 * A plain text title for the resource.
	 */
	private String title;
	/**
	 * Additional custom properties that can be used to customize the tool
	 * stored together in the same field of the database.
	 */
	private final Properties customProperties = new Properties();
	/**
	 * The tool.
	 */
	private Tool tool;
	/**
	 * The parent context.
	 */
	private Context context;
	/**
	 * This field should be no more than 1023 characters long. This value should not
	 * change from one launch to the next and in general, the TP can expect that
	 * there is a one-to-one mapping between the outcomeServiceUrl and a particular
	 * tool key.
	 */
	private String outcomeServiceUrl;
	/**
	 * Last tool key used, to be able to retrieve the scores or send them later.
	 */
	private ToolKey toolKey;

	/**
	 * Add a property to the custom properties.
	 *
	 * @param name  name of the property
	 * @param value value of the property
	 */
	public void setCustomProperty(String name, String value) {
		if (value != null) {
			if (value.length() > 0) {
				customProperties.put(name, value);
			} else {
				customProperties.remove(name);
			}
		} else if (customProperties.containsKey(name)) {
			customProperties.remove(name);
		}
	}

	/**
	 * Gets a custom property by name.
	 *
	 * @param name name of the property
	 * @return the value or null if it not exists
	 */
	public String getCustomProperty(String name) {
		return customProperties.getProperty(name);
	}

	/**
	 * Sets the custom properties from a JSON string.
	 *
	 * @param propertiesString the JSON string with the customProperties
	 */
	public void setCustomPropertiesFromString(String propertiesString) {
		this.customProperties.clear();
		if (propertiesString != null && !propertiesString.isEmpty()) {
			try {
				final Gson gson = new Gson();
				final Map<String, String> newCustomProperties = gson.fromJson(propertiesString,
						new TypeToken<Map<String, String>>() {
						}.getType());
				this.customProperties.putAll(newCustomProperties);
			} catch (final JsonSyntaxException e) {
				// ignore
				LoggerFactory.getLogger(getClass()).error("JSON exception", e);
			}
		}
	}

	/**
	 * Serialize the custom properties to a JSON string.
	 *
	 * @return custom properties in JSON format
	 */
	public String customPropertiesToString() {
		String propertiesString = null;
		if (!customProperties.isEmpty()) {
			propertiesString = new Gson().toJson(customProperties);
		}
		return propertiesString;
	}

	@Override
	public int hashCode() {
		return Objects.hash(context.getSid(), outcomeServiceUrl, resourceId, customProperties, title, tool.getSid());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ResourceLink other = (ResourceLink) obj;
		return Objects.equals(context.getSid(), other.context.getSid())
				&& Objects.equals(outcomeServiceUrl, other.outcomeServiceUrl)
				&& Objects.equals(resourceId, other.resourceId) && Objects.equals(customProperties, other.customProperties)
				&& Objects.equals(title, other.title) && Objects.equals(tool.getSid(), other.tool.getSid())
				&& Objects.equals(toolKey, other.toolKey);
	}

	/**
	 * Gets the resource link ID.
	 *
	 * @return the resource link ID
	 */
	public String getResourceId() {
		return resourceId;
	}

	/**
	 * Sets the resource link ID.
	 *
	 * @param resourceId the resource link ID to set
	 */
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * Gets the title.
	 *
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 *
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the tool.
	 *
	 * @return the tool
	 */
	public Tool getTool() {
		return tool;
	}

	/**
	 * Sets the tool.
	 *
	 * @param tool the tool to set
	 */
	public void setTool(Tool tool) {
		this.tool = tool;
	}

	/**
	 * Gets the parent context.
	 *
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * Sets the parent context.
	 *
	 * @param context the context to set
	 */
	public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * Gets the outcome service URL.
	 *
	 * @return the outcome service URL
	 */
	public String getOutcomeServiceUrl() {
		return outcomeServiceUrl;
	}

	/**
	 * Sets the outcome service URL.
	 *
	 * @param outcomeServiceUrl the outcome service URL to set
	 */
	public void setOutcomeServiceUrl(String outcomeServiceUrl) {
		this.outcomeServiceUrl = outcomeServiceUrl;
	}

	/**
	 * Gets the took key.
	 *
	 * @return the tool key
	 */
	public ToolKey getToolKey() {
		return toolKey;
	}

	/**
	 * Sets the tool key.
	 *
	 * @param toolKey the tool key to set
	 */
	public void setToolKey(ToolKey toolKey) {
		this.toolKey = toolKey;
	}

}
