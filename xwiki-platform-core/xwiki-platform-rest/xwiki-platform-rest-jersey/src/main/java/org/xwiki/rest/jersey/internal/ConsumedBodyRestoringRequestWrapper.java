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
package org.xwiki.rest.jersey.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a form request ({@code multipart/form-data} or {@code application/x-www-form-urlencoded}) to restore its body
 * when it has already been consumed by an earlier call to {@link HttpServletRequest#getParameter(String)} or similar.
 * Jersey reads request entities from the raw input stream, so once that stream has been consumed (typically by an
 * upstream filter reading a request parameter) Jersey sees an empty body. This wrapper detects that situation by
 * peeking at the first byte: if it is EOF, the body is rebuilt from the state the servlet container cached while
 * parsing it, and returned from {@link #getInputStream()}.
 * <ul>
 * <li>for {@code multipart/form-data}, from the cached {@link Part}s;</li>
 * <li>for {@code application/x-www-form-urlencoded}, from the request parameters (excluding the ones coming from the
 * query string, so that only the body is restored).</li>
 * </ul>
 * The multipart reconstruction is streamed (only the small boundary and header lines are held in memory, each part body
 * is streamed straight from the container's storage); the url-encoded reconstruction is small by nature. When the body
 * is reconstructed, {@link #getContentLength()} and {@link #getContentLengthLong()} report its restored length rather
 * than the now-inconsistent original {@code Content-Length}. When the body is still available, the wrapper is
 * transparent: it replays the peeked byte and delegates everything else to the wrapped request.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
class ConsumedBodyRestoringRequestWrapper extends HttpServletRequestWrapper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumedBodyRestoringRequestWrapper.class);

    private static final String QUOTE = "\"";

    private static final String BOUNDARY_PREFIX = "--";

    private static final String CRLF = "\r\n";

    /**
     * Charset used to serialize part headers (and the structural boundary lines, which are pure ASCII anyway). It is
     * deliberately ISO-8859-1 because that is the charset Jersey's multipart parser (MIMEPull) uses to decode part
     * headers: encoding with the same charset makes the reconstruction a faithful, byte-for-byte round trip for the
     * whole Latin-1 range, and keeps the behavior identical to how Jersey would have parsed the original, un-consumed
     * request. Part bodies are copied as raw bytes and are never affected by this. Note that filename characters
     * outside the Latin-1 range cannot be reproduced faithfully, but that is an inherent limitation of MIMEPull's
     * ISO-8859-1 header decoding rather than of this wrapper.
     */
    private static final Charset HEADER_CHARSET = StandardCharsets.ISO_8859_1;

    private boolean built;

    private ServletInputStream inputStream;

    private boolean bodyReconstructed;

    /**
     * Length of the reconstructed body, or -1 when it could not be computed exactly (in which case an unknown length is
     * reported). Only meaningful when {@link #bodyReconstructed} is {@code true}.
     */
    private long reconstructedLength = -1;

    /**
     * A failure that happened while building the stream. It is remembered because the build consumes the original
     * stream and therefore happens exactly once: any later access must see the same failure rather than a half-built
     * (null) stream.
     */
    private IOException buildFailure;

    ConsumedBodyRestoringRequestWrapper(HttpServletRequest request)
    {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        ensureBuilt();

        return this.inputStream;
    }

    @Override
    public int getContentLength()
    {
        long length = getContentLengthLong();

        // Follow the servlet contract: return -1 when the length is unknown or does not fit in an int.
        return (length < 0 || length > Integer.MAX_VALUE) ? -1 : (int) length;
    }

    @Override
    public long getContentLengthLong()
    {
        try {
            ensureBuilt();
        } catch (IOException e) {
            // We could not inspect the body; fall back to whatever the wrapped request reports.
            return super.getContentLengthLong();
        }

        return this.bodyReconstructed ? this.reconstructedLength : super.getContentLengthLong();
    }

    private void ensureBuilt() throws IOException
    {
        if (!this.built) {
            // Set the flag first: the peek in build() consumes the original stream, so building must happen exactly
            // once even if it fails.
            this.built = true;
            try {
                build();
            } catch (IOException e) {
                this.buildFailure = e;
            }
        }

        if (this.buildFailure != null) {
            throw this.buildFailure;
        }
    }

    private void build() throws IOException
    {
        ServletInputStream original = super.getInputStream();

        // Peek at the first byte to detect whether the stream was consumed upstream.
        int firstByte = original.read();

        if (firstByte != -1) {
            // Stream still has data; return it with the peeked byte prepended and keep the original content length.
            this.inputStream = new PrependedServletInputStream(firstByte, original);
        } else if (!reconstructBody()) {
            // EOF on first read but nothing to reconstruct: hand back the (empty) original stream.
            this.inputStream = original;
        }
    }

    /**
     * @return {@code true} if the body was reconstructed (in which case {@link #inputStream} and
     *         {@link #reconstructedLength} have been set), {@code false} when it cannot or need not be rebuilt
     */
    private boolean reconstructBody() throws IOException
    {
        String contentType = getContentType();
        if (contentType != null) {
            String lowerCase = contentType.toLowerCase(Locale.ROOT);
            if (lowerCase.startsWith(MediaTypes.MULTIPART_FORM_DATA)) {
                return reconstructMultipartBody(contentType);
            } else if (lowerCase.startsWith(MediaTypes.FORM_URLENCODED)) {
                return reconstructFormBody();
            }
        }

        return false;
    }

    // -- multipart/form-data --------------------------------------------------------------------------------------

    private boolean reconstructMultipartBody(String contentType) throws IOException
    {
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            LOGGER.debug("The multipart request body was consumed but no boundary could be found in the "
                + "Content-Type [{}]; the body cannot be restored.", contentType);

            return false;
        }

        Collection<Part> parts;
        try {
            parts = getParts();
        } catch (ServletException e) {
            LOGGER.warn("The multipart request body was consumed and its parts could not be retrieved to restore it: "
                + "{}", e.getMessage());

            return false;
        }

        if (parts.isEmpty()) {
            LOGGER.debug("The multipart request body was consumed and no cached part is available to restore it.");

            return false;
        }

        LOGGER.debug("Restoring the consumed multipart request body from [{}] cached part(s).", parts.size());

        assembleMultipartBody(boundary, parts);

        return true;
    }

    /**
     * Assemble the reconstructed multipart body as a sequence of streams and record its length. For each part, the
     * (in-memory) header block, the part body (streamed from the container) and a trailing CRLF are chained, followed
     * by the closing boundary; nothing but the headers is held in memory.
     */
    private void assembleMultipartBody(String boundary, Collection<Part> parts) throws IOException
    {
        byte[] partBoundary = (BOUNDARY_PREFIX + boundary + CRLF).getBytes(HEADER_CHARSET);
        byte[] closingBoundary = (BOUNDARY_PREFIX + boundary + BOUNDARY_PREFIX + CRLF).getBytes(HEADER_CHARSET);
        byte[] crlf = CRLF.getBytes(HEADER_CHARSET);

        List<InputStream> streams = new ArrayList<>(parts.size() * 3 + 1);
        long length = closingBoundary.length;
        boolean lengthKnown = true;
        for (Part part : parts) {
            byte[] header = buildPartHeader(partBoundary, crlf, part);
            streams.add(new ByteArrayInputStream(header));
            streams.add(part.getInputStream());
            streams.add(new ByteArrayInputStream(crlf));

            long size = part.getSize();
            if (size < 0) {
                lengthKnown = false;
            }
            length += header.length + Math.max(size, 0) + crlf.length;
        }
        streams.add(new ByteArrayInputStream(closingBoundary));

        this.inputStream =
            new StreamingServletInputStream(new SequenceInputStream(Collections.enumeration(streams)));
        this.bodyReconstructed = true;
        this.reconstructedLength = lengthKnown ? length : -1;
    }

    private static byte[] buildPartHeader(byte[] partBoundary, byte[] crlf, Part part)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(partBoundary);
        for (String headerName : part.getHeaderNames()) {
            for (String headerValue : part.getHeaders(headerName)) {
                out.writeBytes((headerName + ": " + headerValue + CRLF).getBytes(HEADER_CHARSET));
            }
        }
        // Blank line separating the headers from the body.
        out.writeBytes(crlf);

        return out.toByteArray();
    }

    private static String extractBoundary(String contentType)
    {
        for (String rawParam : contentType.split(";")) {
            String param = rawParam.trim();
            if (param.regionMatches(true, 0, "boundary=", 0, 9)) {
                String boundary = param.substring(9).trim();
                if (boundary.startsWith(QUOTE) && boundary.endsWith(QUOTE)) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                return boundary;
            }
        }
        return null;
    }

    // -- application/x-www-form-urlencoded ------------------------------------------------------------------------

    private boolean reconstructFormBody()
    {
        Charset charset = resolveFormCharset();
        Map<String, List<String>> queryParameters = parseQueryParameters(charset);

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String[]> entry : getParameterMap().entrySet()) {
            appendBodyParameter(builder, entry.getKey(), entry.getValue(), queryParameters, charset);
        }

        if (builder.length() == 0) {
            LOGGER.debug("The url-encoded request body was consumed and no body parameter is available to restore it.");

            return false;
        }

        LOGGER.debug("Restoring the consumed url-encoded request body from the request parameters.");

        byte[] body = builder.toString().getBytes(charset);
        this.inputStream = new StreamingServletInputStream(new ByteArrayInputStream(body));
        this.bodyReconstructed = true;
        this.reconstructedLength = body.length;

        return true;
    }

    private static void appendBodyParameter(StringBuilder builder, String name, String[] values,
        Map<String, List<String>> queryParameters, Charset charset)
    {
        List<String> bodyValues = new ArrayList<>(Arrays.asList(values));

        // Drop the values that actually came from the query string so that only the request body is restored.
        List<String> queryValues = queryParameters.get(name);
        if (queryValues != null) {
            queryValues.forEach(bodyValues::remove);
        }

        for (String value : bodyValues) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(name, charset)).append('=').append(URLEncoder.encode(value, charset));
        }
    }

    private Map<String, List<String>> parseQueryParameters(Charset charset)
    {
        Map<String, List<String>> parameters = new HashMap<>();

        String queryString = getQueryString();
        if (queryString != null) {
            for (String pair : queryString.split("&")) {
                if (!pair.isEmpty()) {
                    int index = pair.indexOf('=');
                    String name = index >= 0 ? pair.substring(0, index) : pair;
                    String value = index >= 0 ? pair.substring(index + 1) : "";
                    parameters.computeIfAbsent(URLDecoder.decode(name, charset), key -> new ArrayList<>())
                        .add(URLDecoder.decode(value, charset));
                }
            }
        }

        return parameters;
    }

    private Charset resolveFormCharset()
    {
        String encoding = getCharacterEncoding();

        return encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
    }

    /**
     * Content-type prefixes handled by this wrapper.
     */
    private static final class MediaTypes
    {
        private static final String MULTIPART_FORM_DATA = "multipart/form-data";

        private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

        private MediaTypes()
        {
        }
    }

    /**
     * {@link ServletInputStream} that streams from an arbitrary {@link InputStream}, tracking end-of-stream for
     * {@link #isFinished()}.
     */
    private static final class StreamingServletInputStream extends ServletInputStream
    {
        private final InputStream delegate;

        private boolean finished;

        StreamingServletInputStream(InputStream delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException
        {
            int read = this.delegate.read();
            if (read == -1) {
                this.finished = true;
            }
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            int read = this.delegate.read(b, off, len);
            if (read == -1) {
                this.finished = true;
            }
            return read;
        }

        @Override
        public int available() throws IOException
        {
            return this.delegate.available();
        }

        @Override
        public void close() throws IOException
        {
            this.delegate.close();
        }

        @Override
        public boolean isFinished()
        {
            return this.finished;
        }

        @Override
        public boolean isReady()
        {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener)
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@link ServletInputStream} that prepends a single already-read byte in front of a delegate stream.
     */
    private static final class PrependedServletInputStream extends ServletInputStream
    {
        private final int firstByte;

        private final ServletInputStream delegate;

        private boolean firstRead = true;

        PrependedServletInputStream(int firstByte, ServletInputStream delegate)
        {
            this.firstByte = firstByte;
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException
        {
            if (this.firstRead) {
                this.firstRead = false;
                return this.firstByte;
            }
            return this.delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            if (len <= 0) {
                return 0;
            }
            if (this.firstRead) {
                this.firstRead = false;
                b[off] = (byte) this.firstByte;
                if (len == 1) {
                    return 1;
                }
                int read = this.delegate.read(b, off + 1, len - 1);
                return 1 + Math.max(0, read);
            }
            return this.delegate.read(b, off, len);
        }

        @Override
        public int available() throws IOException
        {
            return (this.firstRead ? 1 : 0) + this.delegate.available();
        }

        @Override
        public boolean isFinished()
        {
            return !this.firstRead && this.delegate.isFinished();
        }

        @Override
        public boolean isReady()
        {
            return this.delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener)
        {
            this.delegate.setReadListener(readListener);
        }
    }
}
