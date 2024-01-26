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

package es.us.dit.lti.runner;

/**
 * Class for building Tool Runners according to Tool Runner Type.
 *
 * @author Francisco José Fernández Jiménez
 */
public class ToolRunnerFactory {

	/**
	 * Can not create objects.
	 */
	private ToolRunnerFactory() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Buils Tool Runner according to Tool Runner Type.
	 *
	 * @param type type of Tool Runner
	 * @return the tool runner
	 */
	public static ToolRunner fromType(ToolRunnerType type) {
		ToolRunner tr = null;
		switch (type) {
		case TR_LOCAL:
			tr = new LocalToolRunner();
			break;
		case TR_SSH:
			tr = new SshToolRunner();
			break;
		case TR_HTTP:
			tr = new HttpToolRunner();
			break;
		case TR_STORAGE:
			tr = new StorageRunner();
			break;
		default:
			tr = new DummyRunner();
			break;
		}
		return tr;
	}
}
