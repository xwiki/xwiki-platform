/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.officeimporter.server;

import org.xwiki.component.annotation.Role;

/**
 * Configuration properties for the {@link OfficeServer}. They are defined in XWiki's global configuration file using
 * the prefix of "officeimporter".
 * 
 * @version $Id$
 * @since 5.0M2
 */
@Role
public interface OfficeServerConfiguration
{
    /**
     * Indicates an internally managed office server instance.
     */
    int SERVER_TYPE_INTERNAL = 0;

    /**
     * Indicates an externally managed locally deployed office server instance.
     */
    int SERVER_TYPE_EXTERNAL_LOCAL = 1;

    /**
     * Indicates an externally managed remotely deployed office server instance.
     */
    int SERVER_TYPE_EXTERNAL_REMOTE = 2;

    /**
     * Returns the type of the office server instance consumed by office importer module:
     * <ul>
     * <li>0 - Internally managed server instance</li>
     * <li>1 - Externally managed (local) server instance</li>
     * </ul>
     * .
     * 
     * @return type of the office server used by the office importer module
     */
    int getServerType();

    /**
     * @return the port number used for connecting to the office server instance
     */
    int getServerPort();

    /**
     * @return whether office server should be started / connected automatically with XWiki Enterprise
     */
    boolean isAutoStart();

    /**
     * @return the path to the office server installation, or {@code null} if the home path is not configured and the
     *         office installation is not detected (which means the office server is either not installed or it is
     *         installed at a custom path)
     */
    String getHomePath();

    /**
     * @return the path to office server execution profile, {@code null} by default
     */
    String getProfilePath();

    /**
     * @return the maximum number of simultaneous conversion tasks to be handled by a single office process instance
     */
    int getMaxTasksPerProcess();

    /**
     * @return the timeout for document conversion tasks
     */
    long getTaskExecutionTimeout();
}
