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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;
import org.hibernate.cfg.Environment;

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
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * List of Remote Repositories used by the resolver.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepos;

    /**
     * Project builder -- builds a model from a pom.xml.
     *
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     * @required
     * @readonly
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected ArtifactResolver resolver;

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
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository local;

    /**
     * @component
     */
    private ArtifactMetadataSource metadataSource;

    /**
      * Velocity component.
      *
      * @component
      * @readonly
      * @required
      */
    private VelocityComponent velocity;

    /**
     * The user under which the import should be done. If not user is specified then we import with backup pack.
     * For example {@code superadmin}.
     *
     * @parameter
     */
    private String importUser;

    /**
     * List of skin artifacts to include in the packaging.
     *
     * @parameter
     */
    private List<SkinArtifactItem> skinArtifactItems;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Step 1: Expand Jetty resources into the package output directory.
        getLog().info("Expanding Jetty Resources ...");
        Artifact jettyArtifact = resolveJettyArtifact();
        unzip(jettyArtifact.getFile(), this.outputPackageDirectory);

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
        //         code, for example to override existing components for the test purpose. As an example the link
        //         checker might want to override the HTTP Checker component so that checks are not done over the
        //         internet since the tests need to execute in a stable environment to prevent false positives.
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
            Artifact colibriArtifact = resolveArtifact("org.xwiki.platform", "xwiki-platform-colibri",
                this.project.getVersion(), "zip");
            unzip(colibriArtifact.getFile(), skinsDirectory);
        }

        // Step 8: Import specified XAR files into the database
        getLog().info(String.format("Import XAR dependencies %s...",
            this.importUser == null ? "as a backup pack" : "using user [" + this.importUser + "]"));
        importXARs(webInfDirectory);
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
                // Default to the project's version
                if (version == null) {
                    version = this.project.getVersion();
                }
                // Default to JAR
                if (type == null) {
                    type = "jar";
                }
            }
        }

        // Resolve the artifact
        Artifact artifact = this.factory.createArtifact(artifactItem.getGroupId(), artifactItem.getArtifactId(),
            version, "", type);
        resolveArtifact(artifact);
        return artifact;
    }
    
    private void generateConfigurationFiles(File configurationFileTargetDirectory) throws MojoExecutionException
    {
        VelocityContext context = createVelocityContext();
        Artifact configurationResourcesArtifact = this.factory.createArtifact("org.xwiki.platform",
            "xwiki-platform-tool-configuration-resources", this.project.getVersion(), "", "jar");
        resolveArtifact(configurationResourcesArtifact);

        configurationFileTargetDirectory.mkdirs();

        try {
            JarInputStream jarInputStream =
                new JarInputStream(new FileInputStream(configurationResourcesArtifact.getFile()));
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".vm")) {

                    String fileName = entry.getName().replace(".vm", "");
                    File outputFile = new File(configurationFileTargetDirectory, fileName);
                    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile));
                    getLog().info("Writing config file: " + outputFile);
                    this.velocity.getEngine().evaluate(context, writer, "", IOUtils.toString(jarInputStream));
                    writer.close();
                    jarInputStream.closeEntry();
                }
            }
            // Flush and close all the streams
            jarInputStream.close();
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
            for (Artifact xarArtifact : xarArtifacts) {
                // TODO: Modify Importer class to be able to import from a zip file
                File xarTargetDirectory = new File(this.tmpXarDirectory, xarArtifact.getArtifactId());
                unzip(xarArtifact.getFile(), xarTargetDirectory);
                try {
                    getLog().info("  ... Importing XAR: " + xarArtifact.getFile());
                    importer.importDocuments(xarTargetDirectory, "xwiki",
                        new File(webInfDirectory, "hibernate.cfg.xml"), this.importUser);
                } catch (Exception e) {
                    throw new MojoExecutionException(
                        String.format("Failed to import XAR [%s]", xarArtifact.toString()), e);
                }
            }
            copyDirectory(this.databaseDirectory, new File(this.outputPackageDirectory, "database"));
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
                    && artifact.getArtifactId().equals("hsqldb"))
                {
                    hsqldbArtifact = artifact;
                    break;
                }
            }
        }

        // If the HSQLDB artifact wasn't defined, try to resolve the default HSQLDB JAR artifact
        if (hsqldbArtifact == null) {
            hsqldbArtifact = this.factory.createArtifact("org.hsqldb", "hsqldb", "2.2.8", "", "jar");
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
                    && artifact.getArtifactId().equals("xwiki-platform-tool-jetty-resources"))
                {
                    jettyArtifact = artifact;
                    break;
                }
            }
        }

        // If the Jetty artifact wasn't defined, try to resolve the default Jetty artifact
        if (jettyArtifact == null) {
            jettyArtifact = this.factory.createArtifact("org.xwiki.platform",
                "xwiki-platform-tool-jetty-resources", this.project.getVersion(), "", "zip");
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
            warArtifacts.put("xwiki", this.factory.createArtifact("org.xwiki.platform", "xwiki-platform-web",
                this.project.getVersion(), "", "war"));
            warArtifacts.put("root", this.factory.createArtifact("org.xwiki.platform",
                "xwiki-platform-tool-rootwebapp", this.project.getVersion(), "", "war"));
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
        Set<Artifact> jarArtifacts = new HashSet<Artifact>();

        Set<Artifact> artifacts = this.project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact.getType().equals("jar")) {
                    jarArtifacts.add(artifact);
                    // Note that we don't need to resolve transitively since getArtifacts() above will already
                    // contain all transitive dependencies.
                    resolveArtifact(artifact);
                }
            }
        }
        
        // Add mandatory dependencies if they're not explicitly specified.
        jarArtifacts.addAll(getMandatoryJarArtifacts());

        // Resolve all artifacts transitively in one go.
        return resolveTransitively(jarArtifacts);
    }

    private Set<Artifact> getMandatoryJarArtifacts() throws MojoExecutionException
    {
        Set<Artifact> mandatoryTopLevelArtifacts = new HashSet<Artifact>();

        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.xwiki.platform", "xwiki-platform-oldcore",
            this.project.getVersion(), null, "jar"));

        // Required Plugins
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.xwiki.platform",
            "xwiki-platform-skin-skinx", this.project.getVersion(), null, "jar"));

        // We shouldn't need those but right now it's mandatory since they are defined in the default web.xml file we
        // provide. We'll be able to remove them when we start using Servlet 3.0 -->
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.xwiki.platform",
            "xwiki-platform-wysiwyg-server", this.project.getVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.xwiki.platform",
            "xwiki-platform-wysiwyg-client", this.project.getVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.xwiki.platform",
            "xwiki-platform-webdav-server", this.project.getVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.xwiki.platform",
            "xwiki-platform-rest-server", this.project.getVersion(), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.xwiki.platform",
            "xwiki-platform-gwt-api", this.project.getVersion(), null, "jar"));

        // Ensures all logging goes through SLF4J and Logback.
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.xwiki.commons",
            "xwiki-commons-logging-logback", this.project.getVersion(), null, "jar"));
        // Get the logging artifact versions from the top level XWiki Commons POM
        MavenProject pomProject = getTopLevelPOMProject();
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.slf4j", "jcl-over-slf4j",
            getDependencyManagementVersion(pomProject, "org.slf4j", "jcl-over-slf4j"), null, "jar"));
        mandatoryTopLevelArtifacts.add(this.factory.createArtifact("org.slf4j", "log4j-over-slf4j",
            getDependencyManagementVersion(pomProject, "org.slf4j", "log4j-over-slf4j"), null, "jar"));

        return mandatoryTopLevelArtifacts;
    }

    private Set<Artifact> resolveTransitively(Set<Artifact> artifacts) throws MojoExecutionException
    {
        Set<Artifact> resolvedArtifacts = new HashSet<Artifact>();
        try {
            AndArtifactFilter filter = new AndArtifactFilter();
            filter.add(new ScopeArtifactFilter("runtime"));

            // - Exclude JCL and LOG4J since we want all logging to go through SLF4J. Note that we're excluding
            //   log4j-<version>.jar but keeping log4j-over-slf4j-<version>.jar
            // - Exclude batik-js to prevent conflict with the patched version of Rhino used by yuicompressor used for
            //   JSX. See http://jira.xwiki.org/jira/browse/XWIKI-6151 for more details.
            filter.add(new ExcludesArtifactFilter(Arrays.asList(
                "org.apache.xmlgraphic:batik-js",
                "commons-logging:commons-logging",
                "commons-logging:commons-logging-api",
                "log4j:log4j")));

            ArtifactResolutionResult arr = this.resolver.resolveTransitively(artifacts,
                this.project.getArtifact(), this.project.getManagedVersionMap(), this.local, this.remoteRepos,
                this.metadataSource, filter);
            resolvedArtifacts.addAll(arr.getArtifacts());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve mandatory artifacts", e);
        }

        return resolvedArtifacts;
    }

    private MavenProject getTopLevelPOMProject() throws MojoExecutionException
    {
        MavenProject pomProject;
        Artifact pomArtifact = this.factory.createArtifact("org.xwiki.commons", "xwiki-commons",
            this.project.getVersion(), "", "pom");
        try {
            pomProject = this.mavenProjectBuilder.buildFromRepository(pomArtifact, this.remoteRepos, this.local);
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException(String.format("Failed to build project for [%s]", pomArtifact), e);
        }
        return pomProject;
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
            throw new MojoExecutionException(String.format("Failed to copy directory [%] to [%]",
                sourceDirectory, targetDirectory), e);
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
            throw new MojoExecutionException(String.format("Error unpacking file [%s] into [%s]",
                source, targetDirectory), e);
        }
    }

    private void resolveArtifact(Artifact artifact) throws MojoExecutionException
    {
        try {
            this.resolver.resolve(artifact, this.remoteRepos, this.local);
        } catch (Exception e) {
            throw new MojoExecutionException(String.format("Failed to resolve artifact [%s]", artifact), e);
        }
    }
    
    private Artifact resolveArtifact(String groupId, String artifactId, String version, String type)
        throws MojoExecutionException
    {
        Artifact artifact = this.factory.createArtifact(groupId, artifactId, version, "", type);
        resolveArtifact(artifact);
        return artifact;
    }

    private VelocityContext createVelocityContext()
    {
        Properties properties = new Properties();
        properties.putAll(getDefaultConfigurationProperties());
        final Properties projectProperties = this.project.getProperties();
        for (Object key : projectProperties.keySet()) {
            properties.put( key.toString(), projectProperties.get( key ).toString() );
        }

        VelocityContext context = new VelocityContext( properties );

        String inceptionYear = this.project.getInceptionYear();
        String year = new SimpleDateFormat( "yyyy" ).format( new Date() );

        if ( StringUtils.isEmpty(inceptionYear) )
        {
            inceptionYear = year;
        }
        context.put( "project", this.project );
        context.put( "presentYear", year );

        if ( inceptionYear.equals( year ) )
        {
            context.put( "projectTimespan", year );
        }
        else
        {
            context.put( "projectTimespan", inceptionYear + "-" + year );
        }

        return context;
    }

    private Properties getDefaultConfigurationProperties()
    {
        Properties props = new Properties();

        // Default configuration data for hibernate.cfg.xml
        props.setProperty("xwikiDbConnectionUrl", "jdbc:hsqldb:file:database/xwiki_db;shutdown=true");
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
        props.setProperty("xwikiCfgVirtual", "0");
        props.setProperty("xwikiCfgVirtualUsepath", "0");
        props.setProperty("xwikiCfgEditCommentMandatory", "0");
        props.setProperty("xwikiCfgDefaultSkin", "colibri");
        props.setProperty("xwikiCfgDefaultBaseSkin", "colibri");
        props.setProperty("xwikiCfgEncoding", "UTF-8");

        return props;
    }
}
