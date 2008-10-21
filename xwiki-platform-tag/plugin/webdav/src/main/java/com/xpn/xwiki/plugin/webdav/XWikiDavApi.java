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

package com.xpn.xwiki.plugin.webdav;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * The Api class for the webdav plugin.
 * 
 * @version $Id$
 */
public class XWikiDavApi extends PluginApi<XWikiDavPlugin>
{
    /**
     * Default plugin API constructor.
     * 
     * @param plugin The protected plugin instance.
     * @param context The XWiki context.
     */
    public XWikiDavApi(XWikiDavPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Compute the URL that can be used to access an attachment over WebDAV.
     * 
     * @param doc The document which is having the attachment.
     * @param attachment The attachment itself.
     * @return The <code>String</code> representation of the WebDAV url corresponding to the attachment.
     */
    public String getDavURL(Document doc, Attachment attachment)
    {
        return getProtectedPlugin().getDavURL(doc, attachment);
    }
}
