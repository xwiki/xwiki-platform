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
package org.xwiki.watchlist.internal.api;

import java.util.Arrays;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link WatchListEvent}.
 *
 * @version $Id$
 * @since 7.1M1
 */
@ComponentList({LocalStringEntityReferenceSerializer.class, DefaultStringEntityReferenceSerializer.class})
public class WatchListEventTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    private DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

    private DocumentReference userReference = new DocumentReference("wiki", "XWiki", "user");

    @Before
    public void setup()
    {
        // Use the mock component manager.
        Utils.setComponentManager(this.mocker);
    }

    @Test
    public void addEventsOnlyUpdates()
    {
        WatchListEvent event = newEvent(WatchListEventType.UPDATE, "2.2", new Date(1000000));
        event.addEvent(newEvent(WatchListEventType.UPDATE, "2.1", new Date(900000)));
        event.addEvent(newEvent(WatchListEventType.UPDATE, "2.0", new Date(800000)));

        Assert.assertEquals(this.documentReference, event.getDocumentReference());
        Assert.assertEquals(this.userReference, event.getAuthorReference());
        Assert.assertEquals(Arrays.asList("wiki:XWiki.user"), event.getAuthors());
        Assert.assertEquals(WatchListEventType.UPDATE, event.getType());
        Assert.assertEquals(Arrays.asList(new Date(1000000), new Date(900000), new Date(800000)), event.getDates());
        Assert.assertEquals("2.2", event.getVersion());
        Assert.assertEquals(Arrays.asList("2.2", "2.1", "2.0"), event.getVersions());
    }

    @Test
    public void addEventsUpdateThenCreate()
    {
        WatchListEvent event = newEvent(WatchListEventType.UPDATE, "2.2", new Date(1000000));
        event.addEvent(newEvent(WatchListEventType.UPDATE, "2.1", new Date(900000)));
        event.addEvent(newEvent(WatchListEventType.CREATE, "1.1", new Date(800000)));

        Assert.assertEquals(this.documentReference, event.getDocumentReference());
        Assert.assertEquals(this.userReference, event.getAuthorReference());
        Assert.assertEquals(Arrays.asList("wiki:XWiki.user"), event.getAuthors());
        Assert.assertEquals(WatchListEventType.UPDATE, event.getType());
        Assert.assertEquals(Arrays.asList(new Date(1000000), new Date(900000), new Date(800000)), event.getDates());
        Assert.assertEquals("2.2", event.getVersion());
        Assert.assertEquals(Arrays.asList("2.2", "2.1", "1.1"), event.getVersions());
    }

    @Test
    public void addEventsDeleteUpdateUpdate()
    {
        WatchListEvent event = newEvent(WatchListEventType.DELETE, "1.4", new Date(1000000));
        event.addEvent(newEvent(WatchListEventType.UPDATE, "1.3", new Date(900000)));
        event.addEvent(newEvent(WatchListEventType.UPDATE, "1.2", new Date(800000)));

        Assert.assertEquals(this.documentReference, event.getDocumentReference());
        Assert.assertEquals(this.userReference, event.getAuthorReference());
        Assert.assertEquals(Arrays.asList("wiki:XWiki.user"), event.getAuthors());
        Assert.assertEquals(WatchListEventType.DELETE, event.getType());
        Assert.assertEquals(Arrays.asList(new Date(1000000)), event.getDates());
        Assert.assertEquals("1.4", event.getVersion());
        Assert.assertEquals(Arrays.asList("1.4"), event.getVersions());
    }

    // @Test
    // public void addEventsDeleteUpdateCreate()
    // {
    // WatchListEvent event = newEvent(WatchListEventType.DELETE, "1.3", new Date(1000000));
    // event.addEvent(newEvent(WatchListEventType.UPDATE, "1.2", new Date(900000)));
    // event.addEvent(newEvent(WatchListEventType.CREATE, "1.1", new Date(800000)));
    //
    // Assert.assertEquals(this.documentReference, event.getDocumentReference());
    // Assert.assertEquals(this.userReference, event.getAuthorReference());
    // Assert.assertEquals(Arrays.asList("wiki:XWiki.user"), event.getAuthors());
    // Assert.assertEquals(WatchListEventType.DELETE, event.getType());
    // Assert.assertEquals(Arrays.asList(new Date(1000000)), event.getDates());
    // Assert.assertEquals("1.4", event.getVersion());
    // Assert.assertEquals(Arrays.asList("1.4"), event.getVersions());
    // }

    // @Test
    // public void addEventsUpdateThenCreateThenDelete()
    // {
    // WatchListEvent event = newEvent(WatchListEventType.UPDATE, "1.2", new Date(1000000));
    // event.addEvent(newEvent(WatchListEventType.CREATE, "1.1", new Date(900000)));
    // event.addEvent(newEvent(WatchListEventType.DELETE, "2.2", new Date(800000)));
    //
    // Assert.assertEquals(this.documentReference, event.getDocumentReference());
    // Assert.assertEquals(this.userReference, event.getAuthorReference());
    // Assert.assertEquals(Arrays.asList("wiki:XWiki.user"), event.getAuthors());
    // Assert.assertEquals(WatchListEventType.UPDATE, event.getType());
    // Assert.assertEquals(Arrays.asList(new Date(1000000), new Date(900000), new Date(800000)), event.getDates());
    // Assert.assertEquals("1.2", event.getVersion());
    // Assert.assertEquals(Arrays.asList("1.2", "1.1", "2.2"), event.getVersions());
    // }

    private WatchListEvent newEvent(String type, String version, Date date)
    {
        return new WatchListEvent(this.documentReference, type, this.userReference, version, date);
    }
}
