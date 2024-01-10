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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.6RC1
 * @since 9.11.8
 */
@ComponentTest
class DefaultWatchedEntitiesConfigurationTest
{
    @InjectMockComponents
    private DefaultWatchedEntitiesConfiguration watchedEntitiesConfiguration;
    
    private static final DocumentReference CURRENT_USER
            = new DocumentReference("wikiA", "XWiki", "UserA");

    private static final WikiReference CURRENT_WIKI = new WikiReference("wikiA");

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;
    
    @MockComponent
    private ConfigurationSource configurationSource;
    
    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeEach
    public void setUp()
    {
        when(documentAccessBridge.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wikiA", "Main", "WebHome"));
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
    }

    @Test
    void getAutomaticWatchMode() throws Exception
    {
        // Default value
        assertEquals(AutomaticWatchMode.MAJOR, watchedEntitiesConfiguration.getAutomaticWatchMode(CURRENT_USER));

        // User the user's preference
        when(documentAccessBridge.getProperty(
                CURRENT_USER,
                new DocumentReference("wikiA", Arrays.asList("XWiki", "Notifications", "Code"),
                    "AutomaticWatchModeClass"),
                "automaticWatchMode")).thenReturn("ALL");
        assertEquals(AutomaticWatchMode.ALL, watchedEntitiesConfiguration.getAutomaticWatchMode(CURRENT_USER));
    }

    @Test
    void getDefaultAutomaticWatchMode() throws Exception
    {
        // Default value
        assertEquals(AutomaticWatchMode.MAJOR, watchedEntitiesConfiguration.getDefaultAutomaticWatchMode(CURRENT_WIKI));

        // Fallback on the watchlist configuration
        when(configurationSource.getProperty("xwiki.plugin.watchlist.automaticwatch")).thenReturn("new");
        assertEquals(AutomaticWatchMode.NEW, watchedEntitiesConfiguration.getDefaultAutomaticWatchMode(CURRENT_WIKI));

        // Fallback on the notifications configuration
        when(configurationSource.getProperty("notifications.watchedEntities.autoWatch")).thenReturn("all");
        assertEquals(AutomaticWatchMode.ALL, watchedEntitiesConfiguration.getDefaultAutomaticWatchMode(CURRENT_WIKI));

        // Fallback on the main wiki's configuration
        when(documentAccessBridge.getProperty(
                new DocumentReference("mainWiki", Arrays.asList("XWiki", "Notifications", "Code"),
                    "NotificationAdministration"),
                new DocumentReference("mainWiki", Arrays.asList("XWiki", "Notifications", "Code"),
                    "AutomaticWatchModeClass"),
                "automaticWatchMode")).thenReturn("NONE");
        assertEquals(AutomaticWatchMode.NONE, watchedEntitiesConfiguration.getDefaultAutomaticWatchMode(CURRENT_WIKI));

        // Use the wiki's configuration
        when(documentAccessBridge.getProperty(
                new DocumentReference("wikiA", Arrays.asList("XWiki", "Notifications", "Code"),
                    "NotificationAdministration"),
                new DocumentReference("wikiA", Arrays.asList("XWiki", "Notifications", "Code"),
                    "AutomaticWatchModeClass"),
                "automaticWatchMode")).thenReturn("NEW");
        assertEquals(AutomaticWatchMode.NEW, watchedEntitiesConfiguration.getDefaultAutomaticWatchMode(CURRENT_WIKI));
    }
}
