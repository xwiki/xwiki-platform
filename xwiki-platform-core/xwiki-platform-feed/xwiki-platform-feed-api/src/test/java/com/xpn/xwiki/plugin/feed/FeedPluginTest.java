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
package com.xpn.xwiki.plugin.feed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FeedPlugin}.
 *
 * @version $Id$
 */
class FeedPluginTest
{
    private static final Date OLD = new Date(1_000_000L);

    private static final Date RECENT = new Date(2_000_000L);

    private static SyndEntry syndEntry(Date publishedDate)
    {
        SyndEntry entry = new SyndEntryImpl();
        entry.setPublishedDate(publishedDate);

        return entry;
    }

    private static com.xpn.xwiki.api.Object feedEntry(Date date)
    {
        com.xpn.xwiki.api.Object entry = mock();
        when(entry.getValue("date")).thenReturn(date);

        return entry;
    }

    @Test
    void syndEntryComparatorSortsMostRecentFirst()
    {
        SyndEntry old = syndEntry(OLD);
        SyndEntry recent = syndEntry(RECENT);
        List<SyndEntry> entries = new ArrayList<>(Arrays.asList(old, recent));

        entries.sort(new FeedPlugin.SyndEntryComparator());

        assertEquals(Arrays.asList(recent, old), entries);
    }

    @Test
    void syndEntryComparatorSortsEntriesWithoutDateLast()
    {
        SyndEntry undated = syndEntry(null);
        SyndEntry old = syndEntry(OLD);
        SyndEntry recent = syndEntry(RECENT);
        List<SyndEntry> entries = new ArrayList<>(Arrays.asList(undated, old, recent));

        entries.sort(new FeedPlugin.SyndEntryComparator());

        assertEquals(Arrays.asList(recent, old, undated), entries);
    }

    @Test
    void syndEntryComparatorReturnsZeroForEqualDates()
    {
        assertEquals(0, new FeedPlugin.SyndEntryComparator().compare(syndEntry(OLD), syndEntry(OLD)));
        assertEquals(0, new FeedPlugin.SyndEntryComparator().compare(syndEntry(null), syndEntry(null)));
    }

    @Test
    void entriesComparatorSortsMostRecentFirst()
    {
        com.xpn.xwiki.api.Object old = feedEntry(OLD);
        com.xpn.xwiki.api.Object recent = feedEntry(RECENT);
        List<com.xpn.xwiki.api.Object> entries = new ArrayList<>(Arrays.asList(old, recent));

        entries.sort(new FeedPlugin.EntriesComparator());

        assertEquals(Arrays.asList(recent, old), entries);
    }

    @Test
    void entriesComparatorSortsEntriesWithoutDateLast()
    {
        com.xpn.xwiki.api.Object undated = feedEntry(null);
        com.xpn.xwiki.api.Object old = feedEntry(OLD);
        com.xpn.xwiki.api.Object recent = feedEntry(RECENT);
        List<com.xpn.xwiki.api.Object> entries = new ArrayList<>(Arrays.asList(undated, old, recent));

        entries.sort(new FeedPlugin.EntriesComparator());

        assertEquals(Arrays.asList(recent, old, undated), entries);
    }

    @Test
    void entriesComparatorReturnsZeroForEqualDates()
    {
        assertEquals(0, new FeedPlugin.EntriesComparator().compare(feedEntry(OLD), feedEntry(OLD)));
        assertEquals(0, new FeedPlugin.EntriesComparator().compare(feedEntry(null), feedEntry(null)));
    }

    /**
     * The comparator used to read both of its operands from the first entry, which made it always return 0 and left
     * the list in its original order.
     */
    @Test
    void entriesComparatorReadsTheDateOfBothEntries()
    {
        com.xpn.xwiki.api.Object old = feedEntry(OLD);
        com.xpn.xwiki.api.Object recent = feedEntry(RECENT);

        assertEquals(-1, Integer.signum(new FeedPlugin.EntriesComparator().compare(recent, old)));
        assertEquals(1, Integer.signum(new FeedPlugin.EntriesComparator().compare(old, recent)));

        verify(old, times(2)).getValue("date");
        verify(recent, times(2)).getValue("date");
    }

    /**
     * {@code com.xpn.xwiki.api.Object#getXWikiObject()}, which the comparator used to call, returns null without
     * programming rights.
     */
    @Test
    void entriesComparatorDoesNotNeedProgrammingRights()
    {
        com.xpn.xwiki.api.Object old = feedEntry(OLD);
        com.xpn.xwiki.api.Object recent = feedEntry(RECENT);
        when(old.getXWikiObject()).thenReturn(null);
        when(recent.getXWikiObject()).thenReturn(null);
        List<com.xpn.xwiki.api.Object> entries = new ArrayList<>(Arrays.asList(old, recent));

        entries.sort(new FeedPlugin.EntriesComparator());

        assertEquals(Arrays.asList(recent, old), entries);
    }
}
