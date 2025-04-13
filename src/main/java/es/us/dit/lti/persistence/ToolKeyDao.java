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

import java.nio.file.FileAlreadyExistsException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Consumer;
import es.us.dit.lti.entity.Context;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.Tool;
import es.us.dit.lti.entity.ToolKey;

/**
 * The Tool Key Data Access Object is the interface providing access to
 * tool keys related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class ToolKeyDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ToolKeyDao.class);

	/**
	 * Table name of this DAO.
	 */
	public static final String TK_TABLE_NAME = "tool_key";

	/**
	 * SQL statement to get a tool key by the (consumer) key.
	 */
	public static final String SQL_GET_BY_ID = "SELECT sid, tool_sid, consumer_sid, context_sid, resource_link_sid,"
			+ " key, secret, address, enabled, created, updated FROM " + TK_TABLE_NAME + " WHERE key=?";

	/**
	 * SQL statement to add a tool key.
	 */
	public static final String SQL_NEW = "INSERT INTO " + TK_TABLE_NAME
			+ "(tool_sid, consumer_sid, context_sid, resource_link_sid, key, secret, address, enabled, created, updated) VALUES "
			+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL statement to update a tool key.
	 */
	public static final String SQL_UPDATE = "UPDATE " + TK_TABLE_NAME
			+ " SET key=?, secret=?, address=?, enabled=?, updated=? WHERE sid=?";

	/**
	 * SQL statement to delete a tool key.
	 */
	public static final String SQL_DELETE = "DELETE FROM " + TK_TABLE_NAME + " WHERE sid=?";

	/**
	 * SQL statement to delete a reference to a tool key in a resource link.
	 */
	public static final String SQL_DELETE_UPDATE_RL = "UPDATE " + ToolResourceLinkDao.RL_TABLE_NAME
			+ " SET tool_key_sid=NULL" + " WHERE tool_key_sid=?";

	/**
	 * SQL statement to get a tool key by the serial ID.
	 */
	public static final String SQL_GET_BY_SID = "SELECT sid, tool_sid, consumer_sid, context_sid, resource_link_sid,"
			+ " key, secret, address, enabled, created, updated FROM " + TK_TABLE_NAME + " WHERE sid=?";

	/**
	 * SQL statement to get a default tool key (without constraints) for a specific
	 * tool.
	 */
	public static final String SQL_GET_DEFAULT = "SELECT sid, key, secret, address, enabled, created, updated FROM "
			+ TK_TABLE_NAME + " WHERE tool_sid=? AND consumer_sid is NULL";

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
	private ToolKeyDao() {
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
	 * Deletes a cached item.
	 *
	 * @param key key of cache
	 */
	public static void deleteCache(String key) {
		cache.remove(key);
	}

	/**
	 * Gets a cached item.
	 *
	 * @param key key of cache
	 * @return cached item or null
	 */
	public static Tool getCache(String key) {
		return cache.get(key);
	}

	/**
	 * Create a record.
	 *
	 * @param tk record data
	 * @return true if successful
	 * @throws FileAlreadyExistsException if there is a tool key with the same
	 *                                    (consumer) key
	 */
	public static synchronized boolean create(ToolKey tk) throws FileAlreadyExistsException {
		if (tk == null) {
			return false;
		}

		boolean result = true;
		// Get the parameters of the tool key. Check that the mandatory ones are there
		if (tk.getKey() == null || tk.getSecret() == null || tk.getTool() == null) {
			logger.error("Insufficient parameters.");
			return false;
		}

		// Already exists?
		final ToolKey tkOther = get(tk.getKey(), true);
		if (tkOther != null) {
			logger.error("The key exists.");
			throw new FileAlreadyExistsException(null);
		}

		final Connection conn = dbUtil.getConnection();
		// Insert
		logger.info("The tool key: {} does not exist. It will be create.", tk.getKey());
		try (PreparedStatement stmt = conn.prepareStatement(SQL_NEW)) {
			stmt.setInt(1, tk.getTool().getSid());
			if (tk.getConsumer() == null) {
				stmt.setNull(2, java.sql.Types.INTEGER);
			} else {
				stmt.setInt(2, tk.getConsumer().getSid());
			}
			if (tk.getContext() == null) {
				stmt.setNull(3, java.sql.Types.INTEGER);
			} else {
				stmt.setInt(3, tk.getContext().getSid());
			}
			if (tk.getResourceLink() == null) {
				stmt.setNull(4, java.sql.Types.INTEGER);
			} else {
				stmt.setInt(4, tk.getResourceLink().getSid());
			}
			stmt.setString(5, tk.getKey());
			stmt.setString(6, tk.getSecret());
			stmt.setString(7, tk.getAddress());
			stmt.setBoolean(8, tk.isEnabled());
			final Calendar now = Calendar.getInstance();
			stmt.setTimestamp(9, DaoUtil.toTimestamp(now));
			stmt.setTimestamp(10, DaoUtil.toTimestamp(now));
			stmt.executeUpdate();

		} catch (final Exception ex) {
			result = false;
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return result;
	}

	/**
	 * Update a record.
	 *
	 * @param tk record data
	 * @return true if successful
	 * @throws FileAlreadyExistsException if there is a tool key with the same
	 *                                    (consumer) key
	 */
	public static synchronized boolean update(ToolKey tk) throws FileAlreadyExistsException {

		// Get current values from the database
		final ToolKey tkExist = getBySid(tk.getSid());
		if (tkExist == null) {
			logger.error("La clave no existe.");
			return false;
		}
		// Already exists?
		final ToolKey tkOther = get(tk.getKey(), true);
		if (tkOther != null && tkOther.getSid() != tk.getSid()) {
			logger.error("The key exists.");
			throw new FileAlreadyExistsException(null);
		}
		// Delete cache
		cache.remove(tkExist.getKey());

		boolean result = true;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE);) {
			stmt.setString(1, tk.getKey());
			stmt.setString(2, tk.getSecret());
			stmt.setString(3, tk.getAddress());
			stmt.setBoolean(4, tk.isEnabled());
			stmt.setTimestamp(5, DaoUtil.toTimestamp(Calendar.getInstance()));
			stmt.setInt(6, tk.getSid());
			stmt.executeUpdate();
		} catch (final SQLException e) {
			logger.error("Error updating tool key.", e);
			result = false;
		} finally {
			dbUtil.closeConnection(conn);
		}
		return result;

	}

	/**
	 * Delete a record.
	 *
	 * @param tk record data
	 * @return true if successful
	 */
	public static synchronized boolean delete(ToolKey tk) {
		boolean delKey = true;
		final Connection conn = dbUtil.getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE);) {
			stmt.setLong(1, tk.getSid());
			stmt.executeUpdate();
		} catch (final SQLException e) {
			delKey = false;
			logger.error("Error deleting tool_key", e);
		}
		// delete references in resource link, to avoid reuse
		try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_UPDATE_RL);) {
			stmt.setLong(1, tk.getSid());
			stmt.executeUpdate();
		} catch (final SQLException e) {
			delKey = false;
			logger.error("Error deleting tool_key_sid from RL", e);
		}
		// Delete cache
		cache.remove(tk.getKey());

		dbUtil.closeConnection(conn);
		return delKey;
	}

	/**
	 * Gets a tool key by (consumer) key.
	 *
	 * @param key  the (consumer) key
	 * @param lazy true if references should not be completed
	 * @return the object or null if not found
	 */
	public static synchronized ToolKey get(String key, boolean lazy) {
		ToolKey result = null;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_ID);) {
			stmt.setString(1, key);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = new ToolKey();
				result.setSid(rs.getInt(1));

				if (lazy) {
					final Tool t = new Tool();
					t.setSid(rs.getInt(2)); // not null always
					result.setTool(t);
				} else {
					// With cache
					Tool t = cache.get(key);
					if (t == null) {
						t = ToolDao.getBySid(rs.getInt(2));
						if (t != null) {
							cache.put(key, t);
						}
					}
					result.setTool(t);
				}

				int lastId = rs.getInt(3);
				if (!rs.wasNull()) {
					if (lazy) {
						final Consumer c = new Consumer();
						c.setSid(lastId);
						result.setConsumer(c);
					} else {
						result.setConsumer(ToolConsumerDao.getBySid(lastId));
					}
				}

				lastId = rs.getInt(4);
				if (!rs.wasNull()) {
					if (lazy) {
						final Context c = new Context();
						c.setSid(lastId);
						result.setContext(c);
					} else {
						final Context c = ToolContextDao.getBySid(lastId);
						if (c.getConsumer().getSid() == result.getConsumer().getSid()) {
							c.setConsumer(result.getConsumer());
						}
						result.setContext(ToolContextDao.getBySid(lastId));
					}
				}

				lastId = rs.getInt(5);
				if (!rs.wasNull()) {
					if (lazy) {
						final ResourceLink r = new ResourceLink();
						r.setSid(lastId);
						result.setResourceLink(r);
					} else {
						final ResourceLink r = ToolResourceLinkDao.getBySid(lastId);
						if (r.getTool().getSid() == result.getTool().getSid()) {
							r.setTool(result.getTool());
						}
						if (result.getContext() != null && r.getContext().getSid() == result.getContext().getSid()) {
							r.setContext(result.getContext());
						}
						result.setResourceLink(r);
					}

				}

				result.setKey(rs.getString(6));
				result.setSecret(rs.getString(7));
				result.setAddress(rs.getString(8));
				result.setEnabled(rs.getBoolean(9));

				result.setCreated(DaoUtil.toCalendar(rs.getTimestamp(10)));
				result.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(11)));
			}
			rs.close();

		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return result;
	}

	/**
	 * Gets a record by serial ID.
	 *
	 * @param sid the serial ID
	 * @return the object or null if not found
	 */
	public static synchronized ToolKey getBySid(int sid) {
		ToolKey result = null;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_SID);) {
			stmt.setInt(1, sid);
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = new ToolKey();
				result.setSid(rs.getInt(1));
				final Tool t = new Tool();
				t.setSid(rs.getInt(2)); // not null always
				result.setTool(t);

				int lastId = rs.getInt(3);
				if (!rs.wasNull()) {
					final Consumer c = new Consumer();
					c.setSid(lastId);
					result.setConsumer(c);
				}

				lastId = rs.getInt(4);
				if (!rs.wasNull()) {
					final Context c = new Context();
					c.setSid(lastId);
					result.setContext(c);
				}

				lastId = rs.getInt(5);
				if (!rs.wasNull()) {
					final ResourceLink r = new ResourceLink();
					r.setSid(lastId);
					result.setResourceLink(r);
				}

				result.setKey(rs.getString(6));
				result.setSecret(rs.getString(7));
				result.setAddress(rs.getString(8));
				result.setEnabled(rs.getBoolean(9));

				result.setCreated(DaoUtil.toCalendar(rs.getTimestamp(10)));
				result.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(11)));
			}
			rs.close();

		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return result;
	}

	/**
	 * Gets a default tool key (without constraints) for a specific tool.
	 *
	 * @param tool the tool
	 * @return the tool key if found or null
	 */
	public static synchronized ToolKey getDefault(Tool tool) {
		ToolKey result = null;
		final Connection conn = dbUtil.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_DEFAULT);) {
			stmt.setInt(1, tool.getSid());
			final ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = new ToolKey();
				result.setSid(rs.getInt(1));
				result.setTool(tool);
				result.setKey(rs.getString(2));
				result.setSecret(rs.getString(3));
				result.setAddress(rs.getString(4));
				result.setEnabled(rs.getBoolean(5));
				result.setCreated(DaoUtil.toCalendar(rs.getTimestamp(6)));
				result.setUpdated(DaoUtil.toCalendar(rs.getTimestamp(7)));
			}
			rs.close();

		} catch (final Exception ex) {
			logger.error("Error: ", ex);
		} finally {
			dbUtil.closeConnection(conn);
		}

		return result;
	}

}
