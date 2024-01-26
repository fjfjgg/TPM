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

package es.us.dit.lti.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Additional restrictions that will apply when running tools from a management user.
 *
 * <p>Currently you can only set a series of arguments that will be prefixed
 * to the commands to be executed. This allows for controlled execution.
 *
 * @author Francisco José Fernández Jiménez
 */
public class ExecutionRestrictionsConfig {
	/**
	 * Arguments to be added before the executables.
	 */
	private String[] preArgs;

	/**
	 * Gets the prefixed arguments.
	 *
	 * @return the array preArgs
	 */
	public String[] getPreArgs() {
		return preArgs;
	}

	/**
	 * Sets the prefixed arguments.
	 *
	 * @param preArgs new value
	 */
	public void setPreArgs(String[] preArgs) {
		this.preArgs = preArgs;
	}

	/**
	 * Deserialize from JSON string.
	 *
	 * @param json JSON string
	 * @return the deserialized object
	 */
	public static ExecutionRestrictionsConfig fromString(String json) {
		ExecutionRestrictionsConfig t = null;
		try {
			t = new Gson().fromJson(json, ExecutionRestrictionsConfig.class);
		} catch (JsonSyntaxException e) {
			//ignore, t is null
			t = null;
		}
		return t;
	}
}
