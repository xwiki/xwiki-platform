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

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UntypedRecordableEventDescriptorComponentBuilder}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class UntypedRecordableEventDescriptorComponentBuilderTest
{
    @Rule
    public final MockitoComponentMockingRule<UntypedRecordableEventDescriptorComponentBuilder> mocker =
            new MockitoComponentMockingRule<>(UntypedRecordableEventDescriptorComponentBuilder.class);

    private ModelBridge modelBridge;

    private QueryManager queryManager;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private AuthorizationManager authorizationManager;

    private DocumentReference event1;
    private DocumentReference event2;
    private DocumentReference event3;

    @Before
    public void setUp() throws Exception
    {
        queryManager = mocker.registerMockComponent(QueryManager.class);
        modelBridge = mocker.registerMockComponent(ModelBridge.class);
        documentReferenceResolver = mocker.registerMockComponent(DocumentReferenceResolver.class);
        authorizationManager = mocker.registerMockComponent(AuthorizationManager.class);

        Query query = mock(Query.class);
        when(queryManager.createQuery(any(), any())).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("e1", "e2", "e3"));

        event1 = mock(DocumentReference.class);
        event2 = mock(DocumentReference.class);
        event3 = mock(DocumentReference.class);

        when(this.documentReferenceResolver.resolve("e1")).thenReturn(event1);
        when(this.documentReferenceResolver.resolve("e2")).thenReturn(event2);
        when(this.documentReferenceResolver.resolve("e3")).thenReturn(event3);
    }

    @Test
    public void testClassReference() throws Exception
    {
        assertEquals("EventClass",
                this.mocker.getComponentUnderTest().getClassReference().getName());
    }

    @Test
    public void testBuildComponent() throws Exception
    {
        BaseObject baseObject = mock(BaseObject.class);
        XWikiDocument parentDocument = mock(XWikiDocument.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(baseObject.getOwnerDocument()).thenReturn(parentDocument);
        when(parentDocument.getDocumentReference()).thenReturn(documentReference);

        // Ensure that the user rights are correctly checked
        when(this.authorizationManager.hasAccess(any(), any(), any())).thenReturn(true);

        List<WikiComponent> result = this.mocker.getComponentUnderTest().buildComponents(baseObject);

        assertEquals(1, result.size());
    }
}
