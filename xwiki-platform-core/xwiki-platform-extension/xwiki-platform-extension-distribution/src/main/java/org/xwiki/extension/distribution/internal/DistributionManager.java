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
package org.xwiki.extension.distribution.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.job.FarmDistributionJob;
import org.xwiki.extension.distribution.internal.job.FarmDistributionJobStatus;
import org.xwiki.extension.distribution.internal.job.WikiDistributionJob;
import org.xwiki.extension.distribution.internal.job.WikiDistributionJobStatus;

/**
 * @version $Id$
 * @since 4.2M3
 */
@Role
public interface DistributionManager
{
    /**
     * The possible distribution states.
     */
    enum DistributionState
    {
        /** The distribution did not changed. */
        SAME,

        /** No distribution information can be found. */
        NONE,

        /** Probably something to do. */
        NEW,
        UPGRADE,
        DOWNGRADE,
        DIFFERENT
    }

    /**
     * @return the current distribution state
     */
    DistributionState getDistributionState();

    /**
     * @return the extension that defines the current distribution
     */
    CoreExtension getDistributionExtension();

    /**
     * @return the recommended user interface for main wikis
     * @since 5.0M1
     */
    ExtensionId getMainUIExtensionId();

    /**
     * @return the recommended user interface for sub wikis
     * @since 5.0M1
     */
    ExtensionId getWikiUIExtensionId();

    /**
     * @return the previous status of the distribution job (e.g. from last time the distribution was upgraded)
     * @since 5.0M1
     */
    FarmDistributionJobStatus getPreviousFarmJobStatus();

    /**
     * @param wiki the wiki form which to get the distribution status
     * @return the previous status of the distribution job (e.g. from last time the distribution was upgraded)
     * @since 5.0M1
     */
    WikiDistributionJobStatus getPreviousWikiJobStatus(String wiki);

    /**
     * Starts the distribution job.
     * 
     * @return the distribution job object that can be used to get information like the job status
     * @since 5.0M1
     */
    FarmDistributionJob startFarmJob();

    /**
     * Starts the distribution job.
     * 
     * @param wiki the wiki associated to the distribution wyzard
     * @return the distribution job object that can be used to get information like the job status
     * @since 5.0M1
     */
    WikiDistributionJob startWikiJob(String wiki);

    /**
     * @return the distribution job object that can be used to get information like the job status
     * @since 5.0M1
     */
    FarmDistributionJob getFarmJob();

    /**
     * @return the distribution job object that can be used to get information like the job status
     * @since 5.0M1
     */
    WikiDistributionJob getWikiJob(String wiki);
}
