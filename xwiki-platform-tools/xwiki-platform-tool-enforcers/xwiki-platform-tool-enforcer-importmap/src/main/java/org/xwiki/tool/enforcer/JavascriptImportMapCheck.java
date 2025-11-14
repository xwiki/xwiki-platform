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
package org.xwiki.tool.enforcer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapException;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser;
import org.xwiki.webjars.WebjarPathDescriptor;

import static org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser.JAVASCRIPT_IMPORTMAP_PROPERTY;

/**
 * Verify if the {@link JavascriptImportmapParser#JAVASCRIPT_IMPORTMAP_PROPERTY} property is well-formed.
 *
 * @version $Id$
 * @since 18.0.0RC1
 */
@Named("javascriptImportMapCheck")
public class JavascriptImportMapCheck extends AbstractPomCheck
{
    @Override
    public void execute() throws EnforcerRuleException
    {
        Model model = getResolvedModel();
        var property = model.getProperties().getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY);
        if (property == null) {
            return;
        }
        try {
            var importMap = new JavascriptImportmapParser().parse(property);
            List<Dependency> dependencies = model.getDependencies();
            for (Map.Entry<String, WebjarPathDescriptor> entry : importMap.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                var webjarId = value.webjarId().split(":", 2);
                var groupIdWebjar = webjarId[0];
                var artifactIdWebjar = webjarId[1];
                getLog().debug("Checking key [%s] for webjar reference [%s]".formatted(key, value));
                var isSelf =
                    areDependenciesEquals(model.getGroupId(), model.getArtifactId(), groupIdWebjar, artifactIdWebjar);
                if (isSelf) {
                    continue;
                }
                boolean dependencyNotFound = true;
                for (Dependency dependency : dependencies) {
                    if (areDependenciesEquals(dependency.getGroupId(), dependency.getArtifactId(),
                        groupIdWebjar, artifactIdWebjar))
                    {
                        dependencyNotFound = false;
                        checkIfPathExistsInDependency(dependency, artifactIdWebjar, value.path());
                        break;
                    }
                }
                if (dependencyNotFound) {
                    throw new EnforcerRuleException(
                        "Unable to find a declared dependency for [%s]".formatted(value.webjarId()));
                }
            }
        } catch (JavascriptImportmapException e) {
            throw new EnforcerRuleException(
                "Failed to parse the [%s] property".formatted(JAVASCRIPT_IMPORTMAP_PROPERTY), e);
        }
    }

    private static boolean areDependenciesEquals(String groupId0, String artifactId0, String groupId1,
        String artifactId1)
    {
        return Objects.equals(groupId0, groupId1) && Objects.equals(artifactId0, artifactId1);
    }

    private void checkIfPathExistsInDependency(Dependency dependency, String artifactId, String path)
        throws EnforcerRuleException
    {
        var jar = resolveDependencyJar(dependency, this.project);
        if (jar == null) {
            throw new EnforcerRuleException("Unable to resolve jar for dependency [%s]".formatted(dependency));
        }
        try {
            if (!checkPathInJar(jar, computeFullPathInJar(artifactId, dependency.getVersion(), path))) {
                throw new EnforcerRuleException("Unable to find path [%s] in jar [%s]".formatted(path, jar));
            }
        } catch (IOException e) {
            throw new EnforcerRuleException("Failed to open jar [%s] for dependency [%s]".formatted(path, dependency),
                e);
        }
    }

    private String computeFullPathInJar(String artifactId, String version, String path)
    {
        return "META-INF/resources/webjars/%s/%s/%s".formatted(artifactId, version, path);
    }

    private File resolveDependencyJar(Dependency dependency, MavenProject project)
    {
        // Loop over project artifacts.
        for (Artifact artifact : project.getArtifacts()) {
            if (Objects.equals(artifact.getGroupId(), dependency.getGroupId())
                && Objects.equals(artifact.getArtifactId(), dependency.getArtifactId())
                && Objects.equals(artifact.getVersion(), dependency.getVersion()))
            {
                return artifact.getFile();
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
}
