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
package com.xpn.xwiki.internal.model.reference;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link CompactStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 */
@ComponentList({
    DefaultSymbolScheme.class,
    CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class
})
@OldcoreTest
class CompactStringEntityReferenceSerializerTest
{
    @InjectMockComponents
    private CompactStringEntityReferenceSerializer serializer;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Test
    void serializeWhenNoContext()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        assertEquals("wiki:space.page", this.serializer.serialize(reference));
    }

    @Test
    void serializeWhenNoContextDocument()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        assertEquals("wiki:space.page", this.serializer.serialize(reference));
    }

    @Test
    void serializeDocumentReferenceWhenContextDocument()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(reference));
        assertEquals("page", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "otherpage")));
        assertEquals("page", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference("wiki", "otherspace", "otherpage")));
        assertEquals("space.page", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "otherpage")));
        assertEquals("wiki:space.page", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "otherspace", "page")));
        assertEquals("space.page", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "page")));
        assertEquals("wiki:space.page", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "page")));
        assertEquals("wiki:space.page", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference("otherwiki", "space", "otherpage")));
        assertEquals("wiki:space.page", this.serializer.serialize(reference));
    }

    @Test
    void serializeSpaceReferenceWhenHasChildren()
    {
        AttachmentReference reference =
            new AttachmentReference("filename", new DocumentReference("wiki", "space", "page"));

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        assertEquals("page", this.serializer.serialize(reference.getParent()));
        assertEquals("space", this.serializer.serialize(reference.getParent().getParent()));

        this.oldcore.getXWikiContext().setWikiId("xwiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("xwiki", "xspace", "xpage")));
        assertEquals("wiki:space.page", this.serializer.serialize(reference.getParent()));
        assertEquals("wiki:space",
            this.serializer.serialize(reference.getParent().getParent()));

    }

    @Test
    void serializeAttachmentReferenceWhenContextDocument()
    {
        AttachmentReference reference =
            new AttachmentReference("filename", new DocumentReference("wiki", "space", "page"));

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        assertEquals("filename", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "otherpage")));
        assertEquals("page@filename", this.serializer.serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "page")));
        assertEquals("wiki:space.page@filename", this.serializer.serialize(reference));
    }

    @Test
    void serializeEntityReferenceWithExplicit()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        assertEquals(
            "space.page",
            this.serializer.serialize(reference,
                new EntityReference("otherspace", EntityType.SPACE)));
    }

    @Test
    void serializeNestedSpaceFromBaseReference()
    {
        DocumentReference baseReference = new DocumentReference("wiki", "space", "page");
        DocumentReference reference = new DocumentReference("wiki", Arrays.asList("space", "nested"), "page");

        assertEquals("space.nested.page", this.serializer.serialize(reference, baseReference));
    }

    @Test
    void serializeNestedSpaceFromContext()
    {
        DocumentReference reference = new DocumentReference("wiki", Arrays.asList("space", "nested"), "page");

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page2")));

        assertEquals("space.nested.page", this.serializer.serialize(reference));
    }
}
