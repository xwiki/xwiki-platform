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
package org.xwiki.tool.importmap;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser.JAVASCRIPT_IMPORTMAP_PROPERTY;

/**
 * Unit tests for {@link ImportmapMojo}.
 *
 * @version $Id$
 */
class ImportmapMojoTest
{
    @TempDir
    File tempDir;

    @Test
    void executeWithTimestampedSnapshotVersion() throws Exception
    {
        Artifact mockArtifact = mock();
        String artifactId = "my-artifact";
        String baseVersion = "18.4.0-SNAPSHOT";
        // A remote snapshot artifact has a timestamped version instead of the plain SNAPSHOT suffix.
        String timestampedVersion = "18.4.0-20231001.120000-1";
        String path = "dist/main.js";

        // Create a JAR whose internal path uses the base (SNAPSHOT) version, as produced by the build.
        File jarFile = new File(this.tempDir, "my-artifact.jar");
        String pathInJar = "META-INF/resources/webjars/%s/%s/%s".formatted(artifactId, baseVersion, path);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jarFile))) {
            zip.putNextEntry(new ZipEntry(pathInJar));
            zip.closeEntry();
        }

        when(mockArtifact.getGroupId()).thenReturn("org.test");
        when(mockArtifact.getArtifactId()).thenReturn(artifactId);
        when(mockArtifact.getVersion()).thenReturn(timestampedVersion);
        when(mockArtifact.getBaseVersion()).thenReturn(baseVersion);
        when(mockArtifact.getFile()).thenReturn(jarFile);

        Properties properties = new Properties();
        properties.setProperty(JAVASCRIPT_IMPORTMAP_PROPERTY,
            "{\"my-module/\": \"org.test:%s/%s\"}".formatted(artifactId, path));
        Model model = new Model();
        model.setProperties(properties);
        model.setGroupId("org.other");
        model.setArtifactId("other-artifact");

        MavenProject project = new MavenProject(model);
        project.setArtifacts(Set.of(mockArtifact));

        ImportmapMojo mojo = new ImportmapMojo();
        Log logMock = mock(Log.class);
        mojo.setLog(logMock);
        Field projectField = ImportmapMojo.class.getDeclaredField("project");
        projectField.set(mojo, project);

        assertDoesNotThrow(mojo::execute);
        verify(logMock).debug("Checking key [my-module/] for webjar reference "
            + "[ImportmapPathDescriptor[descriptor=WebjarPathDescriptor[webjarId=org.test:my-artifact, namespace=null, "
            + "path=dist/main.js, params={}], eager=false, anonymous=false]]");
    }
}
