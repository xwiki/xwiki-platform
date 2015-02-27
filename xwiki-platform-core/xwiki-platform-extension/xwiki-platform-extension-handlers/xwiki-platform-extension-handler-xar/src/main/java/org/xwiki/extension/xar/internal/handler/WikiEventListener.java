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

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionHandler;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named("extension.xar.WikiCopiedListener")
public class WikiEventListener extends AbstractEventListener
{
    /**
     * The install request property that specifies which user triggered the install.
     */
    private static final String PROPERTY_USER_REFERENCE = "user.reference";

    @Inject
    private InstalledExtensionRepository installedRepository;

    /**
     * Used to install the extension itself depending of its type.
     */
    @Inject
    private ExtensionHandlerManager extensionHandlerManager;

    @Inject
    @Named("xar")
    private Provider<ExtensionHandler> xarHandlerProvider;

    @Inject
    private Logger logger;

    public WikiEventListener()
    {
        super("extension.xar.WikiCopiedListener", new WikiCopiedEvent(), new WikiCreatedEvent(), new WikiDeletedEvent());
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

        Collection<InstalledExtension> installedExtensions =
            this.installedRepository.getInstalledExtensions(sourceNamespace);

        for (InstalledExtension installedExtension : installedExtensions) {
            copyInstalledExtension(installedExtension, sourceNamespace, targetNamespace);
        }
    }

    private void copyInstalledExtension(InstalledExtension installedExtension, String sourceNamespace,
        String targetNamespace)
    {
        if (!installedExtension.isInstalled(targetNamespace) && !installedExtension.isInstalled(null)) {
            // Copy dependencies first
            for (ExtensionDependency dependency : installedExtension.getDependencies()) {
                InstalledExtension installedDependency =
                    this.installedRepository.getInstalledExtension(dependency.getId(), sourceNamespace);
                if (installedDependency != null) {
                    copyInstalledExtension(installedDependency, sourceNamespace, targetNamespace);
                }
            }

            // Copy extension
            try {
                // Installed extension
                this.extensionHandlerManager.initialize(installedExtension, targetNamespace);

                // Register extension as installed
                this.installedRepository.installExtension(installedExtension, targetNamespace,
                    installedExtension.isDependency(sourceNamespace));
            } catch (ExtensionException e) {
                this.logger.error("Failed to copy extension [{}] from namespace [{}] to namespace [{}]",
                    installedExtension, sourceNamespace, targetNamespace, e);
            }
        }
    }

    private void onWikiDeleted(WikiDeletedEvent event)
    {
        String namespace = "wiki:" + event.getWikiId();

        Collection<InstalledExtension> installedExtensions = this.installedRepository.getInstalledExtensions(namespace);

        for (InstalledExtension installedExtension : installedExtensions) {
            if (!installedExtension.isInstalled(null)) {
                try {
                    this.installedRepository.uninstallExtension(installedExtension, namespace);
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

        Collection<InstalledExtension> installedExtensions = this.installedRepository.getInstalledExtensions(null);

        InstallRequest installRequest = new InstallRequest();
        DocumentReference userReference = context.getUserReference();
        if (userReference != null) {
            installRequest.setProperty(PROPERTY_USER_REFERENCE, userReference);
            // We set the string value because the extension repository doesn't know how to serialize/parse an extension
            // property whose value is a DocumentReference, and adding support for it requires considerable refactoring
            // because ExtensionPropertySerializers are not components (they are currently hard-coded).
            installRequest.setExtensionProperty(PROPERTY_USER_REFERENCE, userReference.toString());
        }
        installRequest.setVerbose(false);
        // TODO: make it interactive ? (require wiki creation to be job based)
        installRequest.setInteractive(false);

        ExtensionHandler xarHandler = this.xarHandlerProvider.get();

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
