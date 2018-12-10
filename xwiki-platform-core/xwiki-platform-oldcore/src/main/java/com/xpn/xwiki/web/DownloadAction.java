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
import java.io.InputStream;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.util.Util;

/**
 * The action for downloading attachments from the server.
 *
 * @version $Id$
 */
public class DownloadAction extends XWikiAction
{
    /** The identifier of the download action. */
    public static final String ACTION_NAME = "download";

    /** The identifier of the attachment disposition. */
    public static final String ATTACHMENT = "attachment";

    /** List of authorized attachment mimetypes. */
    public static final List<String> MIMETYPE_WHITELIST =
        Arrays.asList("audio/basic", "audio/L24", "audio/mp4", "audio/mpeg", "audio/ogg", "audio/vorbis",
            "audio/vnd.rn-realaudio", "audio/vnd.wave", "audio/webm", "image/gif", "image/jpeg", "image/pjpeg",
            "image/png", "image/svg+xml", "image/tiff", "text/csv", "text/plain", "text/xml", "text/rtf",
            "video/mpeg", "video/ogg", "video/quicktime", "video/webm", "video/x-matroska", "video/x-ms-wmv",
            "video/x-flv");

    /** Key of the whitelist in xwiki.properties. */
    public static final String WHITELIST_PROPERTY = "attachment.download.whitelist";

    /** Key of the blacklist in xwiki.properties. */
    public static final String BLACKLIST_PROPERTY = "attachment.download.blacklist";

    /** The URL part separator. */
    private static final String SEPARATOR = "/";

    /** The name of the HTTP Header that signals a byte-range request. */
    private static final String RANGE_HEADER_NAME = "Range";

    /** The format of a valid range header. */
    private static final Pattern RANGE_HEADER_PATTERN = Pattern.compile("bytes=([0-9]+)?-([0-9]+)?");

    /**
     * Default constructor.
     */
    public DownloadAction()
    {
       this.handleRedirectObject = true;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        XWikiDocument doc = context.getDoc();
        String filename = getFileName();
        XWikiAttachment attachment = getAttachment(request, doc, filename);

        Map<String, Object> backwardCompatibilityContextObjects = null;

        if (attachment == null) {
            // If some plugins extend the Download URL format for the Standard Scheme the document in the context will
            // most likely not have a reference that corresponds to what the plugin expects. For example imagine that
            // the URL is a Zip Explorer URL like .../download/space/page/attachment/index.html. This will be parsed
            // as space.page.attachment@index.html by the Standard URL scheme parsers. Thus the attachment won't be
            // found since index.html is not the correct attachment for the Zip Explorer plugin's URL format.
            //
            // Thus in order to preserve backward compatibility for existing plugins that have custom URL formats
            // extending the Download URL format, we parse again the URL by considering that it doesn't contain any
            // Nested Space. This also means that those plugins will need to completely reparse the URL if they wish to
            // support Nested Spaces.
            //
            // Also note that this code below is not compatible with the notion of having several URL schemes. The real
            // fix will be to not allow plugins to support custom URL formats and instead to have them register new
            // Actions if they need a different URL format.
            Pair<XWikiDocument, XWikiAttachment> result =
                extractAttachmentAndDocumentFromURLWithoutSupportingNestedSpaces(request, context);

            if (result == null) {
                throwNotFoundException(filename);
            }

            XWikiDocument backwardCompatibilityDocument = result.getLeft();
            attachment = result.getRight();

            // Set the new doc as the context doc so that plugins see it as the context doc
            backwardCompatibilityContextObjects = new HashMap<>();
            pushDocumentInContext(backwardCompatibilityContextObjects,
                backwardCompatibilityDocument.getDocumentReference());
        }

        try {
            XWikiPluginManager plugins = context.getWiki().getPluginManager();
            attachment = plugins.downloadAttachment(attachment, context);

            if (attachment == null) {
                throwNotFoundException(filename);
            }

            // Try to load the attachment content just to make sure that the attachment really exists
            // This will throw an exception if the attachment content isn't available
            try {
                attachment.getContentSize(context);
            } catch (XWikiException e) {
                Object[] args = { filename };
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                    "Attachment content {0} not found", null, args);
            }

            long lastModifiedOnClient = request.getDateHeader("If-Modified-Since");
            long lastModifiedOnServer = attachment.getDate().getTime();
            if (lastModifiedOnClient != -1 && lastModifiedOnClient >= lastModifiedOnServer) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return null;
            }

            // Sending the content of the attachment
            if (request.getHeader(RANGE_HEADER_NAME) != null) {
                try {
                    if (sendPartialContent(attachment, request, response, context)) {
                        return null;
                    }
                } catch (IOException ex) {
                    // Broken response...
                }
            }
            sendContent(attachment, request, response, filename, context);
            return null;
        } finally {
            if (backwardCompatibilityContextObjects != null) {
                popDocumentFromContext(backwardCompatibilityContextObjects);
            }
        }
    }

    private void throwNotFoundException(String filename) throws XWikiException
    {
        String message = filename == null ? "Attachment not found" :
            String.format("Attachment [%s] not found", filename);
        throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
            XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND, message);
    }

    /**
     * Respond to a range request, either with the requested bytes, or with a {@code 416 REQUESTED RANGE NOT
     * SATISFIABLE} response if the requested byte range falls outside the length of the attachment. If the range
     * request header is syntactically invalid, nothing is written, and instead {@code false} is returned, letting the
     * action handler ignore the Range header and treat this as a normal (full) download request.
     *
     * @param attachment the attachment to get content from
     * @param request the current client request
     * @param response the response to write to.
     * @param context the current request context
     * @return {@code true} if the partial content request was syntactically valid and a response was sent,
     *         {@code false} otherwise
     * @throws XWikiException if the attachment content cannot be retrieved
     * @throws IOException if the response cannot be written
     */
    private boolean sendPartialContent(final XWikiAttachment attachment,
        final XWikiRequest request,
        final XWikiResponse response,
        final XWikiContext context)
        throws XWikiException, IOException
    {
        String range = request.getHeader(RANGE_HEADER_NAME);
        Matcher m = RANGE_HEADER_PATTERN.matcher(range);
        if (m.matches()) {
            String startStr = m.group(1);
            String endStr = m.group(2);
            Long start = NumberUtils.createLong(startStr);
            Long end = NumberUtils.createLong(endStr);
            if (start == null && end != null && end > 0) {
                // Tail request, output the last <end> bytes
                start = Math.max(attachment.getContentLongSize(context) - end, 0L);
                end = attachment.getContentLongSize(context) - 1L;
            }
            if (!isValidRange(start, end)) {
                return false;
            }
            if (end == null) {
                end = attachment.getContentSize(context) - 1L;
            }
            end = Math.min(end, attachment.getContentSize(context) - 1L);
            writeByteRange(attachment, start, end, request, response, context);
            return true;
        }
        return false;
    }

    /**
     * Write a byte range from the attachment to the response, if the requested range is valid and falls within the file
     * limits.
     *
     * @param attachment the attachment to get content from
     * @param start the first byte to write
     * @param end the last byte to write
     * @param request the current client request
     * @param response the response to write to.
     * @param context the current request context
     * @throws XWikiException if the attachment content cannot be retrieved
     * @throws IOException if the response cannot be written
     */
    private void writeByteRange(final XWikiAttachment attachment, Long start, Long end,
        final XWikiRequest request,
        final XWikiResponse response,
        final XWikiContext context)
        throws XWikiException, IOException
    {
        if (start >= 0 && start < attachment.getContentLongSize(context)) {
            InputStream data = attachment.getContentInputStream(context);
            data = new BoundedInputStream(data, end + 1);
            data.skip(start);
            setCommonHeaders(attachment, request, response, context);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            if ((end - start + 1L) < Integer.MAX_VALUE) {
                response.setContentLength((int) (end - start + 1));
            }
            response.setHeader("Content-Range", "bytes " + start + "-" + end + SEPARATOR
                + attachment.getContentLongSize(context));
            IOUtils.copyLarge(data, response.getOutputStream());
        } else {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        }
    }

    /**
     * Send the attachment content in the response.
     *
     * @param attachment the attachment to get content from
     * @param request the current client request
     * @param response the response to write to.
     * @param filename the filename to show in the message in case an exception needs to be thrown
     * @param context the XWikiContext just in case it is needed to load the attachment content
     * @throws XWikiException if something goes wrong
     */
    private void sendContent(final XWikiAttachment attachment,
        final XWikiRequest request,
        final XWikiResponse response,
        final String filename,
        final XWikiContext context)
        throws XWikiException
    {
        InputStream stream = null;
        try {
            setCommonHeaders(attachment, request, response, context);
            response.setContentLength(attachment.getContentSize(context));
            stream = attachment.getContentInputStream(context);
            IOUtils.copy(stream, response.getOutputStream());
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                "Exception while sending response", e);
        } finally {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
        }
    }

    /**
     * @return the filename of the attachment or null if the URL didn't point to an attachment
     */
    private String getFileName()
    {
        // Extract the Attachment file name from the parsed request URL that was done before this Action is called
        ResourceReference resourceReference = Utils.getComponent(ResourceReferenceManager.class).getResourceReference();
        EntityResourceReference entityResource = (EntityResourceReference) resourceReference;

        // Try to extract the attachment from the reference but it's possible that the URL didn't point to an
        // attachment, in which case we return null.
        EntityReference attachmentReference =
            entityResource.getEntityReference().extractReference(EntityType.ATTACHMENT);

        return attachmentReference == null ? null : attachmentReference.getName();
    }

    private Pair<XWikiDocument, XWikiAttachment> extractAttachmentAndDocumentFromURLWithoutSupportingNestedSpaces(
        XWikiRequest request, XWikiContext context)
    {
        String path = request.getRequestURI();

        // Extract the path part after the action, e.g. "/space/page/attachment/path1/path2" when you have
        // ".../download/space/page/attachment/path1/path2".
        int pos = path.indexOf(SEPARATOR + ACTION_NAME);
        String subPath = path.substring(pos + (SEPARATOR + ACTION_NAME).length() + 1);

        List<String> segments = new ArrayList<>();
        for (String pathSegment : subPath.split(SEPARATOR, -1)) {
            segments.add(Util.decodeURI(pathSegment, context));
        }

        // We need at least 3 segments
        if (segments.size() < 3) {
            return null;
        }

        String spaceName = segments.get(0);
        String pageName = segments.get(1);
        String attachmentName = segments.get(2);

        // Generate the XWikiDocument and try to load it (if the user has permission to it)
        DocumentReference reference = new DocumentReference(context.getWikiId(), spaceName, pageName);
        XWiki xwiki = context.getWiki();

        XWikiDocument backwardCompatibilityDocument;
        try {
            backwardCompatibilityDocument = xwiki.getDocument(reference, context);
            if (!backwardCompatibilityDocument.isNew()) {
                if (!context.getWiki().checkAccess(context.getAction(), backwardCompatibilityDocument, context)) {
                    // No permission to access the document, consider that the attachment doesn't exist
                    return null;
                }
            } else {
                // Document doesn't exist
                return null;
            }
        } catch (XWikiException e) {
            // An error happened when getting the doc or checking the permission, consider that the attachment
            // doesn't exist
            return null;
        }

        // Look for the attachment and return it
        XWikiAttachment attachment = getAttachment(request, backwardCompatibilityDocument, attachmentName);

        return new ImmutablePair<>(backwardCompatibilityDocument, attachment);
    }

    private void pushDocumentInContext(Map<String, Object> backupObjects, DocumentReference documentReference)
        throws XWikiException
    {
        XWikiContext xcontext = getContext();

        // Backup current context state
        XWikiDocument.backupContext(backupObjects, xcontext);

        // Make sure to get the current XWikiContext after ExcutionContext clone
        xcontext = getContext();

        // Change context document
        xcontext.getWiki().getDocument(documentReference, xcontext).setAsContextDoc(xcontext);
    }

    private void popDocumentFromContext(Map<String, Object> backupObjects)
    {
        XWikiDocument.restoreContext(backupObjects, getContext());
    }

    private XWikiContext getContext()
    {
        Execution execution = Utils.getComponent(Execution.class);
        ExecutionContext econtext = execution.getContext();
        return econtext != null ? (XWikiContext) econtext.getProperty("xwikicontext") : null;
    }

    private XWikiAttachment getAttachment(XWikiRequest request, XWikiDocument document, String filename)
    {
        XWikiAttachment attachment = null;

        String idStr = request.getParameter("id");
        if (StringUtils.isNumeric(idStr)) {
            int id = Integer.parseInt(idStr);
            if (document.getAttachmentList().size() > id) {
                attachment = document.getAttachmentList().get(id);
            }
        } else {
            attachment = document.getAttachment(filename);
        }

        return attachment;
    }

    /**
     * Set the response HTTP headers common to both partial (Range) and full responses.
     *
     * @param attachment the attachment to get content from
     * @param request the current client request
     * @param response the response to write to.
     * @param context the current request context
     */
    private void setCommonHeaders(final XWikiAttachment attachment,
        final XWikiRequest request,
        final XWikiResponse response,
        final XWikiContext context)
    {
        // Choose the right content type
        String mimetype = attachment.getMimeType(context);
        response.setContentType(mimetype);

        // Set the character encoding
        String characterEncoding = attachment.getCharset();
        if (characterEncoding != null) {
            response.setCharacterEncoding(characterEncoding);
        }

        String ofilename =
            Util.encodeURI(attachment.getFilename(), context).replaceAll("\\+", "%20");

        // The inline attribute of Content-Disposition tells the browser that they should display
        // the downloaded file in the page (see http://www.ietf.org/rfc/rfc1806.txt for more
        // details). We do this so that JPG, GIF, PNG, etc are displayed without prompting a Save
        // dialog box. However, all mime types that cannot be displayed by the browser do prompt a
        // Save dialog box (exe, zip, xar, etc).
        String dispType = "inline";

        // Determine whether the user who attached the file has Programming Rights or not.
        boolean hasPR = false;
        String author = attachment.getAuthor();
        try {
            hasPR =
                context.getWiki().getRightService().hasAccessLevel(
                    "programming", author, "XWiki.XWikiPreferences", context);
        } catch (Exception e) {
            hasPR = false;
        }
        // If the mimetype is not authorized to be displayed inline, let's force its content disposition to download.
        if ((!hasPR && !isAuthorized(mimetype)) || "1".equals(request.getParameter("force-download"))) {
            dispType = ATTACHMENT;
        }
        // Use RFC 2231 for encoding filenames, since the normal HTTP headers only allows ASCII characters.
        // See http://tools.ietf.org/html/rfc2231 for more details.
        response.addHeader("Content-disposition", dispType + "; filename*=utf-8''" + ofilename);

        response.setDateHeader("Last-Modified", attachment.getDate().getTime());
        // Advertise that downloads can be resumed
        response.setHeader("Accept-Ranges", "bytes");
    }

    /**
     * Check if the specified byte range first and last bytes form a syntactically valid range. For a range to be valid,
     * at least one of the ends must be specified, and if both are present, the range end must be greater than the range
     * start.
     *
     * @param start the requested range start, i.e. the first byte to be transfered, or {@code null} if missing from the
     *            Range header
     * @param end the requested range end, i.e. the last byte to be transfered, or {@code null} if missing from the
     *            Range header
     * @return {@code true} if the range is valid, {@code false} otherwise
     */
    private boolean isValidRange(Long start, Long end)
    {
        if (start == null && end == null) {
            return false;
        }
        return start == null || end == null || end >= start;
    }

    private boolean isAuthorized(String mimeType)
    {
        ConfigurationSource configuration = Utils.getComponent(ConfigurationSource.class, "xwikiproperties");
        if (configuration.containsKey(BLACKLIST_PROPERTY) && !configuration.containsKey(WHITELIST_PROPERTY)) {
            List<String> blackList = (configuration.getProperty(BLACKLIST_PROPERTY, Collections.<String>emptyList()));
            return !blackList.contains(mimeType);
        } else {
            List<String> whiteList = configuration.getProperty(WHITELIST_PROPERTY, MIMETYPE_WHITELIST);
            return whiteList.contains(mimeType);
        }
    }
}
