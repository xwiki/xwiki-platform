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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;

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
public class CompactStringEntityReferenceSerializerTest
{
    public MockitoComponentMockingRule<EntityReferenceSerializer<String>> mocker =
        new MockitoComponentMockingRule<EntityReferenceSerializer<String>>(CompactStringEntityReferenceSerializer.class);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(this.mocker);

    @Test
    public void testSerializeWhenNoContext() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        Assert.assertEquals("wiki:space.page", this.mocker.getComponentUnderTest().serialize(reference));
    }

    @Test
    public void testSerializeWhenNoContextDocument() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        Assert.assertEquals("wiki:space.page", this.mocker.getComponentUnderTest().serialize(reference));
    }

    @Test
    public void testSerializeDocumentReferenceWhenContextDocument() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(reference));
        Assert.assertEquals("page", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "otherpage")));
        Assert.assertEquals("page", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference("wiki", "otherspace", "otherpage")));
        Assert.assertEquals("space.page", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "otherpage")));
        Assert.assertEquals("wiki:space.page", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "otherspace", "page")));
        Assert.assertEquals("space.page", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "page")));
        Assert.assertEquals("wiki:space.page", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "page")));
        Assert.assertEquals("wiki:space.page", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference("otherwiki", "space", "otherpage")));
        Assert.assertEquals("wiki:space.page", this.mocker.getComponentUnderTest().serialize(reference));
    }

    @Test
    public void testSerializeSpaceReferenceWhenHasChildren() throws Exception
    {
        AttachmentReference reference =
            new AttachmentReference("filename", new DocumentReference("wiki", "space", "page"));

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        Assert.assertEquals("page", this.mocker.getComponentUnderTest().serialize(reference.getParent()));
        Assert.assertEquals("space", this.mocker.getComponentUnderTest().serialize(reference.getParent().getParent()));

        this.oldcore.getXWikiContext().setWikiId("xwiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("xwiki", "xspace", "xpage")));
        Assert.assertEquals("wiki:space.page", this.mocker.getComponentUnderTest().serialize(reference.getParent()));
        Assert.assertEquals("wiki:space",
            this.mocker.getComponentUnderTest().serialize(reference.getParent().getParent()));

    }

    @Test
    public void testSerializeAttachmentReferenceWhenContextDocument() throws Exception
    {
        AttachmentReference reference =
            new AttachmentReference("filename", new DocumentReference("wiki", "space", "page"));

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        Assert.assertEquals("filename", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "otherpage")));
        Assert.assertEquals("page@filename", this.mocker.getComponentUnderTest().serialize(reference));

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "page")));
        Assert.assertEquals("wiki:space.page@filename", this.mocker.getComponentUnderTest().serialize(reference));
    }

    @Test
    public void testSerializeEntityReferenceWithExplicit() throws ComponentLookupException
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        Assert.assertEquals(
            "space.page",
            this.mocker.getComponentUnderTest().serialize(reference,
                new EntityReference("otherspace", EntityType.SPACE)));
    }

    @Test
    public void testSerializeNestedSpaceFromBaseReference() throws ComponentLookupException
    {
        DocumentReference baseReference = new DocumentReference("wiki", "space", "page");
        DocumentReference reference = new DocumentReference("wiki", Arrays.asList("space", "nested"), "page");

        Assert.assertEquals("space.nested.page", this.mocker.getComponentUnderTest().serialize(reference, baseReference));
    }

    @Test
    public void testSerializeNestedSpaceFromContext() throws ComponentLookupException
    {
        DocumentReference reference = new DocumentReference("wiki", Arrays.asList("space", "nested"), "page");

        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page2")));

        Assert.assertEquals("space.nested.page", this.mocker.getComponentUnderTest().serialize(reference));
    }
}
