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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultModelBridge}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class DefaultModelBridgeTest
{
    @Rule
    public final MockitoComponentMockingRule<ModelBridge> mocker =
            new MockitoComponentMockingRule<>(DefaultModelBridge.class);

    private Provider<XWikiContext> contextProvider;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Before
    public void setUp() throws Exception
    {
        this.contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);

        this.documentReferenceResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
    }

    @Test
    public void testAuthorReference() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference authorReference = mock(DocumentReference.class);
        EntityReference entityReference = mock(EntityReference.class);

        when(this.contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(entityReference, context)).thenReturn(document);
        when(document.getAuthorReference()).thenReturn(authorReference);

        DocumentReference result = this.mocker.getComponentUnderTest().getAuthorReference(entityReference);

        assertEquals(authorReference, result);
    }

    @Test
    public void testCheckXObjectPresenceWithEmptyList() throws Exception
    {
        Object object = new Object();
        List<String> xObjectTypes = Collections.EMPTY_LIST;

        assertTrue(this.mocker.getComponentUnderTest().checkXObjectPresence(xObjectTypes, object));
    }

    @Test
    public void testCheckXObjectPresenceWithPresentXObject() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        List<String> xObjectTypes = Arrays.asList("type1");

        DocumentReference documentReferenceFromDocumentXObjects = mock(DocumentReference.class);
        Map<DocumentReference, List<BaseObject>> documentXObjects =
                new HashMap<DocumentReference, List<BaseObject>>() {{
                    put(documentReferenceFromDocumentXObjects, Collections.EMPTY_LIST);
                }};

        DocumentReference resolvedType1 = mock(DocumentReference.class);
        LocalDocumentReference localDocumentReferenceType1 = mock(LocalDocumentReference.class);

        when(resolvedType1.getLocalDocumentReference()).thenReturn(localDocumentReferenceType1);
        when(this.documentReferenceResolver.resolve(eq("type1"))).thenReturn(resolvedType1);
        when(document.getXObjects()).thenReturn(documentXObjects);

        when(documentReferenceFromDocumentXObjects.getLocalDocumentReference()).thenReturn(localDocumentReferenceType1);

        DocumentReference resolvedType2 = mock(DocumentReference.class);
        LocalDocumentReference localDocumentReferenceType2 = mock(LocalDocumentReference.class);

        when(resolvedType2.getLocalDocumentReference()).thenReturn(localDocumentReferenceType2);
        when(this.documentReferenceResolver.resolve(eq("type2"))).thenReturn(resolvedType2);

        assertTrue(this.mocker.getComponentUnderTest().checkXObjectPresence(xObjectTypes, document));
        assertFalse(this.mocker.getComponentUnderTest().checkXObjectPresence(Arrays.asList("type2"), document));
    }
}
