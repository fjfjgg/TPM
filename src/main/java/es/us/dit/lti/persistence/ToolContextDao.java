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
import es.us.dit.lti.entity.Context;

/**
 * The Tool Context Data Access Object is the interface providing access to tool
 * contexts related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolContextDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolContextDao.class);

	/**
	 * Table name of this DAO.
	 */
	public static final String CONTEXT_TABLE_NAME = "context";

	/**
	 * SQL statement to get the serial ID of a context.
	 */
	private static final String SQL_GET_SID = "SELECT sid FROM " + CONTEXT_TABLE_NAME
			+ " WHERE consumer_sid=? AND context_id=?";

	/**
	 * SQL statement to get a context by the serial ID.
	 */
	private static final String SQL_GET_BY_SID = "SELECT consumer_sid, context_id, label, title, created, updated FROM "
			+ CONTEXT_TABLE_NAME + " WHERE sid = ?";

	/**
	 * SQL statement to get a context by the serial ID of the consumer and the
	 * context ID.
	 */
	private static final String SQL_GET_BY_ID = "SELECT sid, label, title, created, updated FROM " + CONTEXT_TABLE_NAME
			+ " WHERE consumer_sid=? AND context_id=?";

	/**
	 * SQL statement to add a context.
	 */
	private static final String SQL_NEW = "INSERT INTO " + CONTEXT_TABLE_NAME
			+ " (consumer_sid, context_id, label, title, created, updated) " + "VALUES (?, ?, ?, ?, ?, ?)";

	/**
	 * SQL statement to update a context.
	 */
	private static final String SQL_UPDATE = "UPDATE " + CONTEXT_TABLE_NAME + " SET label=?, title=?, updated = ? "
			+ "WHERE sid = ?";
	
	/**
	 * SQL statement to delete a context by serial ID.
	 */
	private static final String SQL_DELETE = "DELETE FROM " + CONTEXT_TABLE_NAME + " WHERE sid=?";
	
	/**
	 * SQL statement to get serial IDs of context without resource links and tool
	 * keys (unused).
	 */
	private static final String SQL_GET_UNUSED_CONTEXTS = "SELECT " + CONTEXT_TABLE_NAME + ".sid FROM "
			+ CONTEXT_TABLE_NAME + " LEFT JOIN " + ToolResourceLinkDao.RL_TABLE_NAME + " ON " + CONTEXT_TABLE_NAME
			+ ".sid=" + ToolResourceLinkDao.RL_TABLE_NAME + ".context_sid LEFT JOIN " + ToolKeyDao.TK_TABLE_NAME
			+ " ON " + CONTEXT_TABLE_NAME + ".sid=" + ToolKeyDao.TK_TABLE_NAME + ".context_sid WHERE "
			+ ToolResourceLinkDao.RL_TABLE_NAME + ".sid IS NULL AND " + ToolKeyDao.TK_TABLE_NAME + ".sid IS NULL";

	/**
	 * SQL statement to delete unused consumers.
	 */
	private static final String SQL_DELETE_UNUSED_CONTEXTS = "DELETE FROM " + CONTEXT_TABLE_NAME + " WHERE "
			+ CONTEXT_TABLE_NAME + ".sid IN (" + SQL_GET_UNUSED_CONTEXTS + ")";

	
	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private ToolContextDao() {
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
	 * Gets a context by serial consumer ID an context ID.
	 *
	 * @param consumer  the consumer serial ID
	 * @param contextId the context ID
	 * @return the attempt if exists or null
	 */
	public static Context getById(Consumer consumer, String contextId) {
		Context context = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_ID);) {
			stmt.setInt(1, consumer.getSid());
			stmt.setString(2, contextId);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				context = new Context();
				context.setSid(rs.getInt(1));
				context.setConsumer(consumer);
				context.setContextId(contextId);
				context.setLabel(rs.getString(2));
				context.setTitle(rs.getString(3));
				context.setCreated(DaoUtil.toCalendar(rs.getTimestamp(4)));
				context.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(5)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Context: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return context;
	}

	/**
	 * Gets a record by serial ID.
	 *
	 * @param sid the serial ID
	 * @return the object or null if not found
	 */
	public static Context getBySid(int sid) {
		Context context = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_SID);) {
			stmt.setInt(1, sid);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				context = new Context();
				context.setSid(sid);
				final Consumer aux = new Consumer();
				aux.setSid(rs.getInt(1));
				context.setConsumer(aux);
				context.setContextId(rs.getString(2));
				context.setLabel(rs.getString(3));
				context.setTitle(rs.getString(4));
				context.setCreated(DaoUtil.toCalendar(rs.getTimestamp(5)));
				context.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(6)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Context: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return context;
	}

	/**
	 * Get the serial ID of a object.
	 *
	 * @param context object data
	 * @return true if successful
	 */
	public static boolean getSidByIds(Context context) {
		boolean res = false;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_SID);) {
			stmt.setInt(1, context.getConsumer().getSid());
			stmt.setString(2, context.getContextId());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res = true;
				context.setSid(rs.getInt(1));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Context Sid: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return res;
	}

	/**
	 * Update a record.
	 *
	 * @param context record data
	 * @return true if successful
	 */
	public static boolean update(Context context) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE);) {
			int i = 1;
			stmt.setString(i++, context.getLabel());
			stmt.setString(i++, context.getTitle());
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			stmt.setInt(i++, context.getSid());
			res = stmt.executeUpdate() == 1;
			if (res) {
				context.setUpdated(now);
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
	 * @param context record data
	 * @return true if successful
	 */
	public static boolean create(Context context) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_NEW);) {
			int i = 1;
			stmt.setInt(i++, context.getConsumer().getSid());
			stmt.setString(i++, context.getContextId());
			stmt.setString(i++, context.getLabel());
			stmt.setString(i++, context.getTitle());
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // created
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			res = stmt.executeUpdate() == 1;
			if (res) {
				context.setCreated(now);
				context.setUpdated(now);
			}
		} catch (final SQLException e) {
			logger.error("Create", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}
		if (res) {
			res = getSidByIds(context);
		}

		return res;
	}

	/**
	 * Delete a record.
	 *
	 * @param context record data
	 * @return false
	 */
	public static boolean delete(Context context) {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
				stmt.setInt(1, context.getSid());
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
	 * Deletes unused context.
	 * 
	 * @return true if successful
	 */
	public static boolean deleteUnused() {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_UNUSED_CONTEXTS);) {
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
	 * Get the serial ID of unused contexts.
	 *
	 * @return list of serial IDs of unused contexts
	 */
	public static List<Integer> getUnused() {
		List<Integer> res = new ArrayList<>();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_UNUSED_CONTEXTS);) {
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
