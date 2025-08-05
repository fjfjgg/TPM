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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.SecurityUtil;
import es.us.dit.lti.entity.Attempt;
import es.us.dit.lti.entity.LtiUser;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.ResourceUser;
import es.us.dit.lti.entity.ToolKey;

/**
 * The Management User Data Access Object is the interface providing access to
 * assessment attempts related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolAttemptDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolAttemptDao.class);

	/**
	 * Serial Version UID of an Attempt object.
	 */
	public static final long AT_SERIAL_VERSION_UID = new Attempt().getSerialVersionUid();
	/**
	 * Table name of this DAO.
	 */
	public static final String AT_TABLE_NAME = "attempt";
	
	/**
	 * SQL statement to get the serial ID of an attempt.
	 */
	private static final String SQL_GET_SID = "SELECT sid FROM " + AT_TABLE_NAME
			+ " WHERE resource_user_sid=? AND epoch_seconds=? AND nanoseconds=? ";

	/**
	 * SQL statement to get an attempt by the serial ID.
	 */
	private static final String SQL_GET_BY_SID = "SELECT resource_user_sid, original_ru_sid, epoch_seconds, nanoseconds, fileSaved, outputSaved,"
			+ " filename, storage_type, score, errorCode, " + ToolConsumerUserDao.LTI_USER_TABLE_NAME
			+ ".sid, source_id, " + ToolResourceLinkDao.RL_TABLE_NAME + ".sid, tool_key_sid FROM " + AT_TABLE_NAME
			+ "," + ToolResourceLinkDao.RL_TABLE_NAME + "," + ToolResourceUserDao.RU_TABLE_NAME + ","
			+ ToolConsumerUserDao.LTI_USER_TABLE_NAME + " WHERE " + AT_TABLE_NAME + ".sid = ?"
			+ " AND resource_user_sid=" + ToolResourceUserDao.RU_TABLE_NAME + ".sid AND "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".resource_sid=" + ToolResourceLinkDao.RL_TABLE_NAME + ".sid AND "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".lti_user_sid=" + ToolConsumerUserDao.LTI_USER_TABLE_NAME
			+ ".sid";

	/**
	 * SQL statement to get an attempt by the serial ID of the resource user and its
	 * timestamp.
	 */
	private static final String SQL_GET_BY_ID = "SELECT sid, original_ru_sid, fileSaved, outputSaved, filename, storage_type, score, errorCode FROM "
			+ AT_TABLE_NAME + " WHERE resource_user_sid=? AND epoch_seconds=? AND nanoseconds=?";

	/**
	 * SQL statement to add an attempt.
	 */
	private static final String SQL_NEW = "INSERT INTO " + AT_TABLE_NAME
			+ " (resource_user_sid, original_ru_sid, epoch_seconds, nanoseconds, fileSaved, outputSaved, filename, storage_type, score, errorCode) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL statement to update the score and error code of an attempt.
	 */
	private static final String SQL_UPDATE = "UPDATE " + AT_TABLE_NAME + " SET score=?, errorCode=? WHERE sid=?";

	/**
	 * SQL statement to delete an attempt.
	 */
	private static final String SQL_DELETE = "DELETE FROM " + AT_TABLE_NAME + " WHERE sid=?";

	/**
	 * SQL statement to count the attempts of a LTI user.
	 */
	private static final String SQL_COUNT = "SELECT count(" + AT_TABLE_NAME + ".sid)" + " FROM " + AT_TABLE_NAME + ","
			+ ToolResourceLinkDao.RL_TABLE_NAME + "," + ToolResourceUserDao.RU_TABLE_NAME + " WHERE resource_user_sid="
			+ ToolResourceUserDao.RU_TABLE_NAME + ".sid AND " + ToolResourceUserDao.RU_TABLE_NAME
			+ ".lti_user_sid=? AND " + ToolResourceUserDao.RU_TABLE_NAME + ".resource_sid="
			+ ToolResourceLinkDao.RL_TABLE_NAME + ".sid AND " + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".tool_key_sid=? AND errorCode<=1";

	/**
	 * SQL statement to count the attempts of a LTI user for an specific file name.
	 */
	private static final String SQL_COUNT_FILENAME = "SELECT count(" + AT_TABLE_NAME + ".sid)" + " FROM "
			+ AT_TABLE_NAME + "," + ToolResourceLinkDao.RL_TABLE_NAME + "," + ToolResourceUserDao.RU_TABLE_NAME
			+ " WHERE resource_user_sid=" + ToolResourceUserDao.RU_TABLE_NAME + ".sid AND "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".lti_user_sid=? AND " + ToolResourceUserDao.RU_TABLE_NAME
			+ ".resource_sid=" + ToolResourceLinkDao.RL_TABLE_NAME + ".sid AND "
			+ ToolResourceLinkDao.RL_TABLE_NAME + ".tool_key_sid=? AND errorCode<=1 AND filename=?";

	/**
	 * SQL statement to get all attempts of a LTI user.
	 */
	private static final String SQL_GET_ALL_USER = "SELECT " + AT_TABLE_NAME
			+ ".sid, resource_user_sid, original_ru_sid, epoch_seconds, nanoseconds, fileSaved, outputSaved, "
			+ "filename, storage_type, score, errorCode FROM " + AT_TABLE_NAME + ","
			+ ToolResourceLinkDao.RL_TABLE_NAME + "," + ToolResourceUserDao.RU_TABLE_NAME + " WHERE resource_user_sid="
			+ ToolResourceUserDao.RU_TABLE_NAME + ".sid AND " + ToolResourceUserDao.RU_TABLE_NAME
			+ ".lti_user_sid=? AND " + ToolResourceUserDao.RU_TABLE_NAME + ".resource_sid="
			+ ToolResourceLinkDao.RL_TABLE_NAME + ".sid AND " + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".tool_key_sid=? ORDER BY epoch_seconds ASC, nanoseconds ASC";

	/**
	 * SQL statement to all attempts using the same tool key.
	 */
	private static final String SQL_GET_ALL_TK = "SELECT " + AT_TABLE_NAME
			+ ".sid, resource_user_sid, original_ru_sid, epoch_seconds, nanoseconds, fileSaved, outputSaved, "
			+ "filename, storage_type, score, errorCode, " + ToolConsumerUserDao.LTI_USER_TABLE_NAME + ".sid, source_id FROM "
			+ AT_TABLE_NAME + "," + ToolResourceLinkDao.RL_TABLE_NAME + ","
			+ ToolResourceUserDao.RU_TABLE_NAME + "," + ToolConsumerUserDao.LTI_USER_TABLE_NAME
			+ " WHERE resource_user_sid=" + ToolResourceUserDao.RU_TABLE_NAME + ".sid AND "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".resource_sid=" + ToolResourceLinkDao.RL_TABLE_NAME + ".sid AND "
			+ ToolResourceUserDao.RU_TABLE_NAME + ".lti_user_sid=" + ToolConsumerUserDao.LTI_USER_TABLE_NAME
			+ ".sid AND " + ToolResourceLinkDao.RL_TABLE_NAME
			+ ".tool_key_sid=? ORDER BY epoch_seconds ASC, nanoseconds ASC, " + AT_TABLE_NAME + ".sid ASC";

	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private ToolAttemptDao() {
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
	 * Gets an attempt by resource user and timestamp.
	 *
	 * @param resourceUserSid the resource user serial ID
	 * @param instant         the creation timestamp
	 * @return the attempt if exists or null
	 */
	public static Attempt getById(int resourceUserSid, Instant instant) {
		Attempt attempt = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_ID);) {
			stmt.setInt(1, resourceUserSid);
			stmt.setLong(2, instant.getEpochSecond());
			stmt.setInt(3, instant.getNano());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int i = 1;
				attempt = new Attempt();
				attempt.setSid(rs.getInt(i++));

				ResourceUser auxRu = new ResourceUser();
				auxRu.setSid(resourceUserSid);
				attempt.setResourceUser(auxRu);

				final int auxSid = rs.getInt(i++);
				if (auxSid != resourceUserSid) {
					auxRu = new ResourceUser();
					auxRu.setSid(auxSid);
				}
				attempt.setOriginalResourceUser(auxRu);

				attempt.setInstant(instant);
				attempt.setFileSaved(rs.getBoolean(i++));
				attempt.setOutputSaved(rs.getBoolean(i++));
				attempt.setFileName(rs.getString(i++));
				attempt.setStorageType(rs.getInt(i++));
				attempt.setScore(rs.getInt(i++));
				attempt.setErrorCode(rs.getInt(i++));
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return attempt;
	}

	/**
	 * Gets a record by serial ID.
	 *
	 * @param sid the serial ID
	 * @return the object or null if not found
	 */
	public static Attempt getBySid(int sid) {
		Attempt attempt = null;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_BY_SID);) {
			stmt.setInt(1, sid);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int i = 1;
				attempt = new Attempt();
				attempt.setSid(sid);

				int auxSid = rs.getInt(i++);
				ResourceUser auxRu = new ResourceUser();
				auxRu.setSid(auxSid);
				attempt.setResourceUser(auxRu);

				auxSid = rs.getInt(i++);
				if (auxSid != auxRu.getSid()) {
					auxRu = new ResourceUser();
					auxRu.setSid(auxSid);
				}
				attempt.setOriginalResourceUser(auxRu);

				attempt.setInstant(Instant.ofEpochSecond(rs.getLong(i++), rs.getInt(i++)));
				attempt.setFileSaved(rs.getBoolean(i++));
				attempt.setOutputSaved(rs.getBoolean(i++));
				attempt.setFileName(rs.getString(i++));
				attempt.setStorageType(rs.getInt(i++));
				attempt.setScore(rs.getInt(i++));
				attempt.setErrorCode(rs.getInt(i++));

				auxSid = rs.getInt(i++);
				final LtiUser user = new LtiUser();
				user.setSid(auxSid);
				user.setSourceId(rs.getString(i++));
				attempt.getResourceUser().setUser(user);

				auxSid = rs.getInt(i++);
				final ResourceLink rl = new ResourceLink();
				rl.setSid(auxSid);
				final ToolKey tk = new ToolKey();
				tk.setSid(rs.getInt(i++));
				rl.setToolKey(tk);
				attempt.getResourceUser().setResourceLink(rl);
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		if (attempt != null && attempt.getOriginalResourceUser().getSid() != attempt.getResourceUser().getSid()) {
			attempt.setOriginalResourceUser(ToolResourceUserDao.getBySid(attempt.getOriginalResourceUser().getSid()));
		}

		return attempt;
	}

	/**
	 * Get the serial ID of a object.
	 *
	 * @param attempt object data
	 * @return true if successful
	 */
	public static boolean getSidByIds(Attempt attempt) {
		boolean res = false;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_SID);) {
			stmt.setInt(1, attempt.getResourceUser().getSid());
			stmt.setLong(2, attempt.getInstant().getEpochSecond());
			stmt.setInt(3, attempt.getInstant().getNano());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res = true;
				attempt.setSid(rs.getInt(1));
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
	 * @param attempt record data
	 * @return true if successful
	 */
	public static boolean update(Attempt attempt) {
		boolean res;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE);) {
			int i = 1;
			stmt.setInt(i++, attempt.getScore());
			stmt.setInt(i++, attempt.getErrorCode());
			stmt.setInt(i++, attempt.getSid());
			res = stmt.executeUpdate() == 1;

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
	 * @param attempt record data
	 * @return true if successful
	 */
	public static boolean create(Attempt attempt) {
		boolean res;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_NEW);) {
			int i = 1;
			stmt.setInt(i++, attempt.getResourceUser().getSid());
			stmt.setInt(i++, attempt.getOriginalResourceUser().getSid());
			stmt.setLong(i++, attempt.getInstant().getEpochSecond());
			stmt.setInt(i++, attempt.getInstant().getNano());
			stmt.setBoolean(i++, attempt.isFileSaved());
			stmt.setBoolean(i++, attempt.isOutputSaved());
			stmt.setString(i++, attempt.getFileName());
			stmt.setInt(i++, attempt.getStorageType().getCode());
			stmt.setInt(i++, attempt.getScore());
			stmt.setInt(i++, attempt.getErrorCode());
			res = stmt.executeUpdate() == 1;

		} catch (final SQLException e) {
			logger.error("Create: ", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}
		if (res) {
			res = getSidByIds(attempt);
		}

		return res;
	}

	/**
	 * Delete a record.
	 *
	 * @param attempt record data
	 * @return true if successful
	 */
	public static boolean delete(Attempt attempt) {
		boolean deleted = false;
		final Connection conn = dbUtil.getConnection();
		try {
			// Delete
			try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
				stmt.setInt(1, attempt.getSid());
				deleted = stmt.executeUpdate() > 0;
			}
		} catch (final SQLException e) {
			logger.error("Delete: ", e);
		} finally {
			dbUtil.closeConnection(conn);
		}
		return deleted;
	}

	/**
	 * Counts user attempts for a tool key.
	 *
	 * @param user the user
	 * @param tk   the tool key
	 * @return the count of user attempts for the tool key
	 */
	public static int countUserAttempts(LtiUser user, ToolKey tk) {
		int count = 0;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_COUNT);) {
			stmt.setInt(1, user.getSid());
			stmt.setInt(2, tk.getSid());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return count;
	}

	/**
	 * Counts user attempts for a tool key and a file name.
	 *
	 * @param user     the user
	 * @param tk       the tool key
	 * @param filename the file name
	 * @return the count of user attempts for the tool key and file name
	 */
	public static int countUserAttempts(LtiUser user, ToolKey tk, String filename) {
		int count = 0;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_COUNT_FILENAME);) {
			stmt.setInt(1, user.getSid());
			stmt.setInt(2, tk.getSid());
			stmt.setString(3, filename);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
			rs.close();
		} catch (final SQLException e) {
			logger.error("Get: ", e);
		} finally {
			dbUtil.closeConnection(connection);
		}
		return count;
	}

	/**
	 * Gets a list of user attempts for a tool key.
	 *
	 * @param user the user
	 * @param tk   the tool key
	 * @return the list of user attempts for the tool key
	 */
	public static List<Attempt> getUserAttempts(LtiUser user, ToolKey tk) {
		final List<Attempt> list = new ArrayList<>();
		final List<Attempt> listWithoutOriginalUser = new ArrayList<>();

		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_ALL_USER);) {
			stmt.setInt(1, user.getSid());
			stmt.setInt(2, tk.getSid());
			final ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				int i = 1;
				final Attempt attempt = new Attempt();
				attempt.setSid(rs.getInt(i++));

				int auxSid = rs.getInt(i++);
				ResourceUser auxRu = new ResourceUser();
				auxRu.setSid(auxSid);
				auxRu.setUser(user);
				attempt.setResourceUser(auxRu);

				auxSid = rs.getInt(i++);
				if (auxSid != auxRu.getSid()) {
					auxRu = new ResourceUser();
					auxRu.setSid(auxSid);
				}
				attempt.setOriginalResourceUser(auxRu);

				attempt.setInstant(Instant.ofEpochSecond(rs.getLong(i++), rs.getInt(i++)));
				attempt.setFileSaved(rs.getBoolean(i++));
				attempt.setOutputSaved(rs.getBoolean(i++));
				attempt.setFileName(rs.getString(i++));
				attempt.setStorageType(rs.getInt(i++));
				attempt.setScore(rs.getInt(i++));
				attempt.setErrorCode(rs.getInt(i++));
				if (attempt.getResourceUser().getSid() != attempt.getOriginalResourceUser().getSid()) {
					listWithoutOriginalUser.add(attempt);
				}

				list.add(attempt);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Unable to get attempts", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}
		// Complete without original resource users
		for (final Attempt attempt : listWithoutOriginalUser) {
			logger.info("Retrieving original user");
			final ResourceUser ru = ToolResourceUserDao.getBySid(attempt.getOriginalResourceUser().getSid());
			if (ru != null) {
				attempt.setOriginalResourceUser(ru);
			}
		}
		return list;
	}

	/**
	 * Gets a list of all attempts for a tool key.
	 *
	 * @param tk the tool key
	 * @return the list of all attempts
	 */
	public static List<Attempt> getToolKeyAttempts(ToolKey tk) {
		final List<Attempt> list = new ArrayList<>();
		final List<Attempt> listWithoutOriginalUser = new ArrayList<>();
		final Map<Integer, LtiUser> knownUsers = new HashMap<>();

		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_ALL_TK);) {
			stmt.setInt(1, tk.getSid());
			final ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				int i = 1;
				final Attempt attempt = new Attempt();
				attempt.setSid(rs.getInt(i++));

				int auxSid = rs.getInt(i++);
				ResourceUser auxRu = new ResourceUser();
				auxRu.setSid(auxSid);
				attempt.setResourceUser(auxRu);

				auxSid = rs.getInt(i++);
				if (auxSid != auxRu.getSid()) {
					auxRu = new ResourceUser();
					auxRu.setSid(auxSid);
				}
				attempt.setOriginalResourceUser(auxRu);

				attempt.setInstant(Instant.ofEpochSecond(rs.getLong(i++), rs.getInt(i++)));
				attempt.setFileSaved(rs.getBoolean(i++));
				attempt.setOutputSaved(rs.getBoolean(i++));
				attempt.setFileName(rs.getString(i++));
				attempt.setStorageType(rs.getInt(i++));
				attempt.setScore(rs.getInt(i++));
				attempt.setErrorCode(rs.getInt(i++));

				// User
				auxSid = rs.getInt(i++);
				final LtiUser user = new LtiUser();
				user.setSid(auxSid);
				user.setSourceId(rs.getString(i++));
				attempt.getResourceUser().setUser(user);
				knownUsers.putIfAbsent(attempt.getResourceUser().getSid(), user);
				if (attempt.getResourceUser().getSid() != attempt.getOriginalResourceUser().getSid()) {
					listWithoutOriginalUser.add(attempt);
				}

				list.add(attempt);
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Unable to get attempts", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}
		// Complete without original resource users
		for (final Attempt attempt : listWithoutOriginalUser) {
			final int sid = attempt.getOriginalResourceUser().getSid();
			final LtiUser user = knownUsers.get(sid);
			if (user == null) {
				logger.info("Retrieving original user");
				final ResourceUser ru = ToolResourceUserDao.getBySid(sid);
				if (ru != null) {
					attempt.setOriginalResourceUser(ru);
				}
			} else {
				attempt.getOriginalResourceUser().setUser(user);
			}
		}
		return list;
	}

	/**
	 * Gets an attempt by a secured serial ID.
	 *
	 * @param securedSid the secured serial ID
	 * @param tk         the tool key
	 * @return the attempt or null if not found
	 */
	public static Attempt getBySecuredSid(String securedSid, ToolKey tk) {
		Attempt attempt = null;

		final SecurityUtil.SecuredSid sSid = SecurityUtil.getPlainSecuredSid(securedSid, AT_SERIAL_VERSION_UID);
		if (sSid.sid > 0) {
			attempt = getBySid(sSid.sid);
			if (attempt == null || sSid.verifier != attempt.getInstant().toEpochMilli()
					|| attempt.getResourceUser().getResourceLink().getToolKey().getSid() != tk.getSid()) { 
				// Error
				attempt = null;
			}
		}
		return attempt;
	}

}
