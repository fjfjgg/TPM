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

import java.util.List;

/**
 * A tool runner is the object that actually performs the assessment of an
 * intent.
 *
 * @author Francisco José Fernández Jiménez
 */
public interface ToolRunner {
	/**
	 * Error constants during assessment.
	 */

	/**
	 * Generic error.
	 */
	int ERROR_GENERIC = 101;
	/**
	 * Exception in corrector.
	 */
	int ERROR_CORRECTOR_EXCEPTION = 111;
	/**
	 * Exception in tool runner.
	 */
	int ERROR_RUNNER_EXCEPTION = 112;
	/**
	 * Not assessment due to maximum number of concurrent deliveries exceeded.
	 */
	int ERROR_CONCURRENT_EXCEPTION = 113;
	/**
	 * Maximum assessment time exceeded.
	 */
	int ERROR_TIMEOUT = 137;
	/**
	 * Error writing outcome in tool consumer.
	 */
	int ERROR_WRITE_OUTCOME = 102;

	/**
	 * Initialization.
	 *
	 * @param exeData               File with configuration of the corrector (or
	 *                              local corrector)
	 * @param executionRestrictions Additional restrictions placed on the tool.
	 */
	void init(String exeData, String executionRestrictions);

	/**
	 * Execute the tool.
	 *
	 * @param filePath         uploaded file path
	 * @param outputPath       path of file to write results/output
	 * @param userId           tool consumer ID
	 * @param originalFilename original file name given by user
	 * @param counter          tool counter
	 * @param isInstructor     if user is instructor
	 * @param extraArgs        list of extra arguments
	 * @param maxSecondsWait   maximum number of seconds it should take to finish
	 * @return score/outcome, ok if [0-100], error if &gt; 100
	 */
	int exec(String filePath, String outputPath, String userId, String originalFilename, int counter,
			boolean isInstructor, List<String> extraArgs, long maxSecondsWait);

}
