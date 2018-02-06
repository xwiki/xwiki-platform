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
package org.xwiki.notifications.filters.watch.internal;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.notifications.filters.watch.WatchedEntitiesConfiguration;
import org.xwiki.text.StringUtils;

/**
 * Default implementation of {@link WatchedEntitiesConfiguration}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
public class DefaultWatchedEntitiesConfiguration implements WatchedEntitiesConfiguration
{
    private static final String XWIKI_SPACE = "XWiki";

    private static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference(
            Arrays.asList(XWIKI_SPACE, "Notifications", "Code"), "AutomaticWatchModeClass");

    private static final LocalDocumentReference WATCHLIST_REFERENCE = new LocalDocumentReference(XWIKI_SPACE,
            "WatchListClass");

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ConfigurationSource configurationSource;

    @Override
    public boolean isEnabled()
    {
        return configurationSource.getProperty("notifications.watchedEntities.enabled", true);
    }

    @Override
    public AutomaticWatchMode getAutomaticWatchMode(DocumentReference user)
    {
        Object value = documentAccessBridge.getProperty(user, getAbsoluteClassReference(user),
                "automaticWatchMode");
        if (value != null && StringUtils.isNotBlank((String) value)) {
            return AutomaticWatchMode.valueOf((String) value);
        }

        // Fallback to the value of the Watchlist
        value = documentAccessBridge.getProperty(user, getAbsoluteWatchlistClassReference(user), "automaticwatch");
        if (value != null && StringUtils.isNotBlank((String) value) && !"default".equals(value)) {
            return AutomaticWatchMode.valueOf((String) value);
        }

        // Fallback to the configuration of the watchlist (if it exists)
        value = configurationSource.getProperty("xwiki.plugin.watchlist.automaticwatch");
        if (value != null) {
            return AutomaticWatchMode.valueOf(((String) value).toUpperCase());
        }

        // TODO: make it configurable too by the administrator
        return AutomaticWatchMode.MAJOR;
    }

    private DocumentReference getAbsoluteClassReference(DocumentReference user)
    {
        return new DocumentReference(CLASS_REFERENCE, user.getWikiReference());
    }

    private DocumentReference getAbsoluteWatchlistClassReference(DocumentReference user)
    {
        return new DocumentReference(WATCHLIST_REFERENCE, user.getWikiReference());
    }
}
