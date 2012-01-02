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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

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

    /** The URL part seperator. */
    private static final String SEPARATOR = "/";

    /** The name of the HTTP Header that signals a byte-range request. */
    private static final String RANGE_HEADER_NAME = "Range";

    /** The format of a valid range header. */
    private static final Pattern RANGE_HEADER_PATTERN = Pattern.compile("bytes=([0-9]+)?-([0-9]+)?");

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.web.XWikiAction#render(XWikiContext)
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String path = request.getRequestURI();
        String filename = Util.decodeURI(getFileName(path, ACTION_NAME), context);
        XWikiAttachment attachment = null;

        final String idStr = request.getParameter("id");
        if (StringUtils.isNumeric(idStr)) {
            int id = Integer.parseInt(idStr);
            if (doc.getAttachmentList().size() > id) {
                attachment = doc.getAttachmentList().get(id);
            }
        } else {
            attachment = doc.getAttachment(filename);
        }

        if (attachment == null) {
            Object[] args = {filename};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND, "Attachment {0} not found",
                null, args);
        }

        XWikiPluginManager plugins = context.getWiki().getPluginManager();
        attachment = plugins.downloadAttachment(attachment, context);

        long lastModifiedOnClient = request.getDateHeader("If-Modified-Since");
        long lastModifiedOnServer = attachment.getDate().getTime();
        if (lastModifiedOnClient != -1 && lastModifiedOnClient >= lastModifiedOnServer) {
            response.setStatus(XWikiResponse.SC_NOT_MODIFIED);
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
     * @return {@code true} if the partial content request was syntactically valid and a response was sent, {@code
     *         false} otherwise
     * @throws XWikiException if the attachment content cannot be retrieved
     * @throws IOException if the response cannot be written
     */
    private static boolean sendPartialContent(final XWikiAttachment attachment,
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
                start = Math.max(attachment.getContentSize(context) - end, 0L);
                end = attachment.getContentSize(context) - 1L;
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
    private static void writeByteRange(final XWikiAttachment attachment, Long start, Long end,
        final XWikiRequest request,
        final XWikiResponse response,
        final XWikiContext context)
        throws XWikiException, IOException
    {
        if (start >= 0 && start < attachment.getContentSize(context)) {
            InputStream data = attachment.getContentInputStream(context);
            data = new BoundedInputStream(data, end + 1);
            data.skip(start);
            setCommonHeaders(attachment, request, response, context);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            if ((end - start + 1L) < Integer.MAX_VALUE) {
                response.setContentLength((int) (end - start + 1));
            }
            response.setHeader("Content-Range", "bytes " + start + "-" + end + SEPARATOR
                + attachment.getContentSize(context));
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
    private static void sendContent(final XWikiAttachment attachment,
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
        } catch (XWikiException e) {
            Object[] args = {filename};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                "Attachment content {0} not found", null, args);
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
     * Get the filename of the attachment from the path and the action.
     * 
     * @param path the request URI.
     * @param action the action used to download the attachment.
     * @return the filename of the attachment.
     */
    private static String getFileName(final String path, final String action)
    {
        final String subPath = path.substring(path.indexOf(SEPARATOR + action));
        int pos = 0;
        for (int i = 0; i < 3; i++) {
            pos = subPath.indexOf(SEPARATOR, pos + 1);
        }
        if (subPath.indexOf(SEPARATOR, pos + 1) > 0) {
            return subPath.substring(pos + 1, subPath.indexOf(SEPARATOR, pos + 1));
        }
        return subPath.substring(pos + 1);
    }

    /**
     * Set the response HTTP headers common to both partial (Range) and full responses.
     * 
     * @param attachment the attachment to get content from
     * @param request the current client request
     * @param response the response to write to.
     * @param context the current request context
     */
    private static void setCommonHeaders(final XWikiAttachment attachment,
        final XWikiRequest request,
        final XWikiResponse response,
        final XWikiContext context)
    {
        // Choose the right content type
        String mimetype = attachment.getMimeType(context);
        response.setContentType(mimetype);
        response.setCharacterEncoding("");

        String ofilename =
            Util.encodeURI(attachment.getFilename(), context).replaceAll("\\+", "%20");

        // The inline attribute of Content-Disposition tells the browser that they should display
        // the downloaded file in the page (see http://www.ietf.org/rfc/rfc1806.txt for more
        // details). We do this so that JPG, GIF, PNG, etc are displayed without prompting a Save
        // dialog box. However, all mime types that cannot be displayed by the browser do prompt a
        // Save dialog box (exe, zip, xar, etc).
        String dispType = "inline";

        if ("1".equals(request.getParameter("force-download"))) {
            dispType = "attachment";
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
     *        Range header
     * @param end the requested range end, i.e. the last byte to be transfered, or {@code null} if missing from the
     *        Range header
     * @return {@code true} if the range is valid, {@code false} otherwise
     */
    private static boolean isValidRange(Long start, Long end)
    {
        if (start == null && end == null) {
            return false;
        }
        return start == null || end == null || end >= start;
    }
}
