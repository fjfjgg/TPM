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

package es.us.dit.lti.persistence;

import java.sql.Connection;

/**
 * Utility class that provides methods for managing connections to a database.
 *
 * @author Francisco José Fernández Jiménez
 */
public interface IDbUtil {

	/**
	 * Class init. Throws an SQLException if a connection error occurs.
	 *
	 * @param connectionString DataSource JNDI name or JDBC connection string
	 */
	void init(String connectionString);

	/**
	 * Destroy object. Do not use after destroy.
	 */
	void destroy();

	/**
	 * Returns the database connection and established it if the connection was
	 * closed.
	 *
	 * @return database connection instance
	 */
	Connection getConnection();

	/**
	 * closeConnection close the connection if open.
	 *
	 * @param connection connection to close
	 */
	void closeConnection(Connection connection);

}