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
package org.xwiki.test.docker.internal.junit5;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.utility.DockerImageName;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DurationImagePullPolicy}.
 *
 * @version $Id$
 */
@ExtendWith({XWikiTempDirExtension.class})
public class DurationImagePullPolicyTest
{
    @XWikiTempDir
    private File tmpDir;

    @Test
    void shouldPull() throws Exception
    {
        Path path = new File(this.tmpDir, "duration").toPath();
        DurationImagePullPolicy policy = new DurationImagePullPolicy(500L);
        policy.setPath(path);

        // Make sure the file doesn't exist
        Files.deleteIfExists(path);

        // No record of pulling that image exist, thus we should pull
        assertTrue(policy.shouldPull(DockerImageName.parse("something:latest")));
        // Also verify that the file has been created
        assertTrue(Files.exists(path));
        try (Stream<String> lines = Files.lines(path)) {
            List<String> filteredLines = lines
                .filter(s -> s.startsWith("something:latest|"))
                .toList();
            assertEquals(1, filteredLines.size());
        }

        // Try again, we shouldn't pull since it's not been 500ms yet.
        assertFalse(policy.shouldPull(DockerImageName.parse("something:latest")));

        // Wait 500ms to be sure we should pull again
        Thread.sleep(500L);
        assertTrue(policy.shouldPull(DockerImageName.parse("something:latest")));

        // Try with a different image name and make sure the file contains the new name
        assertTrue(policy.shouldPull(DockerImageName.parse("something2:1.0")));
        try (Stream<String> lines = Files.lines(path)) {
            List<String> filteredLines = lines
                .filter(s -> s.startsWith("something:latest|") || s.startsWith("something2:1.0"))
                .toList();
            assertEquals(2, filteredLines.size());
        }
    }
}
