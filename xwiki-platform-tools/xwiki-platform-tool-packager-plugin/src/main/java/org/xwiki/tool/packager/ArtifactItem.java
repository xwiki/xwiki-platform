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
package org.xwiki.tool.packager;

import org.codehaus.plexus.util.StringUtils;

/**
 * Represents an Artifact Item in the plugin configuration.
 *
 * @version $Id$
 * @since 3.4M1
 */
public class ArtifactItem
{
    /**
     * Group Id of Artifact.
     *
     * @parameter
     * @required
     */
    private String groupId;

    /**
     * Name of Artifact.
     *
     * @parameter
     * @required
     */
    private String artifactId;

    /**
     * Version of Artifact.
     *
     * @parameter
     */
    private String version;

    /**
     * Type of Artifact (War, Jar, etc).
     *
     * @parameter
     * @required
     */
    private String type = "zip";

    /**
     * Classifier for Artifact (tests,sources,etc).
     *
     * @parameter
     */
    private String classifier;

    /**
     * @return the artifact id
     */
    public String getArtifactId()
    {
        return this.artifactId;
    }

    /**
     * @param artifactId the artifactId to set
     */
    public void setArtifactId(String artifactId)
    {
        this.artifactId = filterEmptyString(artifactId);
    }

    /**
     * @return the groupId
     */
    public String getGroupId()
    {
        return this.groupId;
    }

    /**
     * @param groupId the groupId to set
     */
    public void setGroupId(String groupId)
    {
        this.groupId = filterEmptyString(groupId);
    }

    /**
     * @return the type
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type The type to set
     */
    public void setType(String type)
    {
        this.type = filterEmptyString(type);
    }

    /**
     * @return Returns the version.
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version)
    {
        this.version = filterEmptyString(version);
    }

    /**
     * @return the artifact classifier
     */
    public String getClassifier()
    {
        return this.classifier;
    }

    /**
     * @param classifier the artifat classifier
     */
    public void setClassifier(String classifier)
    {
        this.classifier = filterEmptyString(classifier);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(this.groupId);
        builder.append(':');
        builder.append(this.artifactId);
        builder.append(':');

        if (this.classifier != null) {
            builder.append(this.classifier);
            builder.append(':');
        }

        builder.append(StringUtils.defaultString(this.version, "?"));
        builder.append(':');
        builder.append(this.type);

        return builder.toString();
    }

    /**
     * Convert empty string into <code>null</code>.
     *
     * @param in the value to filter
     * @return null the passer value or null if it's empty
     */
    private String filterEmptyString(String in)
    {
        if ("".equals(in)) {
            return null;
        }

        return in;
    }
}
