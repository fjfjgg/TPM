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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.SecurityUtil;
import es.us.dit.lti.entity.MgmtUserType;
import es.us.dit.lti.entity.Settings;

/**
 * The Management User Data Access Object is the interface providing access to
 * application settings related data.
 *
 * @author Francisco José Fernández Jiménez
 */
public final class SettingsDao {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SettingsDao.class);

	/**
	 * Default username of superuser.
	 */
	private static final String DEFAULT_SUPERUSER = "super";
	/**
	 * SQL statement to update application settings.
	 */
	private static final String SQL_UPDATE = "UPDATE settings SET app_name=?, tools_folder=?, max_upload_size=?,"
			+ " concurrent_users=?, corrector_filename=?, default_css_path=?, notice=?";
	/**
	 * SQL statement to create default superuser.
	 */
	private static final String SQL_CREATE_SUPER = "INSERT INTO mgmt_user (username,password,type,is_local,exe_restrictions,created,updated) VALUES ('"
			+ DEFAULT_SUPERUSER + "',?,-1,TRUE,NULL,?,?)";
	/**
	 * SQL statement to check if default superuser exists.
	 */
	private static final String SQL_GET_SUPER = "SELECT username FROM mgmt_user WHERE type=="
			+ MgmtUserType.SUPER.getCode();
	/**
	 * SQL statement to check if applications settings exist in db.
	 */
	private static final String SQL_GET_APP_NAME = "SELECT app_name FROM settings";
	/**
	 * SQL statement to create application settings in db.
	 */
	private static final String SQL_CREATE_SETTINGS = "INSERT INTO settings (app_name,tools_folder,max_upload_size,concurrent_users,corrector_filename) "
			+ "VALUES (?,?,?,?,?)";
	/**
	 * SQL statement to get application settings.
	 */
	private static final String SQL_GET_SETTINGS = "SELECT * FROM settings";

	/**
	 * Utility class that provides methods for managing connections to a database.
	 */
	private static IDbUtil dbUtil = null;

	/**
	 * Can not create objects.
	 */
	private SettingsDao() {
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
	 * Gets the application settings from db.
	 */
	public static synchronized void get() {
		final Connection conn = dbUtil.getConnection();
		try (Statement stmt = conn.createStatement();) {
			final ResultSet rs = stmt.executeQuery(SQL_GET_SETTINGS);
			if (rs.next()) {
				Settings.init(rs.getString("app_name"), rs.getBoolean("datasource_mode"), rs.getString("tools_folder"),
						rs.getInt("max_upload_size"), rs.getInt("concurrent_users"), rs.getString("corrector_filename"),
						rs.getString("default_css_path"), rs.getString("notice"));
			}
			rs.close();
		} catch (final Exception ex) {
			logger.error("Error getting settings {}", ex.getMessage());
		} finally {
			dbUtil.closeConnection(conn);
		}
	}

	/**
	 * Save application settings to db.
	 *
	 * @return true if successful
	 */
	public static synchronized boolean set() {
		boolean res;
		final Connection connection = dbUtil.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE);) {
			int i = 1;
			stmt.setString(i++, Settings.getAppName());
			stmt.setString(i++, Settings.getToolsFolder());
			stmt.setInt(i++, Settings.getMaxUploadSize());
			stmt.setInt(i++, Settings.getConcurrentUsers());
			stmt.setString(i++, Settings.getCorrectorFilename());
			stmt.setString(i++, Settings.getDefaultCssPath());
			stmt.setString(i++, Settings.getNotice());

			res = stmt.executeUpdate() == 1;

		} catch (final SQLException e) {
			logger.error("Set", e);
			res = false;
		} finally {
			dbUtil.closeConnection(connection);
		}

		return res;
	}

	/**
	 * Check if application settings and default superuser exist in db, and create
	 * default ones if they do not exist.
	 *
	 * @return true if successful
	 */
	public static boolean init() {
		boolean ok = true;
		final Connection conn = dbUtil.getConnection();
		if (conn == null) {
			return false;
		}

		try (Statement stmt = conn.createStatement();
				PreparedStatement pstmt = conn.prepareStatement(SQL_CREATE_SETTINGS);
				PreparedStatement pstmt2 = conn.prepareStatement(SQL_CREATE_SUPER);) {
			final DatabaseMetaData dbm = conn.getMetaData();
			logger.info("DB product {} {}", dbm.getDatabaseProductName(), dbm.getDatabaseProductVersion());

			ResultSet rs = stmt.executeQuery(SQL_GET_APP_NAME);
			if (!rs.next()) {
				// settings does not exist, create default
				pstmt.setString(1, Settings.getAppName());
				pstmt.setString(2, Settings.getToolsFolder());
				pstmt.setInt(3, Settings.getMaxUploadSize());
				pstmt.setInt(4, Settings.getConcurrentUsers());
				pstmt.setString(5, Settings.getCorrectorFilename());
				if (pstmt.executeUpdate() > 0) {
					logger.info("Initial application settings created");
				}
			}
			rs.close();

			rs = stmt.executeQuery(SQL_GET_SUPER);
			if (!rs.next()) {
				// No exist
				// Insert default superuser
				final String strongPassword = SecurityUtil.getPasswordHash(DEFAULT_SUPERUSER);
				final Calendar now = Calendar.getInstance();
				final long ts = now.getTimeInMillis();
				pstmt2.setString(1, strongPassword);
				pstmt2.setLong(2, ts);
				pstmt2.setLong(3, ts);

				if (pstmt2.executeUpdate() > 0) {
					logger.info("Default superuser created");
				}

			}
			rs.close();

		} catch (SQLException | InvalidKeySpecException e) {
			e.printStackTrace();
			ok = false;
		} finally {
			dbUtil.closeConnection(conn);
		}

		return ok;
	}

	
	/**
	 * Optimize db.
	 *
	 * @return true if successful
	 */
	public static synchronized boolean optimizeDb() {
		boolean res = false;
		String[] sqls = { "VACUUM FULL",
				"VACUUM",
				"OPTIMIZE TABLE attempt, consumer, context, lti_user, mgmt_user, nonce, resource_link, resource_user, settings, tool, tool_counter,\n"
						+ "tool_key, tool_user" };
		for (String sql: sqls) {
			final Connection connection = dbUtil.getConnection();
			try (PreparedStatement stmt = connection.prepareStatement(sql);) {
				stmt.executeUpdate();
				// if works, ok
				res = true;
				logger.info(sql);
				break;
			} catch (final SQLException e) {
				// Ignore, try another
				logger.error("Error in optimize DB.");
			} finally {
				dbUtil.closeConnection(connection);
			}
		}

		return res;
	}

}
