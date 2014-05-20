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
package org.xwiki.lesscss.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.LESSSkinFileCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener that clears the cache of compiled LESS Skin file when a skin or a color theme is changed.
 *
 * @since 6.1M2
 * @version $Id$
 */
@Component
@Named("lessSkinColorTheme")
@Singleton
public class SkinAndColorThemeListener implements EventListener
{
    private static final LocalDocumentReference COLOR_THEME_CLASS =
            new LocalDocumentReference("ColorThemes", "ColorThemeClass");

    private static final LocalDocumentReference SKIN_CLASS = new LocalDocumentReference("XWiki", "XWikiSkins");

    @Inject
    private LESSSkinFileCache lessSkinFileCache;

    @Override
    public String getName()
    {
        return "LESS Color Theme Listener";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(
                new DocumentCreatedEvent(),
                new DocumentUpdatedEvent(),
                new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        List<BaseObject> colorThemeObjects = document.getXObjects(COLOR_THEME_CLASS);
        if (colorThemeObjects != null && !colorThemeObjects.isEmpty()) {
            clearCache(document);
            return;
        }

        List<BaseObject> skinObjects = document.getXObjects(SKIN_CLASS);
        if (skinObjects != null && !skinObjects.isEmpty()) {
            clearCache(document);
        }
    }

    private void clearCache(XWikiDocument document)
    {
        DocumentReference documentReference = document.getDocumentReference();

        // Clear the cache for the specified wiki and color theme
        lessSkinFileCache.clear(documentReference.getWikiReference().getName());
    }
}
