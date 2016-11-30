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
package com.xpn.xwiki.tool.backup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;
import org.xwiki.extension.repository.internal.local.DefaultLocalExtension;
import org.xwiki.properties.ConverterManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Maven 2 plugin to generate XWiki data folder (database and extensions).
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.2
 */
public abstract class AbstractImportMojo extends AbstractMojo
{
    public static final String MPKEYPREFIX = "xwiki.extension.";

    public static final String MPNAME_NAME = "name";

    public static final String MPNAME_SUMMARY = "summary";

    public static final String MPNAME_WEBSITE = "website";

    public static final String MPNAME_FEATURES = "features";

    public static class ExcludeArtifact
    {
        private String groupId;

        private String artifactId;

        private String version;

        private String type;

        public String getGroupId()
        {
            return groupId;
        }

        public void setGrouId(String grouId)
        {
            this.groupId = grouId;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public void setArtifactId(String artifactId)
        {
            this.artifactId = artifactId;
        }

        public String getVersion()
        {
            return version;
        }

        public void setVersion(String version)
        {
            this.version = version;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }

    /**
     * @see com.xpn.xwiki.tool.backup.Importer#importDocuments(java.io.File, String, java.io.File)
     */
    @Parameter(defaultValue = "xwiki")
    protected String databaseName;

    /**
     * @see com.xpn.xwiki.tool.backup.Importer#importDocuments(java.io.File, String, java.io.File)
     */
    @Parameter(defaultValue = "${basedir}/src/main/packager/hibernate.cfg.xml")
    protected File hibernateConfig;

    /**
     * @see com.xpn.xwiki.tool.backup.Importer#importDocuments(java.io.File, String, java.io.File)
     */
    @Parameter(defaultValue = "${project.build.directory}/data/")
    protected File xwikiDataDir;

    /**
     * The namespace where to register the XAR extensions.
     */
    @Parameter(defaultValue = "wiki:xwiki")
    private String xarNamespace;

    /**
     * The reference of the user who installed the extensions.
     */
    @Parameter(defaultValue = XWikiRightService.SUPERADMIN_USER_FULLNAME)
    protected String installUser;

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The current Maven session being executed.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    /**
     * Project builder -- builds a model from a pom.xml.
     */
    @Component
    protected ProjectBuilder projectBuilder;

    /**
     * @param importer the importer
     * @param databaseName some database name (TODO: find out what this name is really)
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC driver, username and
     *            password, etc)
     * @throws Exception failed to import dependencies
     */
    protected void importDependencies(Importer importer, String databaseName, File hibernateConfig) throws Exception
    {
        XWikiContext xcontext = importer.createXWikiContext(databaseName, hibernateConfig);

        // We need to distinguish between extensions installed explicitly and their transitive dependencies.
        // We have to create our own Set because Maven changes the fields from the dependency Artifacts (e.g. resolves
        // their version) after they are added to the Set of dependencies and this causes the hash code to change. As a
        // result the #contains(Artifact) method doesn't work as expected because it uses the new hash code.
        Set<Artifact> directDependencies = new HashSet<>(this.project.getDependencyArtifacts());

        // Reverse artifact order to have dependencies first (despite the fact that it's a Set it's actually an ordered
        // LinkedHashSet behind the scene)
        List<Artifact> dependenciesFirstArtifacts = new ArrayList<>(this.project.getArtifacts());
        Collections.reverse(dependenciesFirstArtifacts);

        for (Artifact artifact : dependenciesFirstArtifacts) {
            if (!artifact.isOptional()) {
                if ("xar".equals(artifact.getType())) {
                    installXAR(artifact, importer, xcontext);
                }
            }
        }

        // We MUST shutdown HSQLDB because otherwise the last transactions will not be flushed
        // to disk and will be lost. In practice this means the last Document imported has a
        // very high chance of not making it...
        // TODO: Find a way to implement this generically for all databases and inside
        // XWikiHibernateStore (cf http://jira.xwiki.org/jira/browse/XWIKI-471).
        importer.shutdownHSQLDB(xcontext);

        importer.disposeXWikiContext(xcontext);

    }

    protected void installXAR(Artifact artifact, Importer importer, XWikiContext xcontext) throws Exception
    {
        getLog().info("  ... Importing XAR file: " + artifact.getFile());

        // Import XAR into database
        int nb = importer.importXAR(artifact.getFile(), null, xcontext);

        getLog().info("  ..... Imported " + nb + " documents");

        // Install extension
        installExtension(artifact, (ComponentManager) xcontext.get(ComponentManager.class.getName()),
            this.xarNamespace);
    }

    protected void installExtension(Artifact artifact, ComponentManager componentManager, String namespace)
        throws ComponentLookupException, InstallException, LocalExtensionRepositoryException, MojoExecutionException
    {
        // We need to distinguish between extensions installed explicitly and their transitive dependencies.
        // We have to create our own Set because Maven changes the fields from the dependency Artifacts (e.g. resolves
        // their version) after they are added to the Set of dependencies and this causes the hash code to change. As a
        // result the #contains(Artifact) method doesn't work as expected because it uses the new hash code.
        Set<Artifact> directDependencies = new HashSet<>(this.project.getDependencyArtifacts());

        LocalExtensionRepository localExtensionRepository =
            componentManager.getInstance(LocalExtensionRepository.class);
        InstalledExtensionRepository installedExtensionRepository =
            componentManager.getInstance(InstalledExtensionRepository.class);

        MavenProject dependencyProject = getMavenProject(artifact);

        ConverterManager converter = componentManager.getInstance(ConverterManager.class);
        Extension mavenExtension = converter.convert(Extension.class, dependencyProject.getModel());

        DefaultLocalExtension extension = new DefaultLocalExtension(null, mavenExtension);

        extension.setFile(artifact.getFile());

        LocalExtension localExtension = localExtensionRepository.storeExtension(extension);
        installedExtensionRepository.installExtension(localExtension, namespace, !directDependencies.contains(artifact),
            Collections.<String, Object>singletonMap("user.reference", this.installUser));

        getLog().info("  ... Registered extension [" + extension + "]");
    }

    protected MavenProject getMavenProject(Artifact artifact) throws MojoExecutionException
    {
        try {
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest(this.session.getProjectBuildingRequest())
                // We don't want to execute any plugin here
                .setProcessPlugins(false)
                // It's not this plugin job to validate this pom.xml
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                // Use the repositories configured for the built project instead of the default Maven ones
                .setRemoteRepositories(this.session.getCurrentProject().getRemoteArtifactRepositories());
            // Note: build() will automatically get the POM artifact corresponding to the passed artifact.
            ProjectBuildingResult result = this.projectBuilder.build(artifact, request);
            return result.getProject();
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException(String.format("Failed to build project for [%s]", artifact), e);
        }
    }
}
