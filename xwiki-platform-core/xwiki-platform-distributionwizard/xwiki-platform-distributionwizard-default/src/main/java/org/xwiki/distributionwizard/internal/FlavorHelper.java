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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
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
    public static final String NO_FLAVOR_SELECTION = "noFlavor";
    private static final String FLAVOR_SELECTED_KEY = "flavor.selected";
    // FIXME: check the regex
    private static final Pattern FLAVOR_EXTENSION_REGEX =
        Pattern.compile("^(?<flavorId>.+)(:::)(?<flavorVersion>.+)$");

    @Inject
    private FlavorManager flavorManager;

    @Inject
    private DistributionManager distributionManager;

    @Inject
    private ExtensionManager extensionManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    private JobExecutor jobExecutor;

    private Namespace getNamespace()
    {
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        String wiki = distributionJob.getRequest().getWiki();
        return new Namespace("wiki", wiki);
    }

    public void handleFlavorAnswer(String selectedFlavor) throws DistributionWizardException
    {
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        if (NO_FLAVOR_SELECTION.equals(selectedFlavor)) {
            distributionJob.setProperty(FLAVOR_SELECTED_KEY, NO_FLAVOR_SELECTION);
        } else {
            Matcher matcher = FLAVOR_EXTENSION_REGEX.matcher(selectedFlavor);
            if (matcher.matches()) {
                ExtensionId extensionId =
                    new ExtensionId(matcher.group("flavorId"), matcher.group("flavorVersion"));
                try {
                    Extension flavorExtension = this.extensionManager.resolveExtension(extensionId);
                    distributionJob.setProperty(FLAVOR_SELECTED_KEY, flavorExtension);
                } catch (ResolveException e) {
                    throw new DistributionWizardException(
                        String.format("Error while resolving extension [%s] for the selected flavor: [%s]",
                            extensionId, selectedFlavor),
                        e);
                }
            } else {
                throw new DistributionWizardException(
                    String.format("The selected flavor [%s] doesn't match the expected format "
                        + "[<flavorId>:::<flavorVersion>]", selectedFlavor));
            }

        }
    }

    public boolean isFlavorInstalled()
    {
        Namespace namespace = getNamespace();
        InstalledExtension flavor = this.flavorManager.getFlavorExtension(namespace);
        return (flavor != null && flavor.isValid(namespace.toString()));
    }

    public InstalledExtension getInstalledFlavor()
    {
        Namespace namespace = getNamespace();
        return this.flavorManager.getFlavorExtension(namespace);
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

    public boolean isNoFlavorSelected()
    {
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        Object property = distributionJob.getProperty(FLAVOR_SELECTED_KEY);
        if (property instanceof String selectedFlavor) {
            return NO_FLAVOR_SELECTION.equals(selectedFlavor);
        } else {
            return false;
        }
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
        installRequest.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS, true);
        installRequest.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_USER, true);
        installRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            this.documentAccessBridge.getCurrentUserReference());
        return this.jobExecutor.execute(InstallJob.JOBTYPE, installRequest);
    }
}
