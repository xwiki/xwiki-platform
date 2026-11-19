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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.distribution.internal.OrphanedExtensionsResolver;

/**
 * Step in charge of cleaning orphaned extension dependencies.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component
@Named(CleanExtensionsDistributionStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class CleanExtensionsDistributionStep extends AbstractExtensionDistributionStep
{
    /**
     * ID of the distribution step.
     */
    public static final String ID = "extension.clean";

    @Inject
    // The steps are serialized with the job status (see the org.xwiki.job.annotation.Serializable annotation on
    // AbstractDistributionStep), which skips the transient fields.
    @SuppressWarnings("java:S2065")
    private transient OrphanedExtensionsResolver orphanedExtensionsResolver;

    /**
     * Set the step id.
     */
    public CleanExtensionsDistributionStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            setState(State.COMPLETED);

            // Check in the current wiki
            if (!this.orphanedExtensionsResolver.getOrphanedExtensions(getWiki()).isEmpty()) {
                setState(null);
            }
        }
    }
}
