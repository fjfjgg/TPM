/*
    This file is part of Tool Provider Manager - Manager of LTI Tool Providers
    for learning platforms.
    Copyright (C) 2025  Francisco José Fernández Jiménez.

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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Settings;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

/**
 * Tool Runner with remote corrector via SSH.
 *
 * @author Francisco José Fernández Jiménez
 */
public class SshJavaToolRunner implements ToolRunner {
	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Default remote folder path to copy files.
	 */
	public static final String DEFAULT_REMOTE_FOLDER = "/tmp";

	/**
	 * Configuration of tool.
	 */
	private SshToolConfig tc = null;

	/**
	 * SSH Private key. 
	 */
	private String keyName;
	
	/**
	 * Cache of SSHClients.
	 */
	private static Map<String, SSHClient> cache = new HashMap<>();
	
	/**
	 * Count the number of server KO when trying test command.
	 */
	private int koCounter;

	/**
	 * Generates a counter-based server list.
	 *
	 * @param counter the counter value
	 * @return list of servers
	 */
	private List<String> generateServersLine(int counter) {
		final List<String> servers = tc.getServers();
		final List<String> serversOrdered = new ArrayList<>();
		if (servers != null) {
			final int total = servers.size();
			final int modulus = counter % total;
			for (int i = modulus; i < total; i++) {
				serversOrdered.add(servers.get(i));
			}
			for (int i = 0; i < modulus; i++) {
				serversOrdered.add(servers.get(i));
			}
		}
		return serversOrdered;
	}

	/**
	 * Creates a private key file from a list of lines.
	 *
	 * @param lines list of lines
	 * @param name  name of file
	 * @return true if successful
	 */
	private boolean createKeyFile(List<String> lines, String name) {
		boolean res = false;
		try (FileWriter fw = new FileWriter(name, StandardCharsets.UTF_8)) {
			for (final String l : lines) {
				fw.write(l);
				fw.append('\n');
			}
			res = true;
		} catch (final IOException e) {
			// ignore
			res = false;
		}
		if (res) {
			// change permissions to make it work with SSH
			final File kf = new File(name);
			if (!SystemUtils.IS_OS_WINDOWS && (!kf.setReadable(false, false) || !kf.setExecutable(false, false)
					|| !kf.setWritable(false, false) || !kf.setReadable(true) || !kf.setWritable(true))) {
				res = false;
			}
		}
		return res;
	}

	/**
	 * Initializes tool runner.
	 *
	 * <p>Gets arguments to add before the command. <code>executionRestrictions</code>
	 * must be an object containing a <code>preArgs</code> property with a table of
	 * strings (arguments). <code>exeData</code> is the file name of JSON serialized
	 * {@link SshToolConfig}.
	 */
	@Override
	public void init(String exeData, String executionRestrictions) {
		// get working directory.
		final File ed = new File(exeData);

		if (ed.exists() && ed.isFile() && ed.canRead()) {
			try {
				tc = SshToolConfig.fromString(new FileReader(ed, StandardCharsets.UTF_8));
			} catch (final IOException e) {
				// Ignore
				tc = null;
			}
		}
		if (tc != null) {
			checkKeyFile(exeData);
		} else {
			logger.error("Error reading configuration: {}", exeData);
		}

	}

	/**
	 * Creates default environment variables to speed up future executions.
	 *
	 * @param exeData configuration file
	 */
	private void checkKeyFile(String exeData) {
		// process the data as JSON and create environment variables (exeData)

		keyName = exeData + ".key";
		if (tc.getPrivateKeyLines() != null && !tc.getPrivateKeyLines().isEmpty()) {
			// create file and save name in environment variable
			if (!createKeyFile(tc.getPrivateKeyLines(), keyName)) {
				logger.error("Error creating key file.");
			}
		} else {
			// delete old key file if exists
			final File kf = new File(keyName);
			if (kf.exists() && !kf.delete()) {
				logger.error("Error deleting old key file.");
			}
		}
	}

	/**
	 * Execute the tool.
	 */
	@Override
	public int exec(String filePath, String outputPath, String userId, String originalFilename, int counter,
			boolean isInstructor, List<String> extraArgs, long maxSecondsWait) {
		int result = 0;
		final File output = new File(outputPath);
		final File outputErr = new File(outputPath + Settings.OUTPUT_ERROR_EXT);
		try (FileWriter fw = new FileWriter(output, StandardCharsets.UTF_8);
				FileWriter fwe = new FileWriter(outputErr, StandardCharsets.UTF_8)) {
			List<String> servers;
			if (tc != null) {
				if (tc.isRoundRobin()) {
					servers = generateServersLine(counter + koCounter);
					// generate list of server in counter-based order
				} else {
					servers = tc.getServers();
				}
			} else {
				servers = new ArrayList<>();
			}
			if (userId != null && originalFilename != null && userId.equals(tc.getSpecialUser())
					&& originalFilename.equals(tc.getSpecialFile())) {
				// special file
				for (String serveraux : servers) {
					fw.write("<div style='text-align: left'><h3>Sending special file to " + serveraux + "</h3>");
					// send file
					String remotePath = tc.getRemoteFolder() == null ? originalFilename : tc.getRemoteFolder() + "/" + originalFilename;
					if (runScp(serveraux, filePath, remotePath) == 0) {
						// execute
						if (runExec(serveraux, tc.getSpecialCorrector(), fw, fwe, 300) == 0) {
							fw.write("<p>OK</p>");
						} else {
							fw.write("<p>Error</p>");
						}
					}
					fw.write("</div>");
				}
				
			} else {
				// check active server
				String server = null;
				for (String serveraux : servers) {
					logger.debug("Testing server {}...", serveraux);
					// call corrector with "test" to see what works
					int testresult = runExec(serveraux, tc.getCorrector() + " test", fw, fwe, 1);
					if (testresult == 0) {
						server = serveraux;
						logger.info("Server {} OK", serveraux);
					} else if (testresult == ERROR_CORRECTOR_EXCEPTION) {
						// try a second time
						if (runExec(serveraux, tc.getCorrector() + " test", fw, fwe, 1) == 0) {
							server = serveraux;
							logger.info("Server {} OK (2)", serveraux);
						} else {
							logger.info("Server {} KO", serveraux);
						}
					} else if (testresult == ERROR_GENERIC) {
						logger.info("Server {} unreachable", serveraux);
					} else {
						logger.info("Server {} disabled", serveraux);
					}
					if (server != null) {
						break;
					} else {
						koCounter++;
						if (koCounter < 0) {
							koCounter = 0;
						}
					}
				}
				if (server != null) {
					fwe.write("<p>Using server $server.</p>");
					File f = new File(filePath);
					String remoteFile = tc.getRemoteFolder() == null ? f.getName() : tc.getRemoteFolder() + "/" + f.getName();
					// copy
					if (runScp(server, filePath, remoteFile) == 0) {
						// execute
						result = runExec(server, tc.getCorrector() + " " + remoteFile + " " + userId + " "
								+ originalFilename + " " + counter + " " + isInstructor + " " + String.join(" ", extraArgs),
								fw, fwe, maxSecondsWait);
						
					}
				} else {
					fw.write("<p>El servidor de corrección no está disponible. Inténtelo más tarde.</p>\n");
					fw.write("<p>The correction server is not available. Please try again later.</p>\n");
					//should not count as attempt
				}
			}

		} catch (final IOException e) {
			// error
			result = ERROR_CORRECTOR_EXCEPTION;
			logger.error("{}", e.getMessage());
		}

		return result;

	}

	/**
	 * Execute a command in a SSH server.
	 * 
	 * @param server server to connect
	 * @param command command to execute
	 * @param output where to write the standard output
	 * @param outputErr where to write de standard error output
	 * @param maxSecondsWait seconds to wait
	 * @return error code
	 */
	private int runExec(String server, String command, FileWriter output, FileWriter outputErr, long maxSecondsWait) {
		int result = ERROR_CORRECTOR_EXCEPTION;
		SSHClient ssh = connect(server);
		if (ssh != null) {
			try {
				try (Session session = ssh.startSession()) {
					final Session.Command cmd = session.exec(command);
					if (maxSecondsWait > 0) {
						cmd.join(maxSecondsWait, TimeUnit.SECONDS);
						
					} else {
						// infinite wait
						cmd.join();
					}

					String stdout = IOUtils.readFully(cmd.getInputStream()).toString(StandardCharsets.UTF_8);
					String stderr = IOUtils.readFully(cmd.getErrorStream()).toString(StandardCharsets.UTF_8);

					if (!stdout.isEmpty()) {
						// copy stdout
						output.write(stdout);
					}
					if (!stderr.isEmpty()) {
						outputErr.write(stderr);
					}
					result = cmd.getExitStatus();
				}
				
			} catch (IOException e) {
				logger.error("Error during SSH exec: {}", e.getMessage());
				try {
					ssh.disconnect();
				} catch (IOException e1) {
					logger.error("Error disconnect (exec).", e);
				}
			}
		} else {
			result = ERROR_GENERIC;
		}
		return result;
	}

	/**
	 * Transfer file to server.
	 * 
	 * @param server server to transfer
	 * @param localPath path to local file
	 * @param remotePath path to remote file
	 * @return error code
	 */
	private int runScp(String server, String localPath, String remotePath) {
		int result = ERROR_CORRECTOR_EXCEPTION;
		File localFile = new File(localPath);
		if (localFile.exists() && localFile.isFile()) {
			SSHClient ssh = connect(server);
			if (ssh != null) {
				try {
					SCPFileTransfer scp = ssh.newSCPFileTransfer();
					scp.upload(new FileSystemFile(localPath), remotePath);
					result = 0;
				} catch (IOException e) {
					logger.error("Error during SSH transfer: ", e);
					try {
						ssh.disconnect();
					} catch (IOException e1) {
						logger.error("Error disconnect SCP.", e);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Connect to a SSH server.
	 * 
	 * @param hostArg Server with format user@server:port
	 * @return SSHClient connected or null
	 */
	private SSHClient connect(String hostArg) {
		SSHClient ssh = cache.get(hostArg);
		if (ssh != null && !ssh.isConnected()) {
			logger.info("SSH client disconnected!");
			try {
				ssh.disconnect();
			} catch (IOException e) {
				// print
				logger.error("Error disconnect.", e);
			}
			ssh = null;
			cache.put(hostArg, ssh);
		}
		if (ssh == null) {
			String user = System.getProperty("user.name");
			String host;
			int port = 22;

			if (hostArg.contains("@")) {
				String[] userHost = hostArg.split("@", 2);
				user = userHost[0];
				host = userHost[1];
			} else {
				host = hostArg;
			}

			if (host.contains(":")) {
				String[] hostPort = host.split(":", 2);
				host = hostPort[0];
				try {
					port = Integer.parseInt(hostPort[1]);
				} catch (NumberFormatException e) {
					logger.error("Error: Puerto inválido '{}'", hostPort[1]);
				}
			}
			try {
				ssh = new SSHClient();
				ssh.loadKnownHosts();
				ssh.addHostKeyVerifier(new PromiscuousVerifier());
				logger.debug("Connecting to {} port {} as user {}...", host, port, user);
				ssh.setConnectTimeout(2000); //ms
				ssh.connect(host, port);
				final File kf = new File(keyName);
				if (kf.exists()) {
					ssh.authPublickey(user, keyName);
				} else {
					ssh.authPublickey(user);
				}
			} catch (IOException e) {
				logger.error("Error during SSH connection: {}", e.getMessage());
				try {
					ssh.disconnect();
				} catch (IOException e1) {
					logger.error("Error disconnect.", e);
				}
				ssh = null;
			}
			if (ssh != null) {
				ssh.getConnection().setTimeoutMs(2000); // limit wait
				cache.put(hostArg, ssh);
			}
		}
		
		return ssh;
	}
	
	/**
	 * Disconnects SSHClients and clear cache.
	 */
	public static void cleanCache() {
		for (SSHClient ssh : cache.values()) {
			if (ssh != null) {
				try {
					ssh.disconnect();
				} catch (IOException ignored) {
					// Ignored.
				}
			}
		}
		cache.clear();
	}
}
