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

package com.xpn.xwiki.stats.impl;

/**
 * Matching tool able to find if a referer is a particular search engine.
 * 
 * @version $Id$
 */
public class SearchEngineRule extends Object
{
    /**
     * The search engine server address.
     */
    private String host;
    
    /**
     * The regular expression to match.
     */
    private String regEx;

    /**
     * @param host the search engine server address.
     * @param regEx the regular expression to match.
     */
    public SearchEngineRule(String host, String regEx)
    {
        setRegEx(regEx);
        setHost(host);
    }

    /**
     * @return the search engine server address.
     */
    public String getHost()
    {
        return host;
    }

    /**
     * @param host the search engine server address.
     */
    public void setHost(String host)
    {
        this.host = host;
    }
    
    /**
     * @return the regular expression to match.
     */
    public String getRegEx()
    {
        return regEx;
    }

    /**
     * @param regEx the regular expression to match.
     */
    public void setRegEx(String regEx)
    {
        this.regEx = regEx;
    }
}
