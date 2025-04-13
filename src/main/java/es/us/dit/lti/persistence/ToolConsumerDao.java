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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Consumer;

/**
 * The Tool Consumer Data Access Object is the interface providing access to
 * tool consumers related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolConsumerDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolConsumerDao.class);

	/**
	 * Table name of this DAO.
	 */
	public static final String CONSUMER_TABLE_NAME = "consumer";

	/**
	 * SQL statement to get the serial ID of a consumer.
	 */
	private static final String SQL_GET_SID = "SELECT sid FROM " + CONSUMER_TABLE_NAME + " WHERE guid = ?";

	/**
	 * SQL statement to get a consumer by the serial ID.
	 */
	private static final String SQL_GET_BY_SID = "SELECT guid, lti_version, name, version, css_path, created, updated FROM "
			+ CONSUMER_TABLE_NAME + " WHERE sid = ?";

	/**
	 * SQL statement to get an attempt by the consumer GUID.
	 */
	private static final String SQL_GET_BY_GUID = "SELECT sid, lti_version, name, version, css_path, created, updated FROM "
			+ CONSUMER_TABLE_NAME + " WHERE guid = ?";

	/**
	 * SQL statement to create a new record.
	 */
	private static final String SQL_NEW = "INSERT INTO " + CONSUMER_TABLE_NAME
			+ " (guid, lti_version, name, version, css_path, created, updated) " + "VALUES (?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL statement to update the consumer.
	 */
	private static final String SQL_UPDATE = "UPDATE " + CONSUMER_TABLE_NAME
			+ " SET lti_version=?, name=?, version=?, css_path=?, updated = ? " + "WHERE sid = ?";
	
	/**
	 * SQL statement to get serial IDs of consumers without LTI users, tool keys and contexts
	 * (unused).
	 */
	private static final String SQL_GET_UNUSED_CONSUMERS = "SELECT " + CONSUMER_TABLE_NAME + ".sid FROM "
			+ CONSUMER_TABLE_NAME + " LEFT JOIN " + ToolConsumerUserDao.LTI_USER_TABLE_NAME + " ON "
			+ CONSUMER_TABLE_NAME + ".sid=" + ToolConsumerUserDao.LTI_USER_TABLE_NAME + ".consumer_sid LEFT JOIN "
			+ ToolKeyDao.TK_TABLE_NAME + " ON " + CONSUMER_TABLE_NAME + ".sid=" + ToolKeyDao.TK_TABLE_NAME
			+ ".consumer_sid LEFT JOIN " + ToolContextDao.CONTEXT_TABLE_NAME + " ON " + CONSUMER_TABLE_NAME + ".sid="
			+ ToolContextDao.CONTEXT_TABLE_NAME + ".consumer_sid WHERE " + ToolConsumerUserDao.LTI_USER_TABLE_NAME
			+ ".sid IS NULL AND " + ToolKeyDao.TK_TABLE_NAME + ".sid IS NULL AND " + ToolContextDao.CONTEXT_TABLE_NAME
			+ ".sid IS NULL";

	/**
	 * SQL statement to delete unused consumers.
	 */
	private static final String SQL_DELETE_UNUSED_CONSUMERS = "DELETE FROM " + CONSUMER_TABLE_NAME + " WHERE "
			+ CONSUMER_TABLE_NAME + ".sid IN (" + SQL_GET_UNUSED_CONSUMERS + ")";

	/**
	 * SQL statement to delete a consumer by serial ID.
	 */
	private static final String SQL_DELETE = "DELETE FROM " + CONSUMER_TABLE_NAME + " WHERE sid=?";
	
	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private ToolConsumerDao() {
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
	 * Gets a consumer by the GUID.
	 *
	 * @param guid the GUID
	 * @return a consumer or null if not exists
	 */
	public static Consumer getByGuid(String guid) {
		Consumer consumer = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_GUID);) {
			stmt.setString(1, guid);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				consumer = new Consumer();
				consumer.setSid(rs.getInt(1));
				consumer.setGuid(guid);
				consumer.setLtiVersion(rs.getString(2));
				consumer.setName(rs.getString(3));
				consumer.setVersion(rs.getString(4));
				consumer.setCssPath(rs.getString(5));
				consumer.setCreated(DaoUtil.toCalendar(rs.getTimestamp(6)));
				consumer.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(7)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Consumer: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return consumer;
	}

	/**
	 * Gets a record by serial ID.
	 *
	 * @param sid the serial ID
	 * @return the object or null if not found
	 */
	public static Consumer getBySid(int sid) {
		Consumer consumer = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_SID);) {
			stmt.setInt(1, sid);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				consumer = new Consumer();
				consumer.setSid(sid);
				consumer.setGuid(rs.getString(1));
				consumer.setLtiVersion(rs.getString(2));
				consumer.setName(rs.getString(3));
				consumer.setVersion(rs.getString(4));
				consumer.setCssPath(rs.getString(5));
				consumer.setCreated(DaoUtil.toCalendar(rs.getTimestamp(6)));
				consumer.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(7)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Consumer: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return consumer;
	}

	/**
	 * Get the serial ID of a object.
	 *
	 * @param consumer object data
	 * @return true if successful
	 */
	public static boolean getSidByGuid(Consumer consumer) {
		boolean res = false;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_SID);) {
			stmt.setString(1, consumer.getGuid());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res = true;
				consumer.setSid(rs.getInt(1));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Consumer: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return res;
	}

	/**
	 * Update a record.
	 *
	 * @param consumer record data
	 * @return true if successful
	 */
	public static boolean update(Consumer consumer) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE);) {
			int i = 1;
			stmt.setString(i++, consumer.getLtiVersion());
			stmt.setString(i++, consumer.getName());
			stmt.setString(i++, consumer.getVersion());
			stmt.setString(i++, consumer.getCssPath());
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			stmt.setInt(i++, consumer.getSid());
			res = stmt.executeUpdate() == 1;
			if (res) {
				consumer.setUpdated(now);
			}
		} catch (final SQLException e) {
			logger.error("Update", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}

		return res;
	}

	/**
	 * Create a record.
	 *
	 * @param consumer record data
	 * @return true if successful
	 */
	public static boolean create(Consumer consumer) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_NEW);) {
			int i = 1;
			stmt.setString(i++, consumer.getGuid());
			stmt.setString(i++, consumer.getLtiVersion());
			stmt.setString(i++, consumer.getName());
			stmt.setString(i++, consumer.getVersion());
			stmt.setString(i++, consumer.getCssPath());
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // created
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			res = stmt.executeUpdate() == 1;
			if (res) {
				consumer.setCreated(now);
				consumer.setUpdated(now);
			}
		} catch (final SQLException e) {
			logger.error("Create", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}
		if (res) {
			res = getSidByGuid(consumer);
		}

		return res;
	}

	/**
	 * Deletes a record.
	 * 
	 * @param consumer object data
	 * @return true if successful
	 */
	public static boolean delete(Consumer consumer) {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
				stmt.setInt(1, consumer.getSid());
				deleted = stmt.executeUpdate() > 0;
			}
		} catch (final SQLException e) {
			deleted = false;
			logger.error("Delete: ", e);
		} finally {
			dbUtil.closeConnection(conn);
		}
		return deleted;
	}

	/**
	 * Deletes unused consumers.
	 * 
	 * @return true if successful
	 */
	public static boolean deleteUnused() {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_UNUSED_CONSUMERS);) {
				deleted = stmt.executeUpdate() > 0;
			}
		} catch (final SQLException e) {
			deleted = false;
			logger.error("Delete: ", e);
		} finally {
			dbUtil.closeConnection(conn);
		}
		return deleted;
	}

	/**
	 * Get the serial ID of unused consumers.
	 *
	 * @return list of serial IDs of unused consumers
	 */
	public static List<Integer> getUnused() {
		List<Integer> res = new ArrayList<>();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_UNUSED_CONSUMERS);) {
			final ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				res.add(rs.getInt(1));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Consumer: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return res;
	}
	
}
