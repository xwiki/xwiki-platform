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
package com.xpn.xwiki.plugin.zipexplorer;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * ZIP-related APIs to list content of a ZIP attachments and to intercept XWiki download requests so that it's possible
 * to display contents found inside ZIP files. This plugin accepts specially formatted URLs pointing to files inside ZIP
 * files by using the following syntax:
 * <code>http://[...]/download/Document/zipfile.zip/SomeDirectory/SomeFile.txt</code>. In this example, the URL points
 * to the <code>SomeFile.txt</code> file located in a directory named <code>SomeDirectory</code> inside a ZIP file named
 * <code>zipfile.zip</code> and attached to the document named <code>Document</code>.
 * 
 * @version $Id$
 * @see com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPlugin
 */
public class ZipExplorerPluginAPI extends PluginApi<ZipExplorerPlugin>
{
    /**
     * @param plugin the ZIP Explorer plugin that this class is hiding.
     * @param context the XWiki context instance containing the last user request
     */
    public ZipExplorerPluginAPI(ZipExplorerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * For ZIP URLs of the format <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code> return a new
     * attachment containing the file pointed to inside the ZIP. If the original attachment does not point to a ZIP file
     * or if it doesn't specify a location inside the ZIP then do nothing and return the original attachment.
     * 
     * @param attachment the original attachment
     * @return a new attachment pointing to the file pointed to by the URL inside the ZIP or the original attachment if
     *         the requested URL doesn't specify a file inside a ZIP
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#downloadAttachment
     */
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment)
    {
        return getProtectedPlugin().downloadAttachment(attachment, getXWikiContext());
    }

    /**
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @return the list of file entries in the ZIP file attached under the passed attachment name inside the passed
     *         document
     */
    public List<String> getFileList(Document document, String attachmentName)
    {
        return getProtectedPlugin().getFileList(document, attachmentName, getXWikiContext());
    }

    /**
     * Finds the ZIP attachment with passed name from the passed document matching and parse the ZIP to generate a list
     * of {@link com.xpn.xwiki.objects.classes.ListItem} elements representing a tree view of all directories and files
     * in the ZIP. For example the following zip:
     * 
     * <pre>
     * zipfile.zip:
     *   Directory/File.txt
     *   File2.txt
     * </pre>
     * 
     * generates the following ListItem list:
     * 
     * <pre>
     * 
     *   { id = &quot;Directory/&quot;, value = &quot;Directory&quot;, parent = &quot;&quot;}
     *   { id = &quot;Directory/File.txt&quot;, value = &quot;File.txt&quot;, parent = &quot;Directory/&quot;}
     *   { id = &quot;File2.txt&quot;, value = &quot;File2.txt&quot;, parent = &quot;&quot;}
     * 
     * </pre>
     * 
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @return a tree view list of {@link com.xpn.xwiki.objects.classes.ListItem} elements representing the content of
     *         the ZIP file
     */
    public List<ListItem> getFileTreeList(Document document, String attachmentName)
    {
        return getProtectedPlugin().getFileTreeList(document, attachmentName, getXWikiContext());
    }

    /**
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @param fileName the filename to concatenate at the end of the attachment URL
     * @return the attachment URL of the passed attachment located in the passed document to which the passed filename
     *         has been suffixed.
     */
    public String getFileLink(Document document, String attachmentName, String fileName)
    {
        return getProtectedPlugin().getFileLink(document, attachmentName, fileName, getXWikiContext());
    }
}
