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
 */
package com.xpn.xwiki.xmlrpc;

import java.util.Map;

/**
 * @version $Id: $
 */
public class SpaceSummary extends org.codehaus.swizzle.confluence.SpaceSummary
{
    public SpaceSummary()
    {
        super();
    }

    public SpaceSummary(Map data)
    {
        super(data);
    }

    /**
     * @param key of the space (usually the Space's name as it's unique)
     * @param name of the space
     * @param url to view the space online. Example: "http://server/xwiki/bin/view/Space/WebHome"
     */
    public SpaceSummary(String key, String name, String url)
    {
        this.setKey(key);
        this.setName(name);
        this.setUrl(url);
    }
}
