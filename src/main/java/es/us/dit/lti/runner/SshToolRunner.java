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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.config.ExecutionRestrictionsConfig;
import es.us.dit.lti.entity.Settings;

/**
 * Tool Runner with remote corrector via SSH.
 *
 * @author Francisco José Fernández Jiménez
 */
public class SshToolRunner implements ToolRunner {
	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Shell to execute auxiliary script.
	 */
	public static final String BIN_NAME = "sh";
	/**
	 * Auxiliary script path.
	 */
	public static final String SCRIPT_PATH = "GenSshTool.sh";
	/**
	 * Default remote folder path to copy files.
	 */
	public static final String DEFAULT_REMOTE_FOLDER = "/tmp";

	// Environment variable names
	/**
	 * Environment variable name with corrector name.
	 */
	public static final String VAR_CORRECTOR = "TPMcorrector";
	/**
	 * Environment variable name with consumer user ID.
	 */
	public static final String VAR_CONSUMERID = "TPMconsumerid";
	/**
	 * Environment variable name with delivery file path.
	 */
	public static final String VAR_FILEPATH = "TPMfilepath";
	/**
	 * Environment variable name with URLencoded file name. 
	 */
	public static final String VAR_FILE_URLENCODED = "TPMfileurlencoded";
	/**
	 * Environment variable name with original file name.
	 */
	public static final String VAR_FILENAME = "TPMfilename";
	/**
	 * Environment variable name with tool counter value.
	 */
	public static final String VAR_COUNTER = "TPMcounter";
	/**
	 * Environment variable name with indication of whether the user is an
	 * instructor.
	 */
	public static final String VAR_INSTRUCTOR = "TPMinstructor";
	/**
	 * Environment variable name with extra arguments.
	 */
	public static final String VAR_EXTRA = "TPMextraargs";
	/**
	 * Environment variable name with remote folder name.
	 */
	public static final String VAR_REMOTEFOLDER = "TPMremotefolder";
	/**
	 * Environment variable name with list of servers.
	 */
	public static final String VAR_SERVERS = "TPMservers";
	/**
	 * Environment variable name with SSH private key file name.
	 */
	public static final String VAR_SSHKEY = "TPMsshkey";
	/**
	 * Environment variable name with special user name.
	 */
	public static final String VAR_SPECIAL_USER = "TPMspecialuser";
	/**
	 * Environment variable name with special file name.
	 */
	public static final String VAR_SPECIAL_FILE = "TPMspecialfile";
	/**
	 * Environment variable name with special corrector name.
	 */
	public static final String VAR_SPECIAL_CORRECTOR = "TPMspecialcorrector";

	/**
	 * Prefixed arguments.
	 */
	private String[] preArgs;
	/**
	 * Auxiliary script to execute tool via SSH.
	 */
	private String script;
	/**
	 * Working directory.
	 */
	private File workingDirectory;
	/**
	 * Default environment variables.
	 */
	private final Map<String, String> env = new HashMap<>();
	/**
	 * Configuration of tool.
	 */
	private SshToolConfig tc = null;

	/**
	 * Generates a counter-based server list.
	 *
	 * @param counter the counter value
	 * @return list of servers
	 */
	private String generateServersLine(int counter) {
		final StringBuilder sb = new StringBuilder();
		final List<String> servers = tc.getServers();
		if (servers != null) {
			final int total = servers.size();
			final int modulus = counter % total;
			for (int i = modulus; i < total; i++) {
				sb.append(servers.get(i));
				sb.append(" ");
			}
			for (int i = 0; i < modulus; i++) {
				sb.append(servers.get(i));
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	/**
	 * Creates a private key file from a list of lines.
	 *
	 * @param lines list of lines
	 * @param name name of file
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
			if (!kf.setReadable(false, false) || !kf.setExecutable(false, false) || !kf.setWritable(false, false)
					|| !kf.setReadable(true) || !kf.setWritable(true)) {
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
		workingDirectory = ed.getParentFile();

		if (ed.exists() && ed.isFile() && ed.canRead()) {
			try {
				tc = SshToolConfig.fromString(new FileReader(ed, StandardCharsets.UTF_8));
			} catch (final IOException e) {
				// Ignore
				tc = null;
			}
		}
		if (tc != null) {
			createDefaultEnvVars(exeData);
		} else {
			logger.error("Error reading configuration: {}", exeData);
		}

		try (InputStream is = getClass().getResourceAsStream(SCRIPT_PATH);) {
			if (is != null) {
				script = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			} else {
				script = "echo ERROR SCRIPT DOES NOT EXIST; exit " + ERROR_RUNNER_EXCEPTION;
			}
		} catch (final IOException e) {
			script = "echo ERROR READING SCRIPT; exit " + ERROR_RUNNER_EXCEPTION;
		}

		final ExecutionRestrictionsConfig er = ExecutionRestrictionsConfig.fromString(executionRestrictions);
		if (er != null && er.getPreArgs() != null) {
			preArgs = er.getPreArgs();
		} else {
			preArgs = new String[0];
		}
	}

	/**
	 * Creates default environment variables to speed up future executions.
	 *
	 * @param exeData configuration file
	 */
	private void createDefaultEnvVars(String exeData) {
		// process the data as json and create environment variables (exeData)
		if (tc.getCorrector() != null) {
			env.put(VAR_CORRECTOR, tc.getCorrector());
		}
		if (tc.getRemoteFolder() != null) {
			env.put(VAR_REMOTEFOLDER, tc.getRemoteFolder());
		}
		if (tc.getSpecialUser() != null) {
			env.put(VAR_SPECIAL_USER, tc.getSpecialUser());
		}
		if (tc.getSpecialFile() != null) {
			env.put(VAR_SPECIAL_FILE, tc.getSpecialFile());
		}
		if (tc.getSpecialCorrector() != null) {
			env.put(VAR_SPECIAL_CORRECTOR, tc.getSpecialCorrector());
		}
		if (!tc.isRoundRobin()) {
			// generate list of server now
			env.put(VAR_SERVERS, generateServersLine(0));
		}

		final String keyName = exeData + ".key";
		if (tc.getPrivateKeyLines() != null && !tc.getPrivateKeyLines().isEmpty()) {
			// create file and save name in environment variable
			if (createKeyFile(tc.getPrivateKeyLines(), keyName)) {
				env.put(VAR_SSHKEY, keyName);
			} else {
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
		Process program;
		final File output = new File(outputPath);
		final File outputErr = new File(outputPath + Settings.OUTPUT_ERROR_EXT);
		final ArrayList<String> args = new ArrayList<>();
		final String fileUrlEncoded = URLEncoder.encode(new File(filePath).getName(), StandardCharsets.UTF_8);
		// Add preArgs
		args.addAll(Arrays.asList(preArgs));
		args.add(BIN_NAME);
		try {
			final ProcessBuilder pb = new ProcessBuilder(args);
			pb.directory(workingDirectory);
			pb.redirectOutput(output);
			pb.redirectError(outputErr);
			final Map<String, String> penvs = pb.environment();
			penvs.putAll(env);
			penvs.put(VAR_FILEPATH, filePath);
			penvs.put(VAR_FILE_URLENCODED, fileUrlEncoded);
			penvs.put(VAR_CONSUMERID, userId);
			penvs.put(VAR_FILENAME, originalFilename);
			penvs.put(VAR_COUNTER, String.valueOf(counter));
			penvs.put(VAR_INSTRUCTOR, String.valueOf(isInstructor));
			penvs.put(VAR_EXTRA, String.join(" ", extraArgs));
			if (tc != null && tc.isRoundRobin()) {
				// generate list of server in counter-based order
				penvs.put(VAR_SERVERS, generateServersLine(counter));
			}

			// start
			program = pb.start();
			// pass commands to the shell
			final PrintStream ps = new PrintStream(program.getOutputStream(), true, StandardCharsets.UTF_8);
			ps.println(script);
			ps.close();
			if (maxSecondsWait > 0) {
				final boolean terminado = program.waitFor(maxSecondsWait, TimeUnit.SECONDS);
				if (!terminado) {
					program.destroyForcibly().waitFor(1, TimeUnit.MINUTES);
					// wait 1 minute after kill for security
				}
			} else {
				// infinite wait
				program.waitFor();
			}

			result = program.exitValue();

		} catch (final InterruptedException e) {
			// error
			result = ERROR_CORRECTOR_EXCEPTION;
			logger.error("{}", e.getMessage());
			// Restore interrupted state...
			Thread.currentThread().interrupt();
		} catch (final IOException e) {
			// error
			result = ERROR_CORRECTOR_EXCEPTION;
			logger.error("{}", e.getMessage());
		}

		return result;

	}

}