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
package org.xwiki.refactoring.internal.listener;

import java.util.Collections;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UpdateObjectsOnClassRenameListener}.
 * 
 * @version $Id$
 */
@ComponentTest
class UpdateObjectsOnClassRenameListenerTest
{
    @InjectMockComponents
    private UpdateObjectsOnClassRenameListener listener;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki wiki;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private DocumentReference oldClassReference = new DocumentReference("foo", "Code", "OldClass");

    private DocumentReference newClassReference = new DocumentReference("foo", "Code", "NewClass");

    @BeforeEach
    public void configure()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.wiki);

        when(this.localEntityReferenceSerializer.serialize(oldClassReference)).thenReturn("Code.OldClass");
    }

    @Test
    void onClassRenamed() throws Exception
    {
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), any())).thenReturn(query);
        when(query.execute()).thenReturn(Collections.singletonList("Some.Page"));

        DocumentReference documentReference = new DocumentReference("foo", "Some", "Page");
        when(this.documentReferenceResolver.resolve("Some.Page", oldClassReference)).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.xcontext)).thenReturn(document);

        BaseObject oldObject = mock(BaseObject.class, "old");
        when(document.getXObjects(oldClassReference)).thenReturn(Collections.singletonList(oldObject));

        BaseObject newObject = mock(BaseObject.class, "new");
        when(document.newXObject(newClassReference, this.xcontext)).thenReturn(newObject);

        @SuppressWarnings("unchecked")
        BaseProperty<EntityReference> oldProperty = mock(BaseProperty.class);
        when(oldProperty.getName()).thenReturn("age");
        when(oldObject.getProperties()).thenReturn(new Object[] {oldProperty});

        @SuppressWarnings("unchecked")
        BaseProperty<EntityReference> newProperty = mock(BaseProperty.class);
        when(oldProperty.clone()).thenReturn(newProperty);

        this.listener.onEvent(new DocumentRenamedEvent(this.oldClassReference, this.newClassReference), null, null);

        verify(query).setWiki("foo");
        verify(query).bindValue("className", "Code.OldClass");

        verify(newObject).safeput("age", newProperty);
        verify(document).removeXObject(oldObject);
        verify(this.wiki).saveDocument(document, "Rename [foo:Code.OldClass] objects into [foo:Code.NewClass]",
            this.xcontext);

        assertEquals("Updating the xobjects of type [foo:Code.OldClass] after the xclass has been renamed to "
            + "[foo:Code.NewClass].", logCapture.getMessage(0));
    }

    @Test
    void onClassMovedToDifferentWiki() throws Exception
    {
        this.listener.onEvent(
            new DocumentRenamedEvent(this.oldClassReference, new DocumentReference("bar", "Code", "NewClass")), null,
            null);

        verify(this.queryManager, never()).createQuery(any(), any());
        verify(this.wiki, never()).saveDocument(any(), any());
    }

    @Test
    void onClassRenamedWithoutUpdateLinks() throws Exception
    {
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setUpdateLinks(false);

        this.listener.onEvent(new DocumentRenamedEvent(this.oldClassReference, this.newClassReference), null,
            moveRequest);

        verify(this.queryManager, never()).createQuery(any(), any());
        verify(this.wiki, never()).saveDocument(any(), any());
    }
}
