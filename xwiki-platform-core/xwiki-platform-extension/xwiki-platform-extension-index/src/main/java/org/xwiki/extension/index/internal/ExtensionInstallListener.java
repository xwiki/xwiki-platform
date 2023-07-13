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

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
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
    private ExtensionIndexStore store;

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
            if (event instanceof ExtensionUninstalledEvent) {
                onUninstalled(((ExtensionEvent) event).getExtensionId(), ((ExtensionEvent) event).getNamespace());
            } else if (event instanceof ExtensionInstalledEvent) {
                onInstalled(((ExtensionEvent) event).getExtensionId(), ((ExtensionEvent) event).getNamespace());
            } else if (event instanceof ExtensionUpgradedEvent) {
                ExtensionUpgradedEvent extensionUpgradedEvent = (ExtensionUpgradedEvent) event;
                String namespace = extensionUpgradedEvent.getNamespace();
                onUpgraded(extensionUpgradedEvent.getExtensionId(), ((Collection<InstalledExtension>) data), namespace);
            }
        } catch (Exception e) {
            this.logger.error("Failed to update the local extension store", e);
        }
    }

    private void onUpgraded(ExtensionId extensionId, Collection<InstalledExtension> previousExtensions,
        String namespace) throws SolrServerException, IOException
    {
        for (InstalledExtension previousExtension : previousExtensions) {
            this.store.updateInstalled(previousExtension.getId(), namespace, false);
        }

        this.store.updateInstalled(extensionId, namespace, true);
        this.store.commit();
    }

    private void onInstalled(ExtensionId extensionId, String namespace) throws SolrServerException, IOException
    {
        this.store.updateInstalled(extensionId, namespace, true);
        this.store.commit();
    }

    private void onUninstalled(ExtensionId extensionId, String namespace) throws SolrServerException, IOException
    {
        this.store.updateInstalled(extensionId, namespace, false);
        this.store.commit();
    }
}
