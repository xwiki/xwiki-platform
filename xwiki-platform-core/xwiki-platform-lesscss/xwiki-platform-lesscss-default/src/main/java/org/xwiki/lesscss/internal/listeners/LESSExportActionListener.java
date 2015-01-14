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

import org.slf4j.Logger;
import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.cache.ColorThemeCache;
import org.xwiki.lesscss.cache.LESSResourcesCache;
import org.xwiki.lesscss.colortheme.ColorThemeReference;
import org.xwiki.lesscss.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Used to flush the LESS cache if we're doing an HTML export because we need that URLs located in less file be
 * recomputed (see ExportURLFactory).
 *
 * @version $Id$
 * @since 6.2RC1
 */
@Component
@Named("lessexport")
@Singleton
public class LESSExportActionListener implements EventListener
{
    private static final String EVENT_TO_OBSERVE = "export";

    @Inject
    private LESSResourcesCache lessResourcesCache;

    @Inject
    private ColorThemeCache colorThemeCache;

    @Inject
    private CurrentColorThemeGetter currentColorThemeGetter;

    @Inject
    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "lessexport";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new ActionExecutingEvent(EVENT_TO_OBSERVE),
            new ActionExecutedEvent(EVENT_TO_OBSERVE));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Flush the LESS cache if we're doing an HTML export because we need that URLs located in less file be
        // recomputed (see ExportURLFactory).
        // We also flush the cache after the export is done, to avoid having in the cache the LESS output computed for
        // the HTML exporter. Otherwise, it would be served to other requests that have nothing to do with the HTML
        // export (see: http://jira.xwiki.org/browse/XWIKI-11497).
        XWikiContext xcontext = (XWikiContext) data;
        XWikiRequest request = xcontext.getRequest();
        String format = request.get("format");
        if ("html".equals(format)) {
            try {
                ColorThemeReference  colorTheme = colorThemeReferenceFactory.createReference(
                        currentColorThemeGetter.getCurrentColorTheme("default"));
                this.lessResourcesCache.clearFromColorTheme(colorTheme);
                this.colorThemeCache.clearFromColorTheme(colorTheme);
            } catch (LESSCompilerException e) {
                logger.warn("Failed to flush the less cache.", e);
            }
        }
    }
}
