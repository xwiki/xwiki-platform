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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Represents an artifact coordinate but with optional version (the {@link DefaultArtifact} implementation doesn't
 * support optional versions which is why we needed this class).
 *
 * @version $Id$
 * @since 11.2RC1
 */
public class ArtifactCoordinate
{
    private static final Pattern ARTIFACT_COORD_PATTERN =
        Pattern.compile("([^: ]+):([^: ]+)(?::([^: ]+))?(?::([^: ]+))?");

    private String groupId;

    private String artifactId;

    private String type;

    private String version;

    /**
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param type the type (e.g. "jar"), can be null
     * @param version the version, can be null
     */
    public ArtifactCoordinate(String groupId, String artifactId, String type, String version)
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        if (type == null) {
            this.type = "jar";
        } else {
            this.type = type;
        }
        this.version = version;
    }

    /**
     * @return the group id
     */
    public String getGroupId()
    {
        return this.groupId;
    }

    /**
     * @return the artifact id
     */
    public String getArtifactId()
    {
        return this.artifactId;
    }

    /**
     * @return the version, which can be null
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @return the type (e.g. "jar")
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param defaultVersion the default version is none is defined
     * @return the Artifact object
     */
    public Artifact toArtifact(String defaultVersion)
    {
        String newVersion = getVersion();
        if (newVersion == null) {
            newVersion = defaultVersion;
        }
        return new DefaultArtifact(getGroupId(), getArtifactId(), getType(), newVersion);
    }

    /**
     * Parse an Artifact oordinate as a String. Note that version is optional (as opposed to Aether's @link
     * DefaultArtifact} implementation
     *
     * @param coordinateAsString the coordinate to parse (e.g. {@code org.xwiki.contrib:myartifact} or {@code
     * org.xwiki.contrib:myartifact:jar:1.0-SNAPSHOT} or {@code org.xwiki.contrib:myartifact:1.0-SNAPSHOT})
     * @return the {@link ArtifactCoordinate} object representing the parsed string
     */
    public static ArtifactCoordinate parseArtifacts(String coordinateAsString)
    {
        Matcher matcher = ARTIFACT_COORD_PATTERN.matcher(coordinateAsString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Bad artifact coordinates [%s]", coordinateAsString));
        }
        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String type;
        String version;
        if (matcher.group(4) == null) {
            type = null;
            version = matcher.group(3);
        } else {
            type = matcher.group(3);
            version = matcher.group(4);
        }
        return new ArtifactCoordinate(groupId, artifactId, type, version);
    }
}
