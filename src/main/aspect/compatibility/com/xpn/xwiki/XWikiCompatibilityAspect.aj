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
package com.xpn.xwiki;

import com.xpn.xwiki.XWiki;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.XWiki} class.
 *
 * @version $Id: $
 */
public privileged aspect XWikiCompatibilityAspect
{
    private static Map XWiki.threadMap = new HashMap();

    /**
     * Transform a text in a URL compatible text
     *
     * @param content text to transform
     * @return encoded result
     * @deprecated replaced by Util#encodeURI since 1.3M2
     */
    @Deprecated
    public String XWiki.getURLEncoded(String content)
    {
        try {
            return URLEncoder.encode(content, this.getEncoding());            
        } catch (UnsupportedEncodingException e) {
            return content;
        }
    }
    
    /**
     * @return true for multi-wiki/false for mono-wiki
     * @deprecated replaced by {@link XWiki#isVirtualMode()} since 1.4M1.
     */
    @Deprecated
    public boolean XWiki.isVirtual()
    {
        return this.isVirtualMode();
    }

    /**
     * @deprecated Removed since it isn't used; since 1.5M1.
     */
    @Deprecated
    public static Map XWiki.getThreadMap()
    {
        return XWiki.threadMap;
    }

    /**
     * @deprecated Removed since it isn't used; since 1.5M1.
     */
    @Deprecated
    public static void XWiki.setThreadMap(Map threadMap)
    {
        XWiki.threadMap = threadMap;
    }
}
