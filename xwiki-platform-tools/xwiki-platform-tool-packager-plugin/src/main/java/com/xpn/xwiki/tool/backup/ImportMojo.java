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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.xwiki.tool.utils.LogUtils;

/**
 * Maven 2 plugin to import a set of XWiki documents into an existing database.
 *
 * @version $Id$
 */
@Mojo(name = "import", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class ImportMojo extends AbstractImportMojo
{
    /**
     * @see com.xpn.xwiki.tool.backup.Importer#importDocuments(java.io.File, String)
     */
    @Parameter
    protected File sourceDirectory;

    @Override
    public void executeInternal() throws MojoExecutionException, MojoFailureException
    {
        LogUtils.configureXWikiLogs();
        Importer importer = new Importer(this.oldCoreHelper);

        if (this.sourceDirectory != null) {
            try {
                importer.importDocuments(this.sourceDirectory, this.wiki);
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to import XWiki documents", e);
            }
        } else {
            try {
                importDependencies(importer, this.wiki, this.hibernateConfig);
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to import XAR dependencies", e);
            }
        }
    }
}
