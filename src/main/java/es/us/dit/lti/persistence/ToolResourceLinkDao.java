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

import es.us.dit.lti.entity.Context;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.entity.ToolKey;

/**
 * The Resource Link Data Access Object is the interface providing access to
 * assessment attempts related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolResourceLinkDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolResourceLinkDao.class);

	/**
	 * Table name of this DAO.
	 */
	public static final String RL_TABLE_NAME = "resource_link";

	/**
	 * SQL statement to get the serial ID of a resource link.
	 */
	public static final String SQL_GET_SID = "SELECT sid FROM " + RL_TABLE_NAME
			+ " WHERE tool_sid=? AND context_sid=? AND resource_id=?";

	/**
	 * SQL statement to get a resource link by the serial ID.
	 */
	public static final String SQL_GET_BY_SID = "SELECT tool_sid, context_sid, resource_id, title, custom_properties, outcome_service_url, tool_key_sid, created, updated FROM "
			+ RL_TABLE_NAME + " WHERE sid = ?";

	/**
	 * SQL statement to get an attempt by the serial ID of the tool and the context
	 * and its resource link ID.
	 */
	public static final String SQL_GET_BY_ID = "SELECT sid, title, custom_properties, outcome_service_url, tool_key_sid, created, updated FROM "
			+ RL_TABLE_NAME + " WHERE tool_sid=? AND context_sid=? AND resource_id=?";

	/**
	 * SQL statement to add a resource link.
	 */
	public static final String SQL_NEW = "INSERT INTO " + RL_TABLE_NAME
			+ " (tool_sid, context_sid, resource_id, title, custom_properties, outcome_service_url, tool_key_sid, created, updated) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL statement to update a resource link.
	 */
	public static final String SQL_UPDATE = "UPDATE " + RL_TABLE_NAME
			+ " SET title=?, custom_properties=?, outcome_service_url=?, tool_key_sid=?, updated=? " + "WHERE sid = ?";

	
	/**
	 * SQL statement to delete a resource link by serial ID.
	 */
	private static final String SQL_DELETE = "DELETE FROM " + RL_TABLE_NAME + " WHERE sid=?";
	
	/**
	 * SQL statement to get serial IDs of resource links without resource users and
	 * tool keys (unused).
	 */
	private static final String SQL_GET_UNUSED_RL = "SELECT " + RL_TABLE_NAME + ".sid FROM " + RL_TABLE_NAME
			+ " LEFT JOIN " + ToolResourceUserDao.RU_TABLE_NAME + " ON " + RL_TABLE_NAME + ".sid="
			+ ToolResourceUserDao.RU_TABLE_NAME + ".resource_sid LEFT JOIN " + ToolKeyDao.TK_TABLE_NAME + " ON "
			+ RL_TABLE_NAME + ".sid=" + ToolKeyDao.TK_TABLE_NAME + ".resource_link_sid WHERE "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".sid IS NULL AND " + ToolKeyDao.TK_TABLE_NAME + ".sid IS NULL";

	/**
	 * SQL statement to delete unused resource links.
	 */
	private static final String SQL_DELETE_UNUSED_RL = "DELETE FROM " + RL_TABLE_NAME + " WHERE "
			+ RL_TABLE_NAME + ".sid IN (" + SQL_GET_UNUSED_RL + ")";
	
	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private ToolResourceLinkDao() {
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
	 * Gets a resource link by tool SID, context SID an resource link ID.
	 *
	 * @param toolSid    the tool serial ID
	 * @param contextSid the context serial ID
	 * @param rlId       the resource link ID
	 * @return the resource link if exists or null
	 */
	public static ResourceLink getById(Integer toolSid, Integer contextSid, String rlId) {
		ResourceLink rl = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_ID);) {
			if (toolSid != null) {
				stmt.setInt(1, toolSid);
			} else {
				stmt.setNull(1, java.sql.Types.INTEGER);
			}
			if (contextSid != null) {
				stmt.setInt(2, contextSid);
			} else {
				stmt.setNull(2, java.sql.Types.INTEGER);
			}
			stmt.setString(3, rlId);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				rl = new ResourceLink();
				rl.setSid(rs.getInt(1));
				if (toolSid != null) {
					final Tool auxTool = new Tool();
					auxTool.setSid(toolSid);
					rl.setTool(auxTool);
				}
				if (contextSid != null) {
					final Context auxContext = new Context();
					auxContext.setSid(contextSid);
					rl.setContext(auxContext);
				}
				rl.setResourceId(rlId);
				rl.setTitle(rs.getString(2));
				// Custom properties
				rl.setCustomPropertiesFromString(rs.getString(3));
				rl.setOutcomeServiceUrl(rs.getString(4));

				final int aux = rs.getInt(5);
				if (!rs.wasNull()) {
					final ToolKey tk = new ToolKey();
					tk.setSid(aux);
					rl.setToolKey(tk);
				}

				rl.setCreated(DaoUtil.toCalendar(rs.getTimestamp(6)));
				rl.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(7)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Resource Link by Id: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return rl;
	}

	/**
	 * Gets a record by serial ID.
	 *
	 * @param sid the serial ID
	 * @return the object or null if not found
	 */
	public static ResourceLink getBySid(int sid) {
		ResourceLink rl = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_SID);) {
			stmt.setInt(1, sid);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				rl = new ResourceLink();
				rl.setSid(sid);
				int aux = rs.getInt(1);
				if (!rs.wasNull()) {
					final Tool auxTool = new Tool();
					auxTool.setSid(aux);
					rl.setTool(auxTool);
				}
				aux = rs.getInt(2);
				if (!rs.wasNull()) {
					final Context auxContext = new Context();
					auxContext.setSid(aux);
					rl.setContext(auxContext);
				}
				rl.setResourceId(rs.getString(3));
				rl.setTitle(rs.getString(4));
				// Custom properties
				rl.setCustomPropertiesFromString(rs.getString(5));
				rl.setOutcomeServiceUrl(rs.getString(6));

				aux = rs.getInt(7);
				if (!rs.wasNull()) {
					final ToolKey tk = new ToolKey();
					tk.setSid(aux);
					rl.setToolKey(tk);
				}

				rl.setCreated(DaoUtil.toCalendar(rs.getTimestamp(8)));
				rl.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(9)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Resource Link: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return rl;
	}

	/**
	 * Get the serial ID of a object.
	 *
	 * @param rl object data
	 * @return true if successful
	 */
	public static boolean getSidByIds(ResourceLink rl) {
		boolean res = false;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_SID);) {
			if (rl.getTool() != null) {
				stmt.setInt(1, rl.getTool().getSid());
			} else {
				stmt.setNull(1, java.sql.Types.INTEGER);
			}
			if (rl.getContext() != null) {
				stmt.setInt(2, rl.getContext().getSid());
			} else {
				stmt.setNull(2, java.sql.Types.INTEGER);
			}
			stmt.setString(3, rl.getResourceId());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res = true;
				rl.setSid(rs.getInt(1));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get Tool Resource Link: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return res;
	}

	/**
	 * Update a record.
	 *
	 * @param rl record data
	 * @return true if successful
	 */
	public static boolean update(ResourceLink rl) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE);) {
			int i = 1;
			stmt.setString(i++, rl.getTitle());
			stmt.setString(i++, rl.customPropertiesToString());
			stmt.setString(i++, rl.getOutcomeServiceUrl());
			if (rl.getToolKey() != null) {
				stmt.setInt(i++, rl.getToolKey().getSid());
			} else {
				stmt.setNull(i++, java.sql.Types.INTEGER);
			}
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			stmt.setInt(i++, rl.getSid());
			res = stmt.executeUpdate() == 1;
			if (res) {
				rl.setUpdated(now);
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
	 * @param rl record data
	 * @return true if successful
	 */
	public static boolean create(ResourceLink rl) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_NEW);) {
			int i = 1;
			if (rl.getTool() != null) {
				stmt.setInt(i++, rl.getTool().getSid());
			} else {
				stmt.setNull(i++, java.sql.Types.INTEGER);
			}
			if (rl.getContext() != null) {
				stmt.setInt(i++, rl.getContext().getSid());
			} else {
				stmt.setNull(i++, java.sql.Types.INTEGER);
			}
			stmt.setString(i++, rl.getResourceId());
			stmt.setString(i++, rl.getTitle());
			stmt.setString(i++, rl.customPropertiesToString());
			stmt.setString(i++, rl.getOutcomeServiceUrl());
			if (rl.getToolKey() != null) {
				stmt.setInt(i++, rl.getToolKey().getSid());
			} else {
				stmt.setNull(i++, java.sql.Types.INTEGER);
			}
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // created
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			res = stmt.executeUpdate() == 1;
			if (res) {
				rl.setCreated(now);
				rl.setUpdated(now);
			}
		} catch (final SQLException e) {
			logger.error("Create", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}
		if (res) {
			res = getSidByIds(rl);
		}

		return res;
	}

	/**
	 * Delete a record.
	 *
	 * @param rl record data
	 * @return false
	 */
	public static boolean delete(ResourceLink rl) {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
				stmt.setInt(1, rl.getSid());
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
	 * Deletes unused resource links.
	 * 
	 * @return true if successful
	 */
	public static boolean deleteUnused() {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_UNUSED_RL);) {
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
	 * Get the serial ID of unused resource links.
	 *
	 * @return list of serial IDs of unused resource links
	 */
	public static List<Integer> getUnused() {
		List<Integer> res = new ArrayList<>();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_UNUSED_RL);) {
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
