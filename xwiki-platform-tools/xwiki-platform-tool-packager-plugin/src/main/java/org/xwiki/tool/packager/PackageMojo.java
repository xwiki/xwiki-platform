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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.hibernate.cfg.Environment;
import org.sonatype.aether.RepositorySystemSession;
import org.xwiki.velocity.internal.log.SLF4JLogChute;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.tool.backup.Importer;

/**
 * Create a runnable XWiki instance using Jetty as the Servlet Container and HSQLDB as the Database.
 * 
 * @version $Id$
 * @since 3.4M1
 * @goal package
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 * @threadSafe
 */
public class PackageMojo extends AbstractMojo
{
    /**
     * The directory where to create the packaging.
     * 
     * @parameter default-value="${project.build.directory}/xartmp"
     * @required
     */
    private File tmpXarDirectory;

    /**
     * The directory where to create the packaging.
     * 
     * @parameter default-value="${project.build.directory}/xwiki"
     * @required
     */
    private File outputPackageDirectory;

    /**
     * The directory where classes are put.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    private File outputClassesDirectory;

    /**
     * The directory where the HSQLDB database is generated.
     * 
     * @parameter default-value="${project.build.directory}/database"
     * @required
     */
    private File databaseDirectory;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Project builder -- builds a model from a pom.xml.
     *
     * @component role="org.apache.maven.project.ProjectBuilder"
     * @required
     * @readonly
     */
    protected ProjectBuilder projectBuilder;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected RepositorySystem repositorySystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repositorySystemSession;

     /**
      * Local repository to be used by the plugin to resolve dependencies.
      *
      * @parameter expression="${localRepository}"
      */
     protected ArtifactRepository localRepository;

     /**
      * List of remote repositories to be used by the plugin to resolve dependencies.
      *
      * @parameter expression="${project.remoteArtifactRepositories}"
      */
     protected List<ArtifactRepository> remoteRepositories;

    /**
     * The user under which the import should be done. If not user is specified then we import with backup pack. For
     * example {@code superadmin}.
     * 
     * @parameter
     */
    private String importUser;

    /**
     * The platform version to be used by the packager plugin.
     * 
     * @parameter expression="${platform.version}" default-value="${platform.version}"
     */
    private String platformVersion;

    /**
     * List of skin artifacts to include in the packaging.
     * 
     * @parameter
     */
    private List<SkinArtifactItem> skinArtifactItems;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Using platform version: " + getXWikiPlatformVersion());

        // Step 1: Expand Jetty resources into the package output directory.
        getLog().info("Expanding Jetty Resources ...");
        expandJettyDistribution();

        // Step 2: Get the WAR dependencies and expand them in the package output directory.
        getLog().info("Expanding WAR dependencies ...");
        File webappsDirectory = new File(this.outputPackageDirectory, "webapps");
        for (Map.Entry<String, Artifact> warArtifactEntry : resolveWarArtifacts().entrySet()) {
            getLog().info("  ... Unzipping WAR: " + warArtifactEntry.getValue().getFile());
            unzip(warArtifactEntry.getValue().getFile(), new File(webappsDirectory, warArtifactEntry.getKey()));
        }

        // Step 3: Copy all JARs dependencies to the expanded WAR directory in WEB-INF/lib
        getLog().info("Copying JAR dependencies ...");
        File xwikiWebappDirectory = new File(webappsDirectory, "xwiki");
        File webInfDirectory = new File(xwikiWebappDirectory, "WEB-INF");
        File libDirectory = new File(webInfDirectory, "lib");
        createDirectory(libDirectory);
        for (Artifact artifact : resolveJarArtifacts()) {
            getLog().info("  ... Copying JAR: " + artifact.getFile());
            copyFile(artifact.getFile(), libDirectory);
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

        // Step 5: Generate and copy config files.
        getLog().info("Copying Configuration files ...");
        generateConfigurationFiles(webInfDirectory);

        // Step 6: Copy HSQLDB JDBC Driver
        getLog().info("Copying HSQLDB JDBC Driver JAR ...");
        Artifact hsqldbArtifact = resolveHSQLDBArtifact();
        copyFile(hsqldbArtifact.getFile(), libDirectory);

        // Step 7: Unzip the specified Skins. If no skin is specified then unzip the Colibri skin only.
        getLog().info("Copying Skins ...");
        File skinsDirectory = new File(xwikiWebappDirectory, "skins");
        if (this.skinArtifactItems != null) {
            for (SkinArtifactItem skinArtifactItem : this.skinArtifactItems) {
                Artifact skinArtifact = resolveArtifactItem(skinArtifactItem);
                unzip(skinArtifact.getFile(), skinsDirectory);
            }
        } else {
            Artifact colibriArtifact =
                resolveArtifact("org.xwiki.platform", "xwiki-platform-colibri", getXWikiPlatformVersion(), "zip");
            unzip(colibriArtifact.getFile(), skinsDirectory);
        }

        // Step 8: Import specified XAR files into the database
        getLog().info(
            String.format("Import XAR dependencies %s...", this.importUser == null ? "as a backup pack"
                : "using user [" + this.importUser + "]"));
        importXARs(webInfDirectory);
    }

    private void expandJettyDistribution() throws MojoExecutionException
    {
        Artifact jettyArtifact = resolveJettyArtifact();
        unzip(jettyArtifact.getFile(), this.outputPackageDirectory);

        // Replace maven properties in start shell scripts
        VelocityContext context = createVelocityContext();
        Collection<File> startFiles =
            org.apache.commons.io.FileUtils.listFiles(this.outputPackageDirectory, new WildcardFileFilter(
                "start_xwiki*.*"), null);

        // Note: Init is done once even if this method is called several times...
        Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new SLF4JLogChute());
        Velocity.init();
        for (File startFile : startFiles) {
            getLog().info(String.format("  Replacing variables in [%s]...", startFile));
            try {
                String content = org.apache.commons.io.FileUtils.readFileToString(startFile);
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(startFile));
                Velocity.evaluate(context, writer, "", content);
                writer.close();
            } catch (Exception e) {
                // Failed to read or write file...
                throw new MojoExecutionException(String.format("Failed to process start shell script [%s]", startFile),
                    e);
            }
        }
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
        Artifact artifact = this.repositorySystem.createArtifact(
            artifactItem.getGroupId(), artifactItem.getArtifactId(), version, "", type);
        resolveArtifact(artifact);
        return artifact;
    }

    private void generateConfigurationFiles(File configurationFileTargetDirectory) throws MojoExecutionException
    {
        VelocityContext context = createVelocityContext();
        Artifact configurationResourcesArtifact = this.repositorySystem.createArtifact(
            "org.xwiki.platform", "xwiki-platform-tool-configuration-resources", getXWikiPlatformVersion(), "", "jar");
        resolveArtifact(configurationResourcesArtifact);

        configurationFileTargetDirectory.mkdirs();

        try {
            JarInputStream jarInputStream =
                new JarInputStream(new FileInputStream(configurationResourcesArtifact.getFile()));

            try {
                JarEntry entry;
                while ((entry = jarInputStream.getNextJarEntry()) != null) {
                    if (entry.getName().endsWith(".vm")) {

                        String fileName = entry.getName().replace(".vm", "");
                        File outputFile = new File(configurationFileTargetDirectory, fileName);
                        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile));
                        getLog().info("Writing config file: " + outputFile);
                        // Note: Init is done once even if this method is called several times...
                        Velocity.init();
                        Velocity.evaluate(context, writer, "", IOUtils.toString(jarInputStream));
                        writer.close();
                        jarInputStream.closeEntry();
                    }
                }
            } finally {
                // Flush and close all the streams
                jarInputStream.close();
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to extract configuration files", e);
        }
    }

    private void importXARs(File webInfDirectory) throws MojoExecutionException
    {
        Set<Artifact> xarArtifacts = resolveXARs();
        if (!xarArtifacts.isEmpty()) {
            Importer importer = new Importer();
            // Make sure that we generate the Database in the right directory
            // TODO: In the future control completely the Hibernate config from inside the packager plugin and not in
            // the project using the packager plugin
            System.setProperty(Environment.URL, "jdbc:hsqldb:file:" + this.databaseDirectory
                + "/xwiki_db;shutdown=true");

            XWikiContext xcontext;
            try {
                xcontext = importer.createXWikiContext("xwiki", new File(webInfDirectory, "hibernate.cfg.xml"));
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to create context to import XAR files", e);
            }

            for (Artifact xarArtifact : xarArtifacts) {
                getLog().info("  ... Importing XAR file: " + xarArtifact.getFile());

                try {
                    importer.importXAR(xarArtifact.getFile(), this.importUser, xcontext);
                } catch (Exception e) {
                    throw new MojoExecutionException(
                        String.format("Failed to import XAR [%s]", xarArtifact.toString()), e);
                }
            }

            // We MUST shutdown HSQLDB because otherwise the last transactions will not be flushed
            // to disk and will be lost. In practice this means the last Document imported has a
            // very high chance of not making it...
            // TODO: Find a way to implement this generically for all databases and inside
            // XWikiHibernateStore (cf http://jira.xwiki.org/jira/browse/XWIKI-471).
            try {
                importer.shutdownHSQLDB(xcontext);
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to shutdown hsqldb database", e);
            }

            // Copy database files to XWiki's data directory.
            File dataDir = new File(this.outputPackageDirectory, "data");
            copyDirectory(this.databaseDirectory, new File(dataDir, "database"));
        }
    }

    private Set<Artifact> resolveXARs() throws MojoExecutionException
    {
        Set<Artifact> xarArtifacts = new HashSet<Artifact>();

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

        // If the HSQLDB artifact wasn't defined, try to resolve the default HSQLDB JAR artifact
        if (hsqldbArtifact == null) {
            hsqldbArtifact = this.repositorySystem.createArtifact("org.hsqldb", "hsqldb", "2.2.9", "", "jar");
        }

        if (hsqldbArtifact != null) {
            resolveArtifact(hsqldbArtifact);
        } else {
            throw new MojoExecutionException("Failed to locate the HSQLDB artifact in either the project "
                + "dependency list or using the specific [hsqldb:hsqldb] artifact name");
        }

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
            jettyArtifact = this.repositorySystem.createArtifact(
                "org.xwiki.platform", "xwiki-platform-tool-jetty-resources", getXWikiPlatformVersion(), "", "zip");
        }

        if (jettyArtifact != null) {
            resolveArtifact(jettyArtifact);
        } else {
            throw new MojoExecutionException("Failed to locate the Jetty artifact in either the project "
                + "dependency list or using the specific [xwiki-platform-tool-jetty-resources] artifact name");
        }

        return jettyArtifact;
    }

    private Map<String, Artifact> resolveWarArtifacts() throws MojoExecutionException
    {
        Map<String, Artifact> warArtifacts = new HashMap<String, Artifact>();

        Set<Artifact> artifacts = this.project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact.getType().equals("war")) {
                    String id;
                    if (artifact.getArtifactId().equals("xwiki-platform-web")) {
                        id = "xwiki";
                    } else if (artifact.getArtifactId().equals("xwiki-platform-tool-rootwebapp")) {
                        id = "root";
                    } else {
                        id = artifact.getArtifactId();
                    }
                    warArtifacts.put(id, artifact);
                    break;
                }
            }
        }

        // If the WAR artifacts weren't defined, try to resolve the default Web artifacts.
        if (warArtifacts.isEmpty()) {
            warArtifacts.put("xwiki", this.repositorySystem.createArtifact("org.xwiki.platform", "xwiki-platform-web",
                getXWikiPlatformVersion(), "", "war"));
            warArtifacts.put("root", this.repositorySystem.createArtifact("org.xwiki.platform",
                "xwiki-platform-tool-rootwebapp", getXWikiPlatformVersion(), "", "war"));
        }

        if (!warArtifacts.isEmpty()) {
            for (Artifact warArtifact : warArtifacts.values()) {
                resolveArtifact(warArtifact);
            }
        } else {
            throw new MojoExecutionException("Failed to locate any XWiki WAR artifact in either the project "
                + "dependency list or using the specific [xwiki-platform-web]/[xwiki-platform-tool-rootwebapp] "
                + "artifact names");
        }

        return warArtifacts;
    }

    private Collection<Artifact> resolveJarArtifacts() throws MojoExecutionException
    {
        Set<Artifact> mys = resolveTransitively(Collections.singleton(this.repositorySystem.createArtifact("org.xwiki.platform", "xwiki-platform-chart-renderer", "5.0-SNAPSHOT", "jar")));

        Set<Artifact> artifactsToResolve = this.project.getArtifacts();

        // Add mandatory dependencies if they're not explicitly specified.
        artifactsToResolve.addAll(getMandatoryJarArtifacts());

        // Resolve all artifacts transitively in one go.
        Set<Artifact> resolvedArtifacts = resolveTransitively(artifactsToResolve);

        // Remove the non JAR artifacts. Note that we need to include non JAR artifacts before the transitive resolve
        // because for example some XARs mayb depend on JARs and we need those JARs to be packaged!
        Set<Artifact> jarArtifacts = new HashSet<Artifact>();
        for (Artifact artifact : resolvedArtifacts) {
            if (artifact.getType().equals("jar")) {
                jarArtifacts.add(artifact);
            }
        }

        return jarArtifacts;
    }

    private Set<Artifact> getMandatoryJarArtifacts() throws MojoExecutionException
    {
        Set<Artifact> mandatoryTopLevelArtifacts = new HashSet<Artifact>();

        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact(
            "org.xwiki.platform", "xwiki-platform-oldcore", getXWikiPlatformVersion(), null, "jar"));

        // Required Plugins
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact(
            "org.xwiki.platform", "xwiki-platform-skin-skinx", getXWikiPlatformVersion(), null, "jar"));

        // We shouldn't need those but right now it's mandatory since they are defined in the default web.xml file we
        // provide. We'll be able to remove them when we start using Servlet 3.0 -->
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-wysiwyg-server", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-wysiwyg-client", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-webdav-server", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact(
            "org.xwiki.platform", "xwiki-platform-rest-server", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact(
            "org.xwiki.platform", "xwiki-platform-gwt-api", getXWikiPlatformVersion(), null, "jar"));

        // Needed by platform-web but since we don't have any dep in platform-web's pom.xml at the moment (duplication
        // issue with XE/XEM and platform-web) we need to include it here FTM... Solution: get a better maven WAR plugin
        // with proper merge feature and then remove this...
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-uiextension-api", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-localization-script", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-localization-source-legacy", getXWikiPlatformVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.platform",
            "xwiki-platform-security-bridge", getXWikiPlatformVersion(), null, "jar"));

        // Ensures all logging goes through SLF4J and Logback.
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.xwiki.commons",
            "xwiki-commons-logging-logback", this.getXWikiCommonsVersion(), "compile", "jar"));
        // Get the logging artifact versions from the top level XWiki Commons POM
        MavenProject pomProject = getTopLevelPOMProject();
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.slf4j", "jcl-over-slf4j",
            getDependencyManagementVersion(pomProject, "org.slf4j", "jcl-over-slf4j"), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.repositorySystem.createArtifact("org.slf4j", "log4j-over-slf4j",
            getDependencyManagementVersion(pomProject, "org.slf4j", "log4j-over-slf4j"), null, "jar"));

        return mandatoryTopLevelArtifacts;
    }

    private Set<Artifact> resolveTransitively(Set<Artifact> artifacts) throws MojoExecutionException
    {
        AndArtifactFilter filter = new AndArtifactFilter(Arrays.asList(
            new ScopeArtifactFilter("runtime"),
            // - Exclude JCL and LOG4J since we want all logging to go through SLF4J. Note that we're excluding
            // log4j-<version>.jar but keeping log4j-over-slf4j-<version>.jar
            // - Exclude batik-js to prevent conflict with the patched version of Rhino used by yuicompressor used
            // for JSX. See http://jira.xwiki.org/jira/browse/XWIKI-6151 for more details.
            new ExcludesArtifactFilter(Arrays.asList("org.apache.xmlgraphic:batik-js",
                "commons-logging:commons-logging", "commons-logging:commons-logging-api", "log4j:log4j"))));

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact(this.project.getArtifact())
            .setArtifactDependencies(artifacts)
            .setCollectionFilter(filter)
            .setRemoteRepositories(this.remoteRepositories)
            .setLocalRepository(this.localRepository)
            .setManagedVersionMap(getManagedVersionMap())
            .setResolveRoot(false);
        ArtifactResolutionResult resolutionResult = this.repositorySystem.resolve(request);
        if(resolutionResult.hasExceptions()){
            throw new MojoExecutionException(String.format("Failed to resolve artifacts [%s]", artifacts,
                resolutionResult.getExceptions().get(0)));
        }

        return resolutionResult.getArtifacts();
    }

    private Map<String, Artifact> getManagedVersionMap() throws MojoExecutionException
    {
        Map<String, Artifact> dependencyManagementMap = new HashMap<String, Artifact>();

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
        return getMavenProject(this.repositorySystem.createProjectArtifact(
            "org.xwiki.commons", "xwiki-commons", getXWikiCommonsVersion()));
    }

    private MavenProject getPlatformPOMProject() throws MojoExecutionException
    {
        return getMavenProject(this.repositorySystem.createProjectArtifact(
            "org.xwiki.platform", "xwiki-platform-core", getXWikiPlatformVersion()));
    }

    private MavenProject getMavenProject(Artifact artifact) throws MojoExecutionException
    {
        try {
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest()
                .setRepositorySession(this.repositorySystemSession)
                    // We don't want to execute any plugin here
                .setProcessPlugins(false)
                    // It's not this plugin job to validate this pom.xml
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            // Note: build() will automatically get the POM artifact corresponding to the passed artifact.
            ProjectBuildingResult result = this.projectBuilder.build(artifact, request);
            return result.getProject();
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException(String.format("Failed to build project for [%s]", artifact), e);
        }
    }

    /**
     * @return the version of the XWiki Commons project, taken from the {@code commons.version} property, defaulting to
     *         the current project version of this property is not defined
     */
    private String getXWikiCommonsVersion()
    {
        return this.project.getProperties().getProperty("commons.version", this.project.getVersion());
    }

    /**
     * @return the version of the XWiki Platform project, either configured in the project using this plugin or taken
     *         from the {@code platform.version} property if defined, defaulting to the current project version if not
     *         defined
     */
    private String getXWikiPlatformVersion()
    {
        String version = this.platformVersion;
        if (version == null) {
            version = this.project.getVersion();
        }
        return version;
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
        throw new MojoExecutionException(String.format("Failed to find artifact [%s:%s] in dependency management "
            + "for [%s]", groupId, artifactId, project.toString()));
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
            throw new MojoExecutionException(String.format("Failed to copy directory [%] to [%]", sourceDirectory,
                targetDirectory), e);
        }
    }

    private void copyFile(File source, File targetDirectory) throws MojoExecutionException
    {
        try {
            FileUtils.copyFileToDirectoryIfModified(source, targetDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Failed to copy file [%] to [%]", source, targetDirectory),
                e);
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
            throw new MojoExecutionException(String.format("Error unpacking file [%s] into [%s]", source,
                targetDirectory), e);
        }
    }

    private void resolveArtifact(Artifact artifact) throws MojoExecutionException
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact(artifact)
            .setRemoteRepositories(this.remoteRepositories)
            .setLocalRepository(this.localRepository);
        ArtifactResolutionResult resolutionResult = this.repositorySystem.resolve(request);
        if(resolutionResult.hasExceptions()){
            throw new MojoExecutionException(String.format("Failed to resolve artifact [%s]", artifact,
                resolutionResult.getExceptions().get(0)));
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
        Properties properties = new Properties();
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

    private Properties getDefaultConfigurationProperties()
    {
        Properties props = new Properties();

        // Default configuration data for hibernate.cfg.xml
        props.setProperty("xwikiDbConnectionUrl",
            "jdbc:hsqldb:file:${environment.permanentDirectory}/database/xwiki_db;shutdown=true");
        props.setProperty("xwikiDbConnectionUsername", "sa");
        props.setProperty("xwikiDbConnectionPassword", "");
        props.setProperty("xwikiDbConnectionDriverClass", "org.hsqldb.jdbcDriver");
        props.setProperty("xwikiDbDialect", "org.hibernate.dialect.HSQLDialect");
        props.setProperty("xwikiDbHbmXwiki", "xwiki.hbm.xml");
        props.setProperty("xwikiDbHbmFeeds", "feeds.hbm.xml");

        // Default configuration data for xwiki.cfg
        props.setProperty("xwikiCfgPlugins", "com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,\\"
            + "        com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,\\"
            + "        com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,\\"
            + "        com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,\\"
            + "        com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin");
        props.setProperty("xwikiCfgVirtualUsepath", "1");
        props.setProperty("xwikiCfgEditCommentMandatory", "0");
        props.setProperty("xwikiCfgDefaultSkin", "colibri");
        props.setProperty("xwikiCfgDefaultBaseSkin", "colibri");
        props.setProperty("xwikiCfgEncoding", "UTF-8");

        // Other default configuration properties
        props.setProperty("xwikiDataDir", "data");

        return props;
    }
}
