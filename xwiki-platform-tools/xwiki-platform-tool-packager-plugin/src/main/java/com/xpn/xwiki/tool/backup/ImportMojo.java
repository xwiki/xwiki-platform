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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.internal.local.DefaultLocalExtension;

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
     * @parameter default-value = "${project.build.directory}/datas/"
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

        Set<Artifact> artifacts = this.project.getArtifacts();
        if (artifacts != null) {
            ComponentManager componentManager = (ComponentManager) xcontext.get(ComponentManager.class.getName());

            LocalExtensionRepository localExtensionRepository =
                componentManager.getInstance(LocalExtensionRepository.class);
            InstalledExtensionRepository installedExtensionRepository =
                componentManager.getInstance(InstalledExtensionRepository.class);

            for (Artifact artifact : artifacts) {
                if (!artifact.isOptional()) {
                    if ("xar".equals(artifact.getType())) {
                        getLog().info("  ... Importing XAR file: " + artifact.getFile());

                        // Import XAR into database
                        importer.importXAR(artifact.getFile(), null, xcontext);

                        // Install extension
                        DefaultLocalExtension extension =
                            new DefaultLocalExtension(null, new ExtensionId(artifact.getGroupId() + ':'
                                + artifact.getArtifactId(), artifact.getVersion()), artifact.getType());

                        LocalExtension localExtension = localExtensionRepository.storeExtension(extension);
                        installedExtensionRepository.installExtension(localExtension, "xwiki", true);

                        // TODO: add other project informations and especially the features and dependencies
                    }
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
}
