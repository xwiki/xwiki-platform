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
package org.xwiki.extension.xar.internal.repository;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.UnsupportedNamespaceException;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Maintain extension stored in {@link XarInstalledExtensionRepository} in sync with the standard
 * {@link InstalledExtensionRepository} component.
 * 
 * @version $Id$
 * @since 8.1M2
 */
@Component
@Named("org.xwiki.extension.xar.internal.repository.InstalledExtensionSynchronizer")
@Singleton
public class InstalledExtensionSynchronizer extends AbstractEventListener
{
    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository xarRepository;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public InstalledExtensionSynchronizer()
    {
        super(InstalledExtensionSynchronizer.class.getName(), new ExtensionInstalledEvent(),
            new ExtensionUninstalledEvent(), new ExtensionUpgradedEvent());
    }

    private XarInstalledExtensionRepository getXarRepository()
    {
        return (XarInstalledExtensionRepository) this.xarRepository;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ExtensionEvent extensionEvent = (ExtensionEvent) event;

        try {
            if (extensionEvent instanceof ExtensionUninstalledEvent) {
                // Update documents index
                getXarRepository().pagesRemoved(extensionEvent.getExtensionId(), extensionEvent.getNamespace());

                // Update extension cache
                getXarRepository().updateCachedXarExtension(extensionEvent.getExtensionId());
            } else {
                // Previous extensions

                if (data != null) {
                    for (InstalledExtension installedExtension : (Collection<InstalledExtension>) data) {
                        // Update documents index
                        getXarRepository().pagesRemoved(installedExtension.getId(), extensionEvent.getNamespace());

                        // Update extension cache
                        getXarRepository().updateCachedXarExtension(installedExtension.getId());
                    }
                }

                // New extension

                // Update extension cache
                getXarRepository().updateCachedXarExtension(extensionEvent.getExtensionId());

                // Update documents index
                getXarRepository().pagesAdded(extensionEvent.getExtensionId(), extensionEvent.getNamespace());
            }
        } catch (UnsupportedNamespaceException e) {
            logger.error("Failed to extract wiki from namespace [{}]", extensionEvent.getNamespace());
        }
    }
}
