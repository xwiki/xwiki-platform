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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;

/**
 * Finds all the XARs in the current pom (i.e. in the {@code ./pom.xml} in the current directory) and installs each of
 * them as an extension inside a running XWiki.
 *
 * @version $Id$
 * @since 10.9
 */
public class ExtensionInstaller
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionInstaller.class);

    private static final String XAR = "xar";

    private EmbeddableComponentManager ecm;

    private ArtifactResolver artifactResolver;

    private MavenResolver mavenResolver;

    /**
     * Initialize the Component Manager which is later needed to perform the REST calls.
     *
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     */
    public ExtensionInstaller(ArtifactResolver artifactResolver, MavenResolver mavenResolver)
    {
        this.artifactResolver = artifactResolver;
        this.mavenResolver = mavenResolver;

        // Initialize XWiki Component system
        EmbeddableComponentManager cm = new EmbeddableComponentManager();
        cm.initialize(Thread.currentThread().getContextClassLoader());
        this.ecm = cm;
    }

    /**
     * Install all XAR extensions found in the {@code pom.xml} located in the current directory (ie the POM of the test
     * module).
     *
     * @param xwikiRESTURL the XWiki REST URL (e.g. {@code http://localhsot:8080/xwiki/rest})
     * @param username the xwiki user to use to connect for the REST endpoint (e.g. {@code superadmin})
     * @param password the xwiki password to connect for the REST endpoint
     * @param installUserReference the reference to the user who will the user under which pages are installed (e.g.
     * {@code superadmin})
     * @throws Exception if there's a failure to install the extensions in the running XWiki instance
     */
    public void installExtensions(String xwikiRESTURL, String username, String password, String installUserReference)
        throws Exception
    {
        installExtensions(xwikiRESTURL, new UsernamePasswordCredentials(username, password), installUserReference,
            null);
    }

    /**
     * Install all XAR extensions found in the {@code pom.xml} located in the current directory (ie the POM of the test
     * module).
     *
     * @param xwikiRESTURL the XWiki REST URL (e.g. {@code http://localhsot:8080/xwiki/rest})
     * @param credentials the xwiki user and password to use to connect for the REST endpoint
     * @param installUserReference the reference to the user who will the user under which pages are installed (e.g.
     * {@code superadmin})
     * @param namespaces the wikis in which to install the extensions (e.g. {@code wiki:xwiki} for the main wiki). If
     * null they'll be installed in the main wiki
     * @throws Exception if there's a failure to install the extensions in the running XWiki instance
     */
    public void installExtensions(String xwikiRESTURL, UsernamePasswordCredentials credentials,
        String installUserReference, List<String> namespaces) throws Exception
    {
        Set<Artifact> extensions = new LinkedHashSet<>();
        String xwikiVersion = this.mavenResolver.getPlatformVersion();

        // Step 1: Get XAR extensions from the distribution (ie the mandatory ones)
        Collection<ArtifactResult> artifactResults =
            this.artifactResolver.getDistributionDependencies(xwikiVersion, Collections.emptyList());
        for (ArtifactResult artifactResult : artifactResults) {
            Artifact artifact = artifactResult.getArtifact();
            if (artifact.getExtension().equalsIgnoreCase(XAR)) {
                extensions.add(artifact);
            }
        }

        // Step 2: Get extensions from the current POM
        Model model = this.mavenResolver.getModelFromCurrentPOM();
        for (Dependency dependency : model.getDependencies()) {
            if (dependency.getType().equalsIgnoreCase(XAR)) {
                Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                    dependency.getClassifier(), dependency.getType(), dependency.getVersion());
                extensions.add(artifact);
            }
        }

        installExtensions(extensions, xwikiVersion, xwikiRESTURL, credentials, installUserReference, namespaces);
    }

    private void installExtensions(Collection<Artifact> extensions, String xwikiVersion, String xwikiRESTURL,
        UsernamePasswordCredentials credentials, String installUserReference, List<String> namespaces) throws Exception
    {
        try {
            installExtensions(extensions, xwikiVersion, xwikiRESTURL, installUserReference, namespaces, credentials);
        } catch (Exception e) {
            throw new Exception(String.format("Failed to install Extension(s) into XWiki at [%s]", xwikiRESTURL), e);
        }
    }

    private void installExtensions(Collection<Artifact> extensions, String xwikiVersion, String xwikiRESTURL,
        String installUserReference, List<String> namespaces, UsernamePasswordCredentials credentials) throws Exception
    {
        InstallRequest installRequest = new InstallRequest();

        // Set a job id to save the job result
        installRequest.setId("extension", "provision", UUID.randomUUID().toString());

        installRequest.setInteractive(false);

        // Set the extension list to install
        for (Artifact artifact : extensions) {
            org.xwiki.extension.ExtensionId extId = new org.xwiki.extension.ExtensionId(
                String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId()), xwikiVersion);
            LOGGER.info(String.format("...Adding extension [%s] to the list of extensions to provision...", extId));
            installRequest.addExtension(extId);
        }

        // Set the namespaces into which to install the extensions
        if (namespaces == null || namespaces.isEmpty()) {
            installRequest.addNamespace("wiki:xwiki");
        } else {
            for (String namespace : namespaces) {
                installRequest.addNamespace(namespace);
            }
        }

        // Set any user for installing pages (if defined)
        if (installUserReference != null) {
            installRequest.setProperty("user.reference", new DocumentReference("xwiki", "XWiki", "superadmin"));
        }

        JobExecutor jobExecutor = new JobExecutor();
        JobRequest request = getModelFactory().toRestJobRequest(installRequest);
        jobExecutor.execute(request, xwikiRESTURL, credentials);
    }

    private ModelFactory getModelFactory() throws Exception
    {
        return this.ecm.getInstance(ModelFactory.class);
    }
}
