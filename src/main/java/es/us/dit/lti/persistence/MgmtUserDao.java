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

import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.SecurityUtil;
import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.MgmtUserType;

/**
 * The Management User Data Access Object is the interface providing access to
 * management users related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class MgmtUserDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(MgmtUserDao.class);

	/**
	 * ID of db field is_local.
	 */
	private static final String IS_LOCAL_ID = "is_local";
	/**
	 * Error to log when AutoCommit can not be set to false.
	 */
	private static final String ERROR_AUTOCOMMIT = "Failed to set AutoCommit to false";

	/**
	 * Table name of this DAO.
	 */
	public static final String USER_TABLE_NAME = "mgmt_user";

	/**
	 * SQL statement to create a new record.
	 */
	private static final String SQL_CREATE = "INSERT INTO " + USER_TABLE_NAME
			+ "(username, name_full, email, password, type, is_local, exe_restrictions, created, updated) VALUES (?,?,?,?,?,?,?,?,?)";

	/**
	 * SQL statement to disassociate all users of a tool.
	 */
	private static final String SQL_UNASSIGN_ALL = "DELETE FROM " + ToolDao.TOOL_USER_TABLE_NAME + " WHERE user_sid=?";

	/**
	 * SQL statement to delete a record by SID.
	 */
	private static final String SQL_DELETE = "DELETE FROM " + USER_TABLE_NAME + " WHERE sid=?";

	/**
	 * SQL statement to change user password.
	 */
	private static final String SQL_UPDATE_PASSWORD = "UPDATE " + USER_TABLE_NAME
			+ " SET password = ?, updated = ? WHERE username = ? ";

	/**
	 * SQL statement to get a record by username.
	 */
	private static final String SQL_GET = "SELECT sid, name_full, email, type, is_local, exe_restrictions, created, updated FROM "
			+ USER_TABLE_NAME + " WHERE username=?";

	/**
	 * SQL statement to get all management users except superusers.
	 */
	private static final String SQL_GET_ALL = "SELECT sid, username, name_full, email, type, is_local FROM "
			+ USER_TABLE_NAME + " WHERE " + USER_TABLE_NAME + ".type!=" + MgmtUserType.SUPER.getCode();

	/**
	 * SQL statement to get logged user data by username.
	 */
	private static final String SQL_LOGIN = "SELECT sid, username, name_full, email, password, type, is_local, exe_restrictions, created, updated FROM "
			+ USER_TABLE_NAME + " WHERE username=?";

	/**
	 * SQL statement to get all usernames that can be associated to a tool (except
	 * superusers).
	 */
	private static final String SQL_GET_ASSOCIATE = "SELECT username FROM " + USER_TABLE_NAME + " WHERE " + USER_TABLE_NAME
			+ ".type!=" + MgmtUserType.SUPER.getCode() + " AND sid NOT IN (SELECT user_sid FROM "
			+ ToolDao.TOOL_USER_TABLE_NAME + " where tool_sid=?)";

	/**
	 * SQL statement to get all usernames that can be disassociated of a tool
	 * (except current user and superusers).
	 */
	private static final String SQL_GET_DISASSOCIATE = "SELECT username FROM " + ToolDao.TOOL_USER_TABLE_NAME + ", "
			+ USER_TABLE_NAME + " WHERE " + ToolDao.TOOL_USER_TABLE_NAME + ".user_sid=" + USER_TABLE_NAME
			+ ".sid AND tool_sid=? AND " + ToolDao.TOOL_USER_TABLE_NAME + ".type>? AND " + USER_TABLE_NAME + ".type!="
			+ MgmtUserType.SUPER.getCode() + " AND username!=?";
	
	/**
	 * SQL statement to update a management user.
	 */
	public static final String SQL_UPDATE = "UPDATE " + USER_TABLE_NAME
			+ " SET name_full=?, email=?, type=?, is_local=?, exe_restrictions=?, updated=? WHERE sid=?";

	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private MgmtUserDao() {
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
	 * Add a new record to db.
	 *
	 * @param user user data
	 * @return true if successful
	 * @throws InvalidKeySpecException when could not generate a password hash
	 */
	public static synchronized boolean add(MgmtUser user) throws InvalidKeySpecException {
		boolean result = false;
		final String username = user.getUsername();
		// If the user does not exist we register it
		if (get(username) == null) {
			logger.info("User: {} not registered, creating new.", username);
			final Connection conn = dbUtil.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(SQL_CREATE);) {
				int i = 1;
				stmt.setString(i++, username);
				if (user.getNameFull() == null) {
					stmt.setNull(i++, java.sql.Types.VARCHAR);
				} else {
					stmt.setString(i++, user.getNameFull());
				}
				if (user.getEmail() == null) {
					stmt.setNull(i++, java.sql.Types.VARCHAR);
				} else {
					stmt.setString(i++, user.getEmail());
				}
				stmt.setString(i++, SecurityUtil.getPasswordHash(user.getPassword()));
				stmt.setInt(i++, user.getType().getCode());
				stmt.setBoolean(i++, user.isLocal());
				stmt.setString(i++, user.getExecutionRestrictions());
				final Calendar now = Calendar.getInstance();
				stmt.setTimestamp(i++, DaoUtil.toTimestamp(now));
				stmt.setTimestamp(i++, DaoUtil.toTimestamp(now));
				stmt.executeUpdate();
				result = true;
			} catch (final Exception ex) {
				logger.error("Error: ", ex);
			} finally {
				dbUtil.closeConnection(conn);
			}
		} else {
			logger.error("User already exists.");
		}
		return result;
	}

	/**
	 * Delete record.
	 *
	 * @param user user data
	 * @return true if successful
	 */
	public static synchronized boolean delete(MgmtUser user) {

		boolean delUser = true;
		final Connection conn = dbUtil.getConnection();

		boolean transactional = false;
		try {
			conn.setAutoCommit(false);
			transactional = true;
		} catch (final SQLException e1) {
			// do not use this functionality
			logger.error(ERROR_AUTOCOMMIT);
		}

		try (PreparedStatement stmt = conn.prepareStatement(SQL_UNASSIGN_ALL);) {
			stmt.setLong(1, user.getSid());
			stmt.executeUpdate();
		} catch (final SQLException e) {
			delUser = false;
			logger.error("Error deleting from " + ToolDao.TOOL_USER_TABLE_NAME, e);
		}

		if (delUser) {
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
				stmt.setLong(1, user.getSid());
				stmt.executeUpdate();
			} catch (final SQLException e) {
				delUser = false;
				logger.error("Error deleting from " + USER_TABLE_NAME, e);
			}
		}
		// Check final results
		if (transactional) {
			try {
				if (!delUser) {
					// Undo changes
					conn.rollback();
				} else {
					conn.commit();
				}
				// Reset to default no matter what
				conn.setAutoCommit(true);
			} catch (final SQLException e) {
				// do not use this functionality
				logger.error("Failed to commit transaction", e);
			}
		}
		dbUtil.closeConnection(conn);
		return delUser;
	}

	/**
	 * Change a user password.
	 *
	 * @param user     user data
	 * @param password new password
	 * @return true if successful
	 */
	public static synchronized boolean changePassword(MgmtUser user, String password) {
		boolean result = true;

		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_PASSWORD);) {
			stmt.setString(1, SecurityUtil.getPasswordHash(password));
			stmt.setTimestamp(2, DaoUtil.toTimestamp(Calendar.getInstance()));
			stmt.setString(3, user.getUsername());
			stmt.executeUpdate();
		} catch (SQLException | InvalidKeySpecException e) {
			result = false;
			logger.error("Error: ", e);
		} finally {
			dbUtil.closeConnection(conn);
		}

		// Remove temporary password if present
		user.setPassword(null);
		return result;
	}

	/**
	 * Gets management user by username.
	 *
	 * @param username the username
	 * @return the management user or null if it not exists
	 */
	public static synchronized MgmtUser get(String username) {
		final Connection conn = dbUtil.getConnection();
		MgmtUser bean = null;
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET);) {
			stmt.setString(1, username);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) { // exists
				bean = new MgmtUser();
				bean.setSid(rs.getInt("sid"));
				bean.setUsername(username);
				bean.setNameFull(rs.getString("name_full"));
				bean.setEmail(rs.getString("email"));
				bean.setType(MgmtUserType.fromInt(rs.getInt("type")));
				bean.setLocal(rs.getBoolean(IS_LOCAL_ID));
				bean.setExecutionRestrictions(rs.getString("exe_restrictions"));
				bean.setCreated(DaoUtil.toCalendar(rs.getTimestamp("created")));
				bean.setUpdated(DaoUtil.toCalendar(rs.getTimestamp("updated")));
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return bean;
	}

	/**
	 * Gets all management users, except superusers.
	 *
	 * @return the list of management users
	 */
	public static synchronized List<MgmtUser> getAll() {
		final ArrayList<MgmtUser> users = new ArrayList<>();
		final Connection conn = dbUtil.getConnection();
		try (Statement stmt = conn.createStatement();) {
			final ResultSet rs = stmt.executeQuery(SQL_GET_ALL);
			while (rs.next()) {
				final MgmtUser nuevo = new MgmtUser();
				nuevo.setSid(rs.getInt("sid"));
				nuevo.setUsername(rs.getString("username"));
				nuevo.setNameFull(rs.getString("name_full"));
				nuevo.setEmail(rs.getString("email"));
				nuevo.setType(MgmtUserType.fromInt(rs.getInt("type")));
				nuevo.setLocal(rs.getBoolean(IS_LOCAL_ID));
				users.add(nuevo);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return users;
	}

	/**
	 * Gets user data after login.
	 *
	 * @param mgmtUser user data
	 * @return true if successful
	 */
	public static synchronized boolean login(MgmtUser mgmtUser) {
		final Connection conn = dbUtil.getConnection();
		if (conn == null) {
			mgmtUser.setSid(0);
		} else {
			try (PreparedStatement stmt = conn.prepareStatement(SQL_LOGIN)) {
				stmt.setString(1, mgmtUser.getUsername());
				final ResultSet rs = stmt.executeQuery();
				if (rs.next() && SecurityUtil.checkPassword(rs.getString("password"), mgmtUser.getPassword())) {
					// Decode type
					mgmtUser.setSid(rs.getInt("sid"));
					mgmtUser.setNameFull(rs.getString("name_full"));
					mgmtUser.setEmail(rs.getString("email"));
					mgmtUser.setType(MgmtUserType.fromInt(rs.getInt("type")));
					mgmtUser.setLocal(rs.getBoolean(IS_LOCAL_ID));
					mgmtUser.setExecutionRestrictions(rs.getString("exe_restrictions"));
					mgmtUser.setCreated(DaoUtil.toCalendar(rs.getTimestamp("created")));
					mgmtUser.setUpdated(DaoUtil.toCalendar(rs.getTimestamp("updated")));
				} else {
					mgmtUser.setSid(0);
				}
				rs.close();
			} catch (final Exception ex) {
				logger.error("Error: ", ex);
			}
		}
		dbUtil.closeConnection(conn);
		return mgmtUser.getSid() != 0;
	}

	/**
	 * Gets all usernames that can be disassociated of a tool (except current user
	 * and superusers).
	 *
	 * @param user   current user
	 * @param toolId the tool
	 * @return list of management usernames
	 */
	public static synchronized List<String> getNamesForDisassociate(MgmtUser user, int toolId) {
		final ArrayList<String> users = new ArrayList<>();
		final Connection conn = dbUtil.getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_DISASSOCIATE);) {
			stmt.setInt(1, toolId);
			stmt.setInt(2, user.getType().getCode());
			stmt.setString(3, user.getUsername());
			final ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				users.add(rs.getString(1));
			}
			rs.close();

		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return users;
	}

	/**
	 * Get all user names that can be associated to a tool (except superusers).
	 *
	 * @param toolId the tool
	 * @return list of management user names
	 */
	public static synchronized List<String> getNamesForAssociate(int toolId) {
		final ArrayList<String> users = new ArrayList<>();
		final Connection conn = dbUtil.getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_ASSOCIATE);) {
			stmt.setInt(1, toolId);
			final ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				users.add(rs.getString(1));
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return users;
	}
	
	/**
	 * Update a record (without update the password).
	 *
	 * @param user record data
	 * @return true if successful
	 */
	public static synchronized boolean update(MgmtUser user) {
		boolean result = true;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE);) {
			int i = 1;
			if (user.getNameFull() == null) {
				stmt.setNull(i++, java.sql.Types.VARCHAR);
			} else {
				stmt.setString(i++, user.getNameFull());
			}
			if (user.getEmail() == null) {
				stmt.setNull(i++, java.sql.Types.VARCHAR);
			} else {
				stmt.setString(i++, user.getEmail());
			}
			stmt.setInt(i++, user.getType().getCode());
			stmt.setBoolean(i++, user.isLocal());
			stmt.setString(i++, user.getExecutionRestrictions());
			final Calendar now = Calendar.getInstance();
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now));
			stmt.setInt(i++, user.getSid());
			stmt.executeUpdate();
		} catch (final SQLException e) {
			logger.error("Error updating tool key.", e);
			result = false;
		} finally {
			dbUtil.closeConnection(conn);
		}
		return result;

	}
}
