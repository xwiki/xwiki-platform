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

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;

/**
 * See {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI} for documentation.
 *
 * @version $Id: $
 */
public class ZipExplorerPlugin extends XWikiDefaultPlugin {

    /**
     * {@inheritDoc}
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String, String, com.xpn.xwiki.XWikiContext)
     */
    public ZipExplorerPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName() {
        return "zipexplorer";
    }

    /**
     * {@inheritDoc}
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface, com.xpn.xwiki.XWikiContext) 
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new ZipExplorerPluginAPI((ZipExplorerPlugin) plugin, context);
    }

    /**
     * @return the relative file location of a file in the ZIP file pointed to by the passed URL. The ZIP URL
     *         must be of the format <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code>. With the example
     *         above this method would return <code>SomeDirectory/SomeFile.txt</code>. Return an empty string if the
     *         zip URL passed
     * @param url the URL to parse and from which to extract the relative file location
     * @param action the XWiki requested action (for example "download", "edit", "view", etc).
     */
    protected String getFileLocationFromZipURL(String url, String action) {
        String path = url.substring(url.indexOf("/" + action));
        int pos = 0;
        for (int i = 0; i < 4; i++) {
            pos = path.indexOf("/", pos + 1);
        }
        if (pos == -1)
            return "";
        return path.substring(pos + 1);
    }

    /**
     * See {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#downloadAttachment(com.xpn.xwiki.doc.XWikiAttachment)}
     * for documentation. In addition the context parameter represents the XWiki context object containing the
     * requested URL.
     */
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context) {
        String url = context.getRequest().getRequestURI();

        // If the requested download URL doesn't point to a zip, return the original attachment
        if (!attachment.getFilename().endsWith(".zip"))
            return attachment;

        // If the URL doesn't point to a file inside the ZIP, return the original attachement. Otherwise, compute the
        // relative file location inside the ZIP file so that we can extract it content below.
        String filename;
        try {
            filename = getFileLocationFromZipURL(url, context.getAction().trim());
        }
        catch(Exception e){
            filename = "";
        }
        if (filename.length() == 0)
            return attachment;

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

                    // Note: We're copying the content of the file in the ZIP in memory. This is potentially going to
                    // cause an error if the file's size is greater than the maximum size of a byte[] array in Java
                    // or if there's not enough memomry.
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
     * See {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileList(com.xpn.xwiki.api.Document, String)}
     * for documentation. In addition the context parameter is not used for now.
     */
    public List getFileList(Document document, String attachmentName, XWikiContext context) {
        List zipList = null;

        if (attachmentName.endsWith(".zip")) {

            Attachment attachment = document.getAttachment(attachmentName);

            zipList = new ArrayList();
            byte[] stream;
            try {
                stream = attachment.getContent();
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
     * See {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileTreeList(com.xpn.xwiki.api.Document, String)}
     * for documentation. In addition the context parameter is not used for now.
     */
    public Vector getFileTreeList(Document document, String attachmentName, XWikiContext context) {
        List flatList = getFileList(document, attachmentName, context);
        Map fileTree = new HashMap();
        Iterator it = flatList.iterator();
        Vector res = new Vector();
        while(it.hasNext()) {
            String url = (String) it.next();
            StringBuffer buf = new StringBuffer(url.length());
            String parentBuf = "";
            String[] aUrl = url.split("/");
            for (int i = 0; i < aUrl.length; i++) {
                if (i == aUrl.length - 1 && !url.endsWith("/"))
                    buf.append(aUrl[i]);
                else
                    buf.append(aUrl[i] + "/");
                ListItem item = new ListItem(buf.toString(), aUrl[i], parentBuf);
                if (!fileTree.containsKey(buf.toString()))
                    res.add(item);
                fileTree.put(buf.toString(), item);
                parentBuf = buf.toString();
            }
        }
        return res;
    }

    /**
     * See {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPluginAPI#getFileLink(com.xpn.xwiki.api.Document, String, String)}
     * for documentation. In addition the context parameter is not used for now.
     */
    String getFileLink(Document document, String attachmentName, String fileName, XWikiContext context) {
        return document.getAttachmentURL(attachmentName) + "/" + fileName;
    }
}
