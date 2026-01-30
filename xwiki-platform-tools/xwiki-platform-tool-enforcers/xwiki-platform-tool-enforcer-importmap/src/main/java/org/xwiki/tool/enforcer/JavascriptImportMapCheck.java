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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapException;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser;
import org.xwiki.webjars.WebjarPathDescriptor;

import static java.util.Optional.ofNullable;
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
    private final RepositorySystem repositorySystem;

    private final MavenSession session;

    /**
     * Default constructor.
     *
     * @param repositorySystem the repository system, used to resolve transitive dependencies
     * @param session the current maven session, used to resolve transitive dependencies
     */
    @Inject
    public JavascriptImportMapCheck(RepositorySystem repositorySystem, MavenSession session)
    {
        this.repositorySystem = repositorySystem;
        this.session = session;
    }

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
            List<Dependency> dependencies = getProjectTransitiveDependencies();
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
                        throw new EnforcerRuleException(
                            "Unable to find path [%s] in the current module".formatted(path));
                    }
                    continue;
                }
                boolean dependencyNotFound = true;
                for (Dependency dependency : dependencies) {
                    var artifact = dependency.getArtifact();
                    if (areDependenciesEquals(artifact.getGroupId(), artifact.getArtifactId(),
                        groupIdWebjar, artifactIdWebjar))
                    {
                        dependencyNotFound = false;
                        checkIfPathExistsInDependency(dependency, artifactIdWebjar, path);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Dependency> getProjectTransitiveDependencies() throws EnforcerRuleException
    {
        try {
            DefaultRepositorySystemSession defaultRepositorySystemSession =
                new DefaultRepositorySystemSession(this.session.getRepositorySession());

            MavenProject mavenProject = this.session.getCurrentProject();
            ArtifactTypeRegistry artifactTypeRegistry = this.session.getRepositorySession().getArtifactTypeRegistry();

            List<Dependency> dependencies = mavenProject.getDependencies().stream()
                .filter(d -> !d.isOptional())
                .filter(d -> !Objects.equals("test", d.getScope()))
                .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                .toList();

            List<Dependency> managedDependencies =
                ofNullable(mavenProject.getDependencyManagement())
                    .map(DependencyManagement::getDependencies)
                    .map(list -> list.stream()
                        .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                        .toList())
                    .orElse(null);

            CollectRequest collectRequest =
                new CollectRequest(dependencies, managedDependencies, mavenProject.getRemoteProjectRepositories());
            collectRequest.setRootArtifact(RepositoryUtils.toArtifact(mavenProject.getArtifact()));

            var collectedDependencies =
                this.repositorySystem.collectDependencies(defaultRepositorySystemSession, collectRequest);

            var remaining = new LinkedBlockingQueue<>(List.of(collectedDependencies.getRoot()));
            List<Dependency> res = new ArrayList<>();

            while (!remaining.isEmpty()) {
                var current = remaining.poll();
                remaining.addAll(current.getChildren());
                Dependency dependency = current.getDependency();
                if (dependency != null) {
                    res.add(dependency);
                }
            }

            return res;
        } catch (DependencyCollectionException e) {
            throw new EnforcerRuleException("Could not build dependency tree " + e.getLocalizedMessage(), e);
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
            if (!checkPathInJar(jar, computeFullPathInJar(artifactId, dependency.getArtifact().getVersion(), path))) {
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
            var dependencyArtifact = dependency.getArtifact();
            if (Objects.equals(artifact.getGroupId(), dependencyArtifact.getGroupId())
                && Objects.equals(artifact.getArtifactId(), dependencyArtifact.getArtifactId())
                && Objects.equals(artifact.getVersion(), dependencyArtifact.getVersion()))
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

    private boolean checkPathInSelf(MavenProject project, String pathToCheck)
    {
        return Files.exists(
            Paths.get(new File(project.getBuild().getOutputDirectory()).getAbsolutePath(), pathToCheck));
    }
}
