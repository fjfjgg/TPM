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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.config.ExecutionRestrictionsConfig;

/**
 * Dummy Runner.
 *
 * <p>Only wait, do not execute corrector.
 *
 * @author Francisco José Fernández Jiménez
 */
public class DummyRunner implements ToolRunner {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DummyRunner.class);

	/**
	 * Dummy executable.
	 */
	private String exe;
	/**
	 * dummy prefixed arguments.
	 */
	private String[] preArgs;

	/**
	 * Initializes tool runner.
	 *
	 * <p>Gets arguments to add before the command.
	 * <code>executionRestrictions</code> must be an object containing a 
	 * <code>preArgs</code> property with a table of strings (arguments).
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
	 * 
	 * @param extraArgs the number of seconds to wait (5 by default)
	 */
	@Override
	public int exec(String filePath, String outputPath, String userId, String originalFilename, int counter,
			boolean isInstructor, List<String> extraArgs, long maxSecondsWait) {

		int result = 0;
		final File output = new File(outputPath);
		final ArrayList<String> args = new ArrayList<>(Arrays.asList(preArgs));
		args.add(exe);
		args.add(filePath);
		args.add(userId);
		args.add(originalFilename);
		args.add(String.valueOf(counter));
		args.add(String.valueOf(isInstructor));
		args.addAll(extraArgs);

		// Use the first extra argument as timeout
		if (!extraArgs.isEmpty()) {
			try {
				maxSecondsWait = Long.parseLong(extraArgs.get(0));
			} catch (final NumberFormatException e) {
				// ignore
				logger.info("First extra argument must be a integer (seconds of waiting).");
			}
		}

		if (maxSecondsWait <= 0) {
			maxSecondsWait = 5; // Default
		}

		try {
			// Write something in output
			try (PrintWriter out = new PrintWriter(output, StandardCharsets.UTF_8);) {
				out.println("<pre>");
				int i = 0;
				for (final String arg : args) {
					out.println(i++ + ":[" + arg + "]");
				}
				out.println("</pre>");
			}
			Thread.sleep(maxSecondsWait * 1000);
			result = 100;
		} catch (final InterruptedException e) {
			result = ERROR_CORRECTOR_EXCEPTION;
			logger.error("{}", e.getMessage());
			// Restore interrupted state...
			Thread.currentThread().interrupt();
		} catch (final IOException e) {
			result = ERROR_CORRECTOR_EXCEPTION;
			logger.error("{}", e.getMessage());
		}

		return result;

	}

}