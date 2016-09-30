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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.security.authorization.AuthorExecutor;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.Util;

/**
 * <p>
 * Action for serving skin files. It allows skins to be defined using XDocuments as skins, by letting files be placed as
 * text fields in an XWiki.XWikiSkins object, or as attachments to the document, or as a file in the filesystem. If the
 * file is not found in the current skin, then it is searched in its base skin, and eventually in the default base
 * skins,
 * </p>
 * <p>
 * This action indicates that the results should be publicly cacheable for 30 days.
 * </p>
 *
 * @version $Id$
 * @since 1.0
 */
public class SkinAction extends XWikiAction
{
    /** Logging helper. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SkinAction.class);

    /** Path delimiter. */
    private static final String DELIMITER = "/";

    /** The directory where the skins are placed in the webapp. */
    private static final String SKINS_DIRECTORY = "skins";

    /** The directory where resources are placed in the webapp. */
    private static final String RESOURCES_DIRECTORY = "resources";

    /**
     * The encoding to use when reading text resources from the filesystem and when sending css/javascript responses.
     */
    private static final String ENCODING = "UTF-8";

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        try {
            return render(context.getRequest().getPathInfo(), context);
        } catch (IOException e) {
            context.getResponse().setStatus(404);
            return "docdoesnotexist";
        }
    }

    public String render(String path, XWikiContext context) throws XWikiException, IOException
    {
        // This Action expects an incoming Entity URL of the type:
        // http://localhost:8080/xwiki/bin/skin/<path to resource on the filesystem, relative to the xwiki webapp>
        // Example 1 (fs skin file): .../bin/skin/skins/flamingo/style.css?...
        // Example 2 (fs resource file): .../bin/skin/resources/uicomponents/search/searchSuggest.css
        // Example 3 (wiki skin attachment or xproperty): .../bin/skin/XWiki/DefaultSkin/somefile.css
        //
        // TODO: The mapping to an Entity URL is hackish and needs to be fixed,
        // see http://jira.xwiki.org/browse/XWIKI-12449

        // Since we support Nested Spaces, these two examples will be mapped as the following Attachment References:
        // Example 1: skins.flamingo@style\.css
        // Example 2: resources.uicomponents.search@searchSuggest\.css
        // Example 3: XWiki.DefaultSkin@somefile\.css

        XWiki xwiki = context.getWiki();

        // Since skin paths usually contain the name of skin document, it is likely that the context document belongs to
        // the current skin.
        XWikiDocument doc = context.getDoc();

        // The base skin could be either a filesystem directory, or an xdocument.
        String baseskin = xwiki.getBaseSkin(context, true);
        XWikiDocument baseskindoc = xwiki.getDocument(baseskin, context);

        // The default base skin is always a filesystem directory.
        String defaultbaseskin = xwiki.getDefaultBaseSkin(context);

        LOGGER.debug("document: [{}] ; baseskin: [{}] ; defaultbaseskin: [{}]",
            new Object[] { doc.getDocumentReference(), baseskin, defaultbaseskin });

        // Since we don't know exactly what does the URL point at, meaning that we don't know where the skin identifier
        // ends and where the path to the file starts, we must try to split at every '/' character.
        int idx = path.lastIndexOf(DELIMITER);
        boolean found = false;
        while (idx > 0) {
            try {
                String filename = Util.decodeURI(path.substring(idx + 1), context);
                LOGGER.debug("Trying [{}]", filename);

                // Try on the current skin document.
                if (renderSkin(filename, doc, context)) {
                    found = true;
                    break;
                }

                // Try on the base skin document, if it is not the same as above.
                if (StringUtils.isNotEmpty(baseskin) && !doc.getName().equals(baseskin)) {
                    if (renderSkin(filename, baseskindoc, context)) {
                        found = true;
                        break;
                    }
                }

                // Try on the default base skin, if it wasn't already tested above.
                if (StringUtils.isNotEmpty(baseskin)
                    && !(doc.getName().equals(defaultbaseskin) || baseskin.equals(defaultbaseskin))) {
                    // defaultbaseskin can only be on the filesystem, so don't try to use it as a
                    // skin document.
                    if (renderFileFromFilesystem(getSkinFilePath(filename, defaultbaseskin), context)) {
                        found = true;
                        break;
                    }
                }

                // Try in the resources directory.
                if (renderFileFromFilesystem(getResourceFilePath(filename), context)) {
                    found = true;
                    break;
                }
            } catch (XWikiException ex) {
                if (ex.getCode() == XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION) {
                    // This means that the response couldn't be sent, although the file was
                    // successfully found. Signal this further, and stop trying to render.
                    throw ex;
                }
                LOGGER.debug(String.valueOf(idx), ex);
            }
            idx = path.lastIndexOf(DELIMITER, idx - 1);
        }
        if (!found) {
            context.getResponse().setStatus(404);
            return "docdoesnotexist";
        }
        return null;
    }

    /**
     * Get the path for the given skin file in the given skin.
     *
     * @param filename Name of the file.
     * @param skin Name of the skin to search in.
     * @throws IOException if filename is invalid
     */
    public String getSkinFilePath(String filename, String skin) throws IOException
    {
        String path =
            URI.create(DELIMITER + SKINS_DIRECTORY + DELIMITER + skin + DELIMITER + filename).normalize().toString();
        // Test to prevent someone from using "../" in the filename!
        if (!path.startsWith(DELIMITER + SKINS_DIRECTORY)) {
            LOGGER.warn("Illegal access, tried to use file [{}] as a skin. Possible break-in attempt!", path);
            throw new IOException("Invalid filename: '" + filename + "' for skin '" + skin + "'");
        }
        return path;
    }

    /**
     * Get the path for the given file in resources.
     *
     * @param filename Name of the file.
     * @throws IOException if filename is invalid
     */
    public String getResourceFilePath(String filename) throws IOException
    {
        String path = URI.create(DELIMITER + RESOURCES_DIRECTORY + DELIMITER + filename).normalize().toString();
        // Test to prevent someone from using "../" in the filename!
        if (!path.startsWith(DELIMITER + RESOURCES_DIRECTORY)) {
            LOGGER.warn("Illegal access, tried to use file [{}] as a resource. Possible break-in attempt!", path);
            throw new IOException("Invalid filename: '" + filename + "'");
        }
        return path;
    }

    /**
     * Tries to serve a skin file using <tt>doc</tt> as a skin document. The file is searched in the following places:
     * <ol>
     * <li>As the content of a property with the same name as the requested filename, from an XWikiSkins object attached
     * to the document.</li>
     * <li>As the content of an attachment with the same name as the requested filename.</li>
     * <li>As a file located on the filesystem, in the directory with the same name as the current document (in case the
     * URL was actually pointing to <tt>/skins/directory/file</tt>).</li>
     * </ol>
     *
     * @param filename The name of the skin file that should be rendered.
     * @param doc The skin {@link XWikiDocument document}.
     * @param context The current {@link XWikiContext request context}.
     * @return <tt>true</tt> if the attachment was found and the content was successfully sent.
     * @throws XWikiException If the attachment cannot be loaded.
     * @throws IOException if the filename is invalid
     */
    private boolean renderSkin(String filename, XWikiDocument doc, XWikiContext context)
        throws XWikiException, IOException
    {
        LOGGER.debug("Rendering file [{}] within the [{}] document", filename, doc.getDocumentReference());
        try {
            if (doc.isNew()) {
                LOGGER.debug("[{}] is not a document", doc.getDocumentReference().getName());
            } else {
                return renderFileFromObjectField(filename, doc, context)
                    || renderFileFromAttachment(filename, doc, context) || (SKINS_DIRECTORY.equals(doc.getSpace())
                        && renderFileFromFilesystem(getSkinFilePath(filename, doc.getName()), context));
            }
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while sending response:", e);
        }

        return renderFileFromFilesystem(getSkinFilePath(filename, doc.getName()), context);
    }

    /**
     * Tries to serve a file from the filesystem.
     *
     * @param path Path of the file that should be rendered.
     * @param context The current {@link XWikiContext request context}.
     * @return <tt>true</tt> if the file was found and its content was successfully sent.
     * @throws XWikiException If the response cannot be sent.
     */
    private boolean renderFileFromFilesystem(String path, XWikiContext context) throws XWikiException
    {
        LOGGER.debug("Rendering filesystem file from path [{}]", path);

        XWikiResponse response = context.getResponse();
        try {
            byte[] data;
            data = context.getWiki().getResourceContentAsBytes(path);
            if (data != null && data.length > 0) {
                String filename = path.substring(path.lastIndexOf("/") + 1, path.length());

                Date modified = null;

                // Evaluate the file only if it's of a supported type.
                String mimetype = context.getEngineContext().getMimeType(filename.toLowerCase());
                if (isCssMimeType(mimetype) || isJavascriptMimeType(mimetype) || isLessCssFile(filename)) {
                    // Always force UTF-8, as this is the assumed encoding for text files.
                    String rawContent = new String(data, ENCODING);

                    // Evaluate the content with the rights of the superadmin user, since this is a filesystem file.
                    DocumentReference superadminUserReference = new DocumentReference(context.getMainXWiki(),
                        XWiki.SYSTEM_SPACE, XWikiRightService.SUPERADMIN_USER);
                    String evaluatedContent = evaluateVelocity(rawContent, path, superadminUserReference, context);

                    byte[] newdata = evaluatedContent.getBytes(ENCODING);
                    // If the content contained velocity code, then it should not be cached
                    if (Arrays.equals(newdata, data)) {
                        modified = context.getWiki().getResourceLastModificationDate(path);
                    } else {
                        modified = new Date();
                        data = newdata;
                    }

                    response.setCharacterEncoding(ENCODING);
                } else {
                    modified = context.getWiki().getResourceLastModificationDate(path);
                }

                // Write the content to the response's output stream.
                setupHeaders(response, mimetype, modified, data.length);
                try {
                    response.getOutputStream().write(data);
                } catch (IOException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                        XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while sending response", e);
                }

                return true;
            }
        } catch (IOException ex) {
            LOGGER.info("Skin file [{}] does not exist or cannot be accessed", path);
        }
        return false;
    }

    /**
     * Tries to serve the content of an XWikiSkins object field as a skin file.
     *
     * @param filename The name of the skin file that should be rendered.
     * @param doc The skin {@link XWikiDocument document}.
     * @param context The current {@link XWikiContext request context}.
     * @return <tt>true</tt> if the object exists, and the field is set to a non-empty value, and its content was
     *         successfully sent.
     * @throws IOException If the response cannot be sent.
     */
    public boolean renderFileFromObjectField(String filename, XWikiDocument doc, final XWikiContext context)
        throws IOException
    {
        LOGGER.debug("... as object property");

        BaseObject object = doc.getObject("XWiki.XWikiSkins");
        String content = null;
        if (object != null) {
            content = object.getStringValue(filename);
        }

        if (!StringUtils.isBlank(content)) {
            XWiki xwiki = context.getWiki();

            // Evaluate the file only if it's of a supported type.
            String mimetype = xwiki.getEngineContext().getMimeType(filename.toLowerCase());
            if (isCssMimeType(mimetype) || isJavascriptMimeType(mimetype)) {
                final ObjectPropertyReference propertyReference =
                    new ObjectPropertyReference(filename, object.getReference());

                // Evaluate the content with the rights of the document's author.
                content = evaluateVelocity(content, propertyReference, doc.getAuthorReference(), context);
            }

            // Prepare the response.
            XWikiResponse response = context.getResponse();
            // Since object fields are read as unicode strings, the result does not depend on the wiki encoding. Force
            // the output to UTF-8.
            response.setCharacterEncoding(ENCODING);

            // Write the content to the response's output stream.
            byte[] data = content.getBytes(ENCODING);
            setupHeaders(response, mimetype, doc.getDate(), data.length);
            response.getOutputStream().write(data);

            return true;
        } else {
            LOGGER.debug("Object field not found or empty");
        }

        return false;
    }

    private String evaluateVelocity(String content, EntityReference reference, DocumentReference author,
        XWikiContext context)
    {
        EntityReferenceSerializer<String> serializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        String namespace = serializer.serialize(reference);

        return evaluateVelocity(content, namespace, author, context);
    }

    private String evaluateVelocity(final String content, final String namespace, final DocumentReference author,
        final XWikiContext context)
    {
        String result = content;

        try {
            result = Utils.getComponent(AuthorExecutor.class)
                .call(() -> context.getWiki().evaluateVelocity(content, namespace), author);
        } catch (Exception e) {
            // Should not happen since there is nothing in the call() method throwing an exception.
            LOGGER.error("Failed to evaluate velocity content for namespace {} with the rights of the user {}",
                namespace, author, e);
        }

        return result;
    }

    /**
     * Tries to serve the content of an attachment as a skin file.
     *
     * @param filename The name of the skin file that should be rendered.
     * @param doc The skin {@link XWikiDocument document}.
     * @param context The current {@link XWikiContext request context}.
     * @return <tt>true</tt> if the attachment was found and its content was successfully sent.
     * @throws IOException If the response cannot be sent.
     * @throws XWikiException If the attachment cannot be loaded.
     */
    public boolean renderFileFromAttachment(String filename, XWikiDocument doc, XWikiContext context)
        throws IOException, XWikiException
    {
        LOGGER.debug("... as attachment");

        XWikiAttachment attachment = doc.getAttachment(filename);
        if (attachment != null) {
            XWiki xwiki = context.getWiki();
            XWikiResponse response = context.getResponse();

            // Evaluate the file only if it's of a supported type.
            String mimetype = xwiki.getEngineContext().getMimeType(filename.toLowerCase());
            if (isCssMimeType(mimetype) || isJavascriptMimeType(mimetype)) {
                byte[] data = attachment.getContent(context);
                // Always force UTF-8, as this is the assumed encoding for text files.
                String velocityCode = new String(data, ENCODING);

                // Evaluate the content with the rights of the document's author.
                String evaluatedContent =
                    evaluateVelocity(velocityCode, attachment.getReference(), doc.getAuthorReference(), context);

                // Prepare the response.
                response.setCharacterEncoding(ENCODING);

                // Write the content to the response's output stream.
                data = evaluatedContent.getBytes(ENCODING);
                setupHeaders(response, mimetype, attachment.getDate(), data.length);
                response.getOutputStream().write(data);
            } else {
                // Otherwise, return the raw content.
                setupHeaders(response, mimetype, attachment.getDate(), attachment.getContentSize(context));
                IOUtils.copy(attachment.getContentInputStream(context), response.getOutputStream());
            }

            return true;
        } else {
            LOGGER.debug("Attachment not found");
        }

        return false;
    }

    /**
     * Checks if a mimetype indicates a javascript file.
     *
     * @param mimetype The mime type to check.
     * @return <tt>true</tt> if the mime type represents a javascript file.
     */
    public boolean isJavascriptMimeType(String mimetype)
    {
        boolean result =
            "text/javascript".equalsIgnoreCase(mimetype) || "application/x-javascript".equalsIgnoreCase(mimetype)
                || "application/javascript".equalsIgnoreCase(mimetype);
        result |= "application/ecmascript".equalsIgnoreCase(mimetype) || "text/ecmascript".equalsIgnoreCase(mimetype);
        return result;
    }

    /**
     * Checks if a mimetype indicates a CSS file.
     *
     * @param mimetype The mime type to check.
     * @return <tt>true</tt> if the mime type represents a css file.
     */
    public boolean isCssMimeType(String mimetype)
    {
        return "text/css".equalsIgnoreCase(mimetype);
    }

    /**
     * Checks if a file is a LESS file that should be parsed by velocity.
     *
     * @param filename name of the file to check.
     * @return <tt>true</tt> if the filename represents a LESS.vm file.
     */
    private boolean isLessCssFile(String filename)
    {
        return filename.toLowerCase().endsWith(".less.vm");
    }

    /**
     * Sets several headers to properly identify the response.
     *
     * @param response The servlet response object, where the headers should be set.
     * @param mimetype The mimetype of the file. Used in the "Content-Type" header.
     * @param lastChanged The date of the last change of the file. Used in the "Last-Modified" header.
     * @param length The length of the content (in bytes). Used in the "Content-Length" header.
     */
    protected void setupHeaders(XWikiResponse response, String mimetype, Date lastChanged, int length)
    {
        if (!StringUtils.isBlank(mimetype)) {
            response.setContentType(mimetype);
        } else {
            response.setContentType("application/octet-stream");
        }
        response.setDateHeader("Last-Modified", lastChanged.getTime());
        // Cache for one month (30 days)
        response.setHeader("Cache-Control", "public");
        response.setDateHeader("Expires", (new Date()).getTime() + 30 * 24 * 3600 * 1000L);
        response.setContentLength(length);
    }
}
