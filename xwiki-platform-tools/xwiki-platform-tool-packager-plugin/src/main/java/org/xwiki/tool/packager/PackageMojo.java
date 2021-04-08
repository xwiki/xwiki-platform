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
package org.xwiki.tool.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.xwiki.tool.utils.AbstractOldCoreMojo;
import org.xwiki.tool.utils.LogUtils;

import com.xpn.xwiki.tool.backup.Importer;

/**
 * Create a runnable XWiki instance using Jetty as the Servlet Container and HSQLDB as the Database.
 *
 * @version $Id$
 * @since 3.4M1
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, requiresProject = true, threadSafe = true)
public class PackageMojo extends AbstractOldCoreMojo
{
    /**
     * The directory where to create the packaging.
     */
    @Parameter(defaultValue = "${project.build.directory}/xwiki", required = true)
    private File outputPackageDirectory;

    /**
     * The directory where classes are put.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File outputClassesDirectory;

    /**
     * * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected RepositorySystem repositorySystem;

    /**
     * Local repository to be used by the plugin to resolve dependencies.
     */
    @Parameter(property = "localRepository")
    protected ArtifactRepository localRepository;

    /**
     * List of remote repositories to be used by the plugin to resolve dependencies.
     */
    @Parameter(property = "project.remoteArtifactRepositories")
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * The user under which the import should be done. If not user is specified then we import with backup pack. For
     * example {@code superadmin}.
     */
    @Parameter
    private String importUser;

    /**
     * The platform version to be used by the packager plugin.
     */
    @Parameter(defaultValue = "${platform.version}")
    private String platformVersion;

    /**
     * The commons version to be used by the packager plugin.
     */
    @Parameter(defaultValue = "${commons.version}")
    private String commonsVersion;

    /**
     * List of skin artifacts to include in the packaging.
     */
    @Parameter
    private List<SkinArtifactItem> skinArtifactItems;

    /**
     * Maps each dependency of type WAR to a context path which will be used as the target directory when the WAR
     * artifact is extracted. WARs that share the same context path are merged. The order of the WAR artifacts in the
     * dependency list is important because the last one can overwrite files from the previous ones if they share the
     * same context path.
     */
    @Parameter
    private Map<String, String> contextPathMapping;

    /**
     * Indicate of the package mojo is used for tests. Among other things it means it's then possible to skip it using
     * skipTests system property.
     *
     * @since 6.0M2
     */
    @Parameter(defaultValue = "true")
    private boolean test;

    /**
     * The location of the hibernate configuration file.
     */
    @Parameter
    private File hibernateConfiguration;

    /**
     * Automatically drop ProgrammingRights when evaluating scripts in wiki pages, in order to make sure that by default
     * wiki pages don't require PR. Only active if {@link #test} is true and {@link #isSkipTests()} is false.
     * <p>
     * Also note that it's possible to exclude some pages from being tested by setting an XWiki property named
     * {@code test.prchecker.excludePattern} (e.g. {@code .*:XWiki\.DeletedDocuments}) in xwiki.properties.
     *
     * @since 9.8RC1
     */
    @Parameter(defaultValue = "true")
    private boolean testProgrammingRights;

    private File webappsDirectory;

    private File xwikiWebappDirectory;

    private File webInfDirectory;

    private File libDirectory;

    @Override
    protected void before() throws MojoExecutionException
    {
        // Setup folders
        this.webappsDirectory = new File(this.outputPackageDirectory, "webapps");
        this.xwikiWebappDirectory = new File(this.webappsDirectory, "xwiki");
        this.webInfDirectory = new File(this.xwikiWebappDirectory, "WEB-INF");
        this.libDirectory = new File(this.webInfDirectory, "lib");

        this.permanentDirectory = new File(this.outputPackageDirectory, "data");
        if (!this.hibernateConfig.exists()) {
            this.hibernateConfig = new File(this.webInfDirectory, "hibernate.cfg.xml");
        }

        // Generate and copy config files.
        getLog().info("Copying Configuration files ...");
        generateConfigurationFiles(webInfDirectory);

        super.before();
    }

    @Override
    public void executeInternal() throws MojoExecutionException
    {
        LogUtils.configureXWikiLogs();

        getLog().info("Using platform version: " + getXWikiPlatformVersion());

        // Step 1: Expand Jetty resources into the package output directory.
        getLog().info("Expanding Jetty Resources ...");
        expandJettyDistribution();

        // Step 2: Get the WAR dependencies and expand them in the package output directory.
        getLog().info("Expanding WAR dependencies ...");
        for (Artifact warArtifact : resolveWarArtifacts()) {
            getLog().info("  ... Unzipping WAR: " + warArtifact.getFile());
            File warDirectory = new File(this.webappsDirectory, getContextPath(warArtifact));
            unzip(warArtifact.getFile(), warDirectory);
            // Only generate the extension.xed descriptor for the distribution war
            if (warArtifact.getArtifactId().equals("xwiki-platform-web")) {
                generateDistributionXED(warDirectory, warArtifact);
            }
        }

        // Step 3: Copy all JARs dependencies to the expanded WAR directory in WEB-INF/lib
        getLog().info("Copying JAR dependencies ...");
        createDirectory(this.libDirectory);
        for (Artifact artifact : resolveJarArtifacts()) {
            installJAR(artifact, this.libDirectory);
        }

        // Step 4: Copy compiled classes in the WEB-INF/Classes directory. This allows the tests to provide custom
        // code, for example to override existing components for the test purpose. As an example the link
        // checker might want to override the HTTP Checker component so that checks are not done over the
        // internet since the tests need to execute in a stable environment to prevent false positives.
        getLog().info("Copying Java Classes ...");
        File classesDirectory = new File(webInfDirectory, "classes");
        if (this.outputClassesDirectory.exists()) {
            copyDirectory(this.outputClassesDirectory, classesDirectory);
        }

        // Step 6: Copy HSQLDB JDBC Driver
        getLog().info("Copying HSQLDB JDBC Driver JAR ...");
        Artifact hsqldbArtifact = resolveHSQLDBArtifact();
        installJAR(hsqldbArtifact, this.libDirectory);

        // Step 7: Unzip the specified Skins. If no skin is specified then unzip the Flamingo skin only.
        getLog().info("Copying Skins ...");
        File skinsDirectory = new File(this.xwikiWebappDirectory, "skins");
        for (Artifact skinArtifact : getSkinArtifacts()) {
            unzip(skinArtifact.getFile(), skinsDirectory);
        }

        // Step 8: Import specified XAR files into the database
        getLog().info(String.format("Import XAR dependencies %s...",
            this.importUser == null ? "as a backup pack" : "using user [" + this.importUser + "]"));
        importXARs();
    }

    protected void installJAR(Artifact artifact, File libDirectory) throws MojoExecutionException
    {
        // Copy JAR file
        getLog().info("  ... Copying JAR: " + artifact.getFile());
        copyFile(artifact.getFile(), libDirectory);

        // Generate XED file
        this.extensionHelper.serializeExtension(artifact, libDirectory);
    }

    @Override
    protected boolean isSkipExecution()
    {
        return super.isSkipExecution() || isSkipTests();
    }

    private boolean isSkipTests()
    {
        if (this.test) {
            String property = System.getProperty("skipTests");

            return property != null && Boolean.valueOf(property);
        } else {
            return false;
        }
    }

    private void expandJettyDistribution() throws MojoExecutionException
    {
        Artifact jettyArtifact = resolveJettyArtifact();
        unzip(jettyArtifact.getFile(), this.outputPackageDirectory);

        // Replace properties in start shell scripts
        Collection<File> startFiles = org.apache.commons.io.FileUtils.listFiles(this.outputPackageDirectory,
            new WildcardFileFilter("start_xwiki*.*"), null);

        VelocityContext velocityContext = createVelocityContext();
        for (File startFile : startFiles) {
            getLog().info(String.format("  Replacing variables in [%s]...", startFile));
            try {
                String content = org.apache.commons.io.FileUtils.readFileToString(startFile, StandardCharsets.UTF_8);
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(startFile));
                writer.write(replaceProperty(content, velocityContext));
                writer.close();
            } catch (Exception e) {
                // Failed to read or write file...
                throw new MojoExecutionException(String.format("Failed to process start shell script [%s]", startFile),
                    e);
            }
        }
    }

    protected String replaceProperty(String content, VelocityContext velocityContext)
    {
        String result = content;
        for (Object key : velocityContext.getKeys()) {
            Object value = velocityContext.get(key.toString());
            result = StringUtils.replace(result, String.format("${%s}", key.toString()), value.toString());
        }
        return result;
    }

    private Artifact resolveArtifactItem(ArtifactItem artifactItem) throws MojoExecutionException
    {
        // Resolve the version and the type:
        // - if specified in the artifactItem, use them
        // - if not specified look for them in the project dependencies
        String version = artifactItem.getVersion();
        String type = artifactItem.getType();
        if (version == null || type == null) {
            Map<String, Artifact> artifacts = this.project.getArtifactMap();
            String key = ArtifactUtils.versionlessKey(artifactItem.getGroupId(), artifactItem.getArtifactId());
            if (artifacts.containsKey(key)) {
                if (version == null) {
                    version = artifacts.get(key).getVersion();
                }
                if (type == null) {
                    type = artifacts.get(key).getType();
                }
            } else {
                // Default to the platform version
                if (version == null) {
                    version = getXWikiPlatformVersion();
                }
                // Default to JAR
                if (type == null) {
                    type = "jar";
                }
            }
        }

        // Resolve the artifact
        Artifact artifact = this.repositorySystem.createArtifact(artifactItem.getGroupId(),
            artifactItem.getArtifactId(), version, "", type);
        resolveArtifact(artifact);
        return artifact;
    }

    private void generateConfigurationFiles(File configurationFileTargetDirectory) throws MojoExecutionException
    {
        VelocityContext context = createVelocityContext();
        Artifact configurationResourcesArtifact = this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-tool-configuration-resources", getXWikiPlatformVersion(), "", "jar");
        resolveArtifact(configurationResourcesArtifact);

        configurationFileTargetDirectory.mkdirs();

        try (JarInputStream jarInputStream =
            new JarInputStream(new FileInputStream(configurationResourcesArtifact.getFile()))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".vm")) {
                    String fileName = entry.getName().replace(".vm", "");
                    File outputFile = new File(configurationFileTargetDirectory, fileName);
                    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile));
                    getLog().info("Writing config file: " + outputFile);
                    // Note: Init is done once even if this method is called several times...
                    Velocity.init();
                    Velocity.evaluate(context, writer, "", IOUtils.toString(jarInputStream, StandardCharsets.UTF_8));
                    writer.close();
                    jarInputStream.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to extract configuration files", e);
        }
    }

    private void importXARs() throws MojoExecutionException
    {
        Set<Artifact> xarArtifacts = resolveXARs();
        if (!xarArtifacts.isEmpty()) {
            Importer importer = new Importer(this.oldCoreHelper);

            // Reverse artifact order to have dependencies first (despite the fact that it's a Set it's actually an
            // ordered LinkedHashSet behind the scene)
            List<Artifact> dependenciesFirstArtifacts = new ArrayList<>(xarArtifacts);
            Collections.reverse(dependenciesFirstArtifacts);

            // Import the xars
            for (Artifact xarArtifact : dependenciesFirstArtifacts) {
                getLog().info("  ... Importing XAR file: " + xarArtifact.getFile());

                try {
                    int nb = importer.importXAR(xarArtifact.getFile(), this.importUser,
                        this.oldCoreHelper.getXWikiContext());

                    getLog().info("  .... Imported " + nb + " documents");
                } catch (Exception e) {
                    throw new MojoExecutionException(String.format("Failed to import XAR [%s]", xarArtifact.toString()),
                        e);
                }
            }
        }
    }

    private Set<Artifact> resolveXARs() throws MojoExecutionException
    {
        Set<Artifact> xarArtifacts = new LinkedHashSet<>();

        Set<Artifact> artifacts = this.project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact.getType().equals("xar")) {
                    xarArtifacts.add(artifact);
                    resolveArtifact(artifact);
                }
            }
        }

        return xarArtifacts;
    }

    private Artifact resolveHSQLDBArtifact() throws MojoExecutionException
    {
        Artifact hsqldbArtifact = null;

        // Try to find an HSQLDB dependency in the project using the packager plugin
        Set<Artifact> artifacts = this.project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact.getType().equals("jar") && artifact.getGroupId().equals("org.hsqldb")
                    && artifact.getArtifactId().equals("hsqldb")) {
                    hsqldbArtifact = artifact;
                    break;
                }
            }
        }

        // If the HSQLDB artifact wasn't defined in the project, resolve it using ${hsqldb.version} as its version to
        // make sure we use the version defined in the top level POM. And if that property doesn't exist throw an
        // error.
        if (hsqldbArtifact == null) {
            String hsqldbVersion = this.project.getProperties().getProperty("hsqldb.version");
            if (hsqldbVersion == null) {
                throw new MojoExecutionException("The HSQLDB version couldn't be computed. Either define a dependency "
                    + "on it in your project or set the \"hsqldb.version\" Maven property in the project POM or in "
                    + "some of its parents.");
            }
            hsqldbArtifact = this.repositorySystem.createArtifact("org.hsqldb", "hsqldb", hsqldbVersion, "", "jar");
        }

        if (hsqldbArtifact != null) {
            resolveArtifact(hsqldbArtifact);
        } else {
            throw new MojoExecutionException("Failed to locate the HSQLDB artifact in either the project "
                + "dependency list or using the specific [hsqldb:hsqldb] artifact name");
        }

        getLog().info("  ... Using artifact: " + hsqldbArtifact.getFile());

        return hsqldbArtifact;
    }

    private Artifact resolveJettyArtifact() throws MojoExecutionException
    {
        Artifact jettyArtifact = null;

        Set<Artifact> artifacts = this.project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact.getType().equals("zip")
                    && artifact.getArtifactId().equals("xwiki-platform-tool-jetty-resources")) {
                    jettyArtifact = artifact;
                    break;
                }
            }
        }

        // If the Jetty artifact wasn't defined, try to resolve the default Jetty artifact
        if (jettyArtifact == null) {
            jettyArtifact = this.repositorySystem.createArtifact("org.xwiki.platform",
                "xwiki-platform-tool-jetty-resources", getXWikiPlatformVersion(), "", "zip");
        }

        if (jettyArtifact != null) {
            resolveArtifact(jettyArtifact);
        } else {
            throw new MojoExecutionException("Failed to locate the Jetty artifact in either the project "
                + "dependency list or using the specific [xwiki-platform-tool-jetty-resources] artifact name");
        }

        return jettyArtifact;
    }

    private Collection<Artifact> resolveWarArtifacts() throws MojoExecutionException
    {
        List<Artifact> warArtifacts = new ArrayList<>();

        // First look for dependencies of type WAR.
        for (Artifact artifact : this.project.getArtifacts()) {
            if (artifact.getType().equals("war")) {
                warArtifacts.add(artifact);
            }
        }

        // If there are no WAR artifacts specified in the list of dependencies then use the default WAR artifacts.
        if (warArtifacts.isEmpty()) {
            warArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform", "xwiki-platform-web",
                getXWikiPlatformVersion(), "", "war"));
            warArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
                "xwiki-platform-tool-rootwebapp", getXWikiPlatformVersion(), "", "war"));
        }

        for (Artifact warArtifact : warArtifacts) {
            resolveArtifact(warArtifact);
        }

        return warArtifacts;
    }

    private String getContextPath(Artifact warArtifact)
    {
        String contextPath = getContextPathMapping().get(warArtifact.getArtifactId());
        if (contextPath == null) {
            // Should we put this as default "contextPathMapping" configuration in a parent POM? (and rely on
            // configuration merging)
            if (warArtifact.getArtifactId().equals("xwiki-platform-web")) {
                contextPath = "xwiki";
            } else if (warArtifact.getArtifactId().equals("xwiki-platform-tool-rootwebapp")) {
                contextPath = "root";
            } else {
                contextPath = warArtifact.getArtifactId();
            }
        }
        return contextPath;
    }

    private Map<String, String> getContextPathMapping()
    {
        if (this.contextPathMapping == null) {
            this.contextPathMapping = Collections.emptyMap();
        }
        return this.contextPathMapping;
    }

    private Collection<Artifact> getSkinArtifacts() throws MojoExecutionException
    {
        Collection<Artifact> skinArtifacts = new HashSet<>();

        if (this.skinArtifactItems != null) {
            for (SkinArtifactItem skinArtifactItem : this.skinArtifactItems) {
                skinArtifacts.add(resolveArtifactItem(skinArtifactItem));
            }
        } else {
            Artifact defaultSkin = resolveArtifact("org.xwiki.platform", "xwiki-platform-flamingo-skin-resources",
                getXWikiPlatformVersion(), "zip");
            skinArtifacts.add(defaultSkin);
        }

        return skinArtifacts;
    }

    private Collection<Artifact> resolveJarArtifacts() throws MojoExecutionException
    {
        // Get the mandatory jars
        Set<Artifact> artifacts = getMandatoryJarArtifacts();

        // Now resolve mandatory dependencies if they're not explicitly specified
        Set<Artifact> resolvedArtifacts = resolveTransitively(artifacts);

        // Maven is already taking care of resolving project dependencies before the plugin is executed
        resolvedArtifacts.addAll(this.project.getArtifacts());

        // Remove the non JAR artifacts. Note that we need to include non JAR artifacts before the transitive resolve
        // because for example some XARs mayb depend on JARs and we need those JARs to be packaged!
        Set<Artifact> jarArtifacts = new HashSet<>();
        for (Artifact artifact : resolvedArtifacts) {
            // Note: test-jar is used in functional tests from time to time and we need to package them too.
            if (artifact.getType().equals("jar") || artifact.getType().equals("test-jar")) {
                jarArtifacts.add(artifact);
            }
        }

        return jarArtifacts;
    }

    private Set<Artifact> getMandatoryJarArtifacts() throws MojoExecutionException
    {
        Set<Artifact> mandatoryTopLevelArtifacts = new HashSet<>();

        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-oldcore", getXWikiPlatformVersion(), null, "jar"));

        // Required to load macros.vm by default
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-velocity-webapp", getXWikiPlatformVersion(), null, "jar"));

        // Required Plugins
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-skin-skinx", getXWikiPlatformVersion(), null, "jar"));

        // We shouldn't need those but right now it's mandatory since they are defined in the default web.xml file we
        // provide. We'll be able to remove them when we start using Servlet 3.0 -->
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-wysiwyg-api", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-rest-server", getXWikiPlatformVersion(), null, "jar"));

        // Needed by platform-web but since we don't have any dep in platform-web's pom.xml at the moment (duplication
        // issue with XE and platform-web) we need to include it here FTM... Solution: get a better maven WAR plugin
        // with proper merge feature and then remove this...
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-uiextension-api", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-localization-script", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-localization-source-legacy", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-security-bridge", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-url-scheme-standard", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-wiki-default", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-lesscss-default", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-lesscss-script", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-webjars-api", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-configuration-default", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-icon-default", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-resource-servlet", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-xar-script", getXWikiPlatformVersion(), null, "jar"));

        // Velocity Scripting for Model Modules is also core (it's used a bit everywhere in VMs, pages, etc).
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-wiki-script", getXWikiPlatformVersion(), null, "jar"));

        // FIXME: $services.security is a replacement for $xwiki.hasAccessLevel() and we have started using it in the
        // Velocity templates. Most of these templates are located in platform-web and currently we don't declare the
        // dependencies of platform-web (they are declared in enterprise-web) thus we need to bundle this script service
        // here. In the future we may want to create a separate module to hold the Velocity templates from platform-web
        // and this module should have a dependency on platform-security-script.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-security-script", getXWikiPlatformVersion(), null, "jar"));

        // Copy/Delete/Rename/Move actions are currently in the Refactoring module and for now we consider them as
        // core actions.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-refactoring-default", getXWikiPlatformVersion(), null, "jar"));

        // Editing wiki pages is a core action and so we need to provide an implementation for the edit API.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-edit-default", getXWikiPlatformVersion(), null, "jar"));

        // Rendering Script Service is used in several places and it requires a rendering configuration implementation
        // to work. In addition WikiModel component implementation also requires a rendering configuration
        // implementation to work.
        // In addition, by default the Macro and Icon transformations are enabled and thus require configuration
        // component implementations. The Macro one is drawn transitively from other dependencies but this is not the
        // case for the Icon Transformation and thus we need to add it manually.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-rendering-configuration-default", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-rendering-transformation-icon", getXWikiPlatformVersion(), null, "jar"));

        // Most, if not all functional test modules have XAR dependencies and those are installed as extensions inside
        // XWiki. Thus at XWiki initialization time, the Extension Manager initializes those extensions and need to
        // have the AR Extension Handler available to do so.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-extension-handler-xar", getXWikiPlatformVersion(), null, "jar"));

        // Get the platform's pom.xml to get the versions of some needed externals dependencies, so that we do not
        // hardcode them.
        MavenProject platformPomProject = getPlatformPOMProject();
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.webjars", "bootstrap",
            getDependencyManagementVersion(platformPomProject, "org.webjars", "bootstrap"), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.webjars", "requirejs",
            getDependencyManagementVersion(platformPomProject, "org.webjars", "requirejs"), null, "jar"));

        // Ensures all logging goes through SLF4J and Logback.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.commons",
            "xwiki-commons-logging-logback", this.getXWikiCommonsVersion(), "compile", "jar"));
        // Get the logging artifact versions from the top level XWiki Commons POM
        MavenProject pomProject = getTopLevelPOMProject();
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.slf4j", "jcl-over-slf4j",
            getDependencyManagementVersion(pomProject, "org.slf4j", "jcl-over-slf4j"), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.slf4j", "log4j-over-slf4j",
            getDependencyManagementVersion(pomProject, "org.slf4j", "log4j-over-slf4j"), null, "jar"));

        // Filesystem store is the default so we want to test it as much as possible
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-store-filesystem-oldcore", getXWikiPlatformVersion(), null, "jar"));

        // Components for executing Jobs.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.commons",
            "xwiki-commons-job-default", this.getXWikiCommonsVersion(), "compile", "jar"));

        // Add a special JAR used for functional tests to discover if some scripts in some wiki page require Programming
        // Rights.
        if (this.test && !isSkipTests() && this.testProgrammingRights) {
            mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
                "xwiki-platform-test-checker", getXWikiPlatformVersion(), null, "jar"));
        }

        // Also add the skins artifacts, that may have JAR dependencies
        mandatoryTopLevelArtifacts.addAll(getSkinArtifacts());

        // CAPTCHA API used by oldcore in CommentsAdd and RegisterAction.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-captcha-api", getXWikiPlatformVersion(), null, "jar"));
        // CAPTCHA Default module used to avoid cyclic dependency on oldcore but needed to access the configuration.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-captcha-default", getXWikiPlatformVersion(), null, "jar"));

        // Authentication default component used by oldcore in authenticators and script service used by login.vm
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-security-authentication-default", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-security-authentication-script", getXWikiPlatformVersion(), null, "jar"));

        // Name strategies components
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-model-validation-default", getXWikiPlatformVersion(), null, "jar"));

        // Default component for the merge operation used by oldcore.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-store-merge-default", getXWikiPlatformVersion(), null, "jar"));

        // User API implementation required by oldcore, platform web and others.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-user-default", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-user-script", getXWikiPlatformVersion(), null, "jar"));

        return mandatoryTopLevelArtifacts;
    }

    private Set<Artifact> resolveTransitively(Set<Artifact> artifacts) throws MojoExecutionException
    {
        AndArtifactFilter filter = new AndArtifactFilter(Arrays.asList(new ScopeArtifactFilter("runtime"),
            // - Exclude JCL and LOG4J since we want all logging to go through SLF4J. Note that we're excluding
            // log4j-<version>.jar but keeping log4j-over-slf4j-<version>.jar
            // - Exclude batik-js to prevent conflict with the patched version of Rhino used by yuicompressor used
            // for JSX. See https://jira.xwiki.org/browse/XWIKI-6151 for more details.
            new ExcludesArtifactFilter(Arrays.asList("org.apache.xmlgraphic:batik-js",
                "commons-logging:commons-logging", "commons-logging:commons-logging-api", "log4j:log4j"))));

        ArtifactResolutionRequest request = new ArtifactResolutionRequest().setArtifact(this.project.getArtifact())
            .setArtifactDependencies(artifacts).setCollectionFilter(filter)
            .setRemoteRepositories(this.remoteRepositories).setLocalRepository(this.localRepository)
            .setManagedVersionMap(getManagedVersionMap()).setResolveRoot(false);
        ArtifactResolutionResult resolutionResult = this.repositorySystem.resolve(request);
        if (resolutionResult.hasExceptions()) {
            throw new MojoExecutionException(String.format("Failed to resolve artifacts [%s]", artifacts),
                resolutionResult.getExceptions().get(0));
        }

        return resolutionResult.getArtifacts();
    }

    private Map<String, Artifact> getManagedVersionMap() throws MojoExecutionException
    {
        Map<String, Artifact> dependencyManagementMap = new HashMap<>();

        // Add Platform Core's <dependencyManagement> since this is where we keep all our dependencies management
        // information. We absolutely need to include those because Maven 3.x's artifact seems to have a big hole in
        // not handling artifact's parent dependency management information by itself!
        // See http://jira.codehaus.org/browse/MNG-5462
        dependencyManagementMap.putAll(getPlatformPOMProject().getManagedVersionMap());

        // We add the project's dependency management in a second step so that it can override the platform dep mgmt map
        dependencyManagementMap.putAll(this.project.getManagedVersionMap());

        return dependencyManagementMap;
    }

    private MavenProject getTopLevelPOMProject() throws MojoExecutionException
    {
        return this.extensionHelper.getMavenProject(this.repositorySystem.createProjectArtifact("org.xwiki.commons",
            "xwiki-commons", getXWikiCommonsVersion()));
    }

    private MavenProject getPlatformPOMProject() throws MojoExecutionException
    {
        return this.extensionHelper.getMavenProject(this.repositorySystem.createProjectArtifact("org.xwiki.platform",
            "xwiki-platform-core", getXWikiPlatformVersion()));
    }

    /**
     * @return the version of the XWiki Commons project, taken from the {@code commons.version} property, defaulting to
     *         the current project version of this property is not defined
     */
    private String getXWikiCommonsVersion()
    {
        return normalizeVersion(this.commonsVersion);
    }

    /**
     * @return the version of the XWiki Platform project, either configured in the project's pom using this plugin or
     *         taken from the {@code platform.version} property if defined, defaulting to the current project version if
     *         not defined
     */
    private String getXWikiPlatformVersion()
    {
        return normalizeVersion(this.platformVersion);
    }

    private String normalizeVersion(String version)
    {
        String normalizedVersion = version;
        if (normalizedVersion == null) {
            normalizedVersion = this.project.getVersion();
        }
        return normalizedVersion;
    }

    private String getDependencyManagementVersion(MavenProject project, String groupId, String artifactId)
        throws MojoExecutionException
    {
        for (Object dependencyObject : project.getDependencyManagement().getDependencies()) {
            Dependency dependency = (Dependency) dependencyObject;
            if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                return dependency.getVersion();
            }
        }
        throw new MojoExecutionException(
            String.format("Failed to find artifact [%s:%s] in dependency management " + "for [%s]", groupId, artifactId,
                project.toString()));
    }

    /**
     * Create the passed directory if it doesn't already exist.
     *
     * @param directory the directory to create
     */
    private void createDirectory(File directory)
    {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void copyDirectory(File sourceDirectory, File targetDirectory) throws MojoExecutionException
    {
        createDirectory(targetDirectory);
        try {
            FileUtils.copyDirectoryStructureIfModified(sourceDirectory, targetDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException(
                String.format("Failed to copy directory [%s] to [%s]", sourceDirectory, targetDirectory), e);
        }
    }

    private void copyFile(File source, File targetDirectory) throws MojoExecutionException
    {
        try {
            FileUtils.copyFileToDirectoryIfModified(source, targetDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Failed to copy file [%s] to [%s]", source, targetDirectory),
                e);
        }
    }

    private void generateDistributionXED(File warDirectory, Artifact warArtifact) throws MojoExecutionException
    {
        // Generate the XED file for the distribution
        try {
            File xedFile = new File(warDirectory, "META-INF/extension.xed");
            xedFile.getParentFile().mkdirs();
            this.extensionHelper.serializeExtension(xedFile, warArtifact);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate distribution's extension.xed descriptor", e);
        }
    }

    private void unzip(File source, File targetDirectory) throws MojoExecutionException
    {
        createDirectory(targetDirectory);
        try {
            ZipUnArchiver unArchiver = new ZipUnArchiver();
            unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, "Package"));
            unArchiver.setSourceFile(source);
            unArchiver.setDestDirectory(targetDirectory);
            unArchiver.setOverwrite(true);
            unArchiver.extract();
        } catch (Exception e) {
            throw new MojoExecutionException(
                String.format("Error unpacking file [%s] into [%s]", source, targetDirectory), e);
        }
    }

    private void resolveArtifact(Artifact artifact) throws MojoExecutionException
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest().setArtifact(artifact)
            .setRemoteRepositories(this.remoteRepositories).setLocalRepository(this.localRepository);
        ArtifactResolutionResult resolutionResult = this.repositorySystem.resolve(request);
        if (resolutionResult.hasExceptions()) {
            throw new MojoExecutionException(String.format("Failed to resolve artifact [%s]", artifact),
                resolutionResult.getExceptions().get(0));
        }
    }

    private Artifact resolveArtifact(String groupId, String artifactId, String version, String type)
        throws MojoExecutionException
    {
        Artifact artifact = this.repositorySystem.createArtifact(groupId, artifactId, version, "", type);
        resolveArtifact(artifact);
        return artifact;
    }

    private VelocityContext createVelocityContext()
    {
        Map<String, Object> properties = new HashMap<>();
        properties.putAll(getDefaultConfigurationProperties());
        final Properties projectProperties = this.project.getProperties();
        for (Object key : projectProperties.keySet()) {
            properties.put(key.toString(), projectProperties.get(key).toString());
        }

        VelocityContext context = new VelocityContext(properties);

        String inceptionYear = this.project.getInceptionYear();
        String year = new SimpleDateFormat("yyyy").format(new Date());

        if (StringUtils.isEmpty(inceptionYear)) {
            inceptionYear = year;
        }
        context.put("project", this.project);
        context.put("presentYear", year);

        if (inceptionYear.equals(year)) {
            context.put("projectTimespan", year);
        } else {
            context.put("projectTimespan", inceptionYear + "-" + year);
        }

        return context;
    }

    private Map<String, Object> getDefaultConfigurationProperties()
    {
        Map<String, Object> props = new HashMap<>();

        // Default configuration data for hibernate.cfg.xml
        props.put("xwikiDbConnectionUrl",
            "jdbc:hsqldb:file:${environment.permanentDirectory}/database/xwiki_db;shutdown=true");
        props.put("xwikiDbConnectionUsername", "sa");
        props.put("xwikiDbConnectionPassword", "");
        props.put("xwikiDbConnectionDriverClass", "org.hsqldb.jdbcDriver");
        props.put("xwikiDbDialect", "org.hibernate.dialect.HSQLDialect");
        props.put("xwikiDbHbmXwiki", "xwiki.hbm.xml");
        props.put("xwikiDbHbmFeeds", "feeds.hbm.xml");

        // Default configuration data for xwiki.cfg
        props.put("xwikiCfgPlugins",
            "com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin");
        props.put("xwikiCfgVirtualUsepath", "1");
        props.put("xwikiCfgEditCommentMandatory", "0");
        props.put("xwikiCfgDefaultSkin", "flamingo");
        props.put("xwikiCfgDefaultBaseSkin", "flamingo");
        props.put("xwikiCfgEncoding", "UTF-8");

        // Other default configuration properties
        props.put("xwikiDataDir", "data");

        return props;
    }
}
