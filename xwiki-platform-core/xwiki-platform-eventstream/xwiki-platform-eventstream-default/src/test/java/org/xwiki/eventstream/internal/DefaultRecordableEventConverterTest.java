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
package org.xwiki.eventstream.internal;

import com.google.common.collect.Sets;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.TargetableEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultRecordableEventConverter}.
 *
 * @version $Id$
 * @since 11.1RC1
 */
public class DefaultRecordableEventConverterTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultRecordableEventConverter> mocker =
            new MockitoComponentMockingRule<>(DefaultRecordableEventConverter.class);

    private Provider<XWikiContext> contextProvider;

    private static final DocumentReference CURRENT_USER = new DocumentReference("xwiki", "XWiki", "User");

    private static final WikiReference CURRENT_WIKI = new WikiReference("wiki");

    private static final Set<String> TARGETS = Sets.newHashSet("userB", "groupC");

    @Before
    public void setUp() throws Exception
    {
        this.contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        when(context.getUserReference()).thenReturn(CURRENT_USER);
        when(context.getWikiReference()).thenReturn(CURRENT_WIKI);
    }

    private class MockedRecordableEvent implements RecordableEvent, TargetableEvent
    {
        @Override
        public boolean matches(Object otherEvent) {
            return false;
        }

        @Override
        public Set<String> getTarget() {
            return TARGETS;
        }
    }

    @Test
    public void convert() throws Exception
    {
        RecordableEvent recordableEvent = new MockedRecordableEvent();
        Event result = mocker.getComponentUnderTest().convert(recordableEvent, "source", "some data");
        assertEquals("org.xwiki.eventstream.internal.DefaultRecordableEventConverterTest.MockedRecordableEvent",
                result.getType());
        assertEquals("source", result.getApplication());
        assertNotNull(result.getDate());
        assertEquals(CURRENT_USER, result.getUser());
        assertEquals(CURRENT_WIKI, result.getWiki());
        assertEquals("some data", result.getBody());
        assertEquals(TARGETS, result.getTarget());
    }

    @Test
    public void convertDocumentEvent() throws Exception
    {
        RecordableEvent recordableEvent = new MockedRecordableEvent();
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("xwiki", "Some", "Doc");
        when(doc.getDocumentReference()).thenReturn(documentReference);
        when(doc.getVersion()).thenReturn("5.1");
        when(doc.getRenderedTitle(any(XWikiContext.class))).thenReturn("Some title");

        Event result = mocker.getComponentUnderTest().convert(recordableEvent, "source", doc);
        assertEquals("org.xwiki.eventstream.internal.DefaultRecordableEventConverterTest.MockedRecordableEvent",
                result.getType());
        assertEquals("source", result.getApplication());
        assertNotNull(result.getDate());
        assertEquals(CURRENT_USER, result.getUser());
        assertEquals(CURRENT_WIKI, result.getWiki());
        assertNull(result.getBody());
        assertEquals(TARGETS, result.getTarget());
        assertEquals(documentReference, result.getDocument());
        assertEquals("5.1", result.getDocumentVersion());
        assertEquals("Some title", result.getDocumentTitle());
    }
}
