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

import java.io.Reader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * Configuration of the {@link ToolRunnerType#TR_SSH} type tool.
 *
 * @author Francisco José Fernández Jiménez
 */
public class SshToolConfig {
	/**
	 * Name of the remote corrector executable.
	 */
	private String corrector;

	/**
	 * Directory where to copy the file delivered remotely.
	 */
	private String remoteFolder;

	/**
	 * Correction server list.
	 */
	private List<String> servers;

	/**
	 * Counter-based server selection, otherwise always selected in the same order.
	 */
	private boolean roundRobin;

	/**
	 * Private SSH key. It can consist of a single line with
	 * <code>\n</code> as a separator or multiple lines. 
	 * Each line is appended with a <code>\n</code> at the end.
	 */
	private List<String> privateKeyLines;

	/**
	 * Username of authorized user for special deliveries.
	 */
	private String specialUser;

	/**
	 * Special file name.
	 */
	private String specialFile;

	/**
	 * Corrector used for special files. Default is the same.
	 */
	private String specialCorrector;

	/**
	 * Gets the name of the remote corrector executable.
	 * 
	 * @return the name of the remote corrector executable.
	 */
	public String getCorrector() {
		return corrector;
	}

	/**
	 * Sets the name of the remote corrector executable.
	 * 
	 * <p>It should support 2 call types:
	 * <ul>
	 * <li><code>$TPMcorrector test   #and return 0 if ok</code>
	 * <li><code>$TPMcorrector $fileremote $TPMconsumerid $TPMfilename $TPMcounter $TPMinstructor ...</code>
	 * </ul>
	 * 
	 * @param corrector name of the remote corrector executable
	 */
	public void setCorrector(String corrector) {
		this.corrector = corrector;
	}

	/**
	 * Gets the directory where to copy the file delivered remotely.
	 * 
	 * @return the directory where to copy the file delivered remotely
	 */
	public String getRemoteFolder() {
		return remoteFolder;
	}

	/**
	 * Sets the directory where to copy the file delivered remotely.
	 *
	 * <p>The path must be absolute if a server's port is not 22.
	 *
	 * @param remoteFolder the directory where to copy the file delivered remotely
	 */
	public void setRemoteFolder(String remoteFolder) {
		this.remoteFolder = remoteFolder;
	}

	/**
	 * Gets the private SSH key. 
	 *
	 * <p>It can consist of a single line with
	 * <code>\n</code> as a separator or multiple lines. 
	 * Each line is appended with a <code>\n</code> at the end.
	 *
	 * @return the private key lines
	 */
	public List<String> getPrivateKeyLines() {
		return privateKeyLines;
	}

	/**
	 * Sets the private SSH key.
	 *
	 * <p>Access with username/password is not allowed.
	 * If null, system default keys are used (shared by all projects).
	 *
	 * @param privateKeyLines lines of private SSH key
	 */
	public void setPrivateKeyLines(List<String> privateKeyLines) {
		this.privateKeyLines = privateKeyLines;
	}

	/**
	 * Gets the correction server list.
	 * 
	 * @return the server list
	 */
	public List<String> getServers() {
		return servers;
	}

	/**
	 * Sets the correction server list.
	 *
	 * <p>An attempt is sent to the first available server.
	 *
	 * <p>If the port is 22, the servers can be written as
	 * <code>user@server</code>,
	 * otherwise as <code>user@server:port</code> and <code>ssh://</code>
	 * or <code>scp://</code>
	 * is added in front of it depending on the command and the name of the
	 * file must be URLencoded.
	 *
	 * @param servers the server list
	 */
	public void setServers(List<String> servers) {
		this.servers = servers;
	}

	/**
	 * Gets if a counter-based server selection is used,
	 * otherwise always selected in the same order.
	 * 
	 * @return true if a counter-based server selection is used
	 */
	public boolean isRoundRobin() {
		return roundRobin;
	}

	/**
	 * Sets counter-based server selection (round robin) or fixed order.
	 * 
	 * @param roundRobin true for counter-based server selection
	 */
	public void setRoundRobin(boolean roundRobin) {
		this.roundRobin = roundRobin;
	}

	/**
	 * Gets the username of authorized user for special deliveries.
	 *
	 * @return the username of authorized user for special deliveries
	 */
	public String getSpecialUser() {
		return specialUser;
	}

	/**
	 * Sets the username of authorized user for special deliveries.
	 *
	 * @param specialUser the username
	 */
	public void setSpecialUser(String specialUser) {
		this.specialUser = specialUser;
	}

	/**
	 * Special file name.
	 * 
	 * @return the file name
	 */
	public String getSpecialFile() {
		return specialFile;
	}

	/**
	 * Sets the special file name.
	 *
	 * <p>The special file sent by the special user is sent to all servers.
	 * It can be null or empty. Only a special user and a special file name are allowed.
	 *
	 * <p>This can be used to perform the same operation simultaneously on all servers.
	 * For example, an update.
	 *
	 * @param specialFile the special file name
	 */
	public void setSpecialFile(String specialFile) {
		this.specialFile = specialFile;
	}

	/**
	 * Gets the corrector name used for special files.
	 *
	 * <p>If null, it is used the normal corrector.
	 *
	 * @return the corrector name used for special files. Default is the same
	 */
	public String getSpecialCorrector() {
		return specialCorrector;
	}

	/**
	 * Sets the corrector name used for special files.
	 * 
	 * <p>By default (null), the same normal corrector.
	 * 
	 * <p>The special corrector will be called with no arguments. That program must
	 * know that the delivered file is in the path <code>$TPMremotefolder/$TPMspecialfile</code>.
	 *
	 * @param specialCorrector the special corrector name
	 */
	public void setSpecialCorrector(String specialCorrector) {
		this.specialCorrector = specialCorrector;
	}

	/**
	 * Deserialize from JSON string.
	 *
	 * @param json JSON string
	 * @return new object
	 */
	public static SshToolConfig fromString(String json) {
		SshToolConfig t = null;
		try {
			t = new Gson().fromJson(json, SshToolConfig.class);
		} catch (final JsonSyntaxException e) {
			// ignore, t is null
			t = null;
		}
		return t;
	}

	/**
	 * Deserialize from JSON Reader.
	 *
	 * @param jsonReader JSON Reader
	 * @return new object
	 */
	public static SshToolConfig fromString(Reader jsonReader) {
		SshToolConfig t = null;
		try {
			t = new Gson().fromJson(jsonReader, SshToolConfig.class);
		} catch (JsonSyntaxException | JsonIOException e) {
			// ignore, t is null
			t = null;
		}
		return t;
	}
}
