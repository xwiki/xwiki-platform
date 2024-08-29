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

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * See {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI} for documentation.
 * 
 * @version $Id$
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public class ZipExplorerPlugin extends XWikiDefaultPlugin
{
    /**
     * Log object to log messages in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ZipExplorerPlugin.class);

    /**
     * Path separators for URL.
     * 
     * @todo Define this somewhere else as this is not specific to this plugin
     */
    private static final String URL_SEPARATOR = "/";

    /**
     * @param name the plugin name
     * @param className the plugin classname (used in logs for example)
     * @param context the XWiki Context
     *
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public ZipExplorerPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    @Override
    public String getName()
    {
        return "zipexplorer";
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ZipExplorerPluginAPI((ZipExplorerPlugin) plugin, context);
    }

    /**
     * For ZIP URLs of the format <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code> return a new
     * attachment containing the file pointed to inside the ZIP. If the original attachment does not point to a ZIP file
     * or if it doesn't specify a location inside the ZIP then do nothing and return the original attachment.
     * 
     * @param attachment the original attachment
     * @param context the XWiki context, used to get the request URL corresponding to the download request
     * @return a new attachment pointing to the file pointed to by the URL inside the ZIP or the original attachment if
     *         the requested URL doesn't specify a file inside a ZIP
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#downloadAttachment
     */
    @Override
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context)
    {
        // Verify if we should return the original attachment. We do so when:
        // * the passed attachment is null
        // * the requested URL doesn't point to a zip file
        // * or the request URL doesn't point to a file inside a zip file
        // * or if the passed attachment points to a Nested Space. This is because currently the Zip Explorer plugin
        //   doesn't support Nested Spaces (See https://jira.xwiki.org/browse/XWIKI-12448).
        if (attachment == null) {
            return null;
        }
        String url = context.getRequest().getRequestURI();
        if (attachment.getReference().getDocumentReference().getSpaceReferences().size() > 1
            || !isValidZipURL(url, context.getAction().trim()))
        {
            return attachment;
        }

        String filename = getFileLocationFromZipURL(url, context.getAction().trim());

        // Create the new attachment pointing to the file inside the ZIP
        XWikiAttachment newAttachment = new XWikiAttachment();
        newAttachment.setDoc(attachment.getDoc(), false);
        newAttachment.setAuthorReference(attachment.getAuthorReference());
        newAttachment.setDate(attachment.getDate());

        InputStream stream = null;
        try {
            stream = new BufferedInputStream(attachment.getContentInputStream(context));

            if (!isZipFile(stream)) {
                return attachment;
            }

            ZipInputStream zis = new ZipInputStream(stream);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.equals(filename)) {
                    newAttachment.setFilename(entryName);

                    if (entry.getSize() == -1) {
                        // Note: We're copying the content of the file in the ZIP in memory. This is
                        // potentially going to cause an error if the file's size is greater than the
                        // maximum size of a byte[] array in Java or if there's not enough memomry.
                        byte[] data = IOUtils.toByteArray(zis);

                        newAttachment.setContent(data);
                    } else {
                        newAttachment.setContent(zis, (int) entry.getSize());
                    }
                    break;
                }
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return newAttachment;
    }

    /**
     * @param document the document containing the ZIP file as an attachment
     * @param attachmentName the name under which the ZIP file is attached in the document
     * @param context not used
     * @return the list of file entries in the ZIP file attached under the passed attachment name inside the passed
     *         document
     * @see com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileList
     */
    public List<String> getFileList(Document document, String attachmentName, XWikiContext context)
    {
        List<String> zipList = new ArrayList<String>();
        Attachment attachment = document.getAttachment(attachmentName);

        InputStream stream = null;
        try {
            stream = new ByteArrayInputStream(attachment.getContent());

            if (isZipFile(stream)) {
                ZipInputStream zis = new ZipInputStream(stream);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    zipList.add(entry.getName());
                }
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipList;
    }

    /**
     * Finds the ZIP attachment with passed name from the passed document matching and parse the ZIP to generate a list
     * of {@link com.xpn.xwiki.objects.classes.ListItem} elements representing a tree view of all directories and files
     * in the ZIP. For example the following zip:
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
     * @param context not used
     * @return a tree view list of {@link com.xpn.xwiki.objects.classes.ListItem} elements representing the content of
     *         the ZIP file
     * @see com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileTreeList
     */
    public List<ListItem> getFileTreeList(Document document, String attachmentName, XWikiContext context)
    {
        List<String> flatList = getFileList(document, attachmentName, context);
        Map<String, ListItem> fileTree = new HashMap<String, ListItem>();
        List<ListItem> res = new ArrayList<ListItem>();
        for (String url : flatList) {
            StringBuilder buf = new StringBuilder(url.length());
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
     * @return the attachment URL of the passed attachment located in the passed document to which the passed filename
     *         has been suffixed.
     * @see com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileLink
     */
    public String getFileLink(Document document, String attachmentName, String fileName, XWikiContext context)
    {
        return document.getAttachmentURL(attachmentName) + URL_SEPARATOR + fileName;
    }

    /**
     * @param url the URL to parse and from which to extract the relative file location
     * @param action the XWiki requested action (for example "download", "edit", "view", etc).
     * @return the relative file location of a file in the ZIP file pointed to by the passed URL. The ZIP URL must be of
     *         the format <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code>. With the example above this
     *         method would return <code>SomeDirectory/SomeFile.txt</code>. Return an empty string if the zip URL passed
     *         doesn't point inside a zip file.
     */
    // TODO: There should a XWikiURL class possibly extended by a ZipXWikiURL class to handle URL manipulation. Once
    // this exists remove this code. See https://jira.xwiki.org/browse/XWIKI-437
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
        path = path.substring(pos + 1);

        // Unencode any encoding done by the browser on the URL. For example the browser will
        // encode spaces and other special characters.
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (IOException e) {
            // In case of error we log the error and continue with the undecoded URL.
            // TODO: Ideally this should rather fail fast but we have no exception handling
            // framework for scripting code. Change this when we have one.
            LOG.error("Failed to decode URL path [" + path + "]", e);
        }

        return path;
    }

    /**
     * @param filecontent the content of the file
     * @return true if the file is in zip format (.zip, .jar etc) or false otherwise
     */
    protected boolean isZipFile(InputStream filecontent)
    {
        int standardZipHeader = 0x504b0304;
        filecontent.mark(8);
        try {
            DataInputStream datastream = new DataInputStream(filecontent);
            int fileHeader = datastream.readInt();
            return (standardZipHeader == fileHeader);
        } catch (IOException e) {
            // The file doesn't have 4 bytes, so it isn't a zip file
        } finally {
            // Reset the input stream to the beginning. This may be needed for further reading the archive.
            try {
                filecontent.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
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
            // should be enumerated types. See https://jira.xwiki.org/browse/XWIKI-436
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
