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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultTemporaryResourceStore}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultTemporaryResourceStoreTest
{
    @InjectMockComponents
    private DefaultTemporaryResourceStore store;

    @MockComponent
    private Environment environment;

    // Use the module's "target" directory as the temporary directory so that no file leaks outside the build
    // workspace.
    @TempDir
    private File temporaryDirectory;

    @AfterComponent
    void beforeEach()
    {
        when(this.environment.getTemporaryDirectory()).thenReturn(this.temporaryDirectory);
    }

    @Test
    void getTemporaryFile() throws IOException
    {
        File expected = new File(this.temporaryDirectory,
            "tmp/moduleid/c/4/53919da9e226032c090ffc08d7506d/resource/file.txt");
        assertEquals(expected, this.store.getTemporaryFile(new TemporaryResourceReference("moduleid",
            List.of("resource/file.txt"), new DocumentReference("xwiki", "Test", "Test"))));
    }

    @Test
    void getTemporaryFileWithPathTraversal()
    {
        // Make sure it's not possible to read a file outside the root temporary directory.
        // This is a security check to prevent path traversal attacks.
        assertThrows(IOException.class, () -> this.store.getTemporaryFile(new TemporaryResourceReference("moduleid",
            List.of("../../../../../../../../../../../etc/passwd"), new DocumentReference("xwiki", "Test", "Test"))));
    }
}
