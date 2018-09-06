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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScopeNotificationFilterPreference}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public class ScopeNotificationFilterPreferenceTest
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

    @Before
    public void setUp() throws Exception
    {
        referenceResolver = (EntityReferenceResolver<String>) mock(EntityReferenceResolver.class);

        when(referenceResolver.resolve("wiki", EntityType.WIKI)).thenReturn(WIKI_REFERENCE);
        when(referenceResolver.resolve("wiki:space", EntityType.SPACE)).thenReturn(SPACE_REFERENCE);
        when(referenceResolver.resolve("wiki:space.page", EntityType.DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);

        p1 = mockNotificationFilterPreference("wiki",
                NotificationFilterProperty.WIKI, NotificationFilterType.INCLUSIVE);
        p2 = mockNotificationFilterPreference("wiki:space",
                NotificationFilterProperty.SPACE, NotificationFilterType.EXCLUSIVE);
        p3 = mockNotificationFilterPreference("wiki:space.page",
                NotificationFilterProperty.PAGE, NotificationFilterType.INCLUSIVE);

        sp1 = new ScopeNotificationFilterPreference(p1, referenceResolver);
        sp2 = new ScopeNotificationFilterPreference(p2, referenceResolver);
        sp3 = new ScopeNotificationFilterPreference(p3, referenceResolver);
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
    public void getFilterType() throws Exception
    {
        assertEquals(NotificationFilterType.INCLUSIVE, sp1.getFilterType());
        assertEquals(NotificationFilterType.EXCLUSIVE, sp2.getFilterType());
    }

    @Test
    public void getScopeReference() throws Exception
    {
        assertEquals(sp1.getScopeReference(), WIKI_REFERENCE);
        assertEquals(sp2.getScopeReference(), SPACE_REFERENCE);
        assertEquals(sp3.getScopeReference(), DOCUMENT_REFERENCE);
    }

}
