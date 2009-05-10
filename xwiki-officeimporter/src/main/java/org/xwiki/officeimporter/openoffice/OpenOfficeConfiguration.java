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
package org.xwiki.officeimporter.openoffice;

/**
 * Configuration properties for office importer module.
 * <p>
 * These configuration properties are defined in XWiki's global configuration file using the prefix of "officeimporter".
 * </p>
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public interface OpenOfficeConfiguration
{
    /**
     * Indicates an internally managed oo server instance.
     */
    int SERVER_TYPE_INTERNAL = 0;
    
    /**
     * Indicates an externally managed locally deployed oo server instance.
     */
    int SERVER_TYPE_EXTERNAL_LOCAL = 1;
    
    /**
     * Indicates an externally managed remotely deployed oo server instance.
     */
    int SERVER_TYPE_EXTERNAL_REMOTE = 2;
    
    /**
     * Returns the type of the openoffice server instance consumed by officeimporter.<br/>
     * 0 - Internally managed server instance.<br/>
     * 1 - Externally managed (local) server instance.<br/>
     * 
     * @return type of the openoffice server used by officeimporter.
     */
    int getServerType();
    
    /**
     * @return port number used for connecting to the openoffice server instance. 
     */
    int getServerPort();

    /**
     * @return whether openoffice server should be started / connected automatically with XE.
     */
    boolean isAutoStart();
    
    /**
     * @return path to openoffice server installation.
     */
    String getHomePath();

    /**
     * @return path to openoffice execution profile.
     */
    String getProfilePath();
    
    /**
     * @return maximum number of simultaneous conversion tasks to be handled by a single oo process instance.
     */
    int getMaxTasksPerProcess();

    /**
     * @return timeout for document conversion tasks.
     */
    long getTaskExecutionTimeout();
}
