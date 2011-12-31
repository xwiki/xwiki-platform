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
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;

/**
 * Extension uninstallation related task.
 * <p>
 * This task generates related events.
 * 
 * @version $Id$
 */
@Component
@Named("uninstall")
public class UninstallJob extends AbstractJob<UninstallRequest>
{
    /**
     * Used to manipulate local repository.
     */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /**
     * Used to uninstall extensions.
     */
    @Inject
    private ExtensionHandlerManager extensionHandlerManager;

    /**
     * Used to generate the install plan.
     */
    @Inject
    @Named("uninstallplan")
    private Job uninstallPlanJob;

    @Override
    protected void start() throws Exception
    {
        notifyPushLevelProgress(2);

        try {
            // Create the plan

            this.uninstallPlanJob.start(getRequest());

            ExtensionPlan plan = (ExtensionPlan) this.uninstallPlanJob.getStatus();

            List<LogEvent> log = plan.getLog(LogLevel.ERROR);
            if (!log.isEmpty()) {
                throw new UninstallException("Failed to create install plan: " + log.get(0).getFormattedMessage(), log
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
     * @param action the action to perform
     * @throws UninstallException failed to uninstall extension
     */
    private void applyAction(ExtensionPlanAction action) throws UninstallException
    {
        if (action.getAction() != Action.UNINSTALL) {
            throw new UninstallException("Unsupported action [" + action.getAction() + "]");
        }

        LocalExtension localExtension = (LocalExtension) action.getExtension();
        String namespace = action.getNamespace();

        if (namespace != null) {
            this.logger.info("Uninstalling extension [{}] on namespace [{}]", localExtension, namespace);
        } else {
            this.logger.info("Uninstalling extension [{}]", localExtension);
        }

        notifyPushLevelProgress(2);

        try {
            // Unload extension
            this.extensionHandlerManager.uninstall(localExtension, namespace);

            notifyStepPropress();

            // Uninstall from local repository
            this.localExtensionRepository.uninstallExtension(localExtension, namespace);

            if (namespace != null) {
                this.logger
                    .info("Successfully uninstalled extension [{}] on namespace [{}]", localExtension, namespace);
            } else {
                this.logger.info("Successfully uninstalled extension [{}]", localExtension);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }
}
