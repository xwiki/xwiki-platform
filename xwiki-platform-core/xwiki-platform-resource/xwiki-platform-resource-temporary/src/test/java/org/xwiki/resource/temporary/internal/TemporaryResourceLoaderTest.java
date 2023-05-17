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
package org.xwiki.resource.temporary.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemporaryResourceLoader}.
 *
 * @version $Id$
 */
@ComponentTest
class TemporaryResourceLoaderTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @InjectMockComponents
    private TemporaryResourceLoader resourceLoader;

    @MockComponent
    private TemporaryResourceStore temporaryResourceStore;

    @XWikiTempDir
    private File tmpDir;

    @Test
    void load() throws Exception
    {
        TemporaryResourceReference resourceReference = new TemporaryResourceReference("id", "name",
            new DocumentReference("wiki", "space", "page"));
        File contentFile = new File(this.tmpDir, "test");
        FileUtils.writeStringToFile(contentFile, "content", "UTF-8");

        when(this.temporaryResourceStore.getTemporaryFile(resourceReference)).thenReturn(contentFile);

        InputStream is = this.resourceLoader.load(resourceReference);
        assertEquals("content", IOUtils.toString(is, "UTF-8"));
    }

    @Test
    void loadWhenException() throws Exception
    {
        TemporaryResourceReference resourceReference = new TemporaryResourceReference("id", "name",
            new DocumentReference("wiki", "space", "page"));

        when(this.temporaryResourceStore.getTemporaryFile(resourceReference)).thenThrow(new IOException("error"));

        assertNull(this.resourceLoader.load(resourceReference));

        assertEquals("Failed to get the temporary resource's content for [type = [tmp], parameters = [], "
                + "reference = [wiki:space.page], action = [], locale = [<null>], anchor = [<null>]]",
            this.logCapture.getMessage(0));
    }
}
