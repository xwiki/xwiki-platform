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
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import com.xpn.xwiki.cache.api.internal.XWikiCacheServiceStub;
import com.xpn.xwiki.cache.api.internal.XWikiCacheStub;
import com.xpn.xwiki.web.XWikiMessageTool;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.XWiki} class.
 * 
 * @version $Id$
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

    /**
     * @return the cache service.
     * @deprecated replaced by {@link XWiki#getCacheFactory(XWikiContext)} or
     *             {@link XWiki#getLocalCacheFactory(XWikiContext)} since 1.5M2.
     */
    @Deprecated
    public XWikiCacheService XWiki.getCacheService()
    {
        return new XWikiCacheServiceStub(getCacheFactory(), getLocalCacheFactory());
    }

    /**
     * @deprecated replaced by {@link XWiki#getVirtualWikiCache(XWikiContext)} since 1.5M2.
     */
    @Deprecated
    public XWikiCache XWiki.getVirtualWikiMap()
    {
        return new XWikiCacheStub(this.virtualWikiMap);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpaceCopyright(XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebCopyright(XWikiContext context)
    {
        return this.getSpaceCopyright(context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreference(String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreference(String preference, XWikiContext context)
    {
        return this.getSpacePreference(preference, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreference(String, String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreference(String preference, String defaultValue, XWikiContext context)
    {
        return this.getSpacePreference(preference, defaultValue, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreference(String, String, String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreference(String preference, String space, String defaultValue, XWikiContext context)
    {
        return this.getSpacePreference(preference, space, defaultValue, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsLong(String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public long XWiki.getWebPreferenceAsLong(String preference, XWikiContext context)
    {
        return this.getSpacePreferenceAsLong(preference, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsLong(String, long, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public long XWiki.getWebPreferenceAsLong(String preference, long defaultValue, XWikiContext context)
    {
        return this.getSpacePreferenceAsLong(preference, defaultValue, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsInt(String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public int XWiki.getWebPreferenceAsInt(String preference, XWikiContext context)
    {
        return this.getSpacePreferenceAsInt(preference, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsInt(String, int, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public int XWiki.getWebPreferenceAsInt(String preference, int defaultValue, XWikiContext context)
    {
        return this.getSpacePreferenceAsInt(preference, defaultValue, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#copySpaceBetweenWikis(String, String, String, String, XWikiContext)} since
     *             2.3M1
     */
    @Deprecated
    public int XWiki.copyWikiWeb(String space, String sourceWiki, String targetWiki, String language, XWikiContext context)
        throws XWikiException
    {
        return this.copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, context);
    }

    /**
     * @deprecated replaced by
     *             {@link XWiki#copySpaceBetweenWikis(String, String, String, String, boolean, XWikiContext)} since
     *             2.3M1
     */
    @Deprecated
    public int XWiki.copyWikiWeb(String space, String sourceWiki, String targetWiki, String language, boolean clean,
        XWikiContext context) throws XWikiException
    {
        return this.copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, clean, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getDefaultSpace(XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getDefaultWeb(XWikiContext context)
    {
        return this.getDefaultSpace(context);
    }

    /**
     * @deprecated replaced by {@link XWiki#skipDefaultSpaceInURLs(XWikiContext)} since 2.3M1
     */
    @Deprecated
    public boolean XWiki.useDefaultWeb(XWikiContext context)
    {
        return this.skipDefaultSpaceInURLs(context);
    }

    /**
     * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. You can access message tool using
     *             {@link XWikiContext#getMessageTool()}.
     */
    @Deprecated
    public String XWiki.getMessage(String item, XWikiContext context)
    {
        XWikiMessageTool msg = context.getMessageTool();
        if (msg == null) {
            return item;
        } else {
            return msg.get(item);
        }
    }

    /**
     * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. You can access message tool using
     *             {@link XWikiContext#getMessageTool()}.
     */
    @Deprecated
    public String XWiki.parseMessage(String id, XWikiContext context)
    {
        XWikiMessageTool msg = context.getMessageTool();

        return parseContent(msg.get(id), context);
    }

    /**
     * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. You can access message tool using
     *             {@link XWikiContext#getMessageTool()}.
     */
    @Deprecated
    public String XWiki.parseMessage(XWikiContext context)
    {
        String message = (String) context.get("message");
        if (message == null) {
            return null;
        }

        return parseMessage(message, context);
    }
}
