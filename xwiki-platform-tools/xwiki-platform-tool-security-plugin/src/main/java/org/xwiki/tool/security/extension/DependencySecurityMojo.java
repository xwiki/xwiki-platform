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
package org.xwiki.tool.security.extension;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.index.security.SecurityVulnerabilityDescriptor;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.security.analyzer.ExtensionSecurityAnalyzer;
import org.xwiki.extension.security.internal.analyzer.osv.OsvExtensionSecurityAnalyzer;
import org.xwiki.extension.version.Version.Type;
import org.xwiki.tool.extension.util.AbstractExtensionMojo;

/**
 * @version $Id$
 * @since 18.0.0RC1
 * @since 17.10.3
 */
@Mojo(name = "check-dependencies", defaultPhase = LifecyclePhase.VALIDATE,
    requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true)
public class DependencySecurityMojo extends AbstractExtensionMojo
{
    @Override
    public void executeInternal() throws MojoExecutionException
    {
        ExtensionSecurityAnalyzer analyzer;
        try {
            analyzer = this.extensionHelper.getComponentManager().getInstance(ExtensionSecurityAnalyzer.class,
                OsvExtensionSecurityAnalyzer.ID);
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Failed to get the ExtensionSecurityAnalyzer component.", e);
        }

        // Create the install plan for the current project.
        ExtensionPlan plan = this.extensionHelper.createInstallPlan(
            this.extensionHelper.toArtifactModel(this.project.getArtifact(), this.project.getModel()), "wiki:xwiki");

        // Check the known security vulnerabilities for each extension to be installed.
        boolean foundVulnerabily = false;
        for (ExtensionPlanAction action : plan.getActions()) {
            if (action.getExtension().getId().getVersion().getType() != Type.SNAPSHOT) {
                if (action.getAction() == Action.INSTALL) {
                    foundVulnerabily |= check(action.getExtension(), analyzer);
                } else if (action.getAction() != Action.NONE) {
                    throw new MojoExecutionException("Unexpected action [" + action
                        + "] when storing dependencies of project [" + this.project + "]");
                }
            }
        }

        if (foundVulnerabily) {
            throw new MojoExecutionException("Security vulnerabilities were found in the dependencies of the project."
                + " See the error log for more details.");
        }
    }

    private boolean check(Extension extension, ExtensionSecurityAnalyzer analyzer) throws MojoExecutionException
    {
        getLog().info("* Check extension [" + extension + "]");

        try {
            ExtensionSecurityAnalysisResult result = analyzer.analyze(extension);

            if (result != null) {
                List<SecurityVulnerabilityDescriptor> securityVulnerabilities = result.getSecurityVulnerabilities();

                if (CollectionUtils.isNotEmpty(securityVulnerabilities)) {
                    getLog().error("Security vulnerabilities found for extension [" + extension + "]:");

                    for (SecurityVulnerabilityDescriptor vulnerability : securityVulnerabilities) {
                        getLog().error("  - ID: " + vulnerability.getId() + ", Score: " + vulnerability.getScore()
                            + ", FixVersion: " + vulnerability.getFixVersion());
                    }

                    return true;
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException(
                "Failed to analyze security vulnerabilities for extension [" + extension + "].", e);
        }

        return false;
    }
}
