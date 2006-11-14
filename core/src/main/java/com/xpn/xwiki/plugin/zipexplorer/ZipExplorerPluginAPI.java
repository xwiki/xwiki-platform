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
import java.util.Vector;

public class ZipExplorerPluginAPI extends Api {

    private ZipExplorerPlugin plugin;

    public ZipExplorerPluginAPI(ZipExplorerPlugin plugin, XWikiContext context) {
        super(context);
        this.plugin = plugin;
    }

    /**
     * For ZIP URLs of the format <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code> return a new
     * attachment containing the file pointed to inside the ZIP. If the original attachment does not point to a ZIP
     * file or if it doesn't specify a location inside the ZIP then do nothing and return the original attachment.
     *
     * @return a new attachment pointing to the file pointed to by the URL inside the ZIP or the original attachment
     *         if the requested URL doesn't specify a file inside a ZIP
     * @param attachment the original attachment
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#downloadAttachment(com.xpn.xwiki.doc.XWikiAttachment, com.xpn.xwiki.XWikiContext)
     */
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment) {
        return this.plugin.downloadAttachment(attachment, context);
    }

    /**
     * @return the list of file entries in the ZIP file attached under the passed attachment name inside the passed
     *         document
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     */
    public List getFileList(Document document, String attachmentName) {
        return this.plugin.getFileList(document, attachmentName, context);
    }

    /**
     * Finds the ZIP attachement with passed name from the passed document matching and parse the ZIP to generate
     * a list of {@link com.xpn.xwiki.objects.classes.ListItem} elements representing a tree view of all directories
     * and files in the ZIP. For example the following zip:
     * </p>
     * <code><pre>
     * zipfile.zip:
     *   Directory/File.txt
     *   File2.txt
     * </per></code>
     * generates the following ListItem list:
     * <code><pre>
     *   { id = "Directory/", value = "Directory", parent = ""}
     *   { id = "Directory/File.txt", value = "File.txt", parent = "Directory/"}
     *   { id = "File2.txt", value = "File2.txt", parent = ""}
     * </per></code>
     *
     * @return a tree view list of {@link com.xpn.xwiki.objects.classes.ListItem} elements representing the content of
     *         the ZIP file
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     */
    public Vector getFileTreeList(Document document, String attachmentName) {
        return this.plugin.getFileTreeList(document, attachmentName, context);
    }

    /**
     * @return the attachment URL of the passed attachement located in the passed document to which the passed filename
     *         has been suffixed.
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @param fileName the filename to concatenate at the end of the attachment URL
     */
    public String getFileLink(Document document, String attachmentName, String fileName) {
        return this.plugin.getFileLink(document, attachmentName, fileName, context);
    }
}
