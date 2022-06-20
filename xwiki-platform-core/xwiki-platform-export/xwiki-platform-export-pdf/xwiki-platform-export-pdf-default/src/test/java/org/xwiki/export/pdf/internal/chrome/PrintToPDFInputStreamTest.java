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
package org.xwiki.export.pdf.internal.chrome;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.jodconverter.core.util.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.test.junit5.mockito.ComponentTest;

import com.github.kklisura.cdt.protocol.commands.IO;
import com.github.kklisura.cdt.protocol.types.io.Read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PrintToPDFInputStream}.
 * 
 * @version $Id$
 */
@ComponentTest
class PrintToPDFInputStreamTest
{
    @Mock
    private IO io;

    @Mock
    private Runnable closeCallback;

    @Test
    void read() throws IOException
    {
        assertRead("The quick brown fox jumps over the laz\u00FF dog.", 5, false);
    }

    @Test
    void readBase64Encoded() throws IOException
    {
        assertRead("The quick brown fox jumps over the laz\u00FF dog.", 8, true);
    }

    private void assertRead(String content, int bufferSize, boolean useBase64Encoding) throws IOException
    {
        String streamHandle = "test";
        setUpStream(streamHandle, content, bufferSize, useBase64Encoding);

        PrintToPDFInputStream inputStream =
            new PrintToPDFInputStream(this.io, streamHandle, this.closeCallback, bufferSize);
        assertEquals(content, IOUtils.toString(inputStream, StandardCharsets.UTF_8));

        verify(this.closeCallback).run();
    }

    private void setUpStream(String streamHandle, String content, int bufferSize, boolean useBase64Encoding)
    {
        List<Read> reads = getReads(content, bufferSize, useBase64Encoding);
        if (reads.size() > 1) {
            when(this.io.read(streamHandle, null, bufferSize)).thenReturn(reads.get(0),
                reads.subList(1, reads.size()).toArray(new Read[] {}));
        } else if (reads.size() == 1) {
            when(this.io.read(streamHandle, null, bufferSize)).thenReturn(reads.get(0));
        }
    }

    private List<Read> getReads(String content, int bufferSize, boolean useBase64Encoding)
    {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (useBase64Encoding) {
            bytes = Base64.getEncoder().encode(bytes);
        }
        int count = bytes.length / bufferSize;
        if (bytes.length % bufferSize != 0) {
            count++;
        }
        List<Read> reads = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Read read = new Read();
            read.setBase64Encoded(useBase64Encoding);
            read.setEof(i == count - 1);
            int offset = i * bufferSize;
            read.setData(
                new String(bytes, offset, Math.min(bufferSize, bytes.length - offset), StandardCharsets.UTF_8));
            reads.add(read);
        }
        return reads;
    }
}
