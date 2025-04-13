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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Nonce;

/**
 * The Tool Nonce Data Access Object is the interface providing access to
 * tool nonces related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolNonceDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolNonceDao.class);

	/**
	 * Table name of this DAO.
	 */
	public static final String NONCE_TABLE_NAME = "nonce";

	/**
	 * SQL statement to get a nonce by its values.
	 */
	private static final String SQL_GET = "SELECT value FROM " + NONCE_TABLE_NAME
			+ " WHERE key_sid=? AND consumer_sid=? AND value=? AND ts=?";

	/**
	 * SQL statement to add a nonce.
	 */
	private static final String SQL_NEW = "INSERT INTO " + NONCE_TABLE_NAME
			+ " (key_sid, consumer_sid, value, ts, expires) VALUES (?, ?, ?, ?, ?)";

	/**
	 * SQL statement to delete a nonce.
	 */
	private static final String SQL_DELETE = "DELETE FROM " + NONCE_TABLE_NAME + " WHERE expires <= ?";
	
	
	/**
	 * SQL statement to delete a nonce.
	 */
	private static final String SQL_DELETE_ALL = "DELETE FROM " + NONCE_TABLE_NAME;

	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private ToolNonceDao() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Sets the db utility class.
	 *
	 * @param dbu the db utility class to set
	 */
	public static synchronized void setDbUtil(IDbUtil dbu) {
		dbUtil = dbu;
	}

	/**
	 * Gets the db utility class.
	 *
	 * @return the db utility class
	 */
	public static synchronized IDbUtil getDbUtil() {
		return dbUtil;
	}

	/**
	 * Deletes a expired nonce.
	 */
	public static void deleteExpired() {
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete expired nonces
			final Timestamp now = new Timestamp(System.currentTimeMillis());
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
				stmt.setTimestamp(1, now);
				stmt.executeUpdate();
			}
		} catch (final SQLException e) {
			logger.error("deleteExpired", e);
		} finally {
			dbUtil.closeConnection(conn);
		}
	}

	/**
	 * Deletes all nonces.
	 */
	public static void deleteAll() {
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete expired nonces
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL);) {
				stmt.executeUpdate();
			}
		} catch (final SQLException e) {
			logger.error("deleteAll", e);
		} finally {
			dbUtil.closeConnection(conn);
		}
	}
	
	/**
	 * Checks if nonce exists.
	 *
	 * @param nonce nonce data
	 * @return true if it exists
	 */
	public static boolean exist(Nonce nonce) {
		boolean ok = false;
		final Connection conn = dbUtil.getConnection();
		try {
			try (PreparedStatement stmt = conn.prepareStatement(SQL_GET);) {
				stmt.setInt(1, nonce.getKeyId());
				stmt.setInt(2, nonce.getConsumerId());
				stmt.setString(3, nonce.getValue());
				stmt.setInt(4, nonce.getTs());
				final ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					ok = true;
				}
				rs.close();
			}
		} catch (final SQLException e) {
			logger.error("Load", e);
			ok = false;
		} finally {
			dbUtil.closeConnection(conn);
		}
		return ok;
	}

	/**
	 * Create a nonce.
	 *
	 * @param nonce nonce data
	 * @return true if successful
	 */
	public static boolean create(Nonce nonce) {
		boolean ok = false;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_NEW);) {
			stmt.setInt(1, nonce.getKeyId());
			stmt.setInt(2, nonce.getConsumerId());
			stmt.setString(3, nonce.getValue());
			stmt.setInt(4, nonce.getTs());
			stmt.setTimestamp(5, new Timestamp(nonce.getExpires().getTimeInMillis()));
			ok = stmt.executeUpdate() == 1;
		} catch (final SQLException e) {
			logger.error("Save", e);
		} finally {
			dbUtil.closeConnection(conn);
		}
		return ok;
	}

}
