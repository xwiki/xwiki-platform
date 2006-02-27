/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

package com.xpn.xwiki.xmlrpc;

import java.util.Hashtable;

public class ServerInfo {
   private int majorVersion;
   private int minorVersion;
   private int patchLevel;
   private String buildId;
   private boolean developmentBuild;
   private String baseUrl;

    public Hashtable getHashtable() {
        Hashtable ht = new Hashtable();
        ht.put("majorVersion", new Integer(getMajorVersion()));
        ht.put("minorVersion", new Integer(getMinorVersion()));
        ht.put("patcheLevel", new Integer(getPatchLevel()));
        ht.put("buildId", getBuildId());
        ht.put("developmentBuild", new Boolean(isDevelopmentBuild()));
        ht.put("baseUrl", getBaseUrl());
        return ht;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public int getPatchLevel() {
        return patchLevel;
    }

    public void setPatchLevel(int patchLevel) {
        this.patchLevel = patchLevel;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public boolean isDevelopmentBuild() {
        return developmentBuild;
    }

    public void setDevelopmentBuild(boolean developmentBuild) {
        this.developmentBuild = developmentBuild;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
