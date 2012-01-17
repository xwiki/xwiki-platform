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
package org.xwiki.extension.job.internal;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;

/**
 * Extension installation related task.
 * <p>
 * This task generates related events.
 * 
 * @version $Id$
 */
@Component
@Named("install")
public class InstallJob extends AbstractJob<InstallRequest>
{
    /**
     * Used to manipulate local extension repository.
     */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /**
     * Used to install the extension itself depending of its type.
     */
    @Inject
    private ExtensionHandlerManager extensionHandlerManager;

    /**
     * Used to generate the install plan.
     */
    @Inject
    @Named("installplan")
    private Job installPlanJob;

    @Override
    protected void start() throws Exception
    {
        notifyPushLevelProgress(2);

        try {
            // Create the plan

            this.installPlanJob.start(getRequest());

            ExtensionPlan plan = (ExtensionPlan) this.installPlanJob.getStatus();

            List<LogEvent> log = plan.getLog(LogLevel.ERROR);
            if (!log.isEmpty()) {
                throw new InstallException("Failed to create install plan: " + log.get(0).getFormattedMessage(), log
                    .get(0).getThrowable());
            }

            notifyStepPropress();

            // Apply the plan

            Collection<ExtensionPlanAction> actions = plan.getActions();

            notifyPushLevelProgress(actions.size());

            try {
                for (ExtensionPlanAction action : actions) {
                    if (action.getAction() != Action.NONE) {
                        applyAction(action);
                    }

                    notifyStepPropress();
                }
            } finally {
                notifyPopLevelProgress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extension the extension to store
     * @return the local extension
     * @throws LocalExtensionRepositoryException failed to store extension
     */
    private LocalExtension storeExtension(Extension extension) throws LocalExtensionRepositoryException
    {
        LocalExtension localExtension;
        if (extension instanceof LocalExtension) {
            localExtension = (LocalExtension) extension;
        } else {
            localExtension = this.localExtensionRepository.storeExtension(extension);
        }

        return localExtension;
    }

    /**
     * @param extension the extension
     * @param previousExtension the previous extension when upgrading
     * @param namespace the namespace in which to perform the action
     * @param dependency indicate if the extension has been installed as dependency
     * @throws InstallException failed to install extension
     */
    private void installExtension(LocalExtension extension, LocalExtension previousExtension, String namespace,
        boolean dependency) throws InstallException
    {
        if (previousExtension == null) {
            this.extensionHandlerManager.install(extension, namespace);

            this.localExtensionRepository.installExtension(extension, namespace, dependency);

            this.observationManager.notify(new ExtensionInstalledEvent(extension.getId()), extension);
        } else {
            this.extensionHandlerManager.upgrade(previousExtension, extension, namespace);

            try {
                this.localExtensionRepository.uninstallExtension(previousExtension, namespace);
            } catch (UninstallException e) {
                this.logger.error("Failed to uninstall extension [" + previousExtension + "]", e);
            }

            this.localExtensionRepository.installExtension(extension, namespace, dependency);

            this.observationManager.notify(new ExtensionUpgradedEvent(extension.getId()), extension, previousExtension);
        }
    }

    /**
     * @param action the action to perform
     * @throws InstallException failed to install extension
     * @throws LocalExtensionRepositoryException failed to store extension
     */
    private void applyAction(ExtensionPlanAction action) throws InstallException, LocalExtensionRepositoryException
    {
        if (action.getAction() != Action.INSTALL && action.getAction() != Action.UPGRADE) {
            throw new InstallException("Unsupported action [" + action.getAction() + "]");
        }

        Extension extension = action.getExtension();
        String namespace = action.getNamespace();

        if (namespace != null) {
            this.logger.info("Installing extension [{}] on namespace [{}]", extension, namespace);
        } else {
            this.logger.info("Installing extension [{}]", extension);
        }

        notifyPushLevelProgress(2);

        try {
            // Store extension in local repository
            LocalExtension localExtension = storeExtension(extension);

            notifyStepPropress();

            // Install
            installExtension(localExtension, action.getPreviousExtension(), namespace, action.isDependency());

            if (namespace != null) {
                this.logger.info("Successfully installed extension [{}] on namespace [{}]", localExtension, namespace);
            } else {
                this.logger.info("Successfully installed extension [{}]", localExtension);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }
}
