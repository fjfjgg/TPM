/*
    This file is part of Tool Provider Manager - Manager of LTI Tool Providers
    for learning platforms.
    Copyright (C) 2023  Francisco José Fernández Jiménez.

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
 * Unused object information to be displayed to users.
 *
 * @author Francisco José Fernández Jiménez
 */
public class UnusedInfo {
	/**
	 * Number of unused consumer/LMS users.
	 */
	private int users;

	/**
	 * Number of unused resource users.
	 */
	private int resourceUsers;

	/**
	 * Number of unused resource links.
	 */
	private int resourceLinks;

	/**
	 * Number of unused contexts.
	 */
	private int contexts;

	/**
	 * Number of unused consumers.
	 */
	private int consumers;

	/**
	 * Gets the number of unused users.
	 * 
	 * @return the number of users
	 */
	public int getUsers() {
		return users;
	}

	/**
	 * Sets de number of unused users.
	 * 
	 * @param users the number of users to set
	 */
	public void setUsers(int users) {
		this.users = users;
	}

	/**
	 * Gets the number of unused resource users.
	 * 
	 * @return the number of resourceUsers
	 */
	public int getResourceUsers() {
		return resourceUsers;
	}

	/**
	 * Gets the number of unused resource users.
	 * 
	 * @param resourceUsers the number of resourceUsers to set
	 */
	public void setResourceUsers(int resourceUsers) {
		this.resourceUsers = resourceUsers;
	}

	/**
	 * Gets the number of unused resource links.
	 * 
	 * @return the number of resourceLinks
	 */
	public int getResourceLinks() {
		return resourceLinks;
	}

	/**
	 * Sets the number of unused resource links.
	 * 
	 * @param resourceLinks the number of resourceLinks to set
	 */
	public void setResourceLinks(int resourceLinks) {
		this.resourceLinks = resourceLinks;
	}

	/**
	 * Gets the number of unused contexts.
	 * 
	 * @return the number of contexts
	 */
	public int getContexts() {
		return contexts;
	}

	/**
	 * Sets the number of unused contexts.
	 * 
	 * @param contexts the number of contexts to set
	 */
	public void setContexts(int contexts) {
		this.contexts = contexts;
	}

	/**
	 * Gets the number of unused consumers.
	 * 
	 * @return the number of consumers
	 */
	public int getConsumers() {
		return consumers;
	}

	/**
	 * Sets the number of unused consumers.
	 * 
	 * @param consumers the number of consumers to set
	 */
	public void setConsumers(int consumers) {
		this.consumers = consumers;
	}

}
