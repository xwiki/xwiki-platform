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

package org.xwiki.notifications.filters.internal.livedata.custom;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NotificationCustomFiltersQueryHelper}.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@ComponentTest
class NotificationCustomFiltersQueryHelperTest
{
    @InjectMockComponents
    private NotificationCustomFiltersQueryHelper queryHelper;

    @MockComponent
    private QueryManager queryManager;

    @Test
    void getFilterPreferencesNoFilterNoSort() throws QueryException, LiveDataException
    {
        String owner = "xwiki:XWiki.Foo";
        String wikiName = "foo";
        WikiReference wikiReference = new WikiReference(wikiName);
        String queryString = "select nfp from DefaultNotificationFilterPreference nfp where owner = :owner";
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(queryString, Query.HQL)).thenReturn(query);
        when(query.bindValue("owner", owner)).thenReturn(query);
        when(query.setWiki(wikiName)).thenReturn(query);
        Long offset = 3L;
        int limit = 12;

        LiveDataQuery ldQuery = mock(LiveDataQuery.class);
        when(ldQuery.getOffset()).thenReturn(offset);
        when(ldQuery.getLimit()).thenReturn(limit);
        when(query.setOffset(offset.intValue())).thenReturn(query);
        when(query.setLimit(limit)).thenReturn(query);

        List<Object> expectedList = List.of(
            mock(NotificationFilterPreference.class, "filterPref1"),
            mock(NotificationFilterPreference.class, "filterPref2"),
            mock(NotificationFilterPreference.class, "filterPref3")
        );
        when(query.execute()).thenReturn(expectedList);
        assertEquals(expectedList, this.queryHelper.getFilterPreferences(ldQuery, owner, wikiReference));
        verify(query).bindValue("owner", owner);
        verify(query).setWiki(wikiName);
        verify(query).setOffset(offset.intValue());
        verify(query).setLimit(limit);
    }

    @Test
    void countFilterPreferencesNoFilterNoSort() throws QueryException, LiveDataException
    {
        String owner = "xwiki:XWiki.Foo";
        String wikiName = "foo";
        WikiReference wikiReference = new WikiReference(wikiName);
        String queryString = "select count(nfp.id) from DefaultNotificationFilterPreference nfp where owner = :owner";
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(queryString, Query.HQL)).thenReturn(query);
        when(query.bindValue("owner", owner)).thenReturn(query);
        when(query.setWiki(wikiName)).thenReturn(query);
        Long offset = 3L;
        int limit = 12;

        LiveDataQuery ldQuery = mock(LiveDataQuery.class);
        when(ldQuery.getOffset()).thenReturn(offset);
        when(ldQuery.getLimit()).thenReturn(limit);

        when(query.execute()).thenReturn(List.of(3L));
        assertEquals(3L, this.queryHelper.countTotalFilters(ldQuery, owner, wikiReference));
        verify(query).bindValue("owner", owner);
        verify(query).setWiki(wikiName);
        verify(query, never()).setOffset(anyInt());
        verify(query, never()).setLimit(anyInt());
    }

    @Test
    void getFilterPreferencesNoFilterSort() throws QueryException, LiveDataException
    {
        String owner = "xwiki:XWiki.Foo";
        String wikiName = "foo";
        WikiReference wikiReference = new WikiReference(wikiName);
        Long offset = 3L;
        int limit = 12;

        LiveDataQuery ldQuery = mock(LiveDataQuery.class);
        when(ldQuery.getOffset()).thenReturn(offset);
        when(ldQuery.getLimit()).thenReturn(limit);

        LiveDataQuery.SortEntry sortEntry1 = mock(LiveDataQuery.SortEntry.class, "sortEntry1");
        when(sortEntry1.isDescending()).thenReturn(false);
        when(sortEntry1.getProperty()).thenReturn("scope");

        LiveDataQuery.SortEntry sortEntry2 = mock(LiveDataQuery.SortEntry.class, "sortEntry2");
        when(sortEntry2.isDescending()).thenReturn(true);
        when(sortEntry2.getProperty()).thenReturn("isEnabled");

        LiveDataQuery.SortEntry sortEntry3 = mock(LiveDataQuery.SortEntry.class, "sortEntry3");
        when(sortEntry3.isDescending()).thenReturn(true);
        when(sortEntry3.getProperty()).thenReturn("notificationFormats");

        when(ldQuery.getSort()).thenReturn(List.of(sortEntry1, sortEntry2, sortEntry3));

        String queryString = "select nfp from DefaultNotificationFilterPreference nfp where owner = :owner "
            + "order by nfp.pageOnly asc, nfp.page asc, nfp.wiki asc, nfp.user asc, nfp.enabled desc, "
            + "nfp.emailEnabled asc, nfp.alertEnabled desc";
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(queryString, Query.HQL)).thenReturn(query);
        when(query.bindValue("owner", owner)).thenReturn(query);
        when(query.setWiki(wikiName)).thenReturn(query);
        when(query.setOffset(offset.intValue())).thenReturn(query);
        when(query.setLimit(limit)).thenReturn(query);

        List<Object> expectedList = List.of(
            mock(NotificationFilterPreference.class, "filterPref1"),
            mock(NotificationFilterPreference.class, "filterPref2"),
            mock(NotificationFilterPreference.class, "filterPref3")
        );
        when(query.execute()).thenReturn(expectedList);
        assertEquals(expectedList, this.queryHelper.getFilterPreferences(ldQuery, owner, wikiReference));
        verify(query).bindValue("owner", owner);
        verify(query).setWiki(wikiName);
        verify(query).setOffset(offset.intValue());
        verify(query).setLimit(limit);
    }

    @Test
    void countFilterPreferencesNoFilterSort() throws QueryException, LiveDataException
    {
        String owner = "xwiki:XWiki.Foo";
        String wikiName = "foo";
        WikiReference wikiReference = new WikiReference(wikiName);

        Long offset = 3L;
        int limit = 12;

        LiveDataQuery ldQuery = mock(LiveDataQuery.class);
        when(ldQuery.getOffset()).thenReturn(offset);
        when(ldQuery.getLimit()).thenReturn(limit);

        LiveDataQuery.SortEntry sortEntry1 = mock(LiveDataQuery.SortEntry.class, "sortEntry1");
        when(sortEntry1.isDescending()).thenReturn(false);
        when(sortEntry1.getProperty()).thenReturn("scope");

        LiveDataQuery.SortEntry sortEntry2 = mock(LiveDataQuery.SortEntry.class, "sortEntry2");
        when(sortEntry2.isDescending()).thenReturn(true);
        when(sortEntry2.getProperty()).thenReturn("isEnabled");

        LiveDataQuery.SortEntry sortEntry3 = mock(LiveDataQuery.SortEntry.class, "sortEntry3");
        when(sortEntry3.isDescending()).thenReturn(true);
        when(sortEntry3.getProperty()).thenReturn("notificationFormats");

        when(ldQuery.getSort()).thenReturn(List.of(sortEntry1, sortEntry2, sortEntry3));

        String queryString = "select count(nfp.id) from DefaultNotificationFilterPreference nfp where owner = :owner";
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(queryString, Query.HQL)).thenReturn(query);
        when(query.bindValue("owner", owner)).thenReturn(query);
        when(query.setWiki(wikiName)).thenReturn(query);

        when(query.execute()).thenReturn(List.of(3L));
        assertEquals(3L, this.queryHelper.countTotalFilters(ldQuery, owner, wikiReference));
        verify(query).bindValue("owner", owner);
        verify(query).setWiki(wikiName);
        verify(query, never()).setOffset(anyInt());
        verify(query, never()).setLimit(anyInt());
    }

    @Test
    void getFilterPreferencesFilterNoSort() throws QueryException, LiveDataException
    {
        String owner = "xwiki:XWiki.Foo";
        String wikiName = "foo";
        Long offset = 3L;
        int limit = 12;
        WikiReference wikiReference = new WikiReference(wikiName);

        LiveDataQuery ldQuery = mock(LiveDataQuery.class);
        when(ldQuery.getOffset()).thenReturn(offset);
        when(ldQuery.getLimit()).thenReturn(limit);

        LiveDataQuery.Filter filter1 = mock(LiveDataQuery.Filter.class, "filter1");
        when(filter1.getProperty()).thenReturn("location");
        when(filter1.isMatchAll()).thenReturn(false);
        LiveDataQuery.Constraint filter1Constraint1 = mock(LiveDataQuery.Constraint.class, "filter1Constraint1");
        when(filter1Constraint1.getOperator()).thenReturn("startsWith");
        when(filter1Constraint1.getValue()).thenReturn("foo");
        LiveDataQuery.Constraint filter1Constraint2 = mock(LiveDataQuery.Constraint.class, "filter1Constraint2");
        when(filter1Constraint2.getOperator()).thenReturn("contains");
        when(filter1Constraint2.getValue()).thenReturn("bar");
        LiveDataQuery.Constraint filter1Constraint3 = mock(LiveDataQuery.Constraint.class, "filter1Constraint3");
        when(filter1Constraint3.getOperator()).thenReturn("equals");
        when(filter1Constraint3.getValue()).thenReturn("buz");
        when(filter1.getConstraints()).thenReturn(List.of(filter1Constraint1, filter1Constraint2, filter1Constraint3));

        LiveDataQuery.Filter filter2 = mock(LiveDataQuery.Filter.class, "filter2");
        when(filter2.getProperty()).thenReturn("eventTypes");
        LiveDataQuery.Constraint filter2Constraint = mock(LiveDataQuery.Constraint.class, "filter2Constraint");
        when(filter2Constraint.getOperator()).thenReturn("equals");
        when(filter2Constraint.getValue()).thenReturn("__ALL_EVENTS__");
        when(filter2.getConstraints()).thenReturn(List.of(filter2Constraint));

        LiveDataQuery.Filter filter3 = mock(LiveDataQuery.Filter.class, "filter3");
        when(filter3.getProperty()).thenReturn("isEnabled");
        LiveDataQuery.Constraint filter3Constraint = mock(LiveDataQuery.Constraint.class, "filter3Constraint");
        when(filter3Constraint.getOperator()).thenReturn("equals");
        when(filter3Constraint.getValue()).thenReturn("true");
        when(filter3.getConstraints()).thenReturn(List.of(filter3Constraint));

        LiveDataQuery.Filter filter4 = mock(LiveDataQuery.Filter.class, "filter4");
        when(filter4.getProperty()).thenReturn("scope");
        LiveDataQuery.Constraint filter4Constraint = mock(LiveDataQuery.Constraint.class, "filter4Constraint");
        when(filter4Constraint.getOperator()).thenReturn("equals");
        when(filter4Constraint.getValue()).thenReturn("PAGE");
        when(filter4.getConstraints()).thenReturn(List.of(filter4Constraint));

        LiveDataQuery.Filter filter5 = mock(LiveDataQuery.Filter.class, "filter5");
        when(filter5.getProperty()).thenReturn("filterType");
        LiveDataQuery.Constraint filter5Constraint = mock(LiveDataQuery.Constraint.class, "filter5Constraint");
        when(filter5Constraint.getOperator()).thenReturn("equals");
        when(filter5Constraint.getValue()).thenReturn("INCLUSIVE");
        when(filter5.getConstraints()).thenReturn(List.of(filter5Constraint));

        when(ldQuery.getFilters()).thenReturn(List.of(
            filter1,
            filter2,
            filter3,
            filter4,
            filter5
        ));

        String queryString = "select nfp from DefaultNotificationFilterPreference nfp where owner = :owner "
            + "and ((nfp.pageOnly like :constraint_0 or nfp.page like :constraint_0 or nfp.wiki like :constraint_0 or "
            + "nfp.user like :constraint_0) or (nfp.pageOnly like :constraint_1 or nfp.page like :constraint_1 or "
            + "nfp.wiki like :constraint_1 or nfp.user like :constraint_1) or (nfp.pageOnly = :constraint_2 or "
            + "nfp.page = :constraint_2 or nfp.wiki = :constraint_2 or nfp.user = :constraint_2)) and "
            + "length(nfp.allEventTypes) = 0 and nfp.enabled = true and length(nfp.pageOnly) > 0 and "
            + "nfp.filterType = :filterType";

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(queryString, Query.HQL)).thenReturn(query);
        when(query.bindValue("owner", owner)).thenReturn(query);

        DefaultQueryParameter queryParameter1 = new DefaultQueryParameter(null);
        queryParameter1.literal("foo").anyChars();
        when(query.bindValue("constraint_0", queryParameter1)).thenReturn(query);

        DefaultQueryParameter queryParameter2 = new DefaultQueryParameter(null);
        queryParameter2.anyChars().literal("bar").anyChars();
        when(query.bindValue("constraint_1", queryParameter2)).thenReturn(query);

        DefaultQueryParameter queryParameter3 = new DefaultQueryParameter(null);
        queryParameter3.literal("buz");
        when(query.bindValue("constraint_2", queryParameter3)).thenReturn(query);

        when(query.bindValue("filterType", NotificationFilterType.INCLUSIVE)).thenReturn(query);

        when(query.setWiki(wikiName)).thenReturn(query);
        when(query.setOffset(offset.intValue())).thenReturn(query);
        when(query.setLimit(limit)).thenReturn(query);

        List<Object> expectedList = List.of(
            mock(NotificationFilterPreference.class, "filterPref1"),
            mock(NotificationFilterPreference.class, "filterPref2"),
            mock(NotificationFilterPreference.class, "filterPref3")
        );
        when(query.execute()).thenReturn(expectedList);
        assertEquals(expectedList, this.queryHelper.getFilterPreferences(ldQuery, owner, wikiReference));
        verify(query).bindValue("owner", owner);
        verify(query).bindValue("constraint_0", queryParameter1);
        verify(query).bindValue("constraint_1", queryParameter2);
        verify(query).bindValue("constraint_2", queryParameter3);
        verify(query).bindValue("filterType", NotificationFilterType.INCLUSIVE);
        verify(query).setWiki(wikiName);
        verify(query).setOffset(offset.intValue());
        verify(query).setLimit(limit);
    }

    @Test
    void countFilterPreferencesFilterNoSort() throws QueryException, LiveDataException
    {
        String owner = "xwiki:XWiki.Foo";
        String wikiName = "foo";
        Long offset = 3L;
        int limit = 12;
        WikiReference wikiReference = new WikiReference(wikiName);

        LiveDataQuery ldQuery = mock(LiveDataQuery.class);
        when(ldQuery.getOffset()).thenReturn(offset);
        when(ldQuery.getLimit()).thenReturn(limit);

        LiveDataQuery.Filter filter1 = mock(LiveDataQuery.Filter.class, "filter1");
        when(filter1.getProperty()).thenReturn("location");
        when(filter1.isMatchAll()).thenReturn(false);
        LiveDataQuery.Constraint filter1Constraint1 = mock(LiveDataQuery.Constraint.class, "filter1Constraint1");
        when(filter1Constraint1.getOperator()).thenReturn("startsWith");
        when(filter1Constraint1.getValue()).thenReturn("foo");
        LiveDataQuery.Constraint filter1Constraint2 = mock(LiveDataQuery.Constraint.class, "filter1Constraint2");
        when(filter1Constraint2.getOperator()).thenReturn("contains");
        when(filter1Constraint2.getValue()).thenReturn("bar");
        LiveDataQuery.Constraint filter1Constraint3 = mock(LiveDataQuery.Constraint.class, "filter1Constraint3");
        when(filter1Constraint3.getOperator()).thenReturn("equals");
        when(filter1Constraint3.getValue()).thenReturn("buz");
        when(filter1.getConstraints()).thenReturn(List.of(filter1Constraint1, filter1Constraint2, filter1Constraint3));

        LiveDataQuery.Filter filter2 = mock(LiveDataQuery.Filter.class, "filter2");
        when(filter2.getProperty()).thenReturn("eventTypes");
        LiveDataQuery.Constraint filter2Constraint = mock(LiveDataQuery.Constraint.class, "filter2Constraint");
        when(filter2Constraint.getOperator()).thenReturn("equals");
        when(filter2Constraint.getValue()).thenReturn("__ALL_EVENTS__");
        when(filter2.getConstraints()).thenReturn(List.of(filter2Constraint));

        LiveDataQuery.Filter filter3 = mock(LiveDataQuery.Filter.class, "filter3");
        when(filter3.getProperty()).thenReturn("isEnabled");
        LiveDataQuery.Constraint filter3Constraint = mock(LiveDataQuery.Constraint.class, "filter3Constraint");
        when(filter3Constraint.getOperator()).thenReturn("equals");
        when(filter3Constraint.getValue()).thenReturn("true");
        when(filter3.getConstraints()).thenReturn(List.of(filter3Constraint));

        LiveDataQuery.Filter filter4 = mock(LiveDataQuery.Filter.class, "filter4");
        when(filter4.getProperty()).thenReturn("scope");
        LiveDataQuery.Constraint filter4Constraint = mock(LiveDataQuery.Constraint.class, "filter4Constraint");
        when(filter4Constraint.getOperator()).thenReturn("equals");
        when(filter4Constraint.getValue()).thenReturn("PAGE");
        when(filter4.getConstraints()).thenReturn(List.of(filter4Constraint));

        LiveDataQuery.Filter filter5 = mock(LiveDataQuery.Filter.class, "filter5");
        when(filter5.getProperty()).thenReturn("filterType");
        LiveDataQuery.Constraint filter5Constraint = mock(LiveDataQuery.Constraint.class, "filter5Constraint");
        when(filter5Constraint.getOperator()).thenReturn("equals");
        when(filter5Constraint.getValue()).thenReturn("INCLUSIVE");
        when(filter5.getConstraints()).thenReturn(List.of(filter5Constraint));

        when(ldQuery.getFilters()).thenReturn(List.of(
            filter1,
            filter2,
            filter3,
            filter4,
            filter5
        ));

        String queryString = "select count(nfp.id) from DefaultNotificationFilterPreference nfp where owner = :owner "
            + "and ((nfp.pageOnly like :constraint_0 or nfp.page like :constraint_0 or nfp.wiki like :constraint_0 or "
            + "nfp.user like :constraint_0) or (nfp.pageOnly like :constraint_1 or nfp.page like :constraint_1 or "
            + "nfp.wiki like :constraint_1 or nfp.user like :constraint_1) or (nfp.pageOnly = :constraint_2 or "
            + "nfp.page = :constraint_2 or nfp.wiki = :constraint_2 or nfp.user = :constraint_2)) and "
            + "length(nfp.allEventTypes) = 0 and nfp.enabled = true and length(nfp.pageOnly) > 0 and "
            + "nfp.filterType = :filterType";

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(queryString, Query.HQL)).thenReturn(query);
        when(query.bindValue("owner", owner)).thenReturn(query);

        DefaultQueryParameter queryParameter1 = new DefaultQueryParameter(null);
        queryParameter1.literal("foo").anyChars();
        when(query.bindValue("constraint_0", queryParameter1)).thenReturn(query);

        DefaultQueryParameter queryParameter2 = new DefaultQueryParameter(null);
        queryParameter2.anyChars().literal("bar").anyChars();
        when(query.bindValue("constraint_1", queryParameter2)).thenReturn(query);

        DefaultQueryParameter queryParameter3 = new DefaultQueryParameter(null);
        queryParameter3.literal("buz");
        when(query.bindValue("constraint_2", queryParameter3)).thenReturn(query);

        when(query.bindValue("filterType", NotificationFilterType.INCLUSIVE)).thenReturn(query);

        when(query.setWiki(wikiName)).thenReturn(query);

        when(query.execute()).thenReturn(List.of(3L));
        assertEquals(3L, this.queryHelper.countTotalFilters(ldQuery, owner, wikiReference));
        verify(query).bindValue("owner", owner);
        verify(query).bindValue("constraint_0", queryParameter1);
        verify(query).bindValue("constraint_1", queryParameter2);
        verify(query).bindValue("constraint_2", queryParameter3);
        verify(query).bindValue("filterType", NotificationFilterType.INCLUSIVE);
        verify(query).setWiki(wikiName);
        verify(query, never()).setOffset(anyInt());
        verify(query, never()).setLimit(anyInt());
    }

    @Test
    void getFilterPreferencesFilterSort() throws QueryException, LiveDataException
    {
        String owner = "xwiki:XWiki.Foo";
        String wikiName = "foo";
        Long offset = 3L;
        int limit = 12;
        WikiReference wikiReference = new WikiReference(wikiName);

        LiveDataQuery ldQuery = mock(LiveDataQuery.class);
        when(ldQuery.getOffset()).thenReturn(offset);
        when(ldQuery.getLimit()).thenReturn(limit);

        LiveDataQuery.Filter filter1 = mock(LiveDataQuery.Filter.class, "filter1");
        when(filter1.getProperty()).thenReturn("location");
        when(filter1.isMatchAll()).thenReturn(false);
        LiveDataQuery.Constraint filter1Constraint1 = mock(LiveDataQuery.Constraint.class, "filter1Constraint1");
        when(filter1Constraint1.getOperator()).thenReturn("startsWith");
        when(filter1Constraint1.getValue()).thenReturn("foo");
        LiveDataQuery.Constraint filter1Constraint2 = mock(LiveDataQuery.Constraint.class, "filter1Constraint2");
        when(filter1Constraint2.getOperator()).thenReturn("contains");
        when(filter1Constraint2.getValue()).thenReturn("bar");
        LiveDataQuery.Constraint filter1Constraint3 = mock(LiveDataQuery.Constraint.class, "filter1Constraint3");
        when(filter1Constraint3.getOperator()).thenReturn("equals");
        when(filter1Constraint3.getValue()).thenReturn("buz");
        when(filter1.getConstraints()).thenReturn(List.of(filter1Constraint1, filter1Constraint2, filter1Constraint3));

        LiveDataQuery.Filter filter2 = mock(LiveDataQuery.Filter.class, "filter2");
        when(filter2.getProperty()).thenReturn("eventTypes");
        LiveDataQuery.Constraint filter2Constraint = mock(LiveDataQuery.Constraint.class, "filter2Constraint");
        when(filter2Constraint.getOperator()).thenReturn("equals");
        when(filter2Constraint.getValue()).thenReturn("__ALL_EVENTS__");
        when(filter2.getConstraints()).thenReturn(List.of(filter2Constraint));

        LiveDataQuery.Filter filter3 = mock(LiveDataQuery.Filter.class, "filter3");
        when(filter3.getProperty()).thenReturn("isEnabled");
        LiveDataQuery.Constraint filter3Constraint = mock(LiveDataQuery.Constraint.class, "filter3Constraint");
        when(filter3Constraint.getOperator()).thenReturn("equals");
        when(filter3Constraint.getValue()).thenReturn("true");
        when(filter3.getConstraints()).thenReturn(List.of(filter3Constraint));

        LiveDataQuery.Filter filter4 = mock(LiveDataQuery.Filter.class, "filter4");
        when(filter4.getProperty()).thenReturn("scope");
        LiveDataQuery.Constraint filter4Constraint = mock(LiveDataQuery.Constraint.class, "filter4Constraint");
        when(filter4Constraint.getOperator()).thenReturn("equals");
        when(filter4Constraint.getValue()).thenReturn("PAGE");
        when(filter4.getConstraints()).thenReturn(List.of(filter4Constraint));

        LiveDataQuery.Filter filter5 = mock(LiveDataQuery.Filter.class, "filter5");
        when(filter5.getProperty()).thenReturn("filterType");
        LiveDataQuery.Constraint filter5Constraint = mock(LiveDataQuery.Constraint.class, "filter5Constraint");
        when(filter5Constraint.getOperator()).thenReturn("equals");
        when(filter5Constraint.getValue()).thenReturn("INCLUSIVE");
        when(filter5.getConstraints()).thenReturn(List.of(filter5Constraint));

        when(ldQuery.getFilters()).thenReturn(List.of(
            filter1,
            filter2,
            filter3,
            filter4,
            filter5
        ));

        LiveDataQuery.SortEntry sortEntry1 = mock(LiveDataQuery.SortEntry.class, "sortEntry1");
        when(sortEntry1.isDescending()).thenReturn(false);
        when(sortEntry1.getProperty()).thenReturn("scope");

        LiveDataQuery.SortEntry sortEntry2 = mock(LiveDataQuery.SortEntry.class, "sortEntry2");
        when(sortEntry2.isDescending()).thenReturn(true);
        when(sortEntry2.getProperty()).thenReturn("isEnabled");

        LiveDataQuery.SortEntry sortEntry3 = mock(LiveDataQuery.SortEntry.class, "sortEntry3");
        when(sortEntry3.isDescending()).thenReturn(true);
        when(sortEntry3.getProperty()).thenReturn("notificationFormats");

        when(ldQuery.getSort()).thenReturn(List.of(sortEntry1, sortEntry2, sortEntry3));

        String queryString = "select nfp from DefaultNotificationFilterPreference nfp where owner = :owner "
            + "and ((nfp.pageOnly like :constraint_0 or nfp.page like :constraint_0 or nfp.wiki like :constraint_0 or "
            + "nfp.user like :constraint_0) or (nfp.pageOnly like :constraint_1 or nfp.page like :constraint_1 or "
            + "nfp.wiki like :constraint_1 or nfp.user like :constraint_1) or (nfp.pageOnly = :constraint_2 or "
            + "nfp.page = :constraint_2 or nfp.wiki = :constraint_2 or nfp.user = :constraint_2)) and "
            + "length(nfp.allEventTypes) = 0 and nfp.enabled = true and length(nfp.pageOnly) > 0 and "
            + "nfp.filterType = :filterType "
            + "order by nfp.pageOnly asc, nfp.page asc, nfp.wiki asc, nfp.user asc, nfp.enabled desc, "
            + "nfp.emailEnabled asc, nfp.alertEnabled desc";

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(queryString, Query.HQL)).thenReturn(query);
        when(query.bindValue("owner", owner)).thenReturn(query);

        DefaultQueryParameter queryParameter1 = new DefaultQueryParameter(null);
        queryParameter1.literal("foo").anyChars();
        when(query.bindValue("constraint_0", queryParameter1)).thenReturn(query);

        DefaultQueryParameter queryParameter2 = new DefaultQueryParameter(null);
        queryParameter2.anyChars().literal("bar").anyChars();
        when(query.bindValue("constraint_1", queryParameter2)).thenReturn(query);

        DefaultQueryParameter queryParameter3 = new DefaultQueryParameter(null);
        queryParameter3.literal("buz");
        when(query.bindValue("constraint_2", queryParameter3)).thenReturn(query);

        when(query.bindValue("filterType", NotificationFilterType.INCLUSIVE)).thenReturn(query);

        when(query.setWiki(wikiName)).thenReturn(query);
        when(query.setOffset(offset.intValue())).thenReturn(query);
        when(query.setLimit(limit)).thenReturn(query);

        List<Object> expectedList = List.of(
            mock(NotificationFilterPreference.class, "filterPref1"),
            mock(NotificationFilterPreference.class, "filterPref2"),
            mock(NotificationFilterPreference.class, "filterPref3")
        );
        when(query.execute()).thenReturn(expectedList);
        assertEquals(expectedList, this.queryHelper.getFilterPreferences(ldQuery, owner, wikiReference));
        verify(query).bindValue("owner", owner);
        verify(query).bindValue("constraint_0", queryParameter1);
        verify(query).bindValue("constraint_1", queryParameter2);
        verify(query).bindValue("constraint_2", queryParameter3);
        verify(query).bindValue("filterType", NotificationFilterType.INCLUSIVE);
        verify(query).setWiki(wikiName);
        verify(query).setOffset(offset.intValue());
        verify(query).setLimit(limit);
    }

    @Test
    void countFilterPreferencesFilterSort() throws QueryException, LiveDataException
    {
        String owner = "xwiki:XWiki.Foo";
        String wikiName = "foo";
        Long offset = 3L;
        int limit = 12;
        WikiReference wikiReference = new WikiReference(wikiName);

        LiveDataQuery ldQuery = mock(LiveDataQuery.class);
        when(ldQuery.getOffset()).thenReturn(offset);
        when(ldQuery.getLimit()).thenReturn(limit);

        LiveDataQuery.Filter filter1 = mock(LiveDataQuery.Filter.class, "filter1");
        when(filter1.getProperty()).thenReturn("location");
        when(filter1.isMatchAll()).thenReturn(false);
        LiveDataQuery.Constraint filter1Constraint1 = mock(LiveDataQuery.Constraint.class, "filter1Constraint1");
        when(filter1Constraint1.getOperator()).thenReturn("startsWith");
        when(filter1Constraint1.getValue()).thenReturn("foo");
        LiveDataQuery.Constraint filter1Constraint2 = mock(LiveDataQuery.Constraint.class, "filter1Constraint2");
        when(filter1Constraint2.getOperator()).thenReturn("contains");
        when(filter1Constraint2.getValue()).thenReturn("bar");
        LiveDataQuery.Constraint filter1Constraint3 = mock(LiveDataQuery.Constraint.class, "filter1Constraint3");
        when(filter1Constraint3.getOperator()).thenReturn("equals");
        when(filter1Constraint3.getValue()).thenReturn("buz");
        when(filter1.getConstraints()).thenReturn(List.of(filter1Constraint1, filter1Constraint2, filter1Constraint3));

        LiveDataQuery.Filter filter2 = mock(LiveDataQuery.Filter.class, "filter2");
        when(filter2.getProperty()).thenReturn("eventTypes");
        LiveDataQuery.Constraint filter2Constraint = mock(LiveDataQuery.Constraint.class, "filter2Constraint");
        when(filter2Constraint.getOperator()).thenReturn("equals");
        when(filter2Constraint.getValue()).thenReturn("__ALL_EVENTS__");
        when(filter2.getConstraints()).thenReturn(List.of(filter2Constraint));

        LiveDataQuery.Filter filter3 = mock(LiveDataQuery.Filter.class, "filter3");
        when(filter3.getProperty()).thenReturn("isEnabled");
        LiveDataQuery.Constraint filter3Constraint = mock(LiveDataQuery.Constraint.class, "filter3Constraint");
        when(filter3Constraint.getOperator()).thenReturn("equals");
        when(filter3Constraint.getValue()).thenReturn("true");
        when(filter3.getConstraints()).thenReturn(List.of(filter3Constraint));

        LiveDataQuery.Filter filter4 = mock(LiveDataQuery.Filter.class, "filter4");
        when(filter4.getProperty()).thenReturn("scope");
        LiveDataQuery.Constraint filter4Constraint = mock(LiveDataQuery.Constraint.class, "filter4Constraint");
        when(filter4Constraint.getOperator()).thenReturn("equals");
        when(filter4Constraint.getValue()).thenReturn("PAGE");
        when(filter4.getConstraints()).thenReturn(List.of(filter4Constraint));

        LiveDataQuery.Filter filter5 = mock(LiveDataQuery.Filter.class, "filter5");
        when(filter5.getProperty()).thenReturn("filterType");
        LiveDataQuery.Constraint filter5Constraint = mock(LiveDataQuery.Constraint.class, "filter5Constraint");
        when(filter5Constraint.getOperator()).thenReturn("equals");
        when(filter5Constraint.getValue()).thenReturn("INCLUSIVE");
        when(filter5.getConstraints()).thenReturn(List.of(filter5Constraint));

        when(ldQuery.getFilters()).thenReturn(List.of(
            filter1,
            filter2,
            filter3,
            filter4,
            filter5
        ));

        LiveDataQuery.SortEntry sortEntry1 = mock(LiveDataQuery.SortEntry.class, "sortEntry1");
        when(sortEntry1.isDescending()).thenReturn(false);
        when(sortEntry1.getProperty()).thenReturn("scope");

        LiveDataQuery.SortEntry sortEntry2 = mock(LiveDataQuery.SortEntry.class, "sortEntry2");
        when(sortEntry2.isDescending()).thenReturn(true);
        when(sortEntry2.getProperty()).thenReturn("isEnabled");

        LiveDataQuery.SortEntry sortEntry3 = mock(LiveDataQuery.SortEntry.class, "sortEntry3");
        when(sortEntry3.isDescending()).thenReturn(true);
        when(sortEntry3.getProperty()).thenReturn("notificationFormats");

        when(ldQuery.getSort()).thenReturn(List.of(sortEntry1, sortEntry2, sortEntry3));

        String queryString = "select count(nfp.id) from DefaultNotificationFilterPreference nfp where owner = :owner "
            + "and ((nfp.pageOnly like :constraint_0 or nfp.page like :constraint_0 or nfp.wiki like :constraint_0 or "
            + "nfp.user like :constraint_0) or (nfp.pageOnly like :constraint_1 or nfp.page like :constraint_1 or "
            + "nfp.wiki like :constraint_1 or nfp.user like :constraint_1) or (nfp.pageOnly = :constraint_2 or "
            + "nfp.page = :constraint_2 or nfp.wiki = :constraint_2 or nfp.user = :constraint_2)) and "
            + "length(nfp.allEventTypes) = 0 and nfp.enabled = true and length(nfp.pageOnly) > 0 and "
            + "nfp.filterType = :filterType";

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(queryString, Query.HQL)).thenReturn(query);
        when(query.bindValue("owner", owner)).thenReturn(query);

        DefaultQueryParameter queryParameter1 = new DefaultQueryParameter(null);
        queryParameter1.literal("foo").anyChars();
        when(query.bindValue("constraint_0", queryParameter1)).thenReturn(query);

        DefaultQueryParameter queryParameter2 = new DefaultQueryParameter(null);
        queryParameter2.anyChars().literal("bar").anyChars();
        when(query.bindValue("constraint_1", queryParameter2)).thenReturn(query);

        DefaultQueryParameter queryParameter3 = new DefaultQueryParameter(null);
        queryParameter3.literal("buz");
        when(query.bindValue("constraint_2", queryParameter3)).thenReturn(query);

        when(query.bindValue("filterType", NotificationFilterType.INCLUSIVE)).thenReturn(query);

        when(query.setWiki(wikiName)).thenReturn(query);

        when(query.execute()).thenReturn(List.of(3L));
        assertEquals(3L, this.queryHelper.countTotalFilters(ldQuery, owner, wikiReference));
        verify(query).bindValue("owner", owner);
        verify(query).bindValue("constraint_0", queryParameter1);
        verify(query).bindValue("constraint_1", queryParameter2);
        verify(query).bindValue("constraint_2", queryParameter3);
        verify(query).bindValue("filterType", NotificationFilterType.INCLUSIVE);
        verify(query).setWiki(wikiName);
        verify(query, never()).setOffset(anyInt());
        verify(query, never()).setLimit(anyInt());
    }
}