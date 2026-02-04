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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapException;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser;
import org.xwiki.webjars.WebjarPathDescriptor;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VERIFY;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;
import static org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser.JAVASCRIPT_IMPORTMAP_PROPERTY;

/**
 * Verify if the {@link JavascriptImportmapParser#JAVASCRIPT_IMPORTMAP_PROPERTY} property is well-formed.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Mojo(name = "verify", defaultPhase = VERIFY, requiresDependencyResolution = RUNTIME, threadSafe = true)
public class ImportmapMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Disables the plugin execution.
     */
    @Parameter(property = "importmap.verify.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException
    {
        if (this.skip) {
            return;
        }
        var model = this.project.getModel();
        var property = model.getProperties().getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY);
        if (property == null) {
            return;
        }
        try {
            var importMap = new JavascriptImportmapParser().parse(property);
            var dependencies = this.project.getArtifacts();
            for (Map.Entry<String, WebjarPathDescriptor> entry : importMap.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                var webjarId = value.webjarId().split(":", 2);
                var groupIdWebjar = webjarId[0];
                var artifactIdWebjar = webjarId[1];
                var path = value.path();
                getLog().debug("Checking key [%s] for webjar reference [%s]".formatted(key, value));
                var isSelf =
                    areDependenciesEquals(model.getGroupId(), model.getArtifactId(), groupIdWebjar, artifactIdWebjar);
                if (isSelf) {
                    if (!checkPathInSelf(this.project, computeFullPathInJar(artifactIdWebjar,
                        this.project.getVersion(), path)))
                    {
                        throw new MojoExecutionException(
                            "Unable to find path [%s] in the current module".formatted(path));
                    }
                    continue;
                }
                boolean dependencyNotFound = true;
                for (var dependency : dependencies) {
                    if (areDependenciesEquals(dependency.getGroupId(), dependency.getArtifactId(),
                        groupIdWebjar, artifactIdWebjar))
                    {
                        dependencyNotFound = false;
                        checkIfPathExistsInDependency(dependency, artifactIdWebjar, path);
                        break;
                    }
                }
                if (dependencyNotFound) {
                    throw new MojoExecutionException(
                        "Unable to find a declared dependency for [%s]".formatted(value.webjarId()));
                }
            }
        } catch (JavascriptImportmapException e) {
            throw new MojoExecutionException(
                "Failed to parse the [%s] property".formatted(JAVASCRIPT_IMPORTMAP_PROPERTY), e);
        }
    }

    private static boolean areDependenciesEquals(String groupId0, String artifactId0, String groupId1,
        String artifactId1)
    {
        return Objects.equals(groupId0, groupId1) && Objects.equals(artifactId0, artifactId1);
    }

    private void checkIfPathExistsInDependency(Artifact artifact, String artifactId, String path)
        throws MojoExecutionException
    {
        var jar = resolveDependencyJar(artifact, this.project);
        if (jar == null) {
            throw new MojoExecutionException("Unable to resolve jar for dependency [%s]".formatted(artifact));
        }
        try {
            if (!checkPathInJar(jar, computeFullPathInJar(artifactId, artifact.getVersion(), path))) {
                throw new MojoExecutionException("Unable to find path [%s] in jar [%s]".formatted(path, jar));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to open jar [%s] for dependency [%s]".formatted(path, artifact),
                e);
        }
    }

    private String computeFullPathInJar(String artifactId, String version, String path)
    {
        return "META-INF/resources/webjars/%s/%s/%s".formatted(artifactId, version, path);
    }

    private File resolveDependencyJar(Artifact artifact, MavenProject project)
    {
        // Loop over project artifacts.
        for (var projectArtifact : project.getArtifacts()) {
            if (Objects.equals(projectArtifact.getGroupId(), artifact.getGroupId())
                && Objects.equals(projectArtifact.getArtifactId(), artifact.getArtifactId())
                && Objects.equals(projectArtifact.getVersion(), artifact.getVersion()))
            {
                return projectArtifact.getFile();
            }
        }
        return null;
    }

    private boolean checkPathInJar(File jarFile, String path) throws IOException
    {
        try (JarFile jar = new JarFile(jarFile)) {
            ZipEntry entry = jar.getEntry(path);
            return entry != null;
        }
    }

    private boolean checkPathInSelf(MavenProject project, String pathToCheck)
    {
        return Files.exists(
            Paths.get(new File(project.getBuild().getOutputDirectory()).getAbsolutePath(), pathToCheck));
    }
}
