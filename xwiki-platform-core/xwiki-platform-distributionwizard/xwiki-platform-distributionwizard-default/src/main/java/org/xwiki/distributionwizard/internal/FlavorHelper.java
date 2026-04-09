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
package org.xwiki.distributionwizard.internal;

import java.util.Optional;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.script.ScriptExtensionRewriter;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.platform.flavor.FlavorManager;

import com.xpn.xwiki.XWikiContext;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Component(roles = FlavorHelper.class)
@Singleton
public class FlavorHelper
{
    private static final String FLAVOR_SELECTED_KEY = "flavor.selected";

    @Inject
    private FlavorManager flavorManager;

    @Inject
    private DistributionManager distributionManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private JobExecutor jobExecutor;

    private Namespace getNamespace()
    {
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        String wiki = distributionJob.getRequest().getWiki();
        return new Namespace("wiki", wiki);
    }

    public boolean isFlavorInstalled()
    {
        Namespace namespace = getNamespace();
        InstalledExtension flavor = this.flavorManager.getFlavorExtension(namespace);
        return (flavor != null && flavor.isValid(namespace.toString()));
    }

    public Optional<Extension> getSelectedFlavor()
    {
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        Object property = distributionJob.getProperty(FLAVOR_SELECTED_KEY);
        if (property instanceof Extension selectedFlavor) {
            return Optional.of(selectedFlavor);
        } else {
            return Optional.empty();
        }
    }

    public void selectFlavor(Extension flavor)
    {
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        distributionJob.setProperty(FLAVOR_SELECTED_KEY, flavor);
    }

    public Job startSelectedFlavorInstallation() throws DistributionWizardException, JobException
    {
        Optional<Extension> selectedFlavorOpt = getSelectedFlavor();
        if (selectedFlavorOpt.isEmpty()) {
            throw new DistributionWizardException("No flavor selected");
        }
        Extension flavorExtension =  selectedFlavorOpt.get();
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();

        Namespace namespace = getNamespace();
        // start install plan
        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(
            ExtensionRequest.getJobId(
                ExtensionRequest.JOBID_ACTION_PREFIX, flavorExtension.getId().getId(), namespace.toString()));
        installRequest.setInteractive(true);
        installRequest.addExtension(flavorExtension.getId());
        installRequest.addNamespace(namespace.toString());

        XWikiContext xcontext = this.contextProvider.get();

        // Indicate if it's allowed to do modification on root namespace
        installRequest.setRootModificationsAllowed(xcontext.isMainWiki(distributionJob.getRequest().getWiki()));

        // Allow overwritting a few things in extensions descriptors
        installRequest.setRewriter(new ScriptExtensionRewriter());
        return this.jobExecutor.execute(InstallJob.JOBTYPE, installRequest);
    }
}
