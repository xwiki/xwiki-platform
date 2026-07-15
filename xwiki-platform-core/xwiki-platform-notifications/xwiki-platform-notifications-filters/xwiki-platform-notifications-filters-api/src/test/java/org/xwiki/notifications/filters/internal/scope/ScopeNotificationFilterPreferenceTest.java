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
package org.xwiki.notifications.filters.internal.scope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScopeNotificationFilterPreference}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
class ScopeNotificationFilterPreferenceTest
{
    private static final WikiReference WIKI_REFERENCE = new WikiReference("wiki");

    private static final SpaceReference SPACE_REFERENCE =
        new SpaceReference("space", new WikiReference("wiki"));

    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("wiki", "space", "page");

    private EntityReferenceResolver<String> referenceResolver;

    private NotificationFilterPreference p1;

    private NotificationFilterPreference p2;

    private NotificationFilterPreference p3;

    private ScopeNotificationFilterPreference sp1;

    private ScopeNotificationFilterPreference sp2;

    private ScopeNotificationFilterPreference sp3;

    @BeforeEach
    void setUp()
    {
        this.referenceResolver = mock(EntityReferenceResolver.class);

        when(this.referenceResolver.resolve("wiki", EntityType.WIKI)).thenReturn(WIKI_REFERENCE);
        when(this.referenceResolver.resolve("wiki:space", EntityType.SPACE)).thenReturn(SPACE_REFERENCE);
        when(this.referenceResolver.resolve("wiki:space.page", EntityType.DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);

        this.p1 = mockNotificationFilterPreference("wiki",
            NotificationFilterProperty.WIKI, NotificationFilterType.INCLUSIVE);
        this.p2 = mockNotificationFilterPreference("wiki:space",
            NotificationFilterProperty.SPACE, NotificationFilterType.EXCLUSIVE);
        this.p3 = mockNotificationFilterPreference("wiki:space.page",
            NotificationFilterProperty.PAGE, NotificationFilterType.INCLUSIVE);

        this.sp1 = new ScopeNotificationFilterPreference(this.p1, this.referenceResolver);
        this.sp2 = new ScopeNotificationFilterPreference(this.p2, this.referenceResolver);
        this.sp3 = new ScopeNotificationFilterPreference(this.p3, this.referenceResolver);
    }

    private NotificationFilterPreference mockNotificationFilterPreference(String entityReference,
        NotificationFilterProperty property, NotificationFilterType filterType)
    {
        NotificationFilterPreference preference = mock(NotificationFilterPreference.class);

        if (property == NotificationFilterProperty.PAGE) {
            when(preference.getPageOnly()).thenReturn(entityReference);
        }
        if (property == NotificationFilterProperty.SPACE) {
            when(preference.getPage()).thenReturn(entityReference);
        }
        if (property == NotificationFilterProperty.WIKI) {
            when(preference.getWiki()).thenReturn(entityReference);
        }

        when(preference.getFilterType()).thenReturn(filterType);

        return preference;
    }

    @Test
    void getFilterType()
    {
        assertEquals(NotificationFilterType.INCLUSIVE, this.sp1.getFilterType());
        assertEquals(NotificationFilterType.EXCLUSIVE, this.sp2.getFilterType());
    }

    @Test
    void getScopeReference()
    {
        assertEquals(this.sp1.getScopeReference(), WIKI_REFERENCE);
        assertEquals(this.sp2.getScopeReference(), SPACE_REFERENCE);
        assertEquals(this.sp3.getScopeReference(), DOCUMENT_REFERENCE);
    }

    @Test
    void equals()
    {
        assertNotEquals(this.sp1, this.sp2);
        assertNotEquals(this.sp1.hashCode(), this.sp2.hashCode());

        ScopeNotificationFilterPreference sp1Bis =
            new ScopeNotificationFilterPreference(this.p1, this.referenceResolver);
        assertEquals(this.sp1, sp1Bis);
        assertEquals(this.sp1.hashCode(), sp1Bis.hashCode());

        sp1Bis.setEnabled(true);
        verify(this.p1).setEnabled(true);
        assertEquals(this.sp1, sp1Bis);
        assertEquals(this.sp1.hashCode(), sp1Bis.hashCode());

        sp1Bis.setHasParent(true);
        assertNotEquals(this.sp1, sp1Bis);
        assertNotEquals(this.sp1.hashCode(), sp1Bis.hashCode());

        this.sp1.setHasParent(true);
        sp1Bis.addChild(this.sp2);
        assertNotEquals(this.sp1, sp1Bis);
        assertNotEquals(this.sp1.hashCode(), sp1Bis.hashCode());

        this.sp1.addChild(this.sp2);
        assertEquals(this.sp1, sp1Bis);
        assertEquals(this.sp1.hashCode(), sp1Bis.hashCode());

        assertNotEquals(this.sp2, this.sp3);
        assertNotEquals(this.sp1, this.sp3);
    }
}
