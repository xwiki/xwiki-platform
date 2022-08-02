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
package org.xwiki.test.docker.junit5;

import org.junit.jupiter.api.Test;
import org.xwiki.test.integration.maven.ArtifactCoordinate;

import static junit.framework.TestCase.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ArtifactCoordinate}.
 *
 * @version $Id$
 */
public class ArtifactCoordinateTest
{
    @Test
    public void parseWhenNoVersionNoType()
    {
        ArtifactCoordinate artifactCoordinate = ArtifactCoordinate.parseArtifacts("groupId:artifactId");
        assertEquals("groupId", artifactCoordinate.getGroupId());
        assertEquals("artifactId", artifactCoordinate.getArtifactId());
        assertEquals("jar", artifactCoordinate.getType());
        assertNull(artifactCoordinate.getVersion());
    }

    @Test
    public void parseWhenVersionAndNoType()
    {
        ArtifactCoordinate artifactCoordinate = ArtifactCoordinate.parseArtifacts("groupId:artifactId:version");
        assertEquals("groupId", artifactCoordinate.getGroupId());
        assertEquals("artifactId", artifactCoordinate.getArtifactId());
        assertEquals("jar", artifactCoordinate.getType());
        assertEquals("version", artifactCoordinate.getVersion());
    }

    @Test
    public void parseWhenVersionAndType()
    {
        ArtifactCoordinate artifactCoordinate = ArtifactCoordinate.parseArtifacts("groupId:artifactId:type:version");
        assertEquals("groupId", artifactCoordinate.getGroupId());
        assertEquals("artifactId", artifactCoordinate.getArtifactId());
        assertEquals("type", artifactCoordinate.getType());
        assertEquals("version", artifactCoordinate.getVersion());
    }
}
