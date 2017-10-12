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
package org.xwiki.extension.versioncheck.internal;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.versioncheck.ExtensionVersionCheckConfiguration;
import org.xwiki.extension.versioncheck.NewExtensionVersionAvailableEvent;
import org.xwiki.logging.Logger;
import org.xwiki.observation.ObservationManager;

/**
 * Periodically check if a new version of the environment extension is available.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Singleton
@Component(roles = EnvironmentVersionCheckTimer.class)
public class EnvironmentVersionCheckTimer implements Initializable
{
    private class EnvironmentVersionCheckTask extends TimerTask
    {
        /**
         * Store the last resolved version by the task.
         * This allows to trigger only one event per new version.
         */
        private Version latestVersion;

        private ExtensionId environmentExtensionId;

        /**
         * Instantiate a new {@link EnvironmentVersionCheckTask}.
         */
        EnvironmentVersionCheckTask()
        {
            environmentExtensionId = coreExtensionRepository.getEnvironmentExtension().getId();
            latestVersion = environmentExtensionId.getVersion();
        }

        /**
         * Check if a new version of the environment extension is available.
         * If so, send a {@link NewExtensionVersionAvailableEvent} to the {@link ObservationManager}.
         */
        private void performCheck()
        {
            boolean newVersionAvailable = false;

            try {
                for (Version version : extensionRepositoryManager.resolveVersions(
                        environmentExtensionId.getId(), 0, -1)) {

                    if (isCompatibleVersion(latestVersion, version)) {
                        newVersionAvailable = true;
                        latestVersion = version;
                    }
                }
            } catch (ResolveException e) {
                logger.warn("Failed to check if a new environment version is available: [{}]",
                        ExceptionUtils.getRootCauseMessage(e));
            }

            if (newVersionAvailable) {
                observationManager.notify(
                        new NewExtensionVersionAvailableEvent(environmentExtensionId, latestVersion), null, null);
            }
        }

        /**
         * Using the configuration variables given by {@link ExtensionVersionCheckConfiguration}, determine if the
         * given version should be considered as a "new available version" and therefore trigger a
         * {@link NewExtensionVersionAvailableEvent}.
         *
         * @param latestKnownVersion the latest known version
         * @param proposedVersion the version that should be checked for compatibility
         * @return true if the given version is compatible
         */
        private boolean isCompatibleVersion(Version latestKnownVersion, Version proposedVersion) {

            // Ensure that the given version is newer than our last known version
            if (proposedVersion.compareTo(latestKnownVersion) <= 0) {
                return false;
            }

            // If an allowed version preference is defined, try to apply it to the proposed version
            Pattern versionPattern = extensionVersionCheckConfiguration.allowedEnvironmentVersions();
            if (!versionPattern.pattern().isEmpty() && !versionPattern.matcher(proposedVersion.getValue()).matches()) {
                return false;
            }

            return proposedVersion.getType().compareTo(latestKnownVersion.getType()) >= 0;
        }

        @Override
        public void run()
        {
            if (extensionVersionCheckConfiguration.isEnvironmentCheckEnabled()) {
                performCheck();
            }
        }
    }

    @Inject
    private Logger logger;

    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private ExtensionVersionCheckConfiguration extensionVersionCheckConfiguration;

    @Override
    public void initialize() throws InitializationException
    {
        Timer timer = new Timer();

        EnvironmentVersionCheckTask versionCheckTask = new EnvironmentVersionCheckTask();

        timer.schedule(versionCheckTask,  1000 * extensionVersionCheckConfiguration.environmentCheckInterval());
    }
}
