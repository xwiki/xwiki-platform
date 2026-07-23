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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.extension.RestExtensionInstaller;
import org.xwiki.test.integration.maven.ArtifactCoordinate;
import org.xwiki.test.integration.maven.ArtifactResolver;
import org.xwiki.test.integration.maven.MavenResolver;

import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.getComponentManager;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.getTestConfiguration;

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
    private static final String XAR = "xar";

    private static final String JAR = "jar";

    private static final String PLATFORM_GROUPID = "org.xwiki.platform";

    private static final String DEPENDENCIES_SYSTEM_PROPERTY = System.getProperty("xwiki.test.ui.dependencies");

    /**
     * Matches the Extension Manager error raised when a provisioned extension resolves a dependency whose version
     * differs from the one bundled in the test WAR as a core extension. The two capturing groups are the resolved
     * dependency (e.g. {@code org.bouncycastle:bcpkix-jdk18on-1.85}) and the conflicting core extension feature
     * (e.g. {@code org.bouncycastle:bcpkix-jdk18on/1.84}).
     */
    private static final Pattern CORE_EXTENSION_CONFLICT_PATTERN = Pattern.compile(
        "Dependency \\[([^\\]]+)] is not compatible with core extension feature \\[([^\\]]+)]");

    /**
     * Turns the opaque core extension version conflict into an actionable explanation. The raw failure is a job status
     * code with a deeply-nested reason and gives no hint that the cause is a version mismatch between the WAR (built
     * from the local Maven repository) and the extensions resolved by the running server (which also resolves from the
     * remote repositories). The two placeholders are the resolved dependency and the conflicting core extension.
     */
    private static final String CORE_EXTENSION_CONFLICT_DIAGNOSTIC =
        "A provisioned extension requires dependency [%s], which is incompatible with the core extension [%s] bundled "
            + "in the test WAR. In the Docker test framework the WAR is built from your local Maven repository while "
            + "the running server also resolves extensions from the remote repositories, so a version available on "
            + "one side but not the other triggers this conflict. To fix it, use any of: (1) run the test offline by "
            + "adding [-o] to the Maven command so the server resolves extensions only from your local repository, "
            + "matching the WAR; (2) refresh your local SNAPSHOTs with [mvn -U] on XWiki Commons and Platform then "
            + "rebuild; (3) if the newer version is a legitimate release, wait for it to be adopted everywhere so all "
            + "versions realign.";

    private final ExtensionContext context;

    private ArtifactResolver artifactResolver;

    private MavenResolver mavenResolver;

    private TestConfiguration testConfiguration;

    private MavenTimestampVersionConverter mavenVersionConverter;

    private RestExtensionInstaller restExtensionInstaller;

    /**
     * Initialize the Component Manager which is later needed to perform the REST calls.
     *
     * @param context the context of the test
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     */
    public ExtensionInstaller(ExtensionContext context, ArtifactResolver artifactResolver, MavenResolver mavenResolver)
    {
        this.context = context;
        this.artifactResolver = artifactResolver;
        this.mavenResolver = mavenResolver;
        this.testConfiguration = getTestConfiguration(context);
        this.mavenVersionConverter = new MavenTimestampVersionConverter();
        this.restExtensionInstaller = new RestExtensionInstaller(getComponentManager(context), this.mavenResolver);
    }

    /**
     * Install all the extensions in the current pom (i.e. in the {@code ./pom.xml} in the current directory) that are
     * not part of the distribution and installs each of them as an extension inside a running XWiki. Also installs XAR
     * extensions found in the distribution and install them (since they have not been installed in {@code
     * WEB-INF/lib}).
     *
     * @param username the xwiki user to use to connect for the REST endpoint (e.g. {@code superadmin})
     * @param password the xwiki password to connect for the REST endpoint
     * @param installUserReference the reference to the user who will the user under which pages are installed (e.g.
     * {@code superadmin})
     * @throws Exception if there's a failure to install the extensions in the running XWiki instance
     */
    public void installExtensions(String username, String password, String installUserReference) throws Exception
    {
        installExtensions(new UsernamePasswordCredentials(username, password), installUserReference, null);
    }

    /**
     * Install all the extensions in the current pom (i.e. in the {@code ./pom.xml} in the current directory) that are
     * not part of the distribution and installs each of them as an extension inside a running XWiki. Also installs XAR
     * extensions found in the distribution and install them (since they have not been installed in {@code
     * WEB-INF/lib}).
     *
     * @param credentials the xwiki user and password to use to connect for the REST endpoint
     * @param installUserReference the reference to the user who will the user under which pages are installed (e.g.
     * {@code superadmin})
     * @param namespaces the wikis in which to install the extensions (e.g. {@code wiki:xwiki} for the main wiki). If
     * null they'll be installed in the main wiki
     * @throws Exception if there's a failure to install the extensions in the running XWiki instance
     */
    public void installExtensions(UsernamePasswordCredentials credentials, String installUserReference,
        List<String> namespaces) throws Exception
    {
        Set<ExtensionId> extensions = new LinkedHashSet<>();
        String commonsVersion = this.mavenResolver.getCommonsVersion();
        String platformVersion = this.mavenResolver.getPlatformVersion();

        // Step 1: Get XAR extensions from the distribution (ie the mandatory ones), since they're not been installed
        // in WEB-INF/lib.
        List<Artifact> extraArtifacts = this.mavenResolver.convertToArtifacts(this.testConfiguration.getExtraJARs(),
            this.testConfiguration.isResolveExtraJARs());
        this.mavenResolver.addCloverJAR(extraArtifacts);
        // Use the same dependencies root as the one used to build the WAR (see WARBuilder). This is required so that
        // the set of extensions considered as already part of the distribution (i.e. bundled in WEB-INF/lib and thus
        // not to be re-provisioned) matches what the WAR actually contains. In standardFlavor mode this avoids
        // re-installing as extensions the core extensions that are already bundled in the WAR.
        Collection<ArtifactResult> distributionArtifactResults =
            this.artifactResolver.getDistributionDependencies(commonsVersion, platformVersion, extraArtifacts,
                this.testConfiguration.getWARDependenciesRootArtifactId());
        List<ExtensionId> distributionExtensionIds = new ArrayList<>();
        for (ArtifactResult artifactResult : distributionArtifactResults) {
            Artifact artifact = artifactResult.getArtifact();
            ExtensionId extensionId = convertToExtensionId(artifact);
            distributionExtensionIds.add(extensionId);
            // Auto-provision the distribution's mandatory XAR extensions (they're not bundled in WEB-INF/lib), but
            // ONLY in minimal mode. In standardFlavor mode the distribution dependencies pull in
            // xwiki-platform-distribution-ui-base only to bundle its JARs in WEB-INF/lib, and that drags in its whole
            // transitive XAR closure (the entire standard UI). We must not auto-provision those here: they are
            // installed through the flavor (see Step 3), exactly as a real XWiki instance installs the flavor via its
            // Distribution Wizard. XWiki's Extension Manager then resolves the flavor tree server-side and skips the
            // JAR core extensions already bundled in WEB-INF/lib.
            if (!this.testConfiguration.isStandardFlavor() && artifact.getExtension().equalsIgnoreCase(XAR)) {
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

        // Step 3: In standardFlavor mode, install the standard flavor automatically (as on a fresh standard
        // distribution), so the test module doesn't need to declare it as a dependency. The flavor is restricted to
        // the main wiki (wiki:xwiki) by the flavor extension itself. Installing it makes XWiki's Extension Manager
        // resolve and install the whole standard UI server-side while skipping the JAR core extensions already
        // bundled in WEB-INF/lib (see Step 1).
        if (this.testConfiguration.isStandardFlavor()) {
            Artifact flavorArtifact = new DefaultArtifact(PLATFORM_GROUPID,
                "xwiki-platform-distribution-flavor-mainwiki", XAR, platformVersion);
            extensions.add(convertToExtensionId(flavorArtifact));
        }

        installExtensions(extensions, credentials, installUserReference, namespaces, true);
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

    /**
     * @param extensions the extensions to install
     * @param credentials the xwiki user and password to use to connect for the REST endpoint
     * @param installUserReference the reference to the user who will the user under which pages are installed (e.g.
     *            {@code superadmin})
     * @param namespaces the wikis in which to install the extensions (e.g. {@code wiki:xwiki} for the main wiki). If
     *            null they'll be installed in the main wiki
     * @param failOnExist true if the install should fail if one of the extension is already install on one of the
     *            namespaces
     * @throws Exception if there's a failure to install the extensions in the running XWiki instance
     */
    public void installExtensions(Collection<ExtensionId> extensions, UsernamePasswordCredentials credentials,
        String installUserReference, List<String> namespaces, boolean failOnExist) throws Exception
    {
        try {
            this.restExtensionInstaller.installExtensions(
                DockerTestUtils.getCurrentXWikiExecutor(this.context).getHttpClientBaseURL(), extensions, credentials,
                installUserReference, namespaces, failOnExist);
        } catch (Exception e) {
            // Translate the opaque core extension version conflict into an actionable explanation.
            String diagnostic = getCoreExtensionConflictDiagnostic(e);
            if (diagnostic != null) {
                throw new Exception(diagnostic, e);
            }
            throw e;
        }
    }

    /**
     * Detects the "not compatible with core extension feature" failure anywhere in the exception chain and turns it
     * into an actionable explanation (see {@link #CORE_EXTENSION_CONFLICT_DIAGNOSTIC}).
     *
     * @param throwable the failure raised while provisioning the extensions
     * @return the actionable explanation, or {@code null} if the failure is not a core extension version conflict
     */
    static String getCoreExtensionConflictDiagnostic(Throwable throwable)
    {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null) {
                Matcher matcher = CORE_EXTENSION_CONFLICT_PATTERN.matcher(message);
                if (matcher.find()) {
                    return String.format(CORE_EXTENSION_CONFLICT_DIAGNOSTIC, matcher.group(1), matcher.group(2));
                }
            }
        }
        return null;
    }
}
