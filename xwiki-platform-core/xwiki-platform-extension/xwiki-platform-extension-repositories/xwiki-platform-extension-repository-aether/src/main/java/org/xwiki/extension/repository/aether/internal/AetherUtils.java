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
package org.xwiki.extension.repository.aether.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;

public final class AetherUtils
{
    private static final Pattern PARSER_ID = Pattern.compile("([^: ]+):([^: ]+)(:([^: ]+))?");

    static public DefaultArtifact createArtifact(String id, String version) throws ResolveException
    {
        Matcher matcher = PARSER_ID.matcher(id);
        if (!matcher.matches()) {
            throw new ResolveException("Bad id " + id + ", expected format is <groupId>:<artifactId>[:<classifier>]");
        }

        return new DefaultArtifact(matcher.group(1), matcher.group(2), StringUtils.defaultString(matcher.group(4), ""),
            "jar", version);
    }

    static public ExtensionId createExtensionId(Artifact artifact)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(artifact.getGroupId());
        builder.append(':');
        builder.append(artifact.getArtifactId());
        if (StringUtils.isNotEmpty(artifact.getClassifier())) {
            builder.append(':');
            builder.append(artifact.getClassifier());
        }

        return new ExtensionId(builder.toString(), artifact.getVersion());
    }

}
