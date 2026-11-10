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
package com.xpn.xwiki.tool.backup;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AbstractImportMojo}.
 *
 * @version $Id$
 */
class AbstractImportMojoTest
{
    private static Artifact artifact(String artifactId, String type, String classifier)
    {
        return new DefaultArtifact("org.xwiki.test", artifactId, "1.0", Artifact.SCOPE_RUNTIME, type, classifier,
            new DefaultArtifactHandler(type));
    }

    private static Dependency dependency(String artifactId, String type, String classifier)
    {
        Dependency dependency = new Dependency();
        dependency.setGroupId("org.xwiki.test");
        dependency.setArtifactId(artifactId);
        // The version declared in the POM is not always the resolved one (e.g. a version range)
        dependency.setVersion("[1.0,)");
        dependency.setType(type);
        dependency.setClassifier(classifier);

        return dependency;
    }

    @Test
    void isDirectDependency()
    {
        List<Dependency> dependencies = List.of(dependency("direct", "xar", null), dependency("tests", "jar", "tests"));

        assertTrue(AbstractImportMojo.isDirectDependency(artifact("direct", "xar", null), dependencies));
        assertTrue(AbstractImportMojo.isDirectDependency(artifact("direct", "xar", ""), dependencies));
        assertTrue(AbstractImportMojo.isDirectDependency(artifact("tests", "jar", "tests"), dependencies));

        // Transitive dependency
        assertFalse(AbstractImportMojo.isDirectDependency(artifact("transitive", "xar", null), dependencies));
        // Same coordinates but another type or classifier
        assertFalse(AbstractImportMojo.isDirectDependency(artifact("direct", "jar", null), dependencies));
        assertFalse(AbstractImportMojo.isDirectDependency(artifact("tests", "jar", null), dependencies));
    }
}
