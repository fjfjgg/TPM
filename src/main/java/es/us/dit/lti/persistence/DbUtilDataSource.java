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
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a connection to a database, with a DataSource with
 * multiple connections.
 *
 * @author Francisco José Fernández Jiménez
 * @version 1.0
 */
public class DbUtilDataSource implements IDbUtil {
	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());
	/**
	 * The DataSource object.
	 */
	private DataSource dataSource = null;
	/**
	 * JNDI resource name of datasource.
	 */
	private String resourceName = null;

	/**
	 * Class constructor.
	 */
	public DbUtilDataSource() {
		// empty
	}

	@Override
	public void init(String resourceName) {
		if (resourceName != null && !resourceName.isEmpty()) {
			this.resourceName = resourceName;
		}

		try {
			getDataSource();
			if (dataSource == null) {
				logger.info("Error de conexión a DataSource: {}", resourceName);
			}
		} catch (final Exception e) {
			logger.info("Excepción al inicializar", e);
		}
	}

	@Override
	public void destroy() {
		dataSource = null;
		resourceName = null;
	}

	/**
	 * Gets the DtaSource object or null if not used.
	 *
	 * @return the datasource
	 */
	private DataSource getDataSource() {
		if (dataSource == null) {
			try {
				final InitialContext ctx = new InitialContext();
				dataSource = (DataSource) ctx.lookup("java:comp/env/" + resourceName);
			} catch (final NamingException e) {
				logger.info("DataSource not found.");
			}
		}
		return dataSource;
	}

	@Override
	public Connection getConnection() {
		Connection c = null;
		try {
			if (dataSource != null) {
				c = dataSource.getConnection();
			}
		} catch (final SQLException e) {
			logger.error("Error getting connection", e);
		}
		return c;
	}

	@Override
	public void closeConnection(Connection connection) {
		// La conexión no se cierra nunca y se reutiliza
		if (connection != null) {
			try {
				connection.close();
			} catch (final SQLException e) {
				// ignore
				logger.error("Connection close", e);
			}
		}
	}
}
