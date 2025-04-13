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
import es.us.dit.lti.entity.LtiUser;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.ResourceUser;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.entity.ToolKey;

/**
 * The Resource User Data Access Object is the interface providing access to
 * resource users related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolResourceUserDao {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolResourceUserDao.class);

	/**
	 * Table name of this DAO.
	 */
	public static final String RU_TABLE_NAME = "resource_user";

	/**
	 * SQL statement to get the serial ID of a resource user.
	 */
	public static final String SQL_GET_SID = "SELECT sid FROM " + RU_TABLE_NAME
			+ " WHERE resource_sid=? AND lti_user_sid=? ";

	/**
	 * SQL statement to get a resource user by the serial ID.
	 */
	public static final String SQL_GET_BY_SID = "SELECT resource_sid, lti_user_sid, source_id, lti_result_sourcedid, "
			+ RU_TABLE_NAME + ".created, " + RU_TABLE_NAME + ".updated" + " FROM " + RU_TABLE_NAME + ","
			+ ToolConsumerUserDao.LTI_USER_TABLE_NAME + " WHERE " + RU_TABLE_NAME + ".sid = ?" + " AND lti_user_sid="
			+ ToolConsumerUserDao.LTI_USER_TABLE_NAME + ".sid";

	/**
	 * SQL statement to get an attempt by the serial ID of the resource link and the
	 * LTI user.
	 */
	public static final String SQL_GET_BY_ID = "SELECT sid, lti_result_sourcedid, created, updated FROM "
			+ RU_TABLE_NAME + " WHERE resource_sid=? AND lti_user_sid=?";

	/**
	 * SQL statement to add a resource user.
	 */
	public static final String SQL_NEW = "INSERT INTO " + RU_TABLE_NAME
			+ " (resource_sid, lti_user_sid, lti_result_sourcedid, created, updated) " + "VALUES (?, ?, ?, ?, ?)";

	/**
	 * SQL statement to update a resource user.
	 */
	public static final String SQL_UPDATE = "UPDATE " + RU_TABLE_NAME + " SET lti_result_sourcedid=?, updated=? "
			+ "WHERE sid = ?";

	/**
	 * SQL statement to get a resource user for write outcome/score in tool
	 * consumer.
	 */
	public static final String SQL_GET_FOR_OUTCOME = "SELECT " + RU_TABLE_NAME
			+ ".sid, lti_result_sourcedid, resource_sid, " + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".title, outcome_service_url, tool_key_sid, context_sid, " + ToolContextDao.CONTEXT_TABLE_NAME
			+ ".title FROM " + RU_TABLE_NAME + ", " + ToolResourceLinkDao.RL_TABLE_NAME + ", "
			+ ToolContextDao.CONTEXT_TABLE_NAME + " WHERE " + RU_TABLE_NAME + ".lti_result_sourcedid IS NOT NULL AND "
			+ RU_TABLE_NAME + ".lti_user_sid=? AND " + ToolResourceLinkDao.RL_TABLE_NAME + ".sid=" + RU_TABLE_NAME
			+ ".resource_sid AND " + ToolResourceLinkDao.RL_TABLE_NAME + ".outcome_service_url IS NOT NULL AND "
			+ ToolResourceLinkDao.RL_TABLE_NAME + ".tool_key_sid IS NOT NULL AND " + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".tool_sid=? AND " + ToolContextDao.CONTEXT_TABLE_NAME + ".sid=" + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".context_sid";

	/**
	 * SQL statement to get serial IDs of resource users without resource users and
	 * tool keys (unused).
	 */
	private static final String SQL_GET_UNUSED_RU = "SELECT " + RU_TABLE_NAME + ".sid FROM " + RU_TABLE_NAME
			+ " LEFT JOIN " + ToolAttemptDao.AT_TABLE_NAME + " ON " + RU_TABLE_NAME + ".sid="
			+ ToolAttemptDao.AT_TABLE_NAME + ".resource_user_sid WHERE " + ToolAttemptDao.AT_TABLE_NAME
			+ ".sid IS NULL";

	/**
	 * SQL statement to delete unused resource users.
	 */
	private static final String SQL_DELETE_UNUSED_RU = "DELETE FROM " + RU_TABLE_NAME + " WHERE "
			+ RU_TABLE_NAME + ".sid IN (" + SQL_GET_UNUSED_RU + ")";
	
	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private ToolResourceUserDao() {
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
	 * Gets a resource user by resource link SID and LTI user SID.
	 *
	 * @param resourceLinkSid the resource link serial ID
	 * @param ltiUserSid      the LTI user serial ID
	 * @return the resource user if exists or null
	 */
	public static ResourceUser getById(int resourceLinkSid, int ltiUserSid) {
		ResourceUser ru = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_ID);) {
			stmt.setInt(1, resourceLinkSid);
			stmt.setInt(2, ltiUserSid);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				ru = new ResourceUser();
				ru.setSid(rs.getInt(1));

				final ResourceLink auxRl = new ResourceLink();
				auxRl.setSid(resourceLinkSid);
				ru.setResourceLink(auxRl);

				final LtiUser auxUser = new LtiUser();
				auxUser.setSid(ltiUserSid);
				ru.setUser(auxUser);

				ru.setResultSourceId(rs.getString(2));
				ru.setCreated(DaoUtil.toCalendar(rs.getTimestamp(3)));
				ru.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(4)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return ru;
	}

	/**
	 * Gets a record by serial ID.
	 *
	 * @param sid the serial ID
	 * @return the object or null if not found
	 */
	public static ResourceUser getBySid(int sid) {
		ResourceUser ru = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_SID);) {
			stmt.setInt(1, sid);
			final ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				int i = 1;
				ru = new ResourceUser();
				ru.setSid(sid);

				final ResourceLink auxRl = new ResourceLink();
				auxRl.setSid(rs.getInt(i++));
				ru.setResourceLink(auxRl);

				final LtiUser auxUser = new LtiUser();
				auxUser.setSid(rs.getInt(i++));
				auxUser.setSourceId(rs.getString(i++));
				ru.setUser(auxUser);

				ru.setResultSourceId(rs.getString(i++));
				ru.setCreated(DaoUtil.toCalendar(rs.getTimestamp(i++)));
				ru.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(i++)));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return ru;
	}

	/**
	 * Get the serial ID of a object.
	 *
	 * @param ru object data
	 * @return true if successful
	 */
	public static boolean getSidByIds(ResourceUser ru) {
		boolean res = false;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_SID);) {
			stmt.setInt(1, ru.getResourceLink().getSid());
			stmt.setInt(2, ru.getUser().getSid());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res = true;
				ru.setSid(rs.getInt(1));
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
	 * @param ru record data
	 * @return true if successful
	 */
	public static boolean update(ResourceUser ru) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE);) {
			int i = 1;
			stmt.setString(i++, ru.getResultSourceId());
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			stmt.setInt(i++, ru.getSid());
			res = stmt.executeUpdate() == 1;
			if (res) {
				ru.setUpdated(now);
			}
		} catch (final SQLException e) {
			logger.error("Update: ", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}

		return res;
	}

	/**
	 * Create a record.
	 *
	 * @param ru record data
	 * @return true if successful
	 */
	public static boolean create(ResourceUser ru) {
		boolean res;
		final Calendar now = Calendar.getInstance();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_NEW);) {
			int i = 1;
			stmt.setInt(i++, ru.getResourceLink().getSid());
			stmt.setInt(i++, ru.getUser().getSid());
			stmt.setString(i++, ru.getResultSourceId());
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // created
			stmt.setTimestamp(i++, DaoUtil.toTimestamp(now)); // updated
			res = stmt.executeUpdate() == 1;
			if (res) {
				ru.setCreated(now);
				ru.setUpdated(now);
			}
		} catch (final SQLException e) {
			logger.error("Create: ", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}
		if (res) {
			res = getSidByIds(ru);
		}

		return res;
	}

	/**
	 * Delete a record (not implemented).
	 *
	 * @param ru record data
	 * @return false
	 */
	public static boolean delete(ResourceUser ru) {
		return false; // Not implemented
	}

	/**
	 * Gets a list of resource users for write outcome/score in tool consumer.
	 *
	 * @param toolSid    the tool serial ID
	 * @param ltiUserSid the LTI user serial ID
	 * @return the list of resource users
	 */
	public static List<ResourceUser> getForOutcome(int toolSid, int ltiUserSid) {
		final List<ResourceUser> list = new ArrayList<>();
		// Search for all the RUs where the user participates with this tool and that
		// have active sending grades.
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_FOR_OUTCOME);) {
			stmt.setInt(1, ltiUserSid);
			stmt.setInt(2, toolSid);
			final ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				int i = 1;
				final ResourceUser ru = new ResourceUser();
				ru.setSid(rs.getInt(i++));
				final LtiUser auxLtiUser = new LtiUser();
				auxLtiUser.setSid(ltiUserSid);
				ru.setUser(auxLtiUser);
				ru.setResultSourceId(rs.getString(i++));
				final ResourceLink auxRl = new ResourceLink();
				ru.setResourceLink(auxRl);
				auxRl.setSid(rs.getInt(i++));
				auxRl.setTitle(rs.getString(i++));
				auxRl.setOutcomeServiceUrl(rs.getString(i++));
				final ToolKey auxTk = new ToolKey();
				auxTk.setSid(rs.getInt(i++));
				auxRl.setToolKey(auxTk);
				final Tool auxTool = new Tool();
				auxTool.setSid(toolSid);
				auxRl.setTool(auxTool);
				final Context auxContext = new Context();
				auxContext.setSid(rs.getInt(i++));
				auxContext.setTitle(rs.getString(i++));
				auxRl.setContext(auxContext);

				list.add(ru);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Failed to get resource users.", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return list;
	}
	
	/**
	 * Deletes unused resource users.
	 * 
	 * @return true if successful
	 */
	public static boolean deleteUnused() {
		boolean deleted = true;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_UNUSED_RU);) {
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
	 * Get the serial ID of unused resource users.
	 *
	 * @return list of serial IDs of unused resource users
	 */
	public static List<Integer> getUnused() {
		List<Integer> res = new ArrayList<>();
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_UNUSED_RU);) {
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
