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
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.ExtensionQuery.COMPARISON;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

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
            setState(State.COMPLETED);

            ExtensionQuery extensionQuery = new ExtensionQuery();
            extensionQuery.addFilter(field, "flavor", COMPARISON.EQUAL);

            String namespace = "wiki:" + getWiki();

            IterableResult<InstalledExtension> result = this.installedRepository.searchInstalledExtensions(namespace, extensionQuery);

            if (result.getSize() > 0) {
                InstalledExtension extension = result.iterator().next();
                setState(null);
            }
            
            
            
            
            
            if (isMainWiki()) {
                WikiDescriptorManager wikiDescriptorManager = this.wikiDescriptorManagerProvider.get();

                Collection<String> wikiIds;
                try {
                    wikiIds = wikiDescriptorManager.getAllIds();
                } catch (WikiManagerException e) {
                    this.logger.error("Failed to get the list of wikis", e);
                    setState(null);
                    return;
                }

                ExtensionId wikiExtensionUI = this.distributionManager.getWikiUIExtensionId();

                for (String wikiId : wikiIds) {
                    if (!wikiDescriptorManager.getMainWikiId().equals(wikiId)) {
                        String namespace = "wiki:" + wikiId;

                        // Only if the UI is not already installed
                        if (wikiExtensionUI != null) {
                            InstalledExtension installedExtension =
                                this.installedRepository.getInstalledExtension(wikiExtensionUI.getId(), namespace);
                            if (installedExtension == null
                                || !installedExtension.getId().getVersion().equals(wikiExtensionUI.getVersion())) {
                                setState(null);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
