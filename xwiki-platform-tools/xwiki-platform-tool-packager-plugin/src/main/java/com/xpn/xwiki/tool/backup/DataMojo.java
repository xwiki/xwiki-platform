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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.script.ScriptExtensionRewriter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.tool.extension.util.ExtensionArtifact;
import org.xwiki.tool.utils.AbstractOldCoreMojo;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Maven 2 plugin to generate XWiki data folder (database and extensions).
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.2
 */
@Mojo(name = "data", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
public class DataMojo extends AbstractOldCoreMojo
{
    /**
     * The list of artifacts (and their dependencies) to install.
     */
    @Parameter
    private List<ExtensionArtifact> includes;

    @Override
    public void executeInternal() throws MojoExecutionException
    {
        InstallRequest installRequest = new InstallRequest();

        // Allow modifying root namespace
        installRequest.setRootModificationsAllowed(true);

        // Make sure jars are installed on root
        // TODO: use a less script oriented class
        ScriptExtensionRewriter rewriter = new ScriptExtensionRewriter();
        rewriter.installExtensionTypeOnRootNamespace("jar");
        rewriter.installExtensionTypeOnRootNamespace("webjar");
        installRequest.setRewriter(rewriter);

        // Use superadmin as pages author
        installRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            new DocumentReference("xwiki", "XWiki", XWikiRightService.SUPERADMIN_USER));

        this.extensionHelper.install(this.includes, installRequest, "wiki:xwiki", null);
    }
}
