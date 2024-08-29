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
package org.xwiki.icon.internal;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconSetCache;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Component that removes from the cache any icon set corresponding to an updated or deleted wiki document.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Component
@Named("iconTheme")
@Singleton
@Priority(EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY)
public class IconThemeListener implements EventListener
{
    private static final LocalDocumentReference ICON_THEME_CLASS =
            new LocalDocumentReference("IconThemesCode", "IconThemeClass");

    @Inject
    private IconSetCache iconSetCache;

    @Override
    public String getName()
    {
        return "Icon Theme listener.";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(
                new DocumentUpdatedEvent(),
                new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        List<BaseObject> iconThemeObjects = document.getXObjects(ICON_THEME_CLASS);
        if (iconThemeObjects != null && !iconThemeObjects.isEmpty()) {
            // Clear the icon set from the cache (since it has been updated)
            iconSetCache.clear(document.getDocumentReference());
            // Clear the icon set from its name
            BaseObject iconThemeObj = iconThemeObjects.get(0);
            String iconThemeName = iconThemeObj.getStringValue("name");
            String currentWiki = document.getDocumentReference().getWikiReference().getName();
            iconSetCache.clear(iconThemeName, currentWiki);
        }
    }
}
