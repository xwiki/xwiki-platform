/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 17 juin 2004
 * Time: 12:26:03
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
