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
package org.xwiki.officeimporter.internal.converter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.jodconverter.LocalConverter;
import org.jodconverter.job.ConversionJobWithOptionalSourceFormatUnspecified;
import org.jodconverter.job.ConversionJobWithOptionalTargetFormatUnspecified;
import org.jodconverter.office.OfficeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.officeimporter.converter.OfficeConverterException;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultOfficeConverter}.
 *
 * @version $Id$
 */
public class DefaultOfficeConverterTest
{
    private LocalConverter localConverter;

    @XWikiTempDir
    private File tmpDir;

    private DefaultOfficeConverter defaultOfficeConverter;

    @BeforeEach
    public void setup()
    {
        this.localConverter = mock(LocalConverter.class);
        this.defaultOfficeConverter = new DefaultOfficeConverter(localConverter, tmpDir);
    }

    @Test
    public void convert() throws IOException, OfficeException, OfficeConverterException
    {
        OfficeConverterException officeConverterException = assertThrows(OfficeConverterException.class,
            () -> this.defaultOfficeConverter.convert(Collections.emptyMap(), "myFile", "myOutputFile"));
        assertEquals("No input stream specified for main input file [myFile].", officeConverterException.getMessage());

        String content = "my content";

        Map<String, InputStream> inputStreamMap = Collections.singletonMap("myFile",
            new ByteArrayInputStream(content.getBytes()));

        ConversionJobWithOptionalSourceFormatUnspecified jobSource =
            mock(ConversionJobWithOptionalSourceFormatUnspecified.class);
        when(this.localConverter.convert(any(File.class))).thenReturn(jobSource);

        ConversionJobWithOptionalTargetFormatUnspecified jobTarget =
            mock(ConversionJobWithOptionalTargetFormatUnspecified.class);
        when(jobSource.to(any(File.class))).thenReturn(jobTarget);

        Map<String, byte[]> obtainedResult =
            this.defaultOfficeConverter.convert(inputStreamMap, "myFile", "myOutputFile");

        // We obtain an empty result since the list of files is null.
        // And we cannot guess name of files since it's generated with UUID. 
        assertTrue(obtainedResult.isEmpty());

        ArgumentCaptor<File> argument = ArgumentCaptor.forClass(File.class);
        verify(this.localConverter).convert(argument.capture());
        assertEquals("myFile", argument.getValue().getName());

        argument = ArgumentCaptor.forClass(File.class);
        verify(jobSource).to(argument.capture());
        assertEquals("myOutputFile", argument.getValue().getName());

        verify(jobTarget).execute();

    }
}
