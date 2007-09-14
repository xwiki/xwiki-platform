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
 *
 */

package com.xpn.xwiki.xmlrpc.model;

public interface ServerInfo extends MapObject
{

    /**
     * the major version number of the Confluence instance
     */
    int getMajorVersion();

    void setMajorVersion(int majorVersion);

    /**
     * the minor version number of the Confluence instance
     */
    int getMinorVersion();

    void setMinorVersion(int minorVersion);

    /**
     * the patch-level of the Confluence instance
     */
    int getPatchLevel();

    void setPatchLevel(int patchLevel);

    /**
     * the build ID of the Confluence instance (usually a number)
     */
    String getBuildId();

    void setBuildId(String buildId);

    /**
     * Whether the build is a developer-only release or not
     */
    boolean isDevelopmentBuild();

    void setDevelopmentBuild(boolean developmentBuild);

    /**
     * The base URL for the confluence instance
     */
    String getBaseUrl();

    void setBaseUrl(String baseUrl);

}
