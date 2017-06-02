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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.tool.utils.AbstractOldCoreMojo;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Maven 2 plugin to generate XWiki data folder (database and extensions).
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.2
 */
public abstract class AbstractImportMojo extends AbstractOldCoreMojo
{
    public static final String MPKEYPREFIX = "xwiki.extension.";

    public static final String MPNAME_NAME = "name";

    public static final String MPNAME_SUMMARY = "summary";

    public static final String MPNAME_WEBSITE = "website";

    public static final String MPNAME_FEATURES = "features";

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
     * @param importer the importer
     * @param databaseName some database name (TODO: find out what this name is really)
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC driver, username and
     *            password, etc)
     * @throws Exception failed to import dependencies
     */
    protected void importDependencies(Importer importer, String databaseName, File hibernateConfig) throws Exception
    {
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
                    installXAR(artifact, importer, this.oldCoreHelper.getXWikiContext());
                }
            }
        }
    }

    // TODO: actually use Extension Manager instead
    protected void installXAR(Artifact artifact, Importer importer, XWikiContext xcontext) throws Exception
    {
        getLog().info("  ... Importing XAR file: " + artifact.getFile());

        // Import XAR into database
        int nb = importer.importXAR(artifact.getFile(), null, xcontext);

        getLog().info("  ..... Imported " + nb + " documents");

        // Install extension
        installExtension(artifact, this.xarNamespace);
    }

    protected void installExtension(Artifact artifact, String namespace) throws MojoExecutionException
    {
        // We need to distinguish between extensions installed explicitly and their transitive dependencies.
        // We have to create our own Set because Maven changes the fields from the dependency Artifacts (e.g. resolves
        // their version) after they are added to the Set of dependencies and this causes the hash code to change. As a
        // result the #contains(Artifact) method doesn't work as expected because it uses the new hash code.
        Set<Artifact> directDependencies = new HashSet<>(this.project.getDependencyArtifacts());

        InstalledExtension installedExtension =
            this.extensionHelper.registerInstalledExtension(artifact, namespace, !directDependencies.contains(artifact),
                Collections.<String, Object>singletonMap("user.reference", this.installUser));

        getLog().info("  ... Registered extension [" + installedExtension + "]");
    }
}
