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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Maven 2 plugin to export a set of XWiki documents from an existing database to the file system.
 *
 * @version $Id$
 */
@Mojo(name = "export")
public class ExportMojo extends AbstractMojo
{
    /**
     * @see com.xpn.xwiki.tool.backup.Exporter#exportDocuments(java.io.File, String, java.io.File)
     */
    @Parameter(defaultValue="xwiki")
    private String databaseName;

    /**
     * @see com.xpn.xwiki.tool.backup.Exporter#exportDocuments(java.io.File, String, java.io.File)
     */
    @Parameter(defaultValue="${basedir}/src/main/packager/hibernate.cfg.xml")
    private File hibernateConfig;

    /**
     * @see com.xpn.xwiki.tool.backup.Exporter#exportDocuments(java.io.File, String, java.io.File)
     */
    @Parameter(defaultValue="${project.build.directory}/export")
    private File exportDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Ensure that the export directory exists before performing the export
        this.exportDirectory.mkdirs();

        Exporter exporter = new Exporter();

        try {
            exporter.exportDocuments(this.exportDirectory, this.databaseName, this.hibernateConfig);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to export XWiki documents", e);
        }
    }
}
