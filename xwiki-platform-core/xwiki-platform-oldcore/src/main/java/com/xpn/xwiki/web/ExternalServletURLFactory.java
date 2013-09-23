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

import java.net.URL;

import com.xpn.xwiki.XWikiContext;

/**
 * URL Factory which always prints the absolute (external) form of URLs.
 * 
 * @version $Id$
 * @since 2.6 RC2
 */
public class ExternalServletURLFactory extends XWikiServletURLFactory
{
    /**
     * Old-school constructor using the XWikiContext to initialize the factory.
     * 
     * @param context the current request context
     */
    public ExternalServletURLFactory(XWikiContext context)
    {
        super(context);
    }

    @Override
    public String getURL(URL url, XWikiContext context)
    {
        // The URL is already in its absolute form, just return it.
        return url.toString();
    }
}
