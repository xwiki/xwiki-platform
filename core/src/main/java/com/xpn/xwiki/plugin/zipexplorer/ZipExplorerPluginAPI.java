/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author ravenees
 */
package com.xpn.xwiki.plugin.zipexplorer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;

import java.util.List;


public class ZipExplorerPluginAPI extends Api {

    ZipExplorerPlugin plugin;

    public ZipExplorerPluginAPI(ZipExplorerPlugin plugin, XWikiContext context) {
        super(context);
        this.plugin = plugin;
    }

    public XWikiAttachment downloadAttachment(XWikiAttachment attachment) {
        return this.plugin.downloadAttachment(attachment, context);
    }

    public List getFileList(Document doc, String attachmentName) {
        return this.plugin.getFileList(doc, attachmentName, context);
    }

    public String getFileLink(Document doc, String attachmentName, String fileName) {
        return this.plugin.getFileLink(doc, attachmentName, fileName, context);
    }
}
