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
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Optional step to install sub-wikis default UI extensions.
 * 
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named(WikisFlavorDistributionStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikisFlavorDistributionStep extends AbstractDistributionStep
{
    /**
     * ID of the distribution step. 
     */
    public static final String ID = "extension.flavor.wikis";

    /**
     * The flavor manager.
     */
    private transient FlavorManager flavorManager;

    @Inject
    private transient InstalledExtensionRepository installedRepository;

    @Inject
    private transient Logger logger;

    /**
     * Constructs a new WikisFlavorDistributionStep.
     */
    public WikisFlavorDistributionStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            setState(State.COMPLETED);
            
            if (!isMainWiki()) {
                return;
            }
            
            WikiDescriptorManager wikiDescriptorManager = this.wikiDescriptorManagerProvider.get();

            Collection<String> wikiIds;
            try {
                wikiIds = wikiDescriptorManager.getAllIds();
            } catch (WikiManagerException e) {
                this.logger.error("Failed to get the list of wikis", e);
                setState(null);
                return;
            }

            String mainWiki = wikiDescriptorManager.getMainWikiId();

            // Enable if any of the wikis has no valid top level flavor extension
            for (String wikiId : wikiIds) {
                if (mainWiki.equals(wikiId)) {
                    continue;
                }
                String namespace = "wiki:" + wikiId;
                ExtensionId flavor = flavorManager.getFlavorOfWiki(getWiki());
                if (flavor == null || !installedRepository.getInstalledExtension(flavor).isValid(namespace)) {
                    setState(null);
                    return;
                }
            }
        }
    }
}
