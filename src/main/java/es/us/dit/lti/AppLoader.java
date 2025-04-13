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

package es.us.dit.lti;

import java.sql.Connection;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Settings;
import es.us.dit.lti.persistence.DbUtilDataSource;
import es.us.dit.lti.persistence.DbUtilSingleConnection;
import es.us.dit.lti.persistence.IDbUtil;
import es.us.dit.lti.persistence.MgmtUserDao;
import es.us.dit.lti.persistence.SettingsDao;
import es.us.dit.lti.persistence.ToolAttemptDao;
import es.us.dit.lti.persistence.ToolConsumerDao;
import es.us.dit.lti.persistence.ToolConsumerUserDao;
import es.us.dit.lti.persistence.ToolContextDao;
import es.us.dit.lti.persistence.ToolDao;
import es.us.dit.lti.persistence.ToolKeyDao;
import es.us.dit.lti.persistence.ToolNonceDao;
import es.us.dit.lti.persistence.ToolResourceLinkDao;
import es.us.dit.lti.persistence.ToolResourceUserDao;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Web application initialization and destroy.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebListener
public class AppLoader implements ServletContextListener {

	/**
	 * Logger.
	 */
	final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Application scope util to access db.
	 */
	private IDbUtil appDbUtil = null;

	/**
	 * Clean up application.
	 * 
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		logger.info("ServletContextListener destroyed");
		if (appDbUtil != null) {
			appDbUtil.destroy();
		}
	}

	/**
	 * Initialize application scope attributes, db and DAOs.
	 *
	 * <p>Application scope attributes:
	 * <ul>
	 * 	<li><code>appVersion</code>: version of this app to show in web header.
	 * </ul>
	 *
	 * <p>Looks up in JNDI the datasource to use. The name must be in
	 * context parameter <code>datasourceName</code> (<code>jdbc/ltidb</code> by default). If not
	 * exist the datasource in JNDI, treat the value of that context parameter
	 * as a database connection string.
	 *
	 * <p>If the connection succeeds, gets the settings using a {@link DbUtilSingleConnection}.
	 * If setting <code>datasourceMode</code> is true, changes to {@link DbUtilDataSource}.
	 *
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		if (logger.isInfoEnabled()) {
			logger.info("AppLoader");
			logger.info("ServletContextListener started: {}",
					Settings.DATE_TIME_FORMATTER.format(Instant.now()));
			logger.info("Do not forget to set cookie sameSite to strict");
		}

		sce.getServletContext().setAttribute("appVersion", "1.2");

		appDbUtil = new DbUtilSingleConnection();
		SettingsDao.setDbUtil(appDbUtil);
		// Init with DataSource if possible
		String resourceName = sce.getServletContext().getInitParameter("datasourceName");
		appDbUtil.init(resourceName);
		Connection c = appDbUtil.getConnection();
		if (c != null) { // working
			// Init with database
			SettingsDao.get();
		}
		appDbUtil.closeConnection(c);

		if (logger.isInfoEnabled()) {
			logger.info("{}", Settings.printString());
		}

		// DataSource Mode (with pool of connections)
		if (Settings.isDatasourceMode()) {
			// Change appDbUtil
			appDbUtil.destroy();
			appDbUtil = new DbUtilDataSource();
			appDbUtil.init(resourceName);
			SettingsDao.setDbUtil(appDbUtil);
			logger.info("Datasource mode");
		}

		if (SettingsDao.init()) {
			logger.info("Init DB OK");
		} else {
			logger.error("Error init DB");
		}

		// Configure all DAOs
		ToolDao.setDbUtil(appDbUtil);
		MgmtUserDao.setDbUtil(appDbUtil);
		ToolKeyDao.setDbUtil(appDbUtil);
		ToolConsumerDao.setDbUtil(appDbUtil);
		ToolContextDao.setDbUtil(appDbUtil);
		ToolResourceLinkDao.setDbUtil(appDbUtil);
		ToolNonceDao.setDbUtil(appDbUtil);
		ToolConsumerUserDao.setDbUtil(appDbUtil);
		ToolResourceUserDao.setDbUtil(appDbUtil);
		ToolAttemptDao.setDbUtil(appDbUtil);

	}

}