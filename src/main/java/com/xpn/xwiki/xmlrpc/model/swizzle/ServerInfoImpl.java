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

/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.ServerInfo;

/**
 * @author hritcu
 *
 */
public class ServerInfoImpl implements ServerInfo
{
    
    private org.codehaus.swizzle.confluence.ServerInfo target;
    
    public ServerInfoImpl()
    {
        target = new org.codehaus.swizzle.confluence.ServerInfo();
    }

    public ServerInfoImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.ServerInfo(map);
    }
    
    public ServerInfoImpl(org.codehaus.swizzle.confluence.ServerInfo serverInfo)
    {
        target = serverInfo;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#getBaseUrl()
     */
    public String getBaseUrl()
    {
        return target.getBaseUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#getBuildId()
     */
    public String getBuildId()
    {
        return target.getBuildId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#getMajorVersion()
     */
    public int getMajorVersion()
    {
        return target.getMajorVersion();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#getMinorVersion()
     */
    public int getMinorVersion()
    {
        return target.getMinorVersion();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#getPatchLevel()
     */
    public int getPatchLevel()
    {
        return target.getPatchLevel();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#isDevelopmentBuild()
     */
    public boolean isDevelopmentBuild()
    {
        return target.isDevelopmentBuild();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#setBaseUrl(java.lang.String)
     */
    public void setBaseUrl(String baseUrl)
    {
        target.setBaseUrl(baseUrl);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#setBuildId(java.lang.String)
     */
    public void setBuildId(String buildId)
    {
        target.setBuildId(buildId);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#setDevelopmentBuild(boolean)
     */
    public void setDevelopmentBuild(boolean developmentBuild)
    {
        target.setDevelopmentBuild(developmentBuild);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#setMajorVersion(int)
     */
    public void setMajorVersion(int majorVersion)
    {
        target.setMajorVersion(majorVersion);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#setMinorVersion(int)
     */
    public void setMinorVersion(int minorVersion)
    {
        target.setMinorVersion(minorVersion);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.ServerInfo#setPatchLevel(int)
     */
    public void setPatchLevel(int patchLevel)
    {
        target.setPatchLevel(patchLevel);
    }
    
    /**
     * @see com.xpn.xwiki.xmlrpc.model.MapObject#toMap(java.lang.String)
     */
    public Map toMap()
    {
        return target.toMap();
    }
}
