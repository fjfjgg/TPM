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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage Runner.
 *
 * <p>Only stores files, does not execute anything.
 *
 * @author Francisco José Fernández Jiménez
 */
public class StorageRunner implements ToolRunner {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(StorageRunner.class);

	/**
	 * Initializes tool runner.
	 *
	 * <p>Gets arguments to add before the command.
	 * <code>executionRestrictions</code> must be an object containing a 
	 * <code>preArgs</code> property with a table of strings (arguments).
	 */
	@Override
	public void init(String exeData, String executionRestrictions) {
		//do nothing
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

		try {
			// Write something in output
			try (PrintWriter out = new PrintWriter(output, StandardCharsets.UTF_8);) {
				out.println("\u2713");
			}
			
			result = 100;
		} catch (final IOException e) {
			result = ERROR_CORRECTOR_EXCEPTION;
			logger.error("{}", e.getMessage());
		}

		return result;

	}

}