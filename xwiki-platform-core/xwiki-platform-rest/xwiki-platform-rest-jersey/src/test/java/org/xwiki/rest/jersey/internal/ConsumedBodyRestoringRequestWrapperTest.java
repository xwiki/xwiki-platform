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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.mimepull.MIMEMessage;
import org.jvnet.mimepull.MIMEPart;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConsumedBodyRestoringRequestWrapper}.
 *
 * @version $Id$
 */
@ExtendWith(MockitoExtension.class)
class ConsumedBodyRestoringRequestWrapperTest
{
    private static final String BOUNDARY = "testBoundary";

    private static final String MULTIPART_CONTENT_TYPE = "multipart/form-data; boundary=" + BOUNDARY;

    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    @Mock
    private HttpServletRequest request;

    // -- multipart/form-data --------------------------------------------------------------------------------------

    @Test
    void getInputStreamWhenStreamNotConsumedReturnsOriginalDataUntouched() throws Exception
    {
        // A realistic multipart body that is still available on the input stream.
        byte[] originalData = ("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"x\"\r\n\r\nhello\r\n--"
            + BOUNDARY + "--\r\n").getBytes(StandardCharsets.ISO_8859_1);
        when(this.request.getInputStream()).thenReturn(servletInputStream(originalData));

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);
        byte[] result = wrapper.getInputStream().readAllBytes();

        // The peeked byte must be replayed so the body is delivered verbatim, and the parts must not be touched.
        assertArrayEquals(originalData, result);
        verify(this.request, never()).getParts();
    }

    @Test
    void reconstructedMultipartBodyIsFaithfullyParseableByJerseyMimeParser() throws Exception
    {
        when(this.request.getContentType()).thenReturn(MULTIPART_CONTENT_TYPE);
        when(this.request.getInputStream()).thenReturn(emptyStream());

        // A binary payload including bytes that are not valid on their own in any multi-byte encoding, to make sure
        // part bodies are copied as raw bytes.
        byte[] binary = {0, 1, 2, (byte) 0xFF, (byte) 0xC3, (byte) 0x28, 10, 13};

        // A plain field, a text file with a Latin-1 (non-ASCII) filename, and a binary file. The order matters: it
        // must be preserved through the reconstruction.
        Part field = mockPart(headers(CONTENT_DISPOSITION, "form-data; name=\"field1\""),
            "value1".getBytes(StandardCharsets.UTF_8));
        Part textFile = mockPart(
            headers(CONTENT_DISPOSITION, "form-data; name=\"file\"; filename=\"caf\u00e9.txt\"",
                "Content-Type", "text/plain"),
            "file content".getBytes(StandardCharsets.UTF_8));
        Part binaryFile = mockPart(headers(CONTENT_DISPOSITION, "form-data; name=\"bin\"; filename=\"data.bin\""),
            binary);
        when(this.request.getParts()).thenReturn(List.of(field, textFile, binaryFile));

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);
        byte[] reconstructed = wrapper.getInputStream().readAllBytes();

        // Parse the reconstructed body with the very parser Jersey uses (MIMEPull) to prove the round trip works.
        try (MIMEMessage message = new MIMEMessage(new ByteArrayInputStream(reconstructed), BOUNDARY)) {
            message.parseAll();
            List<MIMEPart> parts = message.getAttachments();
            assertEquals(3, parts.size());

            assertEquals(List.of("form-data; name=\"field1\""), parts.get(0).getHeader(CONTENT_DISPOSITION));
            assertArrayEquals("value1".getBytes(StandardCharsets.UTF_8), parts.get(0).read().readAllBytes());

            // The Latin-1 filename must survive because we serialize headers with the same charset MIMEPull decodes.
            assertEquals(List.of("form-data; name=\"file\"; filename=\"caf\u00e9.txt\""),
                parts.get(1).getHeader(CONTENT_DISPOSITION));
            assertEquals(List.of("text/plain"), parts.get(1).getHeader("Content-Type"));
            assertArrayEquals("file content".getBytes(StandardCharsets.UTF_8), parts.get(1).read().readAllBytes());

            // The binary body must be delivered byte-for-byte.
            assertEquals(List.of("form-data; name=\"bin\"; filename=\"data.bin\""),
                parts.get(2).getHeader(CONTENT_DISPOSITION));
            assertArrayEquals(binary, parts.get(2).read().readAllBytes());
        }
    }

    @Test
    void getInputStreamIsBuiltOnlyOnce() throws Exception
    {
        Part part = mockPart(headers(CONTENT_DISPOSITION, "form-data; name=\"x\""), "hello".getBytes());
        when(this.request.getContentType()).thenReturn(MULTIPART_CONTENT_TYPE);
        when(this.request.getInputStream()).thenReturn(emptyStream());
        when(this.request.getParts()).thenReturn(List.of(part));

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);

        assertEquals(wrapper.getInputStream(), wrapper.getInputStream());
        // The original stream is peeked exactly once and the parts fetched exactly once.
        verify(this.request).getInputStream();
        verify(this.request).getParts();
    }

    @Test
    void getContentLengthReportsReconstructedLengthWhenMultipartBodyIsRestored() throws Exception
    {
        Part part = mockPart(headers(CONTENT_DISPOSITION, "form-data; name=\"x\""), "hello".getBytes());
        when(this.request.getContentType()).thenReturn(MULTIPART_CONTENT_TYPE);
        when(this.request.getInputStream()).thenReturn(emptyStream());
        when(this.request.getParts()).thenReturn(List.of(part));

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);
        int bodyLength = wrapper.getInputStream().readAllBytes().length;

        // The reported length must match the reconstructed body, not the (stale) original Content-Length header.
        assertEquals(bodyLength, wrapper.getContentLengthLong());
        assertEquals(bodyLength, wrapper.getContentLength());
        verify(this.request, never()).getContentLengthLong();
    }

    @Test
    void getContentLengthDelegatesToOriginalWhenStreamNotConsumed() throws Exception
    {
        byte[] originalData = ("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.ISO_8859_1);
        when(this.request.getInputStream()).thenReturn(servletInputStream(originalData));
        when(this.request.getContentLengthLong()).thenReturn((long) originalData.length);

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);

        assertEquals(originalData.length, wrapper.getContentLengthLong());
        assertEquals(originalData.length, wrapper.getContentLength());
    }

    @Test
    void getInputStreamFallsBackToOriginalWhenBoundaryIsMissing() throws Exception
    {
        when(this.request.getContentType()).thenReturn("multipart/form-data");
        when(this.request.getInputStream()).thenReturn(emptyStream());

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);

        assertEquals(-1, wrapper.getInputStream().read());
        // Without a boundary we cannot reconstruct anything, so the parts must not even be queried.
        verify(this.request, never()).getParts();
    }

    @Test
    void getInputStreamFallsBackToOriginalWhenNoPartIsCached() throws Exception
    {
        when(this.request.getContentType()).thenReturn(MULTIPART_CONTENT_TYPE);
        when(this.request.getInputStream()).thenReturn(emptyStream());
        when(this.request.getParts()).thenReturn(List.of());

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);

        assertEquals(-1, wrapper.getInputStream().read());
    }

    @Test
    void multipartReconstructionSupportsQuotedBoundary() throws Exception
    {
        Part part = mockPart(headers(CONTENT_DISPOSITION, "form-data; name=\"x\""), "data".getBytes());
        when(this.request.getContentType()).thenReturn("multipart/form-data; boundary=\"" + BOUNDARY + "\"");
        when(this.request.getInputStream()).thenReturn(emptyStream());
        when(this.request.getParts()).thenReturn(List.of(part));

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);
        byte[] reconstructed = wrapper.getInputStream().readAllBytes();

        try (MIMEMessage message = new MIMEMessage(new ByteArrayInputStream(reconstructed), BOUNDARY)) {
            message.parseAll();
            assertEquals(1, message.getAttachments().size());
        }
    }

    // -- application/x-www-form-urlencoded ------------------------------------------------------------------------

    @Test
    void reconstructsConsumedUrlEncodedBodyExcludingQueryParameters() throws Exception
    {
        when(this.request.getContentType()).thenReturn(FORM_CONTENT_TYPE);
        when(this.request.getInputStream()).thenReturn(emptyStream());
        when(this.request.getCharacterEncoding()).thenReturn("UTF-8");
        when(this.request.getQueryString()).thenReturn("method=PUT&shared=fromQuery");

        // getParameterMap() merges query and body parameters: only the body ones must end up in the restored body.
        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("method", new String[] {"PUT"});                     // query only -> excluded
        parameters.put("title", new String[] {"Hello World & Co"});          // body only -> kept (special chars)
        parameters.put("shared", new String[] {"fromQuery", "fromBody"});    // query + body -> only body kept
        when(this.request.getParameterMap()).thenReturn(parameters);

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);
        byte[] body = wrapper.getInputStream().readAllBytes();

        Map<String, List<String>> form = parseForm(new String(body, StandardCharsets.UTF_8));
        assertEquals(Map.of("title", List.of("Hello World & Co"), "shared", List.of("fromBody")), form);
        // Length is reported from the reconstructed body, not from the (stale) original Content-Length header.
        assertEquals(body.length, wrapper.getContentLengthLong());
    }

    @Test
    void urlEncodedBodyLeftUntouchedWhenNotConsumed() throws Exception
    {
        byte[] originalData = "title=Hello&tags=a".getBytes(StandardCharsets.UTF_8);
        when(this.request.getInputStream()).thenReturn(servletInputStream(originalData));

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);

        assertArrayEquals(originalData, wrapper.getInputStream().readAllBytes());
        // The body was available, so the parameters must not be used to rebuild it.
        verify(this.request, never()).getParameterMap();
    }

    // -- shared -----------------------------------------------------------------------------------------------------

    @Test
    void buildFailureIsRememberedSoTheStreamIsNeverNull() throws Exception
    {
        IOException failure = new IOException("cannot read the body");
        when(this.request.getInputStream()).thenThrow(failure);
        when(this.request.getContentLengthLong()).thenReturn(42L);

        ConsumedBodyRestoringRequestWrapper wrapper = new ConsumedBodyRestoringRequestWrapper(this.request);

        // Content length cannot be inspected, so it falls back to what the wrapped request reports...
        assertEquals(42L, wrapper.getContentLengthLong());
        // ...and the failure is surfaced (rather than a null stream) when the input stream is finally requested.
        assertEquals(failure, assertThrows(IOException.class, wrapper::getInputStream));
    }

    // -- helpers --

    private static Map<String, String> headers(String... namesAndValues)
    {
        // A LinkedHashMap keeps the declared header order, which is what we assert is preserved.
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            headers.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return headers;
    }

    private static Part mockPart(Map<String, String> headers, byte[] body) throws Exception
    {
        Part part = mock(Part.class);
        when(part.getHeaderNames()).thenReturn(headers.keySet());
        headers.forEach((name, value) -> when(part.getHeaders(name)).thenReturn(List.of(value)));
        when(part.getInputStream()).thenReturn(new ByteArrayInputStream(body));
        when(part.getSize()).thenReturn((long) body.length);
        return part;
    }

    private static Map<String, List<String>> parseForm(String body)
    {
        Map<String, List<String>> form = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            int index = pair.indexOf('=');
            String name = URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
            form.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        }
        return form;
    }

    private static ServletInputStream emptyStream()
    {
        return servletInputStream(new byte[0]);
    }

    private static ServletInputStream servletInputStream(byte[] data)
    {
        return new ServletInputStream()
        {
            private final InputStream delegate = new ByteArrayInputStream(data);

            @Override
            public int read() throws IOException
            {
                return this.delegate.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException
            {
                return this.delegate.read(b, off, len);
            }

            @Override
            public boolean isFinished()
            {
                return false;
            }

            @Override
            public boolean isReady()
            {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener)
            {
                // Not needed: this test stub is only ever read synchronously (blocking mode), never asynchronously.
            }
        };
    }
}
