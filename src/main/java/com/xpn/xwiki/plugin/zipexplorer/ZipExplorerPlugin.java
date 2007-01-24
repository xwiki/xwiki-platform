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
 * @author jeremi
 */
package com.xpn.xwiki.plugin.zipexplorer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * See {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI} for documentation.
 *
 * @version $Id: $
 */
public class ZipExplorerPlugin extends XWikiDefaultPlugin
{
    /**
     * Path separators for URL.
     * @todo Define this somewhere else as this is not specific to this plugin 
     */
    private static final String URL_SEPARATOR = "/";

    /**
     * {@inheritDoc}
     *
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public ZipExplorerPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return "zipexplorer";
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ZipExplorerPluginAPI((ZipExplorerPlugin) plugin, context);
    }

    /**
     * For ZIP URLs of the format <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code>
     * return a new attachment containing the file pointed to inside the ZIP. If the original
     * attachment does not point to a ZIP file or if it doesn't specify a location inside the ZIP
     * then do nothing and return the original attachment.
     *
     * @param attachment the original attachment
     * @param context the XWiki context, used to get the request URL corresponding to the download
     *        request
     * @return a new attachment pointing to the file pointed to by the URL inside the ZIP or the
     *         original attachment if the requested URL doesn't specify a file inside a ZIP
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#downloadAttachment
     */
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context)
    {
        String url = context.getRequest().getRequestURI();

        // Verify if we should return the original attachment. This will happend if the requested
        // download URL doesn't point to a zip or if the URL doesn't point to a file inside the ZIP.
        if (!isZipFile(attachment.getFilename())
            || !isValidZipURL(url, context.getAction().trim()))
        {
            return attachment;
        }

        String filename = getFileLocationFromZipURL(url, context.getAction().trim());

        // Create the new attachment pointing to the file inside the ZIP
        XWikiAttachment newAttachment = new XWikiAttachment();
        newAttachment.setDoc(attachment.getDoc());
        newAttachment.setAuthor(attachment.getAuthor());
        newAttachment.setDate(attachment.getDate());

        try {
            byte[] stream = attachment.getContent(context);
            ByteArrayInputStream bais = new ByteArrayInputStream(stream);
            ZipInputStream zis = new ZipInputStream(bais);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.equals(filename)) {
                    newAttachment.setFilename(entryName);

                    // Note: We're copying the content of the file in the ZIP in memory. This is
                    // potentially going to cause an error if the file's size is greater than the
                    // maximum size of a byte[] array in Java or if there's not enough memomry.
                    byte[] data = IOUtils.toByteArray(zis);

                    newAttachment.setFilesize(data.length);
                    newAttachment.setContent(data);
                    break;
                }
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newAttachment;
    }

    /**
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @param context not used
     * @return the list of file entries in the ZIP file attached under the passed attachment name
     *         inside the passed document
     * @see com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileList
     */
    public List getFileList(Document document, String attachmentName, XWikiContext context)
    {
        List zipList = null;
        if (isZipFile(attachmentName)) {
            Attachment attachment = document.getAttachment(attachmentName);
            zipList = new ArrayList();
            try {
                byte[] stream = attachment.getContent();
                ByteArrayInputStream bais = new ByteArrayInputStream(stream);
                ZipInputStream zis = new ZipInputStream(bais);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    zipList.add(entry.getName());
                }
            } catch (XWikiException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return zipList;
    }

    /**
     * Finds the ZIP attachement with passed name from the passed document matching and parse the
     * ZIP to generate a list of {@link com.xpn.xwiki.objects.classes.ListItem} elements
     * representing a tree view of all directories and files in the ZIP. For example the following
     * zip:
     * <code><pre>
     * zipfile.zip:
     *   Directory/File.txt
     *   File2.txt
     * </pre></code>
     * generates the following ListItem list:
     * <code><pre>
     *   { id = "Directory/", value = "Directory", parent = ""}
     *   { id = "Directory/File.txt", value = "File.txt", parent = "Directory/"}
     *   { id = "File2.txt", value = "File2.txt", parent = ""}
     * </pre></code>
     *
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @param context not used
     * @return a tree view list of {@link com.xpn.xwiki.objects.classes.ListItem} elements
     *         representing the content of the ZIP file
     * @see com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileTreeList
     */
    public List getFileTreeList(Document document, String attachmentName, XWikiContext context)
    {
        List flatList = getFileList(document, attachmentName, context);
        Map fileTree = new HashMap();
        Iterator it = flatList.iterator();
        List res = new ArrayList();
        while (it.hasNext()) {
            String url = (String) it.next();
            StringBuffer buf = new StringBuffer(url.length());
            String parentBuf = "";
            String[] aUrl = url.split(URL_SEPARATOR);
            for (int i = 0; i < aUrl.length; i++) {
                if (i == aUrl.length - 1 && !url.endsWith(URL_SEPARATOR)) {
                    buf.append(aUrl[i]);
                } else {
                    buf.append(aUrl[i] + URL_SEPARATOR);
                }
                ListItem item = new ListItem(buf.toString(), aUrl[i], parentBuf);
                if (!fileTree.containsKey(buf.toString())) {
                    res.add(item);
                }
                fileTree.put(buf.toString(), item);
                parentBuf = buf.toString();
            }
        }
        return res;
    }

    /**
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @param fileName the filename to concatenate at the end of the attachment URL
     * @param context not used
     * @return the attachment URL of the passed attachement located in the passed document to which
     *         the passed filename has been suffixed.
     * @see com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileLink
     */
    public String getFileLink(Document document, String attachmentName, String fileName,
        XWikiContext context)
    {
        return document.getAttachmentURL(attachmentName) + URL_SEPARATOR + fileName;
    }

    /**
     * @param url the URL to parse and from which to extract the relative file location
     * @param action the XWiki requested action (for example "download", "edit", "view", etc).
     * @return the relative file location of a file in the ZIP file pointed to by the passed URL.
     *         The ZIP URL must be of the format
     *         <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code>.
     *         With the example above this method would return
     *         <code>SomeDirectory/SomeFile.txt</code>. Return an empty string if the zip URL passed
     * @todo There should a XWikiURL class possibly extended by a ZipXWikiURL class to handle URL
     *       manipulation. Once this exists remove this code.
     *       See http://jira.xwiki.org/jira/browse/XWIKI-437
     */
    protected String getFileLocationFromZipURL(String url, String action)
    {
        String path = url.substring(url.indexOf(URL_SEPARATOR + action));
        int pos = 0;
        for (int i = 0; pos > -1 && i < 4; i++) {
            pos = path.indexOf(URL_SEPARATOR, pos + 1);
        }
        if (pos == -1) {
            return "";
        }
        return path.substring(pos + 1);
    }

    /**
     * @param filename the ZIP filename
     * @return true if the file has a zip extension or false otherwise
     */
    protected boolean isZipFile(String filename)
    {
        return filename.endsWith(".zip");
    }

    /**
     * @param url the ZIP URL to check
     * @param action the XWiki requested action (for example "download", "edit", "view", etc).
     * @return true if the ZIP URL points to a file inside the ZIP or false otherwise
     */
    protected boolean isValidZipURL(String url, String action)
    {
        boolean isValidZipURL = false;
        try {
            // TODO: There shouldn't be the need to do a trim() on an Action. Actually actions
            // should be enumerated types. See http://jira.xwiki.org/jira/browse/XWIKI-436
            String filenameInZip = getFileLocationFromZipURL(url, action);

            // TODO: Ideally we should also check to see if the URL points to a file and not to
            // a directory.
            if (filenameInZip.length() > 0) {
                isValidZipURL = true;
            }
        } catch (Exception e) {
            // TODO: This exception block should be removed and possible errors should be
            // handled in getFileLocationFromZipURL.
        }
        return isValidZipURL;
    }
}
