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
package org.xwiki.extension.xar.internal.handler;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionHandler;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named("extension.xar.WikiCopiedListener")
public class WikiEventListener implements EventListener
{
    /**
     * The list of events observed.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new WikiCopiedEvent(), new WikiCreatedEvent(),
        new WikiDeletedEvent());

    @Inject
    private Provider<InstalledExtensionRepository> installedRepositoryProvider;

    @Inject
    @Named("xar")
    private Provider<ExtensionHandler> xarHandlerProvider;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "extension.xar.WikiCopiedListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object o, Object context)
    {
        if (event instanceof WikiCopiedEvent) {
            onWikiCopied((WikiCopiedEvent) event);
        } else if (event instanceof WikiCreatedEvent) {
            onWikiCreated((WikiCreatedEvent) event, (XWikiContext) context);
        } else if (event instanceof WikiDeletedEvent) {
            onWikiDeleted((WikiDeletedEvent) event);
        }
    }

    private void onWikiCopied(WikiCopiedEvent event)
    {
        String sourceNamespace = "wiki:" + event.getSourceWikiId();
        String targetNamespace = "wiki:" + event.getTargetWikiId();

        InstalledExtensionRepository installedRepository = this.installedRepositoryProvider.get();

        Collection<InstalledExtension> installedExtensions =
            installedRepository.getInstalledExtensions(sourceNamespace);

        for (InstalledExtension installedExtension : installedExtensions) {
            // TODO: take care of dependencies first
            if (!installedExtension.isInstalled(null)) {
                try {
                    installedRepository.installExtension(installedExtension, targetNamespace,
                        installedExtension.isDependency(sourceNamespace));
                } catch (InstallException e) {
                    this.logger.error(
                        "Failed to copy install state for extension [{}] from namespace [{}] to namespace [{}]",
                        installedExtension, sourceNamespace, targetNamespace, e);
                }
            }
        }
    }

    private void onWikiDeleted(WikiDeletedEvent event)
    {
        String namespace = "wiki:" + event.getWikiId();

        InstalledExtensionRepository installedRepository = this.installedRepositoryProvider.get();

        Collection<InstalledExtension> installedExtensions = installedRepository.getInstalledExtensions(namespace);

        for (InstalledExtension installedExtension : installedExtensions) {
            if (!installedExtension.isInstalled(null)) {
                try {
                    installedRepository.uninstallExtension(installedExtension, namespace);
                } catch (UninstallException e) {
                    this.logger.error("Failed to uninstall extension [{}] from namespace [{}]", installedExtension,
                        namespace, e);
                }
            }
        }
    }

    private void onWikiCreated(WikiCreatedEvent event, XWikiContext context)
    {
        String namespace = "wiki:" + event.getWikiId();

        InstalledExtensionRepository installedRepository = this.installedRepositoryProvider.get();

        Collection<InstalledExtension> installedExtensions = installedRepository.getInstalledExtensions(null);

        ExtensionHandler xarHandler = xarHandlerProvider.get();

        InstallRequest installRequest = new InstallRequest();
        installRequest.setProperty("user.reference", context.getUserReference());
        // TODO: make it interactive ? (require wiki creation to be job based)
        installRequest.setInteractive(false);

        for (InstalledExtension installedExtension : installedExtensions) {
            if (installedExtension.getType().equals(XarExtensionHandler.TYPE)) {
                installRequest.addExtension(installedExtension.getId());

                try {
                    xarHandler.install(installedExtension, namespace, installRequest);
                } catch (InstallException e) {
                    this.logger.error("Failed to import extension [{}] in wiki [{}]", installedExtension,
                        event.getWikiId(), e);
                }
            }
        }
    }
}
