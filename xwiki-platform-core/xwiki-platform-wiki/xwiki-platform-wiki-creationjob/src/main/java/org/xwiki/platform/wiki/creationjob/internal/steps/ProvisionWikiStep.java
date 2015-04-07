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
package org.xwiki.platform.wiki.creationjob.internal.steps;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiProvisionedEvent;
import org.xwiki.bridge.event.WikiProvisioningEvent;
import org.xwiki.bridge.event.WikiProvisioningFailedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.observation.ObservationManager;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiCreationStep;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.platform.wiki.creationjob.internal.ExtensionInstaller;
import org.xwiki.wiki.provisioning.WikiCopier;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Step that provision a new empty wiki with a template or an extension.
 *
 * @version $Id$
 * @since 7.0M2
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named("provision")
public class ProvisionWikiStep implements WikiCreationStep
{
    @Inject
    private WikiCopier wikiCopier;

    @Inject
    private ExtensionInstaller extensionInstaller;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void execute(WikiCreationRequest request) throws WikiCreationException
    {
        try {
            switch (request.getWikiSource()) {
                case EXTENSION:
                    sendWikiProvisioningEvent(request);

                    extensionInstaller.installExtension(request.getWikiId(), request.getExtensionId());

                    sendWikiProvisionedEvent(request);
                    break;
                case TEMPLATE:
                    sendWikiProvisioningEvent(request);

                    wikiCopier.copyDocuments(request.getTemplateId(), request.getWikiId(), false);
                    observationManager.notify(new WikiCopiedEvent(request.getTemplateId(), request.getWikiId()),
                        request.getTemplateId(), xcontextProvider.get());

                    sendWikiProvisionedEvent(request);
                    break;
                default:
                    // No source is defined, we let the wiki empty and DW will do the rest
                    break;
            }
        } catch (WikiManagerException | WikiCreationException e) {
            observationManager.notify(new WikiProvisioningFailedEvent(request.getWikiId()), request.getWikiId(),
                xcontextProvider.get());
            throw new WikiCreationException(String.format("Failed to provision the wiki [%s].",
                    request.getWikiId()), e);
        }
    }

    private void sendWikiProvisioningEvent(WikiCreationRequest request)
    {
        observationManager.notify(new WikiProvisioningEvent(request.getWikiId()), request.getWikiId(),
            xcontextProvider.get());
    }

    private void sendWikiProvisionedEvent(WikiCreationRequest request)
    {
        observationManager.notify(new WikiProvisionedEvent(request.getWikiId()), request.getWikiId(),
            xcontextProvider.get());
    }

    @Override
    public int getOrder()
    {
        return 3000;
    }
}
