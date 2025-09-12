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
package org.xwiki.extension.index.internal;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import jakarta.inject.Provider;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Update the index when an extension is installed/uninstalled.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Named(ExtensionInstallListener.NAME)
@Singleton
public class ExtensionInstallListener extends AbstractEventListener
{
    /**
     * The unique name of this event listener.
     */
    public static final String NAME = "org.xwiki.extension.index.internal.ExtensionInstallListener";

    @Inject
    // Load the store lazily to limit risks of component cycles (listeners are loaded very early)
    private Provider<ExtensionIndexStore> storeProvider;

    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public ExtensionInstallListener()
    {
        super(NAME, new ExtensionInstalledEvent(), new ExtensionUpgradedEvent(), new ExtensionUninstalledEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // TODO: make that asynchronous
        try {
            if (event instanceof ExtensionUninstalledEvent extensionEvent) {
                onUninstalled(extensionEvent.getExtensionId(), extensionEvent.getNamespace());
            } else if (event instanceof ExtensionInstalledEvent extensionEvent) {
                onInstalled(extensionEvent.getExtensionId(), extensionEvent.getNamespace());
            } else if (event instanceof ExtensionUpgradedEvent extensionEvent) {
                String namespace = extensionEvent.getNamespace();
                onUpgraded(extensionEvent.getExtensionId(), ((Collection<InstalledExtension>) data), namespace);
            }
        } catch (Exception e) {
            this.logger.error("Failed to update the local extension store", e);
        }
    }

    private void onUpgraded(ExtensionId extensionId, Collection<InstalledExtension> previousExtensions,
        String namespace) throws SolrServerException, IOException
    {
        ExtensionIndexStore store = this.storeProvider.get();

        for (InstalledExtension previousExtension : previousExtensions) {
            store.updateInstalled(previousExtension.getId(), namespace, false);
        }

        store.updateInstalled(extensionId, namespace, true);
        store.commit();
    }

    private void onInstalled(ExtensionId extensionId, String namespace) throws SolrServerException, IOException
    {
        ExtensionIndexStore store = this.storeProvider.get();

        store.updateInstalled(extensionId, namespace, true);
        store.commit();
    }

    private void onUninstalled(ExtensionId extensionId, String namespace) throws SolrServerException, IOException
    {
        ExtensionIndexStore store = this.storeProvider.get();

        store.updateInstalled(extensionId, namespace, false);
        store.commit();
    }
}
