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
package org.xwiki.extension.distribution.internal.job;

import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.job.Job;

public interface DistributionJob extends Job
{
    DistributionJobStatus getPreviousStatus();

    ExtensionId getUIExtensionId();

    DistributionStep getCurrentStep();

    @Override
    DistributionRequest getRequest();

    @Override
    DistributionJobStatus getStatus();

    /**
     * Wait until the job is fully initialized.
     * 
     * @throws InterruptedException if the current thread is interrupted (and interruption of thread suspension is
     *             supported)
     */
    void awaitReady() throws InterruptedException;

    /**
     * @since 11.7RC1
     * @since 11.3.3
     * @since 10.11.10
     */
    void setProperty(String key, Object value);

    /**
     * @since 11.7RC1
     * @since 11.3.3
     * @since 10.11.10
     */
    Object getProperty(String key);
}
