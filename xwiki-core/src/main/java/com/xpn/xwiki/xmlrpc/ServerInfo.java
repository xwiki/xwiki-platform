/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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

import java.util.Map;
import java.util.HashMap;

import com.xpn.xwiki.xmlrpc.Convert.ConversionException;

/**
 * This helper class is not referenced (yet)
 * (ConfluenceRpcInterface.getServerInfo throws "Not implemented" exception) 
 * 
 * The parameters here are somehow different than the ones given here:
 * http://docs.atlassian.com/software/jira/docs/api/rpc-jira-plugin/latest/com/atlassian/jira/rpc/soap/beans/RemoteServerInfo.html 
 */
public class ServerInfo
{
    private int majorVersion;

    private int minorVersion;

    private int patchLevel;

    private String buildId;

    private boolean developmentBuild;

    private String baseUrl;

    public ServerInfo(Map map) throws ConversionException
    {
        if (map.containsKey("majorVersion")) {
            setMajorVersion(Convert.str2int((String) map.get("majorVersion")));
        }        
        if (map.containsKey("minorVersion")) {
            setMinorVersion(Convert.str2int((String) map.get("minorVersion")));
        }
        if (map.containsKey("patchLevel")) {
            setPatchLevel(Convert.str2int((String) map.get("patchLevel")));
        }
        setBuildId((String) map.get("buildId"));
        if (map.containsKey("developmentBuild")) {
            setDevelopmentBuild(Convert.str2bool((String) map.get("developmentBuild")));
        }
        setBaseUrl((String) map.get("baseUrl"));
    }
    
    public Map toMap()
    {
        Map map = new HashMap();
        map.put("majorVersion", Convert.int2str(getMajorVersion()));
        map.put("minorVersion", Convert.int2str(getMinorVersion()));
        map.put("patchLevel", Convert.int2str(getPatchLevel()));
        map.put("buildId", getBuildId());
        map.put("developmentBuild", Convert.bool2str(isDevelopmentBuild()));
        map.put("baseUrl", getBaseUrl());
        return map;
    }

    public int getMajorVersion()
    {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion)
    {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion()
    {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion)
    {
        this.minorVersion = minorVersion;
    }

    public int getPatchLevel()
    {
        return patchLevel;
    }

    public void setPatchLevel(int patchLevel)
    {
        this.patchLevel = patchLevel;
    }

    public String getBuildId()
    {
        return buildId;
    }

    public void setBuildId(String buildId)
    {
        this.buildId = buildId;
    }

    public boolean isDevelopmentBuild()
    {
        return developmentBuild;
    }

    public void setDevelopmentBuild(boolean developmentBuild)
    {
        this.developmentBuild = developmentBuild;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }
}
