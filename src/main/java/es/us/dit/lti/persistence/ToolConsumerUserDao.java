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
import es.us.dit.lti.entity.LtiUser;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.entity.ToolKey;

/**
 * The Tool Consumer User Data Access Object is the interface providing access
 * to LTI users related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolConsumerUserDao {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolConsumerUserDao.class);

	/**
	 * Table name of this DAO.
	 */
	public static final String LTI_USER_TABLE_NAME = "lti_user";

	/**
	 * SQL statement to get the serial ID of a LTI user.
	 */
	private static final String SQL_GET_SID = "SELECT sid FROM " + LTI_USER_TABLE_NAME
			+ " WHERE consumer_sid=? AND lti_user_id=?";

	/**
	 * SQL statement to get a LTI user by the serial ID.
	 */
	private static final String SQL_GET_BY_SID = "SELECT consumer_sid, lti_user_id, source_id, "
			+ "name_given, name_family, name_full, email, created, updated FROM " + LTI_USER_TABLE_NAME
			+ " WHERE sid = ?";

	/**
	 * SQL statement to get a LTI user by the serial ID of the consumer and the user
	 * ID.
	 */
	private static final String SQL_GET_BY_ID = "SELECT sid, source_id, "
			+ "name_given, name_family, name_full, email, created, updated FROM " + LTI_USER_TABLE_NAME
			+ " WHERE consumer_sid=? AND lti_user_id=?";

	/**
	 * SQL statement to add a LTI user.
	 */
	private static final String SQL_NEW = "INSERT INTO " + LTI_USER_TABLE_NAME
			+ " (consumer_sid, lti_user_id, source_id, name_given, name_family, name_full, email, created, updated) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL statement to update a LTI user.
	 */
	private static final String SQL_UPDATE = "UPDATE " + LTI_USER_TABLE_NAME
			+ " SET source_id=?, name_given=?, name_family=?, name_full=?, email=?, updated=? WHERE sid = ?";

	/**
	 * SQL statement to delete a LTI user.
	 */
	private static final String SQL_DELETE = "DELETE FROM " + LTI_USER_TABLE_NAME + "WHERE sid = ?";

	/**
	 * SQL statement to get all user full names and source ID that used a tool.
	 */
	private static final String SQL_TOOL_LTI_USERS = "SELECT DISTINCT source_id, name_full" + " FROM "
			+ ToolResourceUserDao.RU_TABLE_NAME + ", " + ToolResourceLinkDao.RL_TABLE_NAME + ", " + LTI_USER_TABLE_NAME
			+ "	WHERE " + ToolResourceUserDao.RU_TABLE_NAME + ".lti_user_sid=" + LTI_USER_TABLE_NAME + ".sid"
			+ "	AND " + ToolResourceUserDao.RU_TABLE_NAME + ".resource_sid=" + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".sid" + " AND " + ToolResourceLinkDao.RL_TABLE_NAME + ".tool_sid=?" + " ORDER BY source_id";

	/**
	 * SQL statement to get all user full names and source ID that used a tool key.
	 */
	private static final String SQL_TOOLKEY_LTI_USERS = "SELECT DISTINCT source_id, name_full" + " FROM "
			+ ToolResourceUserDao.RU_TABLE_NAME + ", " + ToolResourceLinkDao.RL_TABLE_NAME + ", " + LTI_USER_TABLE_NAME
			+ "	WHERE " + ToolResourceUserDao.RU_TABLE_NAME + ".lti_user_sid=" + LTI_USER_TABLE_NAME + ".sid"
			+ "	AND " + ToolResourceUserDao.RU_TABLE_NAME + ".resource_sid=" + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".sid" + " AND " + ToolResourceLinkDao.RL_TABLE_NAME + ".tool_key_sid=?" + " ORDER BY source_id";

	/**
	 * SQL statement to get all serial IDs, full names and source IDs of users that
	 * used a tool key with a specific source ID.
	 */
	private static final String SQL_TOOLKEY_LTI_USERS_BY_SOURCE_ID = "SELECT DISTINCT " + LTI_USER_TABLE_NAME
			+ ".sid, source_id, name_full" + " FROM " + ToolResourceUserDao.RU_TABLE_NAME + ", "
			+ ToolResourceLinkDao.RL_TABLE_NAME + ", " + LTI_USER_TABLE_NAME + " WHERE "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".lti_user_sid=" + LTI_USER_TABLE_NAME + ".sid" + " AND "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".resource_sid=" + ToolResourceLinkDao.RL_TABLE_NAME + ".sid"
			+ "	AND " + ToolResourceLinkDao.RL_TABLE_NAME + ".tool_key_sid=?" + " AND source_id=?";

	/**
	 * SQL statement to get serial IDs of unused LTI users (not using resource links).
	 */
	private static final String SQL_GET_UNUSED_LTI_USERS = "SELECT " + LTI_USER_TABLE_NAME + ".sid FROM "
			+ LTI_USER_TABLE_NAME + " LEFT JOIN " + ToolResourceUserDao.RU_TABLE_NAME + " ON " + LTI_USER_TABLE_NAME
			+ ".sid=" + ToolResourceUserDao.RU_TABLE_NAME + ".lti_user_sid WHERE " + ToolResourceUserDao.RU_TABLE_NAME
			+ ".sid IS NULL";
	
	/**
	 * SQL statement to delete unused LTI users.
	 */
	private static final String SQL_DELETE_UNUSED_LTI_USERS = "DELETE FROM " + LTI_USER_TABLE_NAME + " WHERE "
			+ LTI_USER_TABLE_NAME + ".sid IN (" + SQL_GET_UNUSED_LTI_USERS + ")";
	
	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private ToolConsumerUserDao() {
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
	 * Gets a LTI user by consumer serial ID and user ID.
	 *
	 * @param consumerSid the consumer serial ID
	 * @param userId      the user ID
	 * @return the LTI user or null if not exists
	 */
	public static LtiUser getById(int consumerSid, String userId) {
		LtiUser user = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_ID);) {
			stmt.setInt(1, consumerSid);
			stmt.setString(2, userId);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				user = new LtiUser();
				user.setSid(rs.getInt(1));
				final Consumer aux = new Consumer();
				aux.setSid(consumerSid);
				user.setConsumer(aux);
				user.setUserId(userId);
				user.setSourceId(rs.getString(2));
				user.setNameGiven(rs.getString(3));
				user.setNameFamily(rs.getString(4));
				user.setNameFull(rs.getString(5));
				user.setEmail(rs.getString(6));
				user.setCreated(DaoUtil.toCalendar(rs.getTimestamp(7)));
				user.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(8)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return user;
	}

	/**
	 * Gets a record by serial ID.
	 *
	 * @param sid the serial ID
	 * @return the object or null if not found
	 */
	public static LtiUser getBySid(int sid) {
		LtiUser user = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_SID);) {
			stmt.setInt(1, sid);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				user = new LtiUser();
				user.setSid(sid);
				final Consumer aux = new Consumer();
				aux.setSid(rs.getInt(1));
				user.setConsumer(aux);
				user.setUserId(rs.getString(2));
				user.setSourceId(rs.getString(3));
				user.setNameGiven(rs.getString(4));
				user.setNameFamily(rs.getString(5));
				user.setNameFull(rs.getString(6));
				user.setEmail(rs.getString(7));
				user.setCreated(DaoUtil.toCalendar(rs.getTimestamp(8)));
				user.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(9)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return user;
	}

	/**
	 * Get the serial ID of a object.
	 *
	 * @param user object data
	 * @return true if successful
	 */
	public static boolean getSidByIds(LtiUser user) {
		boolean res = false;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_SID);) {
			stmt.setInt(1, user.getConsumer().getSid());
			stmt.setString(2, user.getUserId());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res = true;
				user.setSid(rs.getInt(1));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return res;
	}

	/**
	 * Update a record.
	 *
	 * @param user record data
	 * @return true if successful
	 */
	public static boolean update(LtiUser user) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE);) {
			int i = 1;
			stmt.setString(i++, user.getSourceId());
			stmt.setString(i++, user.getNameGiven());
			stmt.setString(i++, user.getNameFamily());
			stmt.setString(i++, user.getNameFull());
			stmt.setString(i++, user.getEmail());
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			stmt.setInt(i++, user.getSid());
			res = stmt.executeUpdate() == 1;
			if (res) {
				user.setUpdated(now);
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
	 * @param user record data
	 * @return true if successful
	 */
	public static boolean create(LtiUser user) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_NEW);) {
			int i = 1;
			stmt.setInt(i++, user.getConsumer().getSid());
			stmt.setString(i++, user.getUserId());
			stmt.setString(i++, user.getSourceId());
			stmt.setString(i++, user.getNameGiven());
			stmt.setString(i++, user.getNameFamily());
			stmt.setString(i++, user.getNameFull());
			stmt.setString(i++, user.getEmail());
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // created
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			res = stmt.executeUpdate() == 1;
			if (res) {
				user.setCreated(now);
				user.setUpdated(now);
			}
		} catch (final SQLException e) {
			logger.error("Create", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}
		if (res) {
			res = getSidByIds(user);
		}

		return res;
	}

	/**
	 * Delete a record.
	 *
	 * @param user record data
	 * @return false
	 */
	public static boolean delete(LtiUser user) {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
				stmt.setInt(1, user.getSid());
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
	 * Gets all LTI users that used a tool.
	 *
	 * @param tool the tool
	 * @return list of LTI users (only source id and full name)
	 */
	public static List<LtiUser> getToolLtiUsers(Tool tool) {
		final List<LtiUser> list = new ArrayList<>();

		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_TOOL_LTI_USERS);) {
			stmt.setInt(1, tool.getSid());
			final ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				int i = 1;
				final LtiUser user = new LtiUser();
				user.setSourceId(rs.getString(i++));
				user.setNameFull(rs.getString(i++));
				list.add(user);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Access error", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return list;
	}

	/**
	 * Gets all LTI users that used a tool key.
	 *
	 * @param tk the tool key
	 * @return list of LTI users (only source id and full name)
	 */
	public static List<LtiUser> getToolKeyLtiUsers(ToolKey tk) {
		final List<LtiUser> list = new ArrayList<>();

		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_TOOLKEY_LTI_USERS);) {
			stmt.setInt(1, tk.getSid());
			final ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				int i = 1;
				final LtiUser user = new LtiUser();
				user.setSourceId(rs.getString(i++));
				user.setNameFull(rs.getString(i++));
				list.add(user);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Access error", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return list;
	}

	/**
	 * Gets all serial IDs, full names and source IDs of users that used a tool key
	 * with a specific source ID.
	 *
	 * @param tk       the tool key
	 * @param sourceId the source ID
	 * @return list of LTI users (only source id and full name)
	 */
	public static List<LtiUser> getToolKeyLtiUsers(ToolKey tk, String sourceId) {
		final List<LtiUser> list = new ArrayList<>();

		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_TOOLKEY_LTI_USERS_BY_SOURCE_ID);) {
			stmt.setInt(1, tk.getSid());
			stmt.setString(2, sourceId);
			final ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				int i = 1;
				final LtiUser user = new LtiUser();
				user.setSid(rs.getInt(i++));
				user.setSourceId(rs.getString(i++));
				user.setNameFull(rs.getString(i++));
				list.add(user);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Access error", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return list;
	}
	
	/**
	 * Deletes unused LTI users.
	 * 
	 * @return true if successful
	 */
	public static boolean deleteUnused() {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_UNUSED_LTI_USERS);) {
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
	 * Get the serial ID of unused LTI users.
	 *
	 * @return list of serial IDs of unused LTI users
	 */
	public static List<Integer> getUnused() {
		List<Integer> res = new ArrayList<>();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_UNUSED_LTI_USERS);) {
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
