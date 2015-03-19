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
package org.xwiki.lesscss.internal.listeners;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.internal.cache.ColorThemeCache;
import org.xwiki.lesscss.internal.cache.LESSResourcesCache;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReference;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReferenceFactory;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener that clears the cache of compiled LESS Skin file when a color theme is changed.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Named("lessColorTheme")
@Singleton
public class ColorThemeListener implements EventListener
{
    private static final LocalDocumentReference COLOR_THEME_CLASS =
            new LocalDocumentReference("ColorThemes", "ColorThemeClass");

    private static final LocalDocumentReference FLAMINGO_THEME_CLASS =
            new LocalDocumentReference("FlamingoThemesCode", "ThemeClass");

    @Inject
    private LESSResourcesCache lessResourcesCache;

    @Inject
    private ColorThemeCache colorThemeCache;
    
    @Inject
    private ColorThemeReferenceFactory colorThemeReferenceFactory;

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

        List<BaseObject> flamingoThemeObjects = document.getXObjects(FLAMINGO_THEME_CLASS);
        if (flamingoThemeObjects != null && !flamingoThemeObjects.isEmpty()) {
            clearCacheFromColorTheme(document);
            return;
        }

        List<BaseObject> colorThemeObjects = document.getXObjects(COLOR_THEME_CLASS);
        if (colorThemeObjects != null && !colorThemeObjects.isEmpty()) {
            clearCacheFromColorTheme(document);
            return;
        }
    }

    private void clearCacheFromColorTheme(XWikiDocument document)
    {
        ColorThemeReference colorThemeReference = 
                colorThemeReferenceFactory.createReference(document.getDocumentReference());
        lessResourcesCache.clearFromColorTheme(colorThemeReference);
        colorThemeCache.clearFromColorTheme(colorThemeReference);
    }
}
