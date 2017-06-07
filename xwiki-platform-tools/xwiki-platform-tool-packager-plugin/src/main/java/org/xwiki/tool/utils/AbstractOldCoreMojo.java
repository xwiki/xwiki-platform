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
package org.xwiki.tool.utils;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.xwiki.tool.extension.util.AbstractExtensionMojo;

/**
 * Base class for Maven plugins manipulating OldCore APIs.
 * 
 * @version $Id$
 * @since 9.4RC1
 */
public abstract class AbstractOldCoreMojo extends AbstractExtensionMojo
{
    @Parameter(defaultValue = "xwiki")
    protected String wiki;

    @Parameter(defaultValue = "${basedir}/src/main/packager/hibernate.cfg.xml")
    protected File hibernateConfig;

    protected OldCoreHelper oldCoreHelper;

    @Override
    protected void before() throws MojoExecutionException
    {
        super.before();

        this.oldCoreHelper =
            OldCoreHelper.create(this.extensionHelper.getComponentManager(), this.wiki, this.hibernateConfig);

        System.setProperty("org.slf4j.simpleLogger.log.hsqldb", "warn");
    }

    @Override
    protected void after() throws MojoExecutionException
    {
        try {
            this.oldCoreHelper.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to close OldCoreHelper", e);
        }

        super.after();
    }
}
