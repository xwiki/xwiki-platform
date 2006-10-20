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

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.Properties;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


public class ZipExplorerPlugin extends XWikiDefaultPlugin {

    private XWikiCache zipCache;
    private int capacity = 50;
    private static final Log log = LogFactory.getLog(ZipExplorerPlugin.class);

    /**
     * Constructor
     */
    public ZipExplorerPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
        init(context);
    }

     /**
     *
     * */
     public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new ZipExplorerPluginAPI((ZipExplorerPlugin) plugin, context);
    }

    /**
     *
     * */
    public void initCache(XWikiContext context) {
        String capacityParam = "";
        try {
            capacityParam = context.getWiki().Param("xwiki.plugin.zip.cache.capacity");
            if ((capacityParam != null) && (!capacityParam.equals(""))) {
                capacity = Integer.parseInt(capacityParam);
            }
        } catch (NumberFormatException e) {
            if (log.isErrorEnabled())
                log.error("Error in ZipPlugin reading capacity: " + capacityParam, e);
        }

        Properties props = new Properties();
        props.put("cache.memory", "true");
        props.put("cache.unlimited.disk", "true");
        props.put("cache.persistence.overflow.only", "false");
        props.put("cache.blocking", "false");
        props.put("cache.persistence.class", "com.opensymphony.oscache.plugins.diskpersistence.DiskPersistenceListener");
        props.put("cache.path", "temp/imageCache");

        try {
            zipCache = context.getWiki().getCacheService().newLocalCache(props, capacity);
        } catch (XWikiException e) {
        }
    }


    /**
     * @return the file name from the URI
     */
    public String getFileName(String path, String action) {
        path = path.substring(path.indexOf("/" + action));
        int pos = 0;
        for (int i = 0; i < 4; i++) {
            pos = path.indexOf("/", pos + 1);
        }
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }

    /**
     * @return the extries in the zip file as xwiki attachment
     */
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context) {

        String URL = context.getRequest().getRequestURI();
        String filename = getFileName(URL, context.getAction().trim());
        XWikiAttachment newAttachment = null;

        if (zipCache == null)
            initCache(context);

        newAttachment = new XWikiAttachment();
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
                entryName = entryName.substring(entryName.lastIndexOf("/") + 1, entryName.length()).trim();

                if (entryName.equals(filename)) {
                    newAttachment.setFilename(entryName);
                    byte[] data = readFromInputStream(zis);
                    newAttachment.setFilesize(data.length);
                    newAttachment.setContent(data);
                    break;
                }
            }

        } catch (XWikiException e) {
            e.printStackTrace();
        }

        catch (IOException e) {
            e.printStackTrace();
        }
        return newAttachment;
    }


    /**
     * @return the content of an entry in the zip file
     */
    private byte[] readFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        StringBuffer buffer = new StringBuffer();
        int count;
        while ((count = is.read(data, 0, 4096)) != -1) {
            baos.write(data);
        }
        return baos.toByteArray();
    }


    List getFileList(Document doc, String attachmentName, XWikiContext context) {

        List zipList = null;

        if (attachmentName.endsWith(".zip")) {

            Attachment attachment = null;
            List attachList = doc.getAttachmentList();
            Iterator itr = attachList.iterator();

            while (itr.hasNext()) {
                attachment = (Attachment)itr.next();
                if (attachment.getFilename().equals(attachmentName)) {
                    break;
                }
            }

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

    String getFileLink(Document doc, String attachmentName, String fileName, XWikiContext context) {

        String link = null;

        if (attachmentName.endsWith(".zip")) {
            Attachment attachment = null;
            List attachList = doc.getAttachmentList();
            Iterator itr = attachList.iterator();

            while (itr.hasNext()) {
                attachment = (Attachment) itr.next();
                if (attachment.getFilename().equals(attachmentName)) {
                    break;
                }
            }

            byte[] stream;

            try {

                stream = attachment.getContent();
                ByteArrayInputStream bais = new ByteArrayInputStream(stream);
                ZipInputStream zis = new ZipInputStream(bais);
                ZipEntry entry;
                String fileCaption = null;

                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    fileCaption = entryName.substring(entryName.lastIndexOf("/") + 1, entryName.length());
                    if (fileCaption.equalsIgnoreCase(fileName)) {
                        break;
                    }
                }
                link = doc.getURL("download")+"/"+fileCaption;
                //link = link.substring(0,link.indexOf("download/"))+fileCaption;
            } catch (XWikiException e) {
                //e.printStackTrace();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
        return link;
    }
}
