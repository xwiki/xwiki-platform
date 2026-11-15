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
package org.xwiki.filter.xar.internal.output;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.output.StringWriterOutputTarget;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARFilter;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validate {@link XAROutputFilterStream}.
 *
 * @version $Id$
 */
@ComponentTest
@AllComponents
class XAROutputFilterStreamTest
{
    /**
     * Larger than several chunks of the output stream and not a multiple of the chunk size.
     */
    private static final int CONTENT_SIZE = 3 * 4095 + 855;

    private static final Pattern CONTENT_PATTERN = Pattern.compile("<content(?:>([^<]*)</content>|/>)");

    private static final Pattern SIZE_PATTERN = Pattern.compile("<filesize>([^<]*)</filesize>");

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    /**
     * An input stream returning at most a few bytes per read, like a stream reading from the network does before
     * reaching its end.
     */
    private static class ShortReadInputStream extends FilterInputStream
    {
        private static final int MAX_READ = 100;

        ShortReadInputStream(byte[] content)
        {
            super(new ByteArrayInputStream(content));
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            return super.read(b, off, Math.min(len, MAX_READ));
        }
    }

    private static byte[] randomContent(int size)
    {
        byte[] content = new byte[size];
        new Random(size).nextBytes(content);
        return content;
    }

    private String writeDocument(InputStream attachmentContent, InputStream revisionContent, long size)
        throws Exception
    {
        StringWriterOutputTarget target = new StringWriterOutputTarget();
        XAROutputProperties properties = new XAROutputProperties();
        properties.setTarget(target);

        BeanOutputFilterStreamFactory<XAROutputProperties> factory = this.componentManager
            .getInstance(OutputFilterStreamFactory.class, FilterStreamType.XWIKI_XAR_CURRENT.serialize());
        try (OutputFilterStream outputFilterStream = factory.createOutputFilterStream(properties)) {
            XARFilter filter = (XARFilter) outputFilterStream.getFilter();

            filter.beginWikiSpace("Space", FilterEventParameters.EMPTY);
            filter.beginWikiDocument("Page", FilterEventParameters.EMPTY);
            filter.beginWikiDocumentLocale(Locale.ROOT, FilterEventParameters.EMPTY);
            filter.beginWikiDocumentAttachment("file.bin", new DefaultInputStreamInputSource(attachmentContent), size,
                FilterEventParameters.EMPTY);
            if (revisionContent != null) {
                filter.beginWikiAttachmentRevisions(FilterEventParameters.EMPTY);
                filter.beginWikiAttachmentRevision("1.1", new DefaultInputStreamInputSource(revisionContent), size,
                    FilterEventParameters.EMPTY);
                filter.endWikiAttachmentRevision("1.1", null, size, FilterEventParameters.EMPTY);
                filter.endWikiAttachmentRevisions(FilterEventParameters.EMPTY);
            }
            filter.endWikiDocumentAttachment("file.bin", null, size, FilterEventParameters.EMPTY);
            filter.endWikiDocumentLocale(Locale.ROOT, FilterEventParameters.EMPTY);
            filter.endWikiDocument("Page", FilterEventParameters.EMPTY);
            filter.endWikiSpace("Space", FilterEventParameters.EMPTY);
        }

        return target.getBuffer().toString();
    }

    private String convertXAR(String xml) throws Exception
    {
        XARInputProperties inputProperties = new XARInputProperties();
        inputProperties.setSource(new StringInputSource(xml));
        BeanInputFilterStreamFactory<XARInputProperties> inputFactory = this.componentManager
            .getInstance(InputFilterStreamFactory.class, FilterStreamType.XWIKI_XAR_CURRENT.serialize());

        StringWriterOutputTarget target = new StringWriterOutputTarget();
        XAROutputProperties outputProperties = new XAROutputProperties();
        outputProperties.setTarget(target);
        BeanOutputFilterStreamFactory<XAROutputProperties> outputFactory = this.componentManager
            .getInstance(OutputFilterStreamFactory.class, FilterStreamType.XWIKI_XAR_CURRENT.serialize());

        try (InputFilterStream inputFilterStream = inputFactory.createInputFilterStream(inputProperties);
            OutputFilterStream outputFilterStream = outputFactory.createOutputFilterStream(outputProperties)) {
            inputFilterStream.read(outputFilterStream.getFilter());
        }

        return target.getBuffer().toString();
    }

    private static List<String> findAll(Pattern pattern, String xml)
    {
        List<String> values = new ArrayList<>();
        Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            values.add(Objects.toString(matcher.group(1), ""));
        }
        return values;
    }

    private static void assertContents(byte[] expected, int count, String xml)
    {
        List<String> contents = findAll(CONTENT_PATTERN, xml);
        assertEquals(count, contents.size());
        for (String content : contents) {
            assertArrayEquals(expected, Base64.getMimeDecoder().decode(content));
        }
        assertEquals(Collections.nCopies(count, String.valueOf(expected.length)), findAll(SIZE_PATTERN, xml));
    }

    @Test
    void writeAttachmentFromStreamWithShortReads() throws Exception
    {
        byte[] content = randomContent(CONTENT_SIZE);

        String xml = writeDocument(new ShortReadInputStream(content), new ShortReadInputStream(content),
            content.length);

        assertContents(content, 2, xml);
        // Same output as with a stream filling each read.
        assertEquals(writeDocument(new ByteArrayInputStream(content), new ByteArrayInputStream(content),
            content.length), xml);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 4095, 2 * 4095, 3 * 4095 + 855 })
    void writeAttachmentFromStreamWithShortReadsAroundChunkBoundaries(int size) throws Exception
    {
        byte[] content = randomContent(size);

        String xml = writeDocument(new ShortReadInputStream(content), null, size);

        assertContents(content, 1, xml);
        assertEquals(writeDocument(new ByteArrayInputStream(content), null, size), xml);
    }

    @Test
    void convertXARWithLargeAttachment() throws Exception
    {
        // The XAR input provides the attachment content as a stream decoding the Base64 content of the XML.
        byte[] content = randomContent(CONTENT_SIZE);
        String xml = writeDocument(new ByteArrayInputStream(content), new ByteArrayInputStream(content),
            content.length);

        assertContents(content, 2, convertXAR(xml));
    }

    @Test
    void writeAttachmentWithStreamFailingAfterShortReads()
    {
        // The error comes after several short reads, like a connection dropped while reading the content.
        InputStream failing = new ShortReadInputStream(randomContent(CONTENT_SIZE))
        {
            private int delivered;

            @Override
            public int read(byte[] b, int off, int len) throws IOException
            {
                if (this.delivered >= 1000) {
                    throw new IOException("Read error");
                }
                int read = super.read(b, off, len);
                this.delivered += read;
                return read;
            }
        };

        FilterException exception = assertThrows(FilterException.class, () -> writeDocument(failing, null, 1));
        assertEquals("Failed to read content stream", exception.getCause().getMessage());
        assertInstanceOf(IOException.class, exception.getCause().getCause());
    }
}
