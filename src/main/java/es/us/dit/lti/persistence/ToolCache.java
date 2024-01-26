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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.Tool;

/**
 * Cache of tools by name or tool key.
 *
 * @author Francisco José Fernández Jiménez
 * @version 1.0
 */
public class ToolCache {
	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());
	/**
	 * Cache.
	 */
	private final Map<String, Tool> cacheToolsByKey = new ConcurrentHashMap<>();

	/**
	 * Adds tool to cache.
	 *
	 * @param key cache key
	 * @param t   the tool
	 */
	public void put(String key, Tool t) {
		logger.info("Cached: {}", key);
		cacheToolsByKey.put(key, t);
	}

	/**
	 * Removes a tool from cache.
	 *
	 * @param key cache key
	 */
	public void remove(String key) {
		logger.info("Remove cache: {}", key);
		cacheToolsByKey.remove(key);
	}

	/**
	 * Gets a cached tool.
	 *
	 * @param key cache key
	 * @return cached tool or null
	 */
	public Tool get(String key) {
		return cacheToolsByKey.get(key);
	}
}
