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
package org.xwiki.javascript.importmap.internal;

import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.event.AbstractExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Listen for extensions related events (installed, uninstalled, upgraded) and clear {@link JavascriptImportmapResolver}
 * cache when the events are received.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */
@Component
@Named(JavascriptImportmapEventListener.HINT)
@Singleton
public class JavascriptImportmapEventListener implements EventListener
{
    /**
     * Component hint.
     */
    public static final String HINT = "JavascriptImportmapEventListener";

    @Inject
    private JavascriptImportmapResolver javascriptImportmapResolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public String getName()
    {
        return HINT;
    }

    @Override
    public List<Event> getEvents()
    {
        return List.of(new ExtensionInstalledEvent(), new ExtensionUninstalledEvent(), new ExtensionUpgradedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof AbstractExtensionEvent extensionInstalledEvent) {
            if (!extensionInstalledEvent.hasNamespace() || Objects.equals(extensionInstalledEvent.getNamespace(),
                new WikiNamespace(this.wikiDescriptorManager.getCurrentWikiId()).serialize()))
            {
                // Only clear the cache for events related to the current wiki (i.e., local to the wiki or global to
                // the whole farm).
                this.javascriptImportmapResolver.clearCache();
            }
        }
    }
}
