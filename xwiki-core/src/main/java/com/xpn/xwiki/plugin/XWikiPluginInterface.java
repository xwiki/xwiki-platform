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

package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;

public interface XWikiPluginInterface {
    String getClassName();
    String getName();

    void setClassName(String name);
    void setName(String name);
    void init(XWikiContext context) throws XWikiException;
    void virtualInit(XWikiContext context);

    /*
    Called to flush cache
    */
    void flushCache(XWikiContext context);

    /**
     * @deprecated use flushCache(XWikiContext context) instead
     * @see #flushCache(XWikiContext)
     */
    @Deprecated
    void flushCache();
    void beginRendering(XWikiContext context);
    void endRendering(XWikiContext context);
    /*
    Called at the begin of each request
    */
    void beginParsing(XWikiContext context);
    /*
    Called at the end of each request
    */
    String endParsing(String content, XWikiContext context);

    String commonTagsHandler(String line, XWikiContext context);
    String startRenderingHandler(String line, XWikiContext context);
    String outsidePREHandler(String line, XWikiContext context);
    String insidePREHandler(String line, XWikiContext context);
    String endRenderingHandler(String line, XWikiContext context);
    Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context);

    /**
     * Plugin extension point allowing the plugin to perform modifications to an attachment when the
     * user clicks on an attachment in a document. The plugin is passed the original attachment and it has
     * to return the new modified attachment.
     *
     * @param attachment the original attachment
     * @param context the xwiki context object
     * @return the modified attachment
     */
    XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context);
}
