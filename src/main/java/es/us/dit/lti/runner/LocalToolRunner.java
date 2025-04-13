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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.config.ExecutionRestrictionsConfig;
import es.us.dit.lti.entity.Settings;

/**
 * Tool Runner with local corrector.
 *
 * @author Francisco José Fernández Jiménez
 */
public class LocalToolRunner implements ToolRunner {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(LocalToolRunner.class);
	/**
	 * Local executable.
	 */
	private String exe;
	/**
	 * Prefixed arguments.
	 */
	private String[] preArgs;

	/**
	 * Initializes tool runner.
	 *
	 * <p>Gets arguments to add before the command. <code>executionRestrictions</code>
	 * must be an object containing a <code>preArgs</code> property with a table of
	 * strings (arguments).
	 */
	@Override
	public void init(String exeData, String executionRestrictions) {
		exe = exeData;
		final ExecutionRestrictionsConfig er = ExecutionRestrictionsConfig.fromString(executionRestrictions);
		if (er != null && er.getPreArgs() != null) {
			preArgs = er.getPreArgs();
		} else {
			preArgs = new String[0];
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
		final ArrayList<String> args = new ArrayList<>(Arrays.asList(preArgs));
		args.add(exe);
		args.add(filePath);
		args.add(userId);
		args.add(originalFilename);
		args.add(String.valueOf(counter));
		args.add(String.valueOf(isInstructor));
		args.addAll(extraArgs);

		try {
			final ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectOutput(output);
			pb.redirectError(outputErr);
			program = pb.start();
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