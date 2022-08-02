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
package org.xwiki.extension.distribution.internal.job.step;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.UpgradePlanJob;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * List and allow to upgrade outdated extensions.
 * 
 * @version $Id$
 * @since 5.0RC1
 */
@Component
@Named(OutdatedExtensionsDistributionStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class OutdatedExtensionsDistributionStep extends AbstractExtensionDistributionStep
{
    public static final String ID = "extension.outdatedextensions";

    @Inject
    private transient InstalledExtensionRepository installedRepository;

    @Inject
    private transient Logger logger;

    public OutdatedExtensionsDistributionStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            setState(State.COMPLETED);

            if (isMainWiki()) {
                Collection<InstalledExtension> installedExtensions = this.installedRepository.getInstalledExtensions();

                // Upgrade outdated extensions only when there is invalid extensions
                for (InstalledExtension extension : installedExtensions) {
                    Collection<String> installedNamespaces = extension.getNamespaces();
                    if (installedNamespaces == null) {
                        if (!extension.isValid(null)) {
                            this.logger.debug("Enabling outdate extension step on main wiki "
                                + "because extension [{}] is invalid on root namespace", extension.getId());

                            setState(null);
                            break;
                        }
                    } else {
                        for (String installedNamespace : installedNamespaces) {
                            if (!extension.isValid(installedNamespace)) {
                                this.logger.debug(
                                    "Enabling outdate extension step on main wiki "
                                        + "because extension [{}] is invalid on namespace [{}]",
                                    extension.getId(), installedNamespace);

                                setState(null);
                                break;
                            }
                        }
                    }
                }
            } else {
                Namespace currentNamespace = getNamespace();
                Collection<InstalledExtension> installedExtensions =
                    this.installedRepository.getInstalledExtensions(currentNamespace.toString());

                // Upgrade outdated extensions only when there is invalid extensions
                for (InstalledExtension extension : installedExtensions) {
                    if (!extension.isValid(currentNamespace.toString())) {
                        this.logger.debug(
                            "Enabling outdate extension step on wiki [{}]" + "because extension [{}] is invalid",
                            getWiki(), extension.getId());

                        setState(null);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void executeNonInteractive() throws Exception
    {
        // Handle the extensions installed at the root level
        if (isMainWiki()) {
            upgradeOutdatedExtensions(null);
        }

        // Handle the extensions installed on the current namespace
        upgradeOutdatedExtensions(getNamespace().toString());

        // Complete task
        setState(State.COMPLETED);
    }

    private void upgradeOutdatedExtensions(String namespace) throws JobException, InterruptedException
    {
        Collection<InstalledExtension> installedExtensions = this.installedRepository.getInstalledExtensions(namespace);

        // Upgrade outdated extensions only when there is invalid extensions
        for (InstalledExtension extension : installedExtensions) {
            if (!extension.isValid(namespace)) {
                // Upgrade/repair invalid extensions
                repair(extension.getId(), namespace);
            }
        }
    }

    private void repair(ExtensionId invalidExtension, String namespace) throws JobException, InterruptedException
    {
        // Find valid extension version
        ExtensionPlan plan = createRepairPlan(invalidExtension, namespace);

        // Install valid extension version
        if (plan.getTree().size() > 0) {
            ExtensionPlanAction action = plan.getTree().iterator().next().getAction();
            install(action.getExtension().getId(), namespace, false);
        }
    }

    private ExtensionPlan createRepairPlan(ExtensionId invalidExtension, String namespace)
        throws JobException, InterruptedException
    {
        // Install the default UI
        InstallRequest installRequest = new InstallRequest();
        installRequest
            .setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_PLAN_PREFIX, invalidExtension.getId(), namespace));
        installRequest.addExtension(invalidExtension);
        installRequest.addNamespace(namespace);

        // Don't take any risk
        installRequest.setUninstallAllowed(false);

        // Indicate if it's allowed to do modification on root namespace
        installRequest.setRootModificationsAllowed(true);

        installRequest.setInteractive(false);

        // Set user to use as author (for example) to be superadmin
        installRequest.setExtensionProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            XWikiRightService.SUPERADMIN_USER_FULLNAME);

        Job job = this.jobExecutor.execute(UpgradePlanJob.JOBTYPE, installRequest);
        job.join();

        return (ExtensionPlan) job.getStatus();
    }
}
