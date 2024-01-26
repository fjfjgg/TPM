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

package es.us.dit.lti.servlet;

/**
 * Score information to be displayed to users.
 *
 * @author Francisco José Fernández Jiménez
 */
public class ScoreInfo {
	/**
	 * Title of context of this score.
	 */
	private String contextTitle;

	/**
	 * Title of resource link of this score.
	 */
	private String resourceTitle;

	/**
	 * Score/outcome.
	 */
	private String score;

	/**
	 * Gets the title of context of this score.
	 *
	 * @return the context title
	 */
	public String getContextTitle() {
		return contextTitle;
	}

	/**
	 * Sets the title of context of this score.
	 *
	 * @param contextTitle the context title to set
	 */
	public void setContextTitle(String contextTitle) {
		this.contextTitle = contextTitle;
	}

	/**
	 * Gets the title of resource link of this score.
	 *
	 * @return the resource title
	 */
	public String getResourceTitle() {
		return resourceTitle;
	}

	/**
	 * Sets the title of resource link of this score.
	 *
	 * @param resourceTitle the resource title to set
	 */
	public void setResourceTitle(String resourceTitle) {
		this.resourceTitle = resourceTitle;
	}

	/**
	 * Gets the score.
	 *
	 * @return the score
	 */
	public String getScore() {
		return score;
	}

	/**
	 * Sets the score.
	 *
	 * @param score the score to set
	 */
	public void setScore(String score) {
		this.score = score;
	}
}
