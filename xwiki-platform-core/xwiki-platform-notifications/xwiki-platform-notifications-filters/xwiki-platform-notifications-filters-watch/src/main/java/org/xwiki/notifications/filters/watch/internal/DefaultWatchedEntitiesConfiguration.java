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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.notifications.filters.watch.WatchedEntitiesConfiguration;
import org.xwiki.text.StringUtils;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;

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

    private static final List<String> CODE_SPACE = Arrays.asList(XWIKI_SPACE, "Notifications", "Code");

    private static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference(CODE_SPACE,
            "AutomaticWatchModeClass");

    private static final LocalDocumentReference CONFIGURATION_REFERENCE = new LocalDocumentReference(CODE_SPACE,
            "NotificationAdministration");

    private static final String AUTOMATIC_WATCH_MODE = "automaticWatchMode";

    private static final String WATCHLIST_AUTOWATCH_PROPERTY = "xwiki.plugin.watchlist.automaticwatch";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ConfigurationSource configurationSource;

    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource xwikiCfgConfigurationSource;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public boolean isEnabled()
    {
        return configurationSource.getProperty("notifications.watchedEntities.enabled", true);
    }

    @Override
    public AutomaticWatchMode getAutomaticWatchMode(DocumentReference user)
    {
        Object value = documentAccessBridge.getProperty(user, getAbsoluteClassReference(user),
                AUTOMATIC_WATCH_MODE);
        if (value != null && StringUtils.isNotBlank((String) value)) {
            return AutomaticWatchMode.valueOf((String) value);
        }

        // Fallback to default
        return getDefaultAutomaticWatchMode(user.getWikiReference());
    }

    @Override
    public AutomaticWatchMode getDefaultAutomaticWatchMode(WikiReference wikiReference)
    {
        Object value = documentAccessBridge.getProperty(getAbsoluteConfigurationReference(wikiReference),
                getAbsoluteClassReference(wikiReference), AUTOMATIC_WATCH_MODE);
        if (value != null && StringUtils.isNotBlank((String) value)) {
            return AutomaticWatchMode.valueOf((String) value);
        }

        // Fallback to the value of the main wiki
        WikiReference mainWiki = new WikiReference(wikiDescriptorManager.getMainWikiId());
        if (!wikiReference.equals(mainWiki)) {
            value = documentAccessBridge.getProperty(getAbsoluteConfigurationReference(mainWiki),
                    getAbsoluteClassReference(mainWiki), AUTOMATIC_WATCH_MODE);
            if (value != null && StringUtils.isNotBlank((String) value)) {
                return AutomaticWatchMode.valueOf((String) value);
            }
        }

        // Fallback to the default automatic watch mode as it is configured in properties files.
        return getAutomaticWatchMode();
    }

    private AutomaticWatchMode getAutomaticWatchMode()
    {
        // Look into the configuration.
        Object value = configurationSource.getProperty("notifications.watchedEntities.autoWatch");
        if (value != null) {
            return AutomaticWatchMode.valueOf(((String) value).toUpperCase());
        }

        // Fallback to the configuration of the watchlist (if it exists)
        // For historic reason, we look both in xwiki.properties and xwiki.cfg.
        // - xwiki.cfg because it is where the watchlist configuration belongs to
        // - xwiki.properties because we badly look there between XWiki 9.8RC1 and 10.6 and we don't want to break the
        //   configuration of people who rely on it.
        value = configurationSource.getProperty(WATCHLIST_AUTOWATCH_PROPERTY);
        if (value != null) {
            return AutomaticWatchMode.valueOf(((String) value).toUpperCase());
        }

        // So here we load in xwiki.cfg.
        // Note that it would have been better to just use the "all" ConfigurationSource, but for some reason it does
        // not handle xwiki.cfg.
        value = xwikiCfgConfigurationSource.getProperty(WATCHLIST_AUTOWATCH_PROPERTY);
        if (value != null) {
            return AutomaticWatchMode.valueOf(((String) value).toUpperCase());
        }

        // Default
        return AutomaticWatchMode.MAJOR;
    }

    private DocumentReference getAbsoluteClassReference(DocumentReference user)
    {
        return new DocumentReference(CLASS_REFERENCE, user.getWikiReference());
    }

    private DocumentReference getAbsoluteClassReference(WikiReference wikiReference)
    {
        return new DocumentReference(CLASS_REFERENCE, wikiReference);
    }

    private DocumentReference getAbsoluteConfigurationReference(WikiReference wikiReference)
    {
        return new DocumentReference(CONFIGURATION_REFERENCE, wikiReference);
    }
}
