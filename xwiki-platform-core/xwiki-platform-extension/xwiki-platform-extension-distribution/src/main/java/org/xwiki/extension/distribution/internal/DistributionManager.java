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
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;

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

        /**
         * No previous state.
         */
        NEW,

        /**
         * Previous state is an older version of the same distribution.
         */
        UPGRADE,

        /**
         * Previous state is a newer version of the same distribution.
         */
        DOWNGRADE,

        /**
         * Previous state is a different distribution.
         */
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
     * @return the recommended user interface for {@link #getDistributionExtension()}
     */
    ExtensionId getUIExtensionId();

    /**
     * @return the previous status of the distribution job (e.g. from last time the distribution was upgraded)
     */
    DistributionJobStatus getPreviousJobStatus();

    /**
     * Starts the distribution job.
     * 
     * @return the distribution job object that can be used to get information like the job status
     */
    DistributionJob startJob();

    /**
     * @return the distribution job object that can be used to get information like the job status
     */
    DistributionJob getJob();

    /**
     * @return true it's allowed to display the Distribution Wizard in the current context
     */
    boolean canDisplayDistributionWizard();
}
