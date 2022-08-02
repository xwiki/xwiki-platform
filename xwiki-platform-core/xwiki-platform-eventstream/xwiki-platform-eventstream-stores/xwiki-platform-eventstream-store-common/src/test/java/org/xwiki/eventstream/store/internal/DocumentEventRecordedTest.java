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
package org.xwiki.eventstream.store.internal;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DocumentEventRecorder}
 *
 * @version $Id$
 */
@ComponentTest
class DocumentEventRecordedTest
{
    @InjectMockComponents
    private DocumentEventRecorder documentEventRecorder;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private EventStore eventStore;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private EventFactory eventFactory;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        when(this.eventFactory.createRawEvent()).thenReturn(new DefaultEvent());
    }

    @Test
    void recordUpdateEvent() throws EventStreamException, ExecutionException, InterruptedException
    {
        XWikiDocument source = mock(XWikiDocument.class);
        DocumentReference reference = new DocumentReference("foo", Arrays.asList("Space", "SubSpace"), "PageName");
        when(this.serializer.serialize(reference.getLastSpaceReference())).thenReturn("Space.SubSpace");
        when(source.getDocumentReference()).thenReturn(reference);

        Date updateDate = new Date(42);
        when(source.getDate()).thenReturn(updateDate);
        String version = "12.2";
        when(source.getVersion()).thenReturn(version);
        when(source.isHidden()).thenReturn(true);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(source.getAuthors()).thenReturn(documentAuthors);
        UserReference originalAuthor = mock(UserReference.class);
        when(documentAuthors.getOriginalMetadataAuthor()).thenReturn(originalAuthor);

        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.userReferenceSerializer.serialize(originalAuthor)).thenReturn(userDocReference);
        String myDocTitle = "My document title";
        when(source.getRenderedTitle(Syntax.PLAIN_1_0, this.context)).thenReturn(myDocTitle);

        DefaultEvent expectedEvent = new DefaultEvent();
        expectedEvent.setStream("Space.SubSpace");
        expectedEvent.setDocument(reference);
        expectedEvent.setDate(updateDate);
        expectedEvent.setImportance(org.xwiki.eventstream.Event.Importance.MEDIUM);
        expectedEvent.setType("update");
        expectedEvent.setTitle("activitystream.event.update");
        expectedEvent.setBody("activitystream.event.update");
        expectedEvent.setDocumentVersion(version);
        expectedEvent.setUser(userDocReference);
        expectedEvent.setHidden(true);
        expectedEvent.setDocumentTitle(myDocTitle);

        this.documentEventRecorder.recordEvent(new DocumentUpdatedEvent(), source);
        verify(this.eventStore).saveEvent(expectedEvent);
    }
}
