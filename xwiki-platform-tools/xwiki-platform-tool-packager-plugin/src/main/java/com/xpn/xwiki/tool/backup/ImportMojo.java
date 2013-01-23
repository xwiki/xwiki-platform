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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;
import org.xwiki.extension.repository.internal.local.DefaultLocalExtension;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.properties.ConverterManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Maven 2 plugin to import aset of XWiki documents into an existing database.
 * 
 * @version $Id$
 * @goal import
 * @requiresDependencyResolution compile
 * @requiresProject
 */
public class ImportMojo extends AbstractMojo
{
    public static final String MPKEYPREFIX = "xwiki.extension.";

    public static final String MPNAME_NAME = "name";

    public static final String MPNAME_SUMMARY = "summary";

    public static final String MPNAME_WEBSITE = "website";

    public static final String MPNAME_FEATURES = "features";

    /**
     * @parameter default-value = "xwiki"
     * @see com.xpn.xwiki.tool.backup.Importer#importDocuments(java.io.File,String,java.io.File)
     */
    private String databaseName;

    /**
     * @parameter default-value = "${basedir}/src/main/packager/hibernate.cfg.xml"
     * @see com.xpn.xwiki.tool.backup.Importer#importDocuments(java.io.File,String,java.io.File)
     */
    private File hibernateConfig;

    /**
     * @parameter
     * @see com.xpn.xwiki.tool.backup.Importer#importDocuments(java.io.File,String,java.io.File)
     */
    private File sourceDirectory;

    /**
     * @parameter default-value = "${project.build.directory}/data/"
     * @see com.xpn.xwiki.tool.backup.Importer#importDocuments(java.io.File,String,java.io.File)
     */
    private File xwikiDataDir;

    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

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
     * @required
     */
    protected ArtifactFactory factory;

    /**
     * List of Remote Repositories used by the resolver.
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepos;

    /**
     * Location of the local repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository local;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        Importer importer = new Importer();

        System.setProperty("xwiki.data.dir", this.xwikiDataDir.getAbsolutePath());

        if (this.sourceDirectory != null) {
            try {
                importer.importDocuments(this.sourceDirectory, this.databaseName, this.hibernateConfig);
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to import XWiki documents", e);
            }
        } else {
            try {
                importDependencies(importer, this.databaseName, this.hibernateConfig);
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to import XAR dependencies", e);
            }
        }
    }

    /**
     * @param importer the importer
     * @param databaseName some database name (TODO: find out what this name is really)
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC driver, username and
     *            password, etc)
     * @throws Exception failed to import dependencies
     */
    private void importDependencies(Importer importer, String databaseName, File hibernateConfig) throws Exception
    {
        XWikiContext xcontext = importer.createXWikiContext(databaseName, hibernateConfig);

        // Reverse artifact order to have dependencies first (despite the fact that it's a Set it's actually an ordered
        // LinkedHashSet behind the scene)
        // TODO: upgrade to Maven 3.x APIS and use AETHER
        List<Artifact> dependenciesFirstArtifacts = new ArrayList<Artifact>(this.project.getArtifacts());
        Collections.reverse(dependenciesFirstArtifacts);

        for (Artifact artifact : dependenciesFirstArtifacts) {
            if (!artifact.isOptional()) {
                if ("xar".equals(artifact.getType())) {
                    getLog().info("  ... Importing XAR file: " + artifact.getFile());

                    // Import XAR into database
                    importer.importXAR(artifact.getFile(), null, xcontext);

                    // Install extension
                    installExtension(artifact, xcontext);
                }
            }
        }

        // We MUST shutdown HSQLDB because otherwise the last transactions will not be flushed
        // to disk and will be lost. In practice this means the last Document imported has a
        // very high chance of not making it...
        // TODO: Find a way to implement this generically for all databases and inside
        // XWikiHibernateStore (cf http://jira.xwiki.org/jira/browse/XWIKI-471).
        importer.shutdownHSQLDB(xcontext);
    }

    private void installExtension(Artifact artifact, XWikiContext xcontext) throws ComponentLookupException,
        InstallException, LocalExtensionRepositoryException, MojoExecutionException
    {
        ComponentManager componentManager = (ComponentManager) xcontext.get(ComponentManager.class.getName());

        LocalExtensionRepository localExtensionRepository =
            componentManager.getInstance(LocalExtensionRepository.class);
        InstalledExtensionRepository installedExtensionRepository =
            componentManager.getInstance(InstalledExtensionRepository.class);

        DefaultLocalExtension extension =
            new DefaultLocalExtension(null, new ExtensionId(artifact.getGroupId() + ':' + artifact.getArtifactId(),
                artifact.getBaseVersion()), artifact.getType());

        extension.setFile(artifact.getFile());

        MavenProject project = getMavenProject(artifact);

        toExtension(extension, project.getModel(), componentManager);

        LocalExtension localExtension = localExtensionRepository.storeExtension(extension);
        installedExtensionRepository.installExtension(localExtension, "wiki:xwiki", true);
    }

    // Maven -> Extension
    // TODO: put all this, what's on core extension scanner and maven repository handler in a commons module

    private void toExtension(DefaultLocalExtension extension, Model model, ComponentManager componentManager)
        throws ComponentLookupException
    {
        extension.setName(getPropertyString(model, MPNAME_NAME, model.getName()));
        extension.setSummary(getPropertyString(model, MPNAME_SUMMARY, model.getDescription()));
        extension.setWebsite(getPropertyString(model, MPNAME_WEBSITE, model.getUrl()));

        // authors
        for (Developer developer : (List<Developer>) model.getDevelopers()) {
            URL authorURL = null;
            if (developer.getUrl() != null) {
                try {
                    authorURL = new URL(developer.getUrl());
                } catch (MalformedURLException e) {
                    // TODO: log ?
                }
            }

            extension.addAuthor(new DefaultExtensionAuthor(StringUtils.defaultIfBlank(developer.getName(),
                developer.getId()), authorURL));
        }

        // licenses
        if (!model.getLicenses().isEmpty()) {
            ExtensionLicenseManager licenseManager = componentManager.getInstance(ExtensionLicenseManager.class);
            for (License license : (List<License>) model.getLicenses()) {
                extension.addLicense(getExtensionLicense(license, licenseManager));
            }
        }

        // features
        String featuresString = getProperty(model, MPNAME_FEATURES);
        if (StringUtils.isNotBlank(featuresString)) {
            featuresString = featuresString.replaceAll("[\r\n]", "");
            ConverterManager converter = componentManager.getInstance(ConverterManager.class);
            extension.setFeatures(converter.<Collection<String>> convert(List.class, featuresString));
        }

        // dependencies
        for (Dependency mavenDependency : (List<Dependency>) model.getDependencies()) {
            if (!mavenDependency.isOptional()
                && (mavenDependency.getScope().equals("compile") || mavenDependency.getScope().equals("runtime"))) {
                extension.addDependency(new DefaultExtensionDependency(mavenDependency.getGroupId() + ':'
                    + mavenDependency.getArtifactId(), new DefaultVersionConstraint(mavenDependency.getVersion())));
            }
        }
    }

    private MavenProject getMavenProject(Artifact artifact) throws MojoExecutionException
    {
        MavenProject pomProject;
        Artifact pomArtifact =
            this.factory.createProjectArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        try {
            pomProject = this.mavenProjectBuilder.buildFromRepository(pomArtifact, this.remoteRepos, this.local);
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException(String.format("Failed to build project for [%s]", pomArtifact), e);
        }

        return pomProject;
    }

    private String getProperty(Model model, String propertyName)
    {
        return model.getProperties().getProperty(MPKEYPREFIX + propertyName);
    }

    private String getPropertyString(Model model, String propertyName, String def)
    {
        return StringUtils.defaultString(getProperty(model, propertyName), def);
    }

    // TODO: download custom licenses content
    private ExtensionLicense getExtensionLicense(License license, ExtensionLicenseManager licenseManager)
    {
        if (license.getName() == null) {
            return new ExtensionLicense("noname", null);
        }

        return createLicenseByName(license.getName(), licenseManager);
    }

    private ExtensionLicense createLicenseByName(String name, ExtensionLicenseManager licenseManager)
    {
        ExtensionLicense extensionLicense = licenseManager.getLicense(name);

        return extensionLicense != null ? extensionLicense : new ExtensionLicense(name, null);
    }
}
