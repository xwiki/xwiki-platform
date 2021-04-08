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
package org.xwiki.test.docker.internal.junit5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.integration.maven.ArtifactCoordinate;
import org.xwiki.test.integration.maven.ArtifactResolver;
import org.xwiki.test.integration.maven.MavenResolver;

/**
 * Finds all the extensions in the current pom (i.e. in the {@code ./pom.xml} in the current directory) that are not
 * part of the distribution and installs each of them as an extension inside a running XWiki. Also installs XAR
 * extensions found in the distribution and install them (since they have not been installed in {@code WEB-INF/lib}).
 *
 * @version $Id$
 * @since 10.9
 */
public class ExtensionInstaller
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionInstaller.class);

    private static final String XAR = "xar";

    private static final String JAR = "jar";

    private static final String DEPENDENCIES_SYSTEM_PROPERTY = System.getProperty("xwiki.test.ui.dependencies");

    private EmbeddableComponentManager ecm;

    private ArtifactResolver artifactResolver;

    private MavenResolver mavenResolver;

    private TestConfiguration testConfiguration;

    private MavenTimestampVersionConverter mavenVersionConverter;

    /**
     * Initialize the Component Manager which is later needed to perform the REST calls.
     *
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     */
    public ExtensionInstaller(TestConfiguration testConfiguration, ArtifactResolver artifactResolver,
        MavenResolver mavenResolver)
    {
        this.artifactResolver = artifactResolver;
        this.mavenResolver = mavenResolver;
        this.testConfiguration = testConfiguration;

        // Initialize XWiki Component system
        EmbeddableComponentManager cm = new EmbeddableComponentManager();
        cm.initialize(Thread.currentThread().getContextClassLoader());
        this.ecm = cm;

        this.mavenVersionConverter = new MavenTimestampVersionConverter();
    }

    /**
     * Install all the extensions in the current pom (i.e. in the {@code ./pom.xml} in the current directory) that are
     * not part of the distribution and installs each of them as an extension inside a running XWiki. Also installs XAR
     * extensions found in the distribution and install them (since they have not been installed in {@code
     * WEB-INF/lib}).
     *
     * @param xwikiRESTURL the XWiki REST URL (e.g. {@code http://localhost:8080/xwiki/rest})
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
     * Install all the extensions in the current pom (i.e. in the {@code ./pom.xml} in the current directory) that are
     * not part of the distribution and installs each of them as an extension inside a running XWiki. Also installs XAR
     * extensions found in the distribution and install them (since they have not been installed in {@code
     * WEB-INF/lib}).
     *
     * @param xwikiRESTURL the XWiki REST URL (e.g. {@code http://localhost:8080/xwiki/rest})
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
        Set<ExtensionId> extensions = new LinkedHashSet<>();
        String xwikiVersion = this.mavenResolver.getPlatformVersion();

        // Step 1: Get XAR extensions from the distribution (ie the mandatory ones), since they're not been installed
        // in WEB-INF/lib.
        List<Artifact> extraArtifacts = this.mavenResolver.convertToArtifacts(this.testConfiguration.getExtraJARs(),
            this.testConfiguration.isResolveExtraJARs());
        this.mavenResolver.addCloverJAR(extraArtifacts);
        Collection<ArtifactResult> distributionArtifactResults =
            this.artifactResolver.getDistributionDependencies(xwikiVersion, extraArtifacts);
        List<ExtensionId> distributionExtensionIds = new ArrayList<>();
        for (ArtifactResult artifactResult : distributionArtifactResults) {
            Artifact artifact = artifactResult.getArtifact();
            ExtensionId extensionId = convertToExtensionId(artifact);
            distributionExtensionIds.add(extensionId);
            if (artifact.getExtension().equalsIgnoreCase(XAR)) {
                extensions.add(extensionId);
            }
        }

        // Step 2: Get the project extensions to provision either from the DEPENDENCIES_SYSTEM_PROPERTY passed as
        // System properties by the Maven Surefire or Failsafe plugins. If not defined, then read the dependencies from
        // the current POM and only take the ones not having a "test" scope and being of type "xar" or "jar".
        // Note that the use case for defining the system property is for the cases when you don't want to draw
        // dependencies in your POM (can be useful when you want to test your extension on a vesion of XWiki for which
        // it wasn't developed for).
        extensions.addAll(getProjectExtensionIds(distributionExtensionIds));

        installExtensions(extensions, xwikiRESTURL, credentials, installUserReference, namespaces);
    }

    private Collection<ExtensionId> getProjectExtensionIds(List<ExtensionId> distributionExtensionIds) throws Exception
    {
        Set<ExtensionId> extensions = new LinkedHashSet<>();
        if (DEPENDENCIES_SYSTEM_PROPERTY != null) {
            for (String coordinate : DEPENDENCIES_SYSTEM_PROPERTY.split(",")) {
                ArtifactCoordinate artifactCoordinate = ArtifactCoordinate.parseArtifacts(coordinate);
                Artifact artifact = artifactCoordinate.toArtifact(
                    this.mavenResolver.getModelFromCurrentPOM().getVersion());
                ExtensionId extensionId = convertToExtensionId(artifact);
                if (!distributionExtensionIds.contains(extensionId)) {
                    extensions.add(extensionId);
                }
            }
        } else {
            Model model = this.mavenResolver.getModelFromCurrentPOM();
            for (Dependency dependency : model.getDependencies()) {
                Artifact artifact = this.mavenResolver.convertToArtifact(dependency);
                if (!"test".equals(dependency.getScope()) && isSupportedExtensionType(dependency.getType())) {
                    ExtensionId extensionId = convertToExtensionId(artifact);
                    if (!distributionExtensionIds.contains(extensionId)) {
                        extensions.add(extensionId);
                    }
                }
            }
        }
        return extensions;
    }

    private ExtensionId convertToExtensionId(Artifact artifact)
    {
        // Convert XXX-<DATE>.<HOUR>-<ID> into XXX-SNAPSHOT to avoid EM resolution conflicts such as:
        // Caused by: java.lang.Exception: Job execution failed. Response status code [500], reason
        // [The job failed with error [InstallException: Extension feature
        // [org.xwiki.platform:xwiki-platform-tree-macro/10.11-20181128.193513-21] is incompatible with existing
        // constraint [[10.11-SNAPSHOT]]]]
        return new ExtensionId(String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId()),
            this.mavenVersionConverter.convert(artifact.getVersion()));
    }

    private boolean isSupportedExtensionType(String type)
    {
        return XAR.equals(type) || JAR.equals(type);
    }

    private void installExtensions(Collection<ExtensionId> extensions, String xwikiRESTURL,
        UsernamePasswordCredentials credentials, String installUserReference, List<String> namespaces) throws Exception
    {
        try {
            installExtensions(extensions, xwikiRESTURL, installUserReference, namespaces, credentials);
        } catch (Exception e) {
            throw new Exception(String.format("Failed to install Extension(s) into XWiki at [%s]", xwikiRESTURL), e);
        }
    }

    private void installExtensions(Collection<ExtensionId> extensions, String xwikiRESTURL, String installUserReference,
        List<String> namespaces, UsernamePasswordCredentials credentials) throws Exception
    {
        InstallRequest installRequest = new InstallRequest();

        // Set a job id to save the job result
        installRequest.setId("extension", "provision", UUID.randomUUID().toString());

        installRequest.setInteractive(false);

        // Set the extension list to install
        for (ExtensionId extensionId : extensions) {
            LOGGER.info("...Adding extension [{}] to the list of extensions to provision...", extensionId);
            installRequest.addExtension(extensionId);
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
