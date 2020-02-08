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
package org.xwiki.test.integration.maven;

import org.eclipse.aether.artifact.Artifact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests fr {@link ArtifactCoordinate}.
 *
 * @version $Id$
 */
public class ArtifactCoordinateTest
{
    @Test
    void parseArtifacts()
    {
        ArtifactCoordinate coordinate = ArtifactCoordinate.parseArtifacts("groupid:artifactid:type:version");
        assertEquals("groupid", coordinate.getGroupId());
        assertEquals("artifactid", coordinate.getArtifactId());
        assertEquals("version", coordinate.getVersion());
        assertEquals("type", coordinate.getType());

        coordinate = ArtifactCoordinate.parseArtifacts("groupid:artifactid:version");
        assertEquals("groupid", coordinate.getGroupId());
        assertEquals("artifactid", coordinate.getArtifactId());
        assertEquals("version", coordinate.getVersion());
        assertEquals("jar", coordinate.getType());

        coordinate = ArtifactCoordinate.parseArtifacts("groupid:artifactid");
        assertEquals("groupid", coordinate.getGroupId());
        assertEquals("artifactid", coordinate.getArtifactId());
        assertNull(coordinate.getVersion());
        assertEquals("jar", coordinate.getType());
    }

    @Test
    void parseArtifactsWhenInvalid()
    {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            ArtifactCoordinate.parseArtifacts("invalid");
        });
        assertEquals("Bad artifact coordinates [invalid]", exception.getMessage());
    }

    @Test
    void toArtifacts()
    {
        ArtifactCoordinate coordinate = ArtifactCoordinate.parseArtifacts("groupid:artifactid:type:version1");
        Artifact artifact = coordinate.toArtifact("version2");

        assertEquals("groupid", artifact.getGroupId());
        assertEquals("artifactid", artifact.getArtifactId());
        assertEquals("version1", artifact.getVersion());

        coordinate = ArtifactCoordinate.parseArtifacts("groupid:artifactid");
        artifact = coordinate.toArtifact("version");

        assertEquals("groupid", artifact.getGroupId());
        assertEquals("artifactid", artifact.getArtifactId());
        assertEquals("version", artifact.getVersion());
    }
}