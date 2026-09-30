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
package org.xwiki.documenttabs.internal;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.documenttabs.DocumentTabsConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultDocumentTabsConfiguration}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultDocumentTabsConfigurationTest
{
    private static final DocumentReference CURRENT_DOCUMENT =
        new DocumentReference("wiki", List.of("Parent", "Child"), "WebHome");

    private static final DocumentReference PREFERENCES_CLASS =
        new DocumentReference("wiki", "XWiki", "XWikiPreferences");

    private static final DocumentReference CHILD_PREFERENCES =
        new DocumentReference("wiki", List.of("Parent", "Child"), "WebPreferences");

    private static final DocumentReference PARENT_PREFERENCES =
        new DocumentReference("wiki", "Parent", "WebPreferences");

    private static final DocumentReference WIKI_PREFERENCES =
        new DocumentReference("wiki", "XWiki", "XWikiPreferences");

    private static final String VISIBILITY = DocumentTabsConfiguration.TABS_VISIBILITY_PROPERTY;

    private static final String SHOW_TABS = DocumentTabsConfiguration.SHOW_TABS_PROPERTY;

    @InjectMockComponents
    private DefaultDocumentTabsConfiguration configuration;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @BeforeEach
    void beforeEach()
    {
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(CURRENT_DOCUMENT);
    }

    private void setVisibility(DocumentReference preferencesDocument, String... entries)
    {
        when(this.documentAccessBridge.getProperty(preferencesDocument, PREFERENCES_CLASS, VISIBILITY))
            .thenReturn(List.of(entries));
    }

    private void setShowTabs(DocumentReference preferencesDocument, String value)
    {
        when(this.documentAccessBridge.getProperty(preferencesDocument, PREFERENCES_CLASS, SHOW_TABS))
            .thenReturn(value);
    }

    @Test
    void isTabVisibleFallsBackOnTheDefaultWhenNoLevelDefinesIt()
    {
        assertTrue(this.configuration.isTabVisible("tab", true));
        assertFalse(this.configuration.isTabVisible("tab", false));
    }

    @Test
    void isTabVisibleUsesTheWikiLevelWhenNoSpaceDefinesIt()
    {
        setVisibility(WIKI_PREFERENCES, "-tab");

        assertFalse(this.configuration.isTabVisible("tab", true));
    }

    @Test
    void isTabVisibleUsesTheClosestSpaceDefiningTheTab()
    {
        setVisibility(WIKI_PREFERENCES, "-tab");
        setVisibility(PARENT_PREFERENCES, "+tab");

        assertTrue(this.configuration.isTabVisible("tab", false));

        setVisibility(CHILD_PREFERENCES, "-tab");

        assertFalse(this.configuration.isTabVisible("tab", true));
    }

    @Test
    void isTabVisibleIgnoresLevelsThatDoNotMentionTheTab()
    {
        setVisibility(WIKI_PREFERENCES, "-tab");
        setVisibility(CHILD_PREFERENCES, "+otherTab");

        assertFalse(this.configuration.isTabVisible("tab", true));
        assertTrue(this.configuration.isTabVisible("otherTab", false));
    }

    @Test
    void isTabVisibleWithoutCurrentDocument()
    {
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(null);

        assertTrue(this.configuration.isTabVisible("tab", true));
        assertTrue(this.configuration.areTabsDisplayed());
    }

    @Test
    void areTabsDisplayedByDefault()
    {
        assertTrue(this.configuration.areTabsDisplayed());
    }

    @Test
    void areTabsDisplayedUsesTheClosestLevelWithAValue()
    {
        setShowTabs(WIKI_PREFERENCES, "1");
        setShowTabs(PARENT_PREFERENCES, "0");

        assertFalse(this.configuration.areTabsDisplayed());

        setShowTabs(CHILD_PREFERENCES, "1");

        assertTrue(this.configuration.areTabsDisplayed());
    }

    @Test
    void areTabsDisplayedAcceptsTheLegacyFalseValues()
    {
        setShowTabs(WIKI_PREFERENCES, "No");

        assertFalse(this.configuration.areTabsDisplayed());

        setShowTabs(WIKI_PREFERENCES, "false");

        assertFalse(this.configuration.areTabsDisplayed());

        setShowTabs(WIKI_PREFERENCES, "yes");

        assertTrue(this.configuration.areTabsDisplayed());
    }
}
