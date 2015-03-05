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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.ExtensionQuery.COMPARISON;
import org.xwiki.extension.repository.search.SearchException;

/**
 * Install and upgrade flavor extension.
 * 
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named(FlavorDistributionStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class FlavorDistributionStep extends AbstractDistributionStep
{
    public static final String ID = "extension.flavor";

    @Inject
    private transient InstalledExtensionRepository installedRepository;

    /**
     * The component used to get information about the current distribution.
     */
    @Inject
    protected transient DistributionManager distributionManager;

    @Inject
    private transient Logger logger;

    public FlavorDistributionStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            InstalledExtension flavor = getFlavor();

            // If the extension is invalid it probably means it needs to be upgraded
            if (flavor.isValid(getNamespace())) {
                setState(State.COMPLETED);
            }
        }
    }

    private InstalledExtension getFlavor()
    {
        IterableResult<InstalledExtension> result;
        try {
            ExtensionQuery extensionQuery = new ExtensionQuery();
            extensionQuery.addFilter(Extension.FIELD_CATEGORY, "flavor", COMPARISON.EQUAL);
            result = this.installedRepository.searchInstalledExtensions(getNamespace(), extensionQuery);
        } catch (SearchException e) {
            this.logger.error("Failed to search installed extension", e);
            return null;
        }

        if (result.getSize() > 0) {
            return result.iterator().next();
        }

        return null;
    }
}
