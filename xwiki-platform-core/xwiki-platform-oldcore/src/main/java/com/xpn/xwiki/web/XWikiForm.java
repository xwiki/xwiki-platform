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
package com.xpn.xwiki.web;

import javax.servlet.http.HttpServletRequest;

/**
 * @version $Id$
 */
public abstract class XWikiForm
{
    private XWikiRequest request;

    /**
     * Reset all properties to their default values.
     *
     * @param request The servlet request we are processing
     */
    public void setRequest(HttpServletRequest request)
    {
        this.request = new XWikiServletRequest(request);
    }

    /**
     * Reset all properties to their default values.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     * @since 7.1.4-struts2
     */
    public void reset(HttpServletRequest request)
    {
        setRequest(request);
        readRequest();
    }

    /**
     * Reset all properties to their default values.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     * @since 7.1.4-struts2
     */
    public void reset(XWikiRequest request)
    {
        this.request = request;
        readRequest();
    }

    public XWikiRequest getRequest()
    {
        return this.request;
    }

    public abstract void readRequest();
}
