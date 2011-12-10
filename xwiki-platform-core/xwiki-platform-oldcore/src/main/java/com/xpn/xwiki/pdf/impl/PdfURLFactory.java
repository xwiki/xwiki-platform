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
package com.xpn.xwiki.pdf.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiServletURLFactory;

/**
 * Special URL Factory used during exports, which stores referenced attachments and resources on the filesystem, in a
 * temporary folder, so that they can be included in the export result. The returned URLs point to these resources as
 * {@code file://} links, and not as {@code http://} links.
 * 
 * @version $Id$
 */
public class PdfURLFactory extends XWikiServletURLFactory
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfURLFactory.class);

    /** Segment separator used in the collision-free key generation. */
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
            LOGGER.warn("Failed to save image for PDF export", ex);
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
            LOGGER.warn("Failed to save image for PDF export: " + ex.getMessage());
            return super.createAttachmentRevisionURL(filename, space, name, revision, wiki, context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory.createSkinURL(String, String, XWikiContext)
     */
    @Override
    public URL createSkinURL(String filename, String skin, XWikiContext context)
    {
        try {
            Map<String, File> usedFiles = getFileMapping(context);
            String key = getSkinfileKey(filename, skin);
            if (!usedFiles.containsKey(key)) {
                if (!copyResource("/skins/" + skin + '/' + filename, key, usedFiles, context)) {
                    // The resource does not exist, just return a http:// URL
                    return super.createSkinURL(filename, skin, context);
                }
            }
            return usedFiles.get(key).toURI().toURL();
        } catch (Exception ex) {
            // Shouldn't happen
            return super.createSkinURL(filename, skin, context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory.createResourceURL(String, boolean, XWikiContext)
     */
    @Override
    public URL createResourceURL(String filename, boolean forceSkinAction, XWikiContext context)
    {
        try {
            Map<String, File> usedFiles = getFileMapping(context);
            String key = getResourceKey(filename);
            if (!usedFiles.containsKey(key)) {
                if (!copyResource("/resources/" + filename, key, usedFiles, context)) {
                    return super.createResourceURL(filename, forceSkinAction, context);
                }
            }
            return usedFiles.get(key).toURI().toURL();
        } catch (Exception ex) {
            // Shouldn't happen
            return super.createResourceURL(filename, forceSkinAction, context);
        }
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
    private URL getURL(String wiki, String space, String name, String filename, String revision, XWikiContext context)
        throws Exception
    {
        Map<String, File> usedFiles = getFileMapping(context);
        String key = getAttachmentKey(space, name, filename, revision);
        if (!usedFiles.containsKey(key)) {
            File file = getTemporaryFile(key, context);
            LOGGER.debug("Temporary PDF export file [{}]", file.toString());
            XWikiDocument doc =
                context.getWiki()
                    .getDocument(
                        new DocumentReference(StringUtils.defaultString(wiki, context.getDatabase()), space, name),
                        context);
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
     * Copy a resource from the filesystem into a temporary file and map this resulting file to the requested resource
     * location.
     * 
     * @param resourceName the name of the file to copy, possibly including a path to it, for example
     *            {@code icons/silk/add.gif}
     * @param key the collision-free identifier of the resource
     * @param usedFiles the mapping of resource keys to temporary files where to put the resulting temporary file
     * @param context the current request context
     * @return {@code true} if copying the resource succeeded and the new temporary file was mapped to the resource key,
     *         {@code false} otherwise
     */
    private boolean copyResource(String resourceName, String key, Map<String, File> usedFiles, XWikiContext context)
    {
        try {
            InputStream data = context.getWiki().getResourceAsStream(resourceName);
            if (data != null) {
                // Copy the resource to a temporary file
                File file = getTemporaryFile(key, context);
                FileOutputStream fos = new FileOutputStream(file);
                IOUtils.copy(data, fos);
                fos.close();
                usedFiles.put(key, file);
                return true;
            }
        } catch (Exception ex) {
            // Can't access the resource, let's hope FOP can handle the http:// URL
        }
        return false;
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
            return "attachment" + SEPARATOR + URLEncoder.encode(space, XWiki.DEFAULT_ENCODING) + SEPARATOR
                + URLEncoder.encode(name, XWiki.DEFAULT_ENCODING) + SEPARATOR
                + URLEncoder.encode(filename, XWiki.DEFAULT_ENCODING) + SEPARATOR
                + URLEncoder.encode(StringUtils.defaultString(revision), XWiki.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            // This should never happen, UTF-8 is always available
            return space + SEPARATOR + name + SEPARATOR + filename + SEPARATOR + StringUtils.defaultString(revision);
        }
    }

    /**
     * Computes a safe identifier for a resource file, guaranteed to be collision-free.
     * 
     * @param filename the name of the file, possibly including a path to it, for example {@code icons/silk/add.gif}
     * @return an identifier for this file
     */
    private String getResourceKey(String filename)
    {
        try {
            return "resource" + SEPARATOR + URLEncoder.encode(filename, XWiki.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            // This should never happen, UTF-8 is always available
            return filename;
        }
    }

    /**
     * Computes a safe identifier for a skin filename, guaranteed to be collision-free.
     * 
     * @param filename the name of the file, possibly including a path to it, for example {@code css/colors/black.css}
     * @param skin the name of the skin where the file is expected to be
     * @return an identifier for this file
     */
    private String getSkinfileKey(String filename, String skin)
    {
        try {
            return "skin" + SEPARATOR + URLEncoder.encode(skin, XWiki.DEFAULT_ENCODING) + SEPARATOR
                + URLEncoder.encode(filename, XWiki.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            // This should never happen, UTF-8 is always available
            return skin + SEPARATOR + filename;
        }
    }

    /**
     * Retrieve the Map that relates resource keys to their corresponding temporary file.
     * 
     * @param context the current request context
     * @return the mapping as it was found in the context (read-write)
     */
    private Map<String, File> getFileMapping(XWikiContext context)
    {
        @SuppressWarnings("unchecked")
        Map<String, File> usedFiles = (Map<String, File>) context.get("pdfexport-file-mapping");
        return usedFiles;
    }

    /**
     * Create a new temporary file for the given resource key and return it.
     * 
     * @param key the resource key, needed for getting the file extension, if any
     * @param context the current request context
     * @return a new empty file
     * @throws IOException if creating the file fails
     */
    private File getTemporaryFile(String key, XWikiContext context) throws IOException
    {
        File tempdir = (File) context.get("pdfexportdir");
        String prefix = "pdf";
        String suffix = "." + FilenameUtils.getExtension(key);
        try {
            return File.createTempFile(prefix, suffix, tempdir);
        } catch (IOException e) {
            throw new IOException("Failed to create temporary PDF export file with prefix [" + prefix + "], suffix ["
                + suffix + "] in directory [" + tempdir + "]", e);
        }
    }
}
