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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.6
 * @since 9.11.8
 */
public class DefaultWatchedEntitiesConfigurationTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultWatchedEntitiesConfiguration> mocker =
            new MockitoComponentMockingRule<>(DefaultWatchedEntitiesConfiguration.class);

    private static final DocumentReference CURRENT_USER
            = new DocumentReference("wikiA", "XWiki", "UserA");

    private static final WikiReference CURRENT_WIKI = new WikiReference("wikiA");

    private DocumentAccessBridge documentAccessBridge;
    private ConfigurationSource configurationSource;
    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        configurationSource = mocker.getInstance(ConfigurationSource.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);

        when(documentAccessBridge.getCurrentDocumentReference()).thenReturn(new DocumentReference("wikiA", "Main", "WebHome"));
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
    }

    @Test
    public void getAutomaticWatchMode() throws Exception
    {
        // Default value
        assertEquals(AutomaticWatchMode.MAJOR, mocker.getComponentUnderTest().getAutomaticWatchMode(CURRENT_USER));

        // Fallback to the value of the Watchlist
        when(documentAccessBridge.getProperty(
                CURRENT_USER,
                new DocumentReference("wikiA", "XWiki", "WatchListClass"),
                "automaticwatch")).thenReturn("NEW");
        assertEquals(AutomaticWatchMode.NEW, mocker.getComponentUnderTest().getAutomaticWatchMode(CURRENT_USER));

        // User the user's preference
        when(documentAccessBridge.getProperty(
                CURRENT_USER,
                new DocumentReference("wikiA", Arrays.asList("XWiki", "Notifications", "Code"), "AutomaticWatchModeClass"),
                "automaticWatchMode")).thenReturn("ALL");
        assertEquals(AutomaticWatchMode.ALL, mocker.getComponentUnderTest().getAutomaticWatchMode(CURRENT_USER));
    }

    @Test
    public void getDefaultAutomaticWatchMode() throws Exception
    {
        // Default value
        assertEquals(AutomaticWatchMode.MAJOR, mocker.getComponentUnderTest().getDefaultAutomaticWatchMode(CURRENT_WIKI));

        // Fallback on the watchlist configuration
        when(configurationSource.getProperty("xwiki.plugin.watchlist.automaticwatch")).thenReturn("new");
        assertEquals(AutomaticWatchMode.NEW, mocker.getComponentUnderTest().getDefaultAutomaticWatchMode(CURRENT_WIKI));

        // Fallback on the notifications configuration
        when(configurationSource.getProperty("notifications.watchedEntities.autoWatch")).thenReturn("all");
        assertEquals(AutomaticWatchMode.ALL, mocker.getComponentUnderTest().getDefaultAutomaticWatchMode(CURRENT_WIKI));

        // Fallback on the main wiki's configuration
        when(documentAccessBridge.getProperty(
                new DocumentReference("mainWiki", Arrays.asList("XWiki", "Notifications", "Code"), "NotificationAdministration"),
                new DocumentReference("mainWiki", Arrays.asList("XWiki", "Notifications", "Code"), "AutomaticWatchModeClass"),
                "automaticWatchMode")).thenReturn("NONE");
        assertEquals(AutomaticWatchMode.NONE, mocker.getComponentUnderTest().getDefaultAutomaticWatchMode(CURRENT_WIKI));

        // Use the wiki's configuration
        when(documentAccessBridge.getProperty(
                new DocumentReference("wikiA", Arrays.asList("XWiki", "Notifications", "Code"), "NotificationAdministration"),
                new DocumentReference("wikiA", Arrays.asList("XWiki", "Notifications", "Code"), "AutomaticWatchModeClass"),
                "automaticWatchMode")).thenReturn("NEW");
        assertEquals(AutomaticWatchMode.NEW, mocker.getComponentUnderTest().getDefaultAutomaticWatchMode(CURRENT_WIKI));
    }
}
