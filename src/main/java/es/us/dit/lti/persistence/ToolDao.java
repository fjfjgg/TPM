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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Consumer;
import es.us.dit.lti.entity.Context;
import es.us.dit.lti.entity.LtiUser;
import es.us.dit.lti.entity.MgmtUser;
import es.us.dit.lti.entity.MgmtUserType;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.entity.ToolKey;
import es.us.dit.lti.runner.ToolRunnerType;
import es.us.dit.lti.servlet.UploadedFile;

/**
 * The Tool Data Access Object is the interface providing access to tools
 * related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolDao.class);

	/**
	 * Error to log when AutoCommit can not be set to false.
	 */
	private static final String ERROR_AUTOCOMMIT = "Failed to set AutoCommit to false";

	/**
	 * Table name of this DAO.
	 */
	public static final String TOOL_TABLE_NAME = "tool";
	/**
	 * Table name of table with tool-management user relationship.
	 */
	public static final String TOOL_USER_TABLE_NAME = "tool_user";
	/**
	 * Table name of tool counters.
	 */
	public static final String COUNTER_TABLE_NAME = "tool_counter";

	/**
	 * SQL statement to unassign/disassociate a user from a tool.
	 */
	private static final String SQL_UNASSIGN_USER = "DELETE FROM " + TOOL_USER_TABLE_NAME
			+ " WHERE tool_sid=? AND user_sid=? AND type > ?";

	/**
	 * SQL statement to assign/associate a management user to a tool.
	 */
	private static final String SQL_ASSOCIATE_USER = "INSERT INTO " + TOOL_USER_TABLE_NAME
			+ " (tool_sid, user_sid, type) VALUES (?,?,?)";

	/**
	 * SQL statement to disassociate all users from a tool.
	 */
	private static final String SQL_DELETE_USERS = "DELETE FROM " + TOOL_USER_TABLE_NAME + " WHERE tool_sid=?";

	/**
	 * SQL statement to create a tool counter.
	 */
	private static final String SQL_CREATE_COUNTER = "INSERT INTO " + COUNTER_TABLE_NAME
			+ " (tool_sid, counter) VALUES (?, ?)";

	/**
	 * SQL statement to get a tool counter.
	 */
	private static final String SQL_GET_COUNTER = "SELECT counter FROM " + COUNTER_TABLE_NAME + " WHERE tool_sid=?";

	/**
	 * SQL statement to update a tool counter.
	 */
	private static final String SQL_UPDATE_COUNTER = "UPDATE " + COUNTER_TABLE_NAME + " SET counter=? WHERE tool_sid=?";

	/**
	 * SQL statement to detelet a tool counter.
	 */
	private static final String SQL_DELETE_COUNTER = "DELETE FROM " + COUNTER_TABLE_NAME + " WHERE tool_sid=?";

	/**
	 * SQL statement to create a new tool.
	 */
	private static final String SQL_CREATE_TOOL = "insert into " + TOOL_TABLE_NAME
			+ " (name, description, deliveryPassword, enabled, enabled_from, enabled_until, outcome, "
			+ "extra_args, type, json_config, created, updated) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL statement to get the serial ID of a tool.
	 */
	private static final String SQL_GET_SID_BY_ID = "SELECT sid FROM " + TOOL_TABLE_NAME + " WHERE name=?";

	/**
	 * SQL statement to delete all nonces of tool keys of this tool.
	 */
	private static final String SQL_DELETE_NONCES = "DELETE FROM " + ToolNonceDao.NONCE_TABLE_NAME
			+ " WHERE key_sid in (SELECT sid FROM " + ToolKeyDao.TK_TABLE_NAME + " WHERE tool_sid=?)";

	/**
	 * SQL statement to delete all tool keys of this tool.
	 */
	private static final String SQL_DELETE_TOOL_KEYS = "DELETE FROM " + ToolKeyDao.TK_TABLE_NAME + " WHERE tool_sid=?";

	/**
	 * SQL statement to delete all resource users of this tool.
	 */
	private static final String SQL_DELETE_RESOURCE_USERS = "DELETE FROM " + ToolResourceUserDao.RU_TABLE_NAME
			+ " WHERE resource_sid in (SELECT sid FROM " + ToolResourceLinkDao.RL_TABLE_NAME + " WHERE tool_sid=?)";

	/**
	 * SQL statement to delete all attempts of this tool.
	 */
	private static final String SQL_DELETE_ATTEMPTS = "DELETE FROM " + ToolAttemptDao.AT_TABLE_NAME
			+ " WHERE resource_user_sid in " + "(SELECT sid FROM " + ToolResourceUserDao.RU_TABLE_NAME
			+ " WHERE resource_sid in " + "(SELECT sid FROM " + ToolResourceLinkDao.RL_TABLE_NAME
			+ " WHERE tool_sid=?))";

	/**
	 * SQL statement to delete all resource links of this tool.
	 */
	private static final String SQL_DELETE_RESOURCE_LINKS = "DELETE FROM " + ToolResourceLinkDao.RL_TABLE_NAME
			+ " WHERE tool_sid=?";

	/**
	 * SQL statement to delete a tool by serial ID.
	 */
	private static final String SQL_DELETE = "DELETE FROM " + TOOL_TABLE_NAME + " WHERE sid=?";

	/**
	 * SQL statement to get a list of tool names to display to a super user.
	 */
	private static final String SQL_GET_LIST_SUPER = "SELECT sid, name FROM " + TOOL_TABLE_NAME;

	/**
	 * SQL statement to get a list of tool names to display to a normal user.
	 */
	private static final String SQL_GET_LIST = "SELECT DISTINCT sid, name, " + TOOL_USER_TABLE_NAME + ".type FROM "
			+ TOOL_USER_TABLE_NAME + ", " + TOOL_TABLE_NAME + " WHERE user_sid=? AND " + TOOL_USER_TABLE_NAME
			+ ".tool_sid=" + TOOL_TABLE_NAME + ".sid";

	/**
	 * SQL statement to get the type of management user that a management user is
	 * with respect to a tool.
	 */
	private static final String SQL_GET_USER_TYPE = "SELECT type FROM " + TOOL_USER_TABLE_NAME
			+ " WHERE tool_sid=? AND user_sid=?";

	/**
	 * SQL statement to get administrator users of a tool (it must be only one).
	 */
	private static final String SQL_GET_ADMIN = "SELECT username FROM " + TOOL_USER_TABLE_NAME + ", "
			+ MgmtUserDao.USER_TABLE_NAME + " WHERE tool_sid=? AND " + MgmtUserDao.USER_TABLE_NAME + ".sid="
			+ TOOL_USER_TABLE_NAME + ".user_sid AND " + TOOL_USER_TABLE_NAME + ".type=" + MgmtUserType.ADMIN.getCode();

	/**
	 * SQL statement to get all management users and their types associated to a
	 * tool.
	 */
	private static final String SQL_GET_USERS = "SELECT username, " + TOOL_USER_TABLE_NAME + ".type FROM "
			+ TOOL_USER_TABLE_NAME + ", " + MgmtUserDao.USER_TABLE_NAME + " WHERE tool_sid=? AND "
			+ TOOL_USER_TABLE_NAME + ".user_sid=" + MgmtUserDao.USER_TABLE_NAME + ".sid";

	/**
	 * SQL statement to get all tool keys of a tool.
	 */
	private static final String SQL_GET_TK_KEYS = "SELECT key FROM " + ToolKeyDao.TK_TABLE_NAME + " WHERE tool_sid=?";

	/**
	 * SQL statement to get tool data.
	 */
	private static final String SQL_GET = "SELECT sid, name, description, deliveryPassword,"
			+ " enabled, enabled_from, enabled_until, outcome, extra_args, type, json_config,"
			+ " created, updated FROM " + TOOL_TABLE_NAME;

	/**
	 * SQL statement to get all resource links of a tool.
	 */
	private static final String SQL_GET_LINKS = "SELECT " + ToolConsumerDao.CONSUMER_TABLE_NAME + ".sid AS cs_sid, "
			+ ToolConsumerDao.CONSUMER_TABLE_NAME + ".guid AS cs_id, " + ToolConsumerDao.CONSUMER_TABLE_NAME
			+ ".name AS cs_title ," + ToolContextDao.CONTEXT_TABLE_NAME + ".sid AS c_sid, "
			+ ToolContextDao.CONTEXT_TABLE_NAME + ".context_id AS c_id, " + ToolContextDao.CONTEXT_TABLE_NAME
			+ ".title AS c_title, " + ToolResourceLinkDao.RL_TABLE_NAME + ".sid AS rl_sid, "
			+ ToolResourceLinkDao.RL_TABLE_NAME + ".resource_id AS rl_id, " + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".title AS rl_title" + " FROM " + TOOL_TABLE_NAME + ", " + ToolResourceLinkDao.RL_TABLE_NAME
			+ " LEFT JOIN " + ToolContextDao.CONTEXT_TABLE_NAME + " LEFT JOIN " + ToolConsumerDao.CONSUMER_TABLE_NAME
			+ " WHERE" + "  " + TOOL_TABLE_NAME + ".sid=?" + "  AND " + ToolResourceLinkDao.RL_TABLE_NAME + ".tool_sid="
			+ TOOL_TABLE_NAME + ".sid" + "  AND " + ToolResourceLinkDao.RL_TABLE_NAME + ".context_sid="
			+ ToolContextDao.CONTEXT_TABLE_NAME + ".sid" + "  AND " + ToolContextDao.CONTEXT_TABLE_NAME
			+ ".consumer_sid=" + ToolConsumerDao.CONSUMER_TABLE_NAME + ".sid" + " ORDER BY cs_sid, c_sid, rl_sid;";

	/**
	 * SQL statement to get all tool keys of a tool ordered by constraints.
	 */
	private static final String SQL_GET_TKS = "SELECT sid, consumer_sid, context_sid, resource_link_sid,"
			+ " key, secret, address, enabled, created, updated FROM " + ToolKeyDao.TK_TABLE_NAME + " WHERE tool_sid=? "
			+ " ORDER BY consumer_sid NULLS FIRST," + "   context_sid NULLS FIRST,"
			+ "   resource_link_sid NULLS FIRST";

	/**
	 * SQL statement to get LTI users of a tool with a specific source ID.
	 */
	private static final String SQL_GET_LTI_USERS_BY_SOURCEID = "SELECT DISTINCT "
			+ ToolConsumerUserDao.LTI_USER_TABLE_NAME + ".* FROM " + ToolResourceUserDao.RU_TABLE_NAME + ", "
			+ ToolConsumerUserDao.LTI_USER_TABLE_NAME + " WHERE resource_sid in (SELECT sid FROM "
			+ ToolResourceLinkDao.RL_TABLE_NAME + " WHERE tool_sid=?)" + " AND " + ToolResourceUserDao.RU_TABLE_NAME
			+ ".lti_user_sid = " + ToolConsumerUserDao.LTI_USER_TABLE_NAME + ".sid AND source_id=?";

	/**
	 * SQL statement to change the name of a tool.
	 */
	private static final String SQL_UPDATE_ONLY_NAME = "UPDATE " + TOOL_TABLE_NAME
			+ " SET name=?,updated=? WHERE name=?";

	/**
	 * SQL statement to change tool data except its name.
	 */
	private static final String SQL_UPDATE_NOT_NAME = "UPDATE " + TOOL_TABLE_NAME
			+ " SET description=?,deliveryPassword=?,"
			+ "enabled=?,enabled_from=?,enabled_until=?,outcome=?,extra_args=?,type=?,json_config=?,updated=? WHERE name=?";

	/**
	 * SQL statement to change all tool data.
	 */
	private static final String SQL_UPDATE = "UPDATE " + TOOL_TABLE_NAME + " SET description=?,deliveryPassword=?,"
			+ "enabled=?,enabled_from=?,enabled_until=?,outcome=?,extra_args=?,type=?,json_config=?,name=?,updated=? WHERE name=?";

	/**
	 * SQL statement to DELETE resource Users without attempts of a tool.
	 */
	private static final String SQL_DELETE_RESOURCE_USERS_WITHOUT_ATTEMPTS = "DELETE FROM "
			+ ToolResourceUserDao.RU_TABLE_NAME + " WHERE " + ToolResourceUserDao.RU_TABLE_NAME + ".sid IN ( SELECT "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".sid FROM " + ToolResourceUserDao.RU_TABLE_NAME + " LEFT JOIN "
			+ ToolAttemptDao.AT_TABLE_NAME + " ON " + ToolAttemptDao.AT_TABLE_NAME + ".resource_user_sid = "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".sid WHERE " + ToolAttemptDao.AT_TABLE_NAME + ".sid IS NULL AND "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".sid IN ( SELECT sid FROM " + ToolResourceUserDao.RU_TABLE_NAME
			+ " WHERE resource_sid IN (SELECT sid FROM " + ToolResourceLinkDao.RL_TABLE_NAME + " WHERE tool_sid=? )))";

	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;
	/**
	 * Tool cache, to reduce the use of db.
	 */
	private static ToolCache cache = new ToolCache();

	/**
	 * Can not create objects.
	 */
	private ToolDao() {
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
	 * Tool counter atomic increment.
	 *
	 * @param tool the tool
	 * @return the new counter value
	 */
	public static synchronized int incrementCounter(Tool tool) {
		int counter = 0;
		if (tool != null) {
			// Counter may have changed from the time the first page was displayed until the
			// file was sent.
			// Refresh it.
			counter = tool.getCounter();
			final Connection conn = dbUtil.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_COUNTER);) {
				stmt.setInt(1, tool.getSid());
				final ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					counter = rs.getInt(1);
				}
				rs.close();
			} catch (final SQLException e) {
				logger.error("Error retrieving database counter", e);
			}
			// We assume that the counter has been read, we increment
			if (counter >= Integer.MAX_VALUE) {
				counter = 0;
			} else {
				counter++;
			}
			tool.setCounter(counter);
			try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_COUNTER);) {
				stmt.setInt(1, counter);
				stmt.setInt(2, tool.getSid());
				stmt.executeUpdate();
			} catch (final SQLException e) {
				logger.error("Error incrementing counter", e);
			}
			dbUtil.closeConnection(conn);
		}
		return counter;
	}

	/**
	 * Gets tool counter.
	 *
	 * @param tool the tool
	 * @return counter value
	 */
	public static synchronized int getCounter(Tool tool) {
		int counter = 0;
		if (tool != null) {
			final Connection conn = dbUtil.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_COUNTER);) {
				stmt.setInt(1, tool.getSid());
				final ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					counter = rs.getInt(1);
				}
				rs.close();
			} catch (final SQLException e) {
				logger.error("Error retrieving database counter", e);
				counter = tool.getCounter();
			}

			tool.setCounter(counter);
			dbUtil.closeConnection(conn);
		}
		return counter;
	}

	/**
	 * Changes tool counter.
	 *
	 * @param tool the tool
	 * @param newCounter new counter value
	 * @return true if successful
	 */
	public static synchronized boolean changeCounter(Tool tool, int newCounter) {
		boolean res = true;
		if (tool != null) {
			final Connection conn = dbUtil.getConnection();
			tool.setCounter(newCounter);
			try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_COUNTER);) {
				stmt.setInt(1, newCounter);
				stmt.setInt(2, tool.getSid());
				stmt.executeUpdate();
			} catch (final SQLException e) {
				logger.error("Error changing counter", e);
				res = false;
			}
			dbUtil.closeConnection(conn);
		}
		return res;
	}

	/**
	 * Creates a new tool.
	 *
	 * @param user            administrator user
	 * @param tool            tool data
	 * @param correctorFile   corrector file
	 * @param descriptionFile user description file
	 * @param extraZipFile    additional user zip file with the files referenced by
	 *                        the description file
	 * @return true if successful
	 * @throws FileAlreadyExistsException when a tool with the same name exists
	 * @throws FileSystemException        when it could not write files
	 */
	public static synchronized boolean create(MgmtUser user, Tool tool, UploadedFile correctorFile,
			UploadedFile descriptionFile, UploadedFile extraZipFile)
			throws FileAlreadyExistsException, FileSystemException {

		boolean result = true;
		final String toolName = tool.getName();
		// Get the parameters of the tool. Check that the mandatory ones are there.
		if (toolName == null || toolName.isEmpty() || tool.getDeliveryPassword() == null
				|| tool.getDescription() == null || tool.getToolType() == null) {
			logger.error("Insufficient parameters");
			return false;
		}

		// Does the old tool exist? So finish immediately
		final File newFolder = new File(tool.getToolPath());
		if (get(toolName) != null || newFolder.exists()) {
			logger.error("Tool exists.");
			throw new FileAlreadyExistsException(null);
		}

		// Modifying files and directories.
		if (!tool.createToolFiles(correctorFile, descriptionFile, extraZipFile)) {
			throw new FileSystemException(null);
		}

		final Connection conn = dbUtil.getConnection();
		boolean transactional = false;
		try {
			conn.setAutoCommit(false);
			transactional = true;
		} catch (final SQLException e1) {
			// do not use this functionality
			logger.error(ERROR_AUTOCOMMIT);
		}

		if (tool.getCounter() < 0) {
			tool.setCounter(0);
		}

		try {
			logger.info("The tool: {} does not exist. It will be create.", toolName);
			try (PreparedStatement stmt = conn.prepareStatement(SQL_CREATE_TOOL)) {
				stmt.setString(1, toolName);
				stmt.setString(2, tool.getDescription());
				stmt.setString(3, tool.getDeliveryPassword());
				stmt.setBoolean(4, tool.isEnableable());
				if (tool.getEnabledFrom() == null) {
					stmt.setNull(5, java.sql.Types.TIMESTAMP);
				} else {
					stmt.setTimestamp(5, DaoUtil.toTimestamp(tool.getEnabledFrom()));
				}
				if (tool.getEnabledUntil() == null) {
					stmt.setNull(6, java.sql.Types.TIMESTAMP);
				} else {
					stmt.setTimestamp(6, DaoUtil.toTimestamp(tool.getEnabledUntil()));
				}
				stmt.setBoolean(7, tool.isOutcome());
				stmt.setString(8, tool.getExtraArgs());
				stmt.setInt(9, tool.getToolType().getCode());
				stmt.setString(10, tool.getJsonConfig());
				final Calendar now = Calendar.getInstance();
				stmt.setTimestamp(11, DaoUtil.toTimestamp(now));
				stmt.setTimestamp(12, DaoUtil.toTimestamp(now));
				stmt.executeUpdate();
			}
		} catch (final Exception ex) {
			result = false;
			logger.error("Error: ", ex);
		}
		// Get the new SID
		if (result) {
			try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_SID_BY_ID);) {
				stmt.setString(1, toolName);
				final ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					tool.setSid(rs.getInt(1));
				}
				rs.close();
			} catch (final Exception ex) {
				logger.error("Error: ", ex);
			}
		}

		// Create counter
		logger.info("Creating tool counter.");
		try {
			try (PreparedStatement stmt = conn.prepareStatement(SQL_CREATE_COUNTER)) {
				stmt.setInt(1, tool.getSid());
				stmt.setInt(2, tool.getCounter());
				stmt.executeUpdate();
			}
		} catch (final Exception ex) {
			result = false;
			logger.error("Error: ", ex);
		}

		logger.info("Associating administrator user.");
		if (!associateUser(user, tool, MgmtUserType.ADMIN)) {
			result = false;
		}

		// Check result
		if (!result) {
			// Undo changes
			if (transactional) {
				try {
					conn.rollback();
				} catch (final SQLException e1) {
					// do not use this functionality
					logger.error("Unable to rollback");
				}
			}
			// Delete folder
			try {
				final File dir = new File(tool.getToolPath());
				if (dir.exists() && dir.isDirectory()) {
					FileUtils.deleteDirectory(dir);
				}
			} catch (final IOException e) {
				logger.error("Could not delete directory", e);
			}

		} else if (transactional) {
			try {
				conn.commit();
			} catch (final SQLException e1) {
				// do not use this functionality
				logger.error("Could not commit");
			}

		}
		if (transactional) {
			// We restore to default value no matter what
			try {
				conn.setAutoCommit(true);
			} catch (final SQLException e1) {
				// do not use this functionality
				logger.error("Failed to set AutoCommit to true");
			}
		}
		dbUtil.closeConnection(conn);

		return result;
	}

	/**
	 * Updates a tool.
	 *
	 * @param tool            new tool data
	 * @param oldName         old name
	 * @param correctorFile   corrector file
	 * @param descriptionFile description file
	 * @param extraZipFile    additional user zip file with the files referenced by
	 *                        the description file
	 * @return true if successful
	 * @throws FileAlreadyExistsException when a tool with the same name exists
	 * @throws FileSystemException        when it could not write files
	 */
	public static synchronized boolean update(Tool tool, String oldName, UploadedFile correctorFile,
			UploadedFile descriptionFile, UploadedFile extraZipFile)
			throws FileAlreadyExistsException, FileSystemException {

		// Get the parameters of the tool. Check that the mandatory ones are there.
		if (tool.getName() == null || tool.getName().isEmpty() || tool.getDeliveryPassword() == null
				|| tool.getDescription() == null || tool.getToolType() == null || oldName == null) {
			logger.error("Insufficient parameters");
			return false;
		}

		// Does the old tool exist?
		final Tool oldTool = get(oldName);
		if (oldTool == null || !oldTool.createToolFiles(null, null, null)) {
			logger.error("The old tool either does not exist or does not have the directories created.");
			throw new FileSystemException(null);
		}

		final String newToolTitle = tool.getName();

		final boolean changeName = !oldName.equals(tool.getName());
		final boolean changeFiles = correctorFile != null || descriptionFile != null || extraZipFile != null;
		final boolean changeOthers = !oldTool.equalsExceptName(tool);
		final boolean changeCounter = tool.getCounter() >= 0 && tool.getCounter() != oldTool.getCounter();
		if (changeName) {
			// Verify that there is no tool with the new name
			if (get(tool.getName()) != null) {
				throw new FileAlreadyExistsException(null);
			}
			// Verify that the new directories do not exist
			final File newFolder = new File(tool.getToolPath());
			if (newFolder.exists()) {
				throw new FileAlreadyExistsException(null);
			}
		}

		// Remove cache
		for (final String key : getAllKeys(oldTool)) {
			ToolKeyDao.deleteCache(key);
		}
		cache.remove(oldTool.getName());

		// Modifying files and directories. The following variables allow undo.
		boolean folderRenamed = false;
		boolean filesCopied = false;
		if (changeName) {
			// Rename the tool directory
			final File newFolder = new File(tool.getToolPath());
			final File oldFolder = new File(oldTool.getToolPath());

			if (!oldFolder.renameTo(newFolder)) {
				throw new FileSystemException(null);
			} else {
				folderRenamed = true;
				// from this moment you have to start undoing
			}
		}
		if (changeFiles) {
			if (tool.createToolFiles(correctorFile, descriptionFile, extraZipFile)) {
				// something may have been created
				filesCopied = true;
			} else {
				throw new FileSystemException(null);
			}
		}

		final Connection conn = dbUtil.getConnection();
		boolean result = true;
		boolean transactional = false;
		// If it is necessary to change something in the database
		if (changeOthers || changeName || changeCounter) {
			try {
				conn.setAutoCommit(false);
				transactional = true;
			} catch (final SQLException e1) {
				// do not use this functionality
				logger.error(ERROR_AUTOCOMMIT);
			}

			if (changeOthers || changeName) {
				// Update tool
				final String updateQuery;
				if (changeOthers && changeName) {
					updateQuery = SQL_UPDATE;
				} else if (changeOthers) {
					updateQuery = SQL_UPDATE_NOT_NAME;
				} else {
					updateQuery = SQL_UPDATE_ONLY_NAME;
				}
				try (PreparedStatement stmt = conn.prepareStatement(updateQuery);) {
					int i = 1;
					if (changeOthers) {
						stmt.setString(i++, tool.getDescription());
						stmt.setString(i++, tool.getDeliveryPassword());
						stmt.setBoolean(i++, tool.isEnableable());
						if (tool.getEnabledFrom() == null) {
							stmt.setNull(i++, java.sql.Types.TIMESTAMP);
						} else {
							stmt.setTimestamp(i++, DaoUtil.toTimestamp(tool.getEnabledFrom()));
						}
						if (tool.getEnabledUntil() == null) {
							stmt.setNull(i++, java.sql.Types.TIMESTAMP);
						} else {
							stmt.setTimestamp(i++, DaoUtil.toTimestamp(tool.getEnabledUntil()));
						}
						stmt.setBoolean(i++, tool.isOutcome());
						stmt.setString(i++, tool.getExtraArgs());
						stmt.setInt(i++, tool.getToolType().getCode());
						stmt.setString(i++, tool.getJsonConfig());
					}
					if (changeName) {
						stmt.setString(i++, newToolTitle);
					}
					stmt.setTimestamp(i++, DaoUtil.toTimestamp(Calendar.getInstance()));
					stmt.setString(i++, oldName);
					stmt.executeUpdate();
				} catch (final Exception e) {
					result = false;
					logger.error("Error updating tool.", e);
				}
			}

			if (result && changeCounter && !changeCounter(oldTool, tool.getCounter())) {
				result = false;
			}

		}

		// Check result
		if (!result) {
			// Undo changes
			if (transactional) {
				try {
					conn.rollback();
				} catch (final SQLException e1) {
					// do not use this functionality
					logger.error("Unable to rollback.");
				}
			}
			if (filesCopied && !tool.restoreToolFileBackups()) {
				logger.error("Error restoring previous files.");
			}
			if (folderRenamed) {
				// change name again
				final File newFolder = new File(oldTool.getToolPath());
				final File oldFolder = new File(tool.getToolPath());
				if (!oldFolder.renameTo(newFolder)) {
					logger.error("Error restoring old directory name.");
				}
			}
		} else if (transactional) {
			try {
				conn.commit();
			} catch (final SQLException e1) {
				// do not use this functionality
				logger.error("Cound not commit.");
			}

		}
		if (transactional) {
			// We restore to default value no matter what
			try {
				conn.setAutoCommit(true);
			} catch (final SQLException e1) {
				// do not use this functionality
				logger.error("Failed to set AutoCommit to true");
			}
		}
		// Delete backups if files have been copied
		if (filesCopied && !tool.deleteToolFileBackups()) {
			logger.error("Error deleting backups");
		}
		dbUtil.closeConnection(conn);

		return result;
	}

	/**
	 * Associates a management user to a tool.
	 *
	 * @param user the management user
	 * @param tool the tool
	 * @param type the type of association
	 * @return true if successful
	 */
	public static synchronized boolean associateUser(MgmtUser user, Tool tool, MgmtUserType type) {
		boolean assign = true;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_ASSOCIATE_USER);) {
			stmt.setInt(1, tool.getSid());
			stmt.setInt(2, user.getSid());
			stmt.setInt(3, type.getCode());
			stmt.executeUpdate();

		} catch (final Exception ex) {
			assign = false;
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return assign;
	}

	/**
	 * Disassociates a management user from a tool.
	 *
	 * @param user the user
	 * @param tool the tool
	 * @param type the type of initiator user
	 * @return true if successful
	 */
	public static synchronized boolean disassociateUser(MgmtUser user, Tool tool, MgmtUserType type) {
		boolean assign = true;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_UNASSIGN_USER);) {
			stmt.setInt(1, tool.getSid());
			stmt.setInt(2, user.getSid());
			stmt.setInt(3, type.getCode());
			stmt.executeUpdate();
		} catch (final Exception ex) {
			assign = false;
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return assign;
	}

	/**
	 * Deletes all attempts of a tool.
	 * 
	 * @param tool the tool
	 * @return true if successful
	 */
	public static synchronized boolean deleteAttempts(Tool tool) {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ATTEMPTS);) {
				stmt.setInt(1, tool.getSid());
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
	 * Deletes a tool and all dependences.
	 *
	 * @param tool the tool
	 * @return true if successful
	 */
	public static synchronized boolean delete(Tool tool) {
		// Deleting a tool means:
		// delete it from the database and
		// delete files
		if (tool == null) {
			return false;
		}

		final Connection conn = dbUtil.getConnection();
		boolean transactional = false;
		try {
			conn.setAutoCommit(false);
			transactional = true;
		} catch (final SQLException e1) {
			// do not use this functionality
			logger.error(ERROR_AUTOCOMMIT);
		}
		// Remove cache
		for (final String key : getAllKeys(tool)) {
			ToolKeyDao.deleteCache(key);
		}
		cache.remove(tool.getName());
		boolean result = false;
		try {
			/*
			 * Dependencies in the database with respect to the tool: tool_counter,
			 * tool_user, tool_key, nonce, resource_link, resource_user
			 */
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_COUNTER);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_USERS);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_NONCES);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ATTEMPTS);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_RESOURCE_USERS);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_RESOURCE_LINKS);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_TOOL_KEYS);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			result = true;
		} catch (final SQLException ex) {
			result = false;
			logger.error("Error: ", ex);
		}

		if (result) {
			try {
				final File dir = new File(tool.getToolPath());
				if (dir.exists() && dir.isDirectory()) {
					FileUtils.deleteDirectory(dir);
					// if something is deleted before giving an error,
					// we will not be able to recover it
				}
			} catch (final Exception e) {
				logger.error("Could not delete directory.", e);
				result = false;
			}
		}
		// Check results
		if (transactional) {
			try {
				if (!result) {
					// Undo changes
					conn.rollback();
				} else {
					conn.commit();
				}
				// We restore to default value no matter what
				conn.setAutoCommit(true);
			} catch (final SQLException e) {
				// do not use this functionality
				logger.error("Error ending transaction.", e);
			}
		}
		dbUtil.closeConnection(conn);

		return result;
	}

	/**
	 * Gets all tools for a management user.
	 *
	 * @param user the user
	 * @return list of tools (serial IDs, names and user types only)
	 */
	public static synchronized List<Tool> getAll(MgmtUser user) {
		final ArrayList<Tool> tools = new ArrayList<>();
		final Connection conn = dbUtil.getConnection();

		if (user.getType() == MgmtUserType.SUPER) {
			try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_LIST_SUPER);) {
				final ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					final Tool nuevo = new Tool();
					nuevo.setSid(rs.getInt(1));
					nuevo.setName(rs.getString(2));
					nuevo.setUserTypeCode(MgmtUserType.ADMIN.getCode());
					tools.add(nuevo);
				}
				rs.close();
			} catch (final Exception ex) {
				logger.error("Error: ", ex);
			}
		} else {
			try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_LIST);) {
				stmt.setInt(1, user.getSid());
				final ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					final Tool nuevo = new Tool();
					nuevo.setSid(rs.getInt(1));
					nuevo.setName(rs.getString(2));
					nuevo.setUserTypeCode(rs.getInt(3));
					tools.add(nuevo);
				}
				rs.close();
			} catch (final Exception ex) {
				logger.error("Error: ", ex);
			}
		}
		dbUtil.closeConnection(conn);

		return tools;
	}

	/**
	 * Gets the user type of a user with respect to a tool.
	 *
	 * <p>Always returns ADMIN for super users.
	 *
	 * @param user the user
	 * @param tool the tool
	 * @return user type or UNKNOWN
	 */
	public static synchronized int getToolUserType(MgmtUser user, Tool tool) {
		int type = MgmtUserType.UNKNOWN.getCode();

		if (user.getType() == MgmtUserType.SUPER) {
			type = MgmtUserType.ADMIN.getCode();
		} else {
			// look for the type of the user for that tool
			final Connection conn = dbUtil.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_USER_TYPE);) {
				stmt.setInt(1, tool.getSid());
				stmt.setInt(2, user.getSid());
				final ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					type = rs.getInt(1);
				}
				rs.close();
			} catch (final Exception ex) {
				ex.printStackTrace();
			} finally {
				dbUtil.closeConnection(conn);
			}
		}
		return type;
	}

	/**
	 * Gets the user name of the (first) administrator of a tool.
	 *
	 * @param tool the tool
	 * @return user name or null
	 */
	public static synchronized String getAdmin(Tool tool) {
		String user = null;
		final Connection conn = dbUtil.getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_ADMIN);) {
			stmt.setInt(1, tool.getSid());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				user = rs.getString(1);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return user;
	}

	/**
	 * Gets list of management users that are associated to a tool.
	 *
	 * <p>The type of user of each management users is with respect to the tool.
	 *
	 * @param tool the tool
	 * @return list of management users
	 */
	public static synchronized List<MgmtUser> getUsers(Tool tool) {
		final ArrayList<MgmtUser> users = new ArrayList<>();
		final Connection conn = dbUtil.getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_USERS);) {
			stmt.setInt(1, tool.getSid());
			final ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				final MgmtUser nuevo = new MgmtUser();
				nuevo.setUsername(rs.getString(1));
				nuevo.setType(MgmtUserType.fromInt(rs.getInt(2)));
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
	 * Gets a tool based on the value of a field.
	 *
	 * @param field    field name
	 * @param value    value of the field (if it is a string)
	 * @param intValue value of the field (if it is a integer)
	 * @return the tool or null
	 */
	private static synchronized Tool getByFieldValue(String field, String value, int intValue) {
		Tool result = null;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET + " WHERE " + field + "=?");) {
			if (value != null) {
				stmt.setString(1, value);
			} else {
				stmt.setInt(1, intValue);
			}
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = new Tool();
				result.setSid(rs.getInt(1));
				result.setName(rs.getString(2));
				result.setDescription(rs.getString(3));
				result.setDeliveryPassword(rs.getString(4));
				result.setEnabled(rs.getBoolean(5));
				result.setEnabledFrom(DaoUtil.toCalendar(rs.getTimestamp(6)));
				result.setEnabledUntil(DaoUtil.toCalendar(rs.getTimestamp(7)));
				result.setOutcome(rs.getBoolean(8));
				result.setExtraArgs(rs.getString(9));
				result.setToolType(ToolRunnerType.fromInt(rs.getInt(10)));
				result.setJsonConfig(rs.getString(11));
				result.setCreated(DaoUtil.toCalendar(rs.getTimestamp(12)));
				result.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(13)));
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}
		// gets counter
		if (result != null) {
			getCounter(result);
		}
		return result;
	}

	/**
	 * Gets a tool by its name.
	 *
	 * @param toolName the tool name
	 * @return the tool or null
	 */
	public static synchronized Tool get(String toolName) {
		if (toolName == null) {
			return null;
		}
		Tool result = cache.get(toolName);
		if (result == null) {
			result = getByFieldValue("name", toolName, 0);
			if (result != null) {
				cache.put(toolName, result);
			}
		}
		return result;
	}

	/**
	 * Gets a tool by serial ID.
	 *
	 * @param sid the serial ID
	 * @return found tool or null
	 */
	public static synchronized Tool getBySid(int sid) {
		return getByFieldValue("sid", null, sid);
	}

	/**
	 * Gets all tool key names (consumer key) associated with this tool.
	 *
	 * @param tool the tool
	 * @return list of consumer keys
	 */
	public static synchronized List<String> getAllKeys(Tool tool) {
		final ArrayList<String> keys = new ArrayList<>();
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_TK_KEYS);) {
			stmt.setInt(1, tool.getSid());
			final ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				keys.add(rs.getString(1));
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return keys;
	}

	/**
	 * Returns the possible different tool keys a tool could have, combining all
	 * consumers, contexts, and resource links currently in use.
	 *
	 * @param tool the tool
	 * @return list of possible tool keys
	 */
	public static synchronized List<ToolKey> getAllPossibleToolKeys(Tool tool) {
		final ArrayList<ToolKey> possibleTk = new ArrayList<>();

		final Connection conn = dbUtil.getConnection();
		// Get all possible tool keys
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_LINKS);) {
			stmt.setInt(1, tool.getSid());
			final ResultSet rs = stmt.executeQuery();
			// Default tool key
			// lastTk: to indicate the last key added
			ToolKey lastTk = new ToolKey();
			lastTk.setTool(tool);
			possibleTk.add(lastTk);

			while (rs.next()) {
				final ToolKey tk = new ToolKey();
				tk.setTool(tool);
				// Consumer
				int lastSid = rs.getInt(1);
				String lastId = rs.getString(2);
				String lastTitle = rs.getString(3);
				// It is necessary to compare with the last one and decide if it is necessary
				// to create a new one.
				// tk only with consumer or only with consumer and context.
				if (lastTk.getConsumer() == null || lastTk.getConsumer().getSid() != lastSid) {
					// Create a new one for the consumer only
					final ToolKey tkConsumer = new ToolKey();
					tkConsumer.setTool(tool);
					final Consumer c = new Consumer();
					c.setSid(lastSid);
					c.setGuid(lastId);
					c.setName(lastTitle);
					tkConsumer.setConsumer(c);
					possibleTk.add(tkConsumer);
					lastTk = tkConsumer;
				}
				tk.setConsumer(lastTk.getConsumer());

				// Context
				lastSid = rs.getInt(4);
				lastId = rs.getString(5);
				lastTitle = rs.getString(6);
				if (lastTk.getContext() == null || lastTk.getContext().getSid() != lastSid) {
					// Create a new one for the consumer/context only
					final ToolKey tkContext = new ToolKey();
					tkContext.setTool(tool);
					tkContext.setConsumer(lastTk.getConsumer());
					final Context ct = new Context();
					ct.setSid(lastSid);
					ct.setContextId(lastId);
					ct.setTitle(lastTitle);
					tkContext.setContext(ct);
					possibleTk.add(tkContext);
					lastTk = tkContext;
				}
				tk.setContext(lastTk.getContext());

				// Resource link
				lastSid = rs.getInt(7);
				lastId = rs.getString(8);
				lastTitle = rs.getString(9);
				final ResourceLink rl = new ResourceLink();
				rl.setSid(lastSid);
				rl.setResourceId(lastId);
				rl.setTitle(lastTitle);
				tk.setResourceLink(rl);

				possibleTk.add(tk);
				lastTk = tk;
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		}

		// A sid == 0 is like NULL
		// Iterator for searching
		final ListIterator<ToolKey> it = possibleTk.listIterator();
		ToolKey lastTk = null;
		if (it.hasNext()) {
			lastTk = it.next();
		}
		final ArrayList<ToolKey> incompleteTk = new ArrayList<>();
		// Match existing tool keys
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_TKS);) {
			stmt.setInt(1, tool.getSid());
			final ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				final ToolKey currentTk = new ToolKey();
				currentTk.setSid(rs.getInt(1));
				currentTk.setTool(tool);

				int lastId = rs.getInt(2);
				if (!rs.wasNull()) {
					final Consumer c = new Consumer();
					c.setSid(lastId);
					currentTk.setConsumer(c);
				}

				lastId = rs.getInt(3);
				if (!rs.wasNull()) {
					final Context c = new Context();
					c.setSid(lastId);
					currentTk.setContext(c);
				}

				lastId = rs.getInt(4);
				if (!rs.wasNull()) {
					final ResourceLink r = new ResourceLink();
					r.setSid(lastId);
					currentTk.setResourceLink(r);
				}

				currentTk.setKey(rs.getString(5));
				currentTk.setSecret(rs.getString(6));
				currentTk.setAddress(rs.getString(7));
				currentTk.setEnabled(rs.getBoolean(8));

				currentTk.setCreated(DaoUtil.toCalendar(rs.getTimestamp(9)));
				currentTk.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(10)));
				// Match or add
				boolean found = false;
				if (lastTk != null) {
					while (lastTk != null && !found) {
						final int comp = currentTk.compareResource(lastTk.getConsumer(), lastTk.getContext(),
								lastTk.getResourceLink());
						if (comp == 0) {
							found = true;
							// copy data
							lastTk.setSid(currentTk.getSid());
							lastTk.setEnabled(currentTk.isEnabled());
							lastTk.setKey(currentTk.getKey());
							lastTk.setSecret(currentTk.getSecret());
							lastTk.setAddress(currentTk.getAddress());
							lastTk.setCreated(currentTk.getCreated());
							lastTk.setUpdated(currentTk.getUpdated());
							// get next
							if (it.hasNext()) {
								lastTk = it.next();
							} else {
								lastTk = null;
							}
						} else if (comp < 0) {
							found = true;
							// insert before
							it.previous();
							it.add(currentTk);
							it.next();
							incompleteTk.add(currentTk);
						} else if (it.hasNext()) { // get next
							lastTk = it.next();
						} else {
							lastTk = null;
						}
					}
				}
				if (!found) {
					possibleTk.add(currentTk);
					incompleteTk.add(currentTk);
				}

			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		}
		dbUtil.closeConnection(conn);

		// Complete ids and titles if additional keys had been added to the possible
		// ones
		for (final ToolKey tk : incompleteTk) {
			if (tk.getConsumer() != null) {
				tk.setConsumer(ToolConsumerDao.getBySid(tk.getConsumer().getSid()));
			}

			if (tk.getContext() != null) {
				tk.setContext(ToolContextDao.getBySid(tk.getContext().getSid()));
			}

			if (tk.getResourceLink() != null) {
				tk.setResourceLink(ToolResourceLinkDao.getBySid(tk.getResourceLink().getSid()));
			}
		}

		// Set correctly if they are enabled or not by inheritance if they do not have
		// sid
		ToolKey defaultTk = null;
		ToolKey consumerTk = null;
		ToolKey contextTk = null;
		ToolKey parentTk = null;
		for (final ToolKey tk : possibleTk) {
			if (tk.getConsumer() == null) {
				defaultTk = tk;
				parentTk = null;
			} else if (tk.getContext() == null) {
				consumerTk = tk;
				parentTk = defaultTk;
			} else if (tk.getResourceLink() == null) {
				contextTk = tk;
				parentTk = consumerTk;
			} else {
				parentTk = contextTk;
			}
			if (tk.getKey() == null && parentTk != null) {
				// Without key
				tk.setEnabled(parentTk.isEnabled());
			}
		}

		return possibleTk;
	}

	/**
	 * Gets list of LTI users by source ID.
	 *
	 * @param tool     the tool
	 * @param sourceId the source ID
	 * @return list of LTI users with that source ID
	 */
	public static synchronized List<LtiUser> getLtiUserBySourceId(Tool tool, String sourceId) {
		final ArrayList<LtiUser> users = new ArrayList<>();

		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_LTI_USERS_BY_SOURCEID);) {
			stmt.setInt(1, tool.getSid());
			stmt.setString(2, sourceId);
			final ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				final LtiUser user = new LtiUser();
				user.setSid(rs.getInt(1));
				final Consumer aux = new Consumer();
				aux.setSid(rs.getInt(2));
				user.setConsumer(aux);
				user.setUserId(rs.getString(3));
				user.setSourceId(rs.getString(4));
				user.setNameGiven(rs.getString(5));
				user.setNameFamily(rs.getString(6));
				user.setNameFull(rs.getString(7));
				user.setEmail(rs.getString(8));
				user.setCreated(DaoUtil.toCalendar(rs.getTimestamp(9)));
				user.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(10)));
				users.add(user);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		// Complete consumer info
		for (final LtiUser u : users) {
			u.setConsumer(ToolConsumerDao.getBySid(u.getConsumer().getSid()));
		}

		return users;
	}
	
	/**
	 * Deletes resource users without attempts of a tool.
	 * 
	 * @param tool the tool
	 * @return true if successful
	 */
	public static synchronized boolean deleteResourceUsersWithoutAttempts(Tool tool) {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_RESOURCE_USERS_WITHOUT_ATTEMPTS);) {
				stmt.setInt(1, tool.getSid());
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
	 * Deletes tool data (attempts and resource users).
	 * 
	 * @param tool the tool
	 * @return true if successful
	 */
	public static synchronized boolean deleteToolData(Tool tool) {
		boolean deleted = true;
		if (tool == null) {
			return false;
		}

		final Connection conn = dbUtil.getConnection();
		boolean transactional = false;
		try {
			conn.setAutoCommit(false);
			transactional = true;
		} catch (final SQLException e1) {
			// do not use this functionality
			logger.error(ERROR_AUTOCOMMIT);
		}
		try {
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ATTEMPTS);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_RESOURCE_USERS_WITHOUT_ATTEMPTS);) {
				stmt.setInt(1, tool.getSid());
				stmt.executeUpdate();
			}
			deleted = true;
		} catch (final SQLException ex) {
			deleted = false;
			logger.error("Error: ", ex);
		}
		
		// Check results
		if (transactional) {
			try {
				if (!deleted) {
					// Undo changes
					conn.rollback();
				} else {
					conn.commit();
				}
				// We restore to default value no matter what
				conn.setAutoCommit(true);
			} catch (final SQLException e) {
				// do not use this functionality
				logger.error("Error ending transaction.", e);
			}
		}
		dbUtil.closeConnection(conn);
		
		return deleted;
	}
	
}
