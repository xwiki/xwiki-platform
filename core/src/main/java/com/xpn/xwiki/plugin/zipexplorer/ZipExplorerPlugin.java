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
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;

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
     * @return the relative file name of a file in the ZIP file from the path representing a URL.
     *         The URLs that the ZIP plugin manipulates have a format of
     *         <code>http://[...]/zipfile.zip/SomeDirectory/SomeFile.txt</code>. With the example
     *         above this method would return <code>SomeDirectory/SomeFile.txt</code>.
     */
    public String getFileName(String path, String action) {
        path = path.substring(path.indexOf("/" + action));
        int pos = 0;
        for (int i = 0; i < 4; i++) {
            pos = path.indexOf("/", pos + 1);
        }
        if (pos == -1)
            return "";
        return path.substring(pos + 1);
    }

    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context) {
        String url = context.getRequest().getRequestURI();
        String filename;

        if (!attachment.getFilename().endsWith(".zip"))
            return attachment;
        try {
            filename = getFileName(url, context.getAction().trim());
        }
        catch(Exception e){
            filename = "";
        }

        if (filename.length() == 0)
            return attachment;
        XWikiAttachment newAttachment;


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
        } catch (IOException e) {
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
        int count;
        while ((count = is.read(data, 0, 4096)) != -1) {
            baos.write(data, 0, count);
        }
        return baos.toByteArray();
    }


    public List getFileList(Document doc, String attachmentName, XWikiContext context) {
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

     public Vector getFileTreeList(Document doc, String attachmentName, XWikiContext context) {
        List flatList = getFileList(doc, attachmentName, context);
         Map fileTree = new HashMap();
         Iterator it = flatList.iterator();
         Vector res = new Vector();
         while(it.hasNext()){
             String url = (String) it.next();
             StringBuffer buf = new StringBuffer(url.length());
             String parentBuf = "";
             String[] aUrl = url.split("/");
             for (int i = 0; i < aUrl.length; i++){
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

    String getFileLink(Document doc, String attachmentName, String fileName, XWikiContext context) {
        String link = doc.getAttachmentURL(attachmentName);
        return link + "/" + fileName;
    }
}
