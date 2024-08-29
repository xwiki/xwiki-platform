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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link OfficeConverterFileStorage}.
 *
 * @version $Id$
 */
@ExtendWith(XWikiTempDirExtension.class)
class OfficeConverterFileStorageStorageTest
{
    @XWikiTempDir
    private File tmpDir;

    @Test
    void invalidCharacters() throws IOException
    {
        OfficeConverterFileStorage storage =
            new OfficeConverterFileStorage(this.tmpDir, "{/in\\§ä.ext", "{/out\\$ä.out");

        assertEquals("{_in_§a.ext", storage.getInputFile().getName());
        assertEquals("{_out_$a.out", storage.getOutputFile().getName());
        assertEquals(storage.getInputDir(), storage.getInputFile().getParentFile());
        assertEquals(storage.getOutputDir(), storage.getOutputFile().getParentFile());

        assertTrue(storage.getInputDir().isDirectory());
        assertTrue(storage.getOutputDir().isDirectory());
    }

    @Test
    void longFilename() throws IOException
    {
        String longName = " ".repeat(300) + ".ext";
        OfficeConverterFileStorage storage = new OfficeConverterFileStorage(this.tmpDir, longName, longName);

        String expectedName = " ".repeat(251) + ".ext";
        assertEquals(expectedName, storage.getInputFile().getName());
        assertEquals(expectedName, storage.getOutputFile().getName());
    }

    @Test
    void fallback() throws IOException
    {
        OfficeConverterFileStorage storage = new OfficeConverterFileStorage(this.tmpDir, "", "");

        String fallback = "fallback";
        assertEquals(fallback, storage.getInputFile().getName());
        assertEquals(fallback, storage.getOutputFile().getName());
    }
}
