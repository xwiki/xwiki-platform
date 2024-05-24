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
package org.xwiki.index.tree.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Unit tests for {@link ObjectsOfTypeTreeNode}.
 * 
 * @version $Id$
 * @since 10.7
 */
@ComponentTest
class ObjectsOfTypeTreeNodeTest
{
    private static String NODE_ID = "objectsOfType:wiki:Path.To.Page/Path.To.Class";

    private static DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");

    private static DocumentReference CLASS_REFERENCE =
        new DocumentReference("wiki", Arrays.asList("Path", "To"), "Class");

    @InjectMockComponents
    private ObjectsOfTypeTreeNode objectsOfTypeTreeNode;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWikiDocument document;

    @BeforeEach
    void before() throws Exception
    {
        when(this.currentDocumentReferenceResolver.resolve("wiki:Path.To.Page")).thenReturn(DOCUMENT_REFERENCE);
        when(this.defaultEntityReferenceSerializer.serialize(DOCUMENT_REFERENCE)).thenReturn("wiki:Path.To.Page");

        when(this.currentDocumentReferenceResolver.resolve("Path.To.Class")).thenReturn(CLASS_REFERENCE);

        XWiki wiki = mock(XWiki.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(wiki);
        when(wiki.getDocument(DOCUMENT_REFERENCE, this.xcontext)).thenReturn(this.document);
    }

    @Test
    void getChildCount()
    {
        int count = 3;
        List<BaseObject> objects = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BaseObject object = mock(BaseObject.class, String.valueOf(i));
            BaseObjectReference reference = mock(BaseObjectReference.class, String.valueOf(i));
            when(object.getReference()).thenReturn(reference);
            objects.add(object);
        }
        objects.add(2, null);
        when(this.document.getXObjects(CLASS_REFERENCE)).thenReturn(objects);

        assertEquals(count, this.objectsOfTypeTreeNode.getChildCount(NODE_ID));
    }

    @Test
    void getChildren()
    {
        List<BaseObject> objects = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            BaseObject object = mock(BaseObject.class, String.valueOf(i));
            BaseObjectReference reference = mock(BaseObjectReference.class, String.valueOf(i));
            when(this.defaultEntityReferenceSerializer.serialize(reference)).thenReturn(String.valueOf(i));
            when(object.getReference()).thenReturn(reference);
            objects.add(object);
        }
        when(this.document.getXObjects(CLASS_REFERENCE)).thenReturn(objects);

        assertEquals(Arrays.asList("object:1"), this.objectsOfTypeTreeNode.getChildren(NODE_ID, 1, 3));
    }

    @Test
    void getParent()
    {
        assertEquals("objects:wiki:Path.To.Page", this.objectsOfTypeTreeNode.getParent(NODE_ID));
    }
}
