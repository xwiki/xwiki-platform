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
 *
 */
package com.xpn.xwiki.pdf.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiServletURLFactory;

/**
 * Special URL Factory used during exports, which stores referenced attachments on the filesystem so that they can be
 * included in the export result.
 * 
 * @version $Id$
 */
public class PdfURLFactory extends XWikiServletURLFactory
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(PdfURLFactory.class);

    private static final String SEPARATOR = "/";

    /**
     * {@inheritDoc}
     */
    @Override
    public URL createAttachmentURL(String filename, String space, String name, String action, String querystring,
        String wiki, XWikiContext context)
    {
        try {
            return getURL(wiki, space, name, filename, null, context);
        } catch (Exception ex) {
            LOG.warn("Failed to save image for PDF export", ex);
            return super.createAttachmentURL(filename, space, name, action, null, wiki, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL createAttachmentRevisionURL(String filename, String space, String name, String revision, String wiki,
        XWikiContext context)
    {
        try {
            return getURL(wiki, space, name, filename, revision, context);
        } catch (Exception ex) {
            LOG.warn("Failed to save image for PDF export: " + ex.getMessage());
            return super.createAttachmentRevisionURL(filename, space, name, revision, wiki, context);
        }
    }

    /**
     * Store the requested attachment on the filesystem and return a {@code file://} URL where FOP can access that file.
     * 
     * @param wiki the name of the owner document's wiki
     * @param space the name of the owner document's space
     * @param name the name of the owner document
     * @param filename the name of the attachment
     * @param revision an optional attachment version
     * @param context the current request context
     * @return a {@code file://} URL where the attachment has been stored
     * @throws Exception if the attachment can't be retrieved from the database and stored on the filesystem
     */
    public URL getURL(String wiki, String space, String name, String filename, String revision, XWikiContext context)
        throws Exception
    {
        @SuppressWarnings("unchecked")
        Map<String, File> usedFiles = (Map<String, File>) context.get("pdfexport-file-mapping");
        String key = getAttachmentKey(space, name, filename, revision);
        if (!usedFiles.containsKey(key)) {
            File tempdir = (File) context.get("pdfexportdir");
            File file = File.createTempFile("pdf", "." + FilenameUtils.getExtension(filename), tempdir);
            XWikiDocument doc = context.getWiki().getDocument(
                new DocumentReference(StringUtils.defaultString(wiki, context.getDatabase()), space, name), context);
            XWikiAttachment attachment = doc.getAttachment(filename);
            if (StringUtils.isNotEmpty(revision)) {
                attachment = attachment.getAttachmentRevision(revision, context);
            }
            FileOutputStream fos = new FileOutputStream(file);
            IOUtils.copy(attachment.getContentInputStream(context), fos);
            fos.close();
            usedFiles.put(key, file);
        }
        return usedFiles.get(key).toURI().toURL();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getURL(URL url, XWikiContext context)
    {
        if (url == null) {
            return "";
        }
        return Util.escapeURL(url.toString());
    }

    /**
     * Computes a safe identifier for an attachment, guaranteed to be collision-free.
     * 
     * @param space the name of the owner document's space
     * @param name the name of the owner document
     * @param filename the name of the attachment
     * @param revision an optional attachment version
     * @return an identifier for this attachment
     */
    private String getAttachmentKey(String space, String name, String filename, String revision)
    {
        try {
            return URLEncoder.encode(space, "UTF-8") + SEPARATOR
                + URLEncoder.encode(name, "UTF-8") + SEPARATOR
                + URLEncoder.encode(filename, "UTF-8") + SEPARATOR
                + URLEncoder.encode(StringUtils.defaultString(revision), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // This should never happen, UTF-8 is always available
            return space + SEPARATOR + name + SEPARATOR + filename + SEPARATOR + StringUtils.defaultString(revision);
        }
    }
}
