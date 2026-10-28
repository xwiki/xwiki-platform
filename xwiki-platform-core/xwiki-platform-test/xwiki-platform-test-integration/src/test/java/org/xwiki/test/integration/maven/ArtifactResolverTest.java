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

import java.util.List;
import java.util.Set;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ArtifactResolver}.
 *
 * @version $Id$
 */
class ArtifactResolverTest
{
    private static final String PLATFORM_GROUPID = "org.xwiki.platform";

    @Test
    void getDistributionDependenciesAppliesRootExclusionsToDependentArtifacts() throws Exception
    {
        RepositorySystem system = mock();
        RepositoryResolver repositoryResolver = mock();
        when(repositoryResolver.getSystem()).thenReturn(system);

        // The root distribution artifact excludes a JAR brought by the dependent artifacts, one of the dependent
        // artifacts itself, and all the artifacts of a group.
        Exclusion oldcoreExclusion = new Exclusion(PLATFORM_GROUPID, "xwiki-platform-oldcore", "*", "*");
        Exclusion xarHandlerExclusion =
            new Exclusion(PLATFORM_GROUPID, "xwiki-platform-extension-handler-xar", "*", "*");
        Exclusion contribExclusion = new Exclusion("org.xwiki.contrib", "*", "*", "*");
        ArtifactDescriptorResult descriptorResult = new ArtifactDescriptorResult(new ArtifactDescriptorRequest());
        descriptorResult.addDependency(new Dependency(
            new DefaultArtifact("org.xwiki.platform:xwiki-platform-distribution-war-dependencies:pom:1.0"), "runtime",
            false, List.of(oldcoreExclusion, xarHandlerExclusion, contribExclusion)));
        when(system.readArtifactDescriptor(any(), any())).thenReturn(descriptorResult);

        CollectResult collectResult = new CollectResult(new CollectRequest());
        collectResult.setRoot(new DefaultDependencyNode((Dependency) null));
        when(system.collectDependencies(any(), any())).thenReturn(collectResult);
        when(system.resolveDependencies(any(), any())).thenReturn(new DependencyResult(new DependencyRequest()));

        new ArtifactResolver(false, false, repositoryResolver).getDistributionDependencies("1.0", "1.0",
            List.of(new DefaultArtifact("org.xwiki.contrib:extension:1.0")),
            "xwiki-platform-distribution-war-legacydependencies");

        ArgumentCaptor<CollectRequest> collectRequestCaptor = ArgumentCaptor.forClass(CollectRequest.class);
        verify(system).collectDependencies(any(), collectRequestCaptor.capture());
        List<Dependency> dependencies = collectRequestCaptor.getValue().getDependencies();

        // The dependent artifacts excluded by the root distribution artifact are not added.
        assertTrue(dependencies.stream().map(dependency -> dependency.getArtifact().getArtifactId())
            .noneMatch(artifactId -> artifactId.equals("xwiki-platform-extension-handler-xar")
                || artifactId.equals("extension")));
        // The other ones are added, with the exclusions of the root distribution artifact.
        Dependency jarHandler = dependencies.stream()
            .filter(
                dependency -> dependency.getArtifact().getArtifactId().equals("xwiki-platform-extension-handler-jar"))
            .findFirst().orElseThrow();
        assertEquals(Set.of(oldcoreExclusion, xarHandlerExclusion, contribExclusion),
            Set.copyOf(jarHandler.getExclusions()));
    }
}
