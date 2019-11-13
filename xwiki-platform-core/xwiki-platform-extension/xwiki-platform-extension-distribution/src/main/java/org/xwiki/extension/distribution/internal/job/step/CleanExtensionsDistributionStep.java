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
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;

/**
 * Base step in charge of cleaning orphan extension.
 * 
 * @version $Id$
 * @since 11.10RC1
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
    private transient InstalledExtensionRepository installedRepository;

    @Inject
    private transient Logger logger;

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

            // Check in the current namespace
            if (containsOrphanExtension(getNamespace().toString())) {
                setState(null);
            }
        }
    }

    private boolean containsOrphanExtension(String namespace)
    {
        Collection<InstalledExtension> installedExtensions = this.installedRepository.getInstalledExtensions(namespace);

        for (InstalledExtension installedExtension : installedExtensions) {
            if (installedExtension.isDependency(namespace)) {
                try {
                    // Check in root namespace when on main wiki
                    if (installedExtension.isInstalled(null) && isMainWiki()) {
                        if (this.installedRepository.getBackwardDependencies(installedExtension.getId(), true)
                            .isEmpty()) {
                            return true;
                        }
                    } else {
                        if (this.installedRepository
                            .getBackwardDependencies(installedExtension.getId().getId(), namespace, true).isEmpty()) {
                            return true;
                        }
                    }
                } catch (ResolveException e) {
                    this.logger.warn("Failed to resolve backward dependencies for extension id [{}] on namespace [{}]",
                        installedExtension.getId().getId(), namespace);
                }
            }
        }

        return false;
    }
}
