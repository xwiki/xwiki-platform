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
 *
 */
package com.xpn.xwiki.plugin.zipexplorer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;

import java.util.List;

/**
 * ZIP-related APIs to list content of a ZIP attachments and to intercept XWiki download requests so
 * that it's possible to display contents found inside ZIP files. This plugin accepts specially
 * formatted URLs pointing to files inside ZIP files by using the following syntax:
 *
 * <code>http://[...]/download/Document/zipfile.zip/SomeDirectory/SomeFile.txt</code>.
 *
 * In this example, the URL points to the <code>SomeFile.txt</code> file located in a directory
 * named <code>SomeDirectory</code> inside a ZIP file named <code>zipfile.zip</code> and attached to
 * the document named <code>Document</code>.
 *
 * @version $Id: $
 * @see com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPlugin
 */
public class ZipExplorerPluginAPI extends Api
{
    /**
     * The ZIP Explorer plugin hidden by this API class. This is to create a separation between
     * the API used by users in XWiki documents and the actual implementations.
     */
    private ZipExplorerPlugin plugin;

    /**
     * @param plugin the ZIP Explorer plugin that this class is hiding.
     * @param context the XWiki context instance containing the last user request
     */
    public ZipExplorerPluginAPI(ZipExplorerPlugin plugin, XWikiContext context)
    {
        super(context);
        this.plugin = plugin;
    }

    /**
     * For ZIP URLs of the format <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code>
     * return a new attachment containing the file pointed to inside the ZIP. If the original
     * attachment does not point to a ZIP file or if it doesn't specify a location inside the ZIP
     * then do nothing and return the original attachment.
     *
     * @param attachment the original attachment
     * @return a new attachment pointing to the file pointed to by the URL inside the ZIP or the
     *         original attachment if the requested URL doesn't specify a file inside a ZIP
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#downloadAttachment
     */
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment)
    {
        return this.plugin.downloadAttachment(attachment, getXWikiContext());
    }

    /**
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @return the list of file entries in the ZIP file attached under the passed attachment name
     *         inside the passed document
     */
    public List getFileList(Document document, String attachmentName)
    {
        return this.plugin.getFileList(document, attachmentName, getXWikiContext());
    }

    /**
     * Finds the ZIP attachement with passed name from the passed document matching and parse the
     * ZIP to generate a list of {@link com.xpn.xwiki.objects.classes.ListItem} elements
     * representing a tree view of all directories and files in the ZIP. For example the following
     * zip:
     * <pre><code>
     * zipfile.zip:
     *   Directory/File.txt
     *   File2.txt
     * </code></pre>
     * generates the following ListItem list:
     * <pre><code>
     *   { id = "Directory/", value = "Directory", parent = ""}
     *   { id = "Directory/File.txt", value = "File.txt", parent = "Directory/"}
     *   { id = "File2.txt", value = "File2.txt", parent = ""}
     * </code></pre>
     *
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @return a tree view list of {@link com.xpn.xwiki.objects.classes.ListItem} elements
     *         representing the content of the ZIP file
     */
    public List getFileTreeList(Document document, String attachmentName)
    {
        return this.plugin.getFileTreeList(document, attachmentName, getXWikiContext());
    }

    /**
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @param fileName the filename to concatenate at the end of the attachment URL
     * @return the attachment URL of the passed attachement located in the passed document to which
     *         the passed filename has been suffixed.
     */
    public String getFileLink(Document document, String attachmentName, String fileName)
    {
        return this.plugin.getFileLink(document, attachmentName, fileName, getXWikiContext());
    }
}
