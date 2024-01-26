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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that provides methods for managing connections to a database.
 *
 * <p>Only one connection is used and its access is shared between different tasks,
 * but not simultaneously. Access is synchronized with a ReentrantLock.
 *
 * <p>The connection can only be used by one task. It is necessary to call
 * closeConnection to release the lock. Failure to do so will result in a
 * deadlock.
 *
 * @author Francisco José Fernández Jiménez
 * @version 1.0
 */
public class DbUtilSingleConnection implements IDbUtil {
	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());
	/**
	 * A single connection that remains open as long as the object is not destroyed.
	 */
	private Connection connection = null;
	/**
	 * Optional DataSource object.
	 */
	private DataSource dataSource = null;
	/**
	 * JNDI name of a DataSource or JDBC connection string.
	 */
	private String connectionString = null;
	/**
	 * Lock to synchronize access.
	 */
	private final ReentrantLock rl;

	/**
	 * Class constructor.
	 */
	public DbUtilSingleConnection() {
		rl = new ReentrantLock(true);
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
				dataSource = (DataSource) ctx.lookup("java:comp/env/" + connectionString);
			} catch (final NamingException e) {
				logger.info("DataSource not found.");
			}
		}
		return dataSource;
	}

	@Override
	public void init(String connectionString) {
		if (connectionString != null && !connectionString.isEmpty()) {
			this.connectionString = connectionString;
		}

		try {
			getDataSource();
			if (dataSource == null) {
				// Obtain connection
				if (connectionString != null) {
					connection = DriverManager.getConnection(connectionString);
				}
			} else {
				connection = dataSource.getConnection();
				logger.info("Connection created from the DataSource");
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
		if (connection != null) {
			try {
				connection.close();
			} catch (final Exception e) {
				// ignore
				logger.error("Connection close", e);
			}
			connection = null;
		}
		dataSource = null;
		connectionString = null;
	}

	@Override
	public Connection getConnection() {
		try {
			if (connection != null && connection.isClosed()) {
				if (dataSource == null) {
					if (connectionString != null) {
						connection = DriverManager.getConnection(connectionString);
					} else {
						connection = null;
					}
				} else {
					// with DataSource
					connection = dataSource.getConnection();
				}
			}
		} catch (final SQLException e) {
			logger.error("Error getting connection", e);
		}
		if (connection != null) {
			try {
				if (!rl.tryLock(10, java.util.concurrent.TimeUnit.SECONDS)) {
					// The lock could not be obtained.
					logger.error("Lock unavailable. {}, {}", rl.getHoldCount(), rl);
				}
			} catch (final InterruptedException e) {
				logger.error("Interrupted", e);
				Thread.currentThread().interrupt();
			}
		}
		return connection;
	}

	@Override
	public void closeConnection(Connection connection) {
		// Reuse connection
		if (connection != null) {
			rl.unlock();
		}
	}
}
