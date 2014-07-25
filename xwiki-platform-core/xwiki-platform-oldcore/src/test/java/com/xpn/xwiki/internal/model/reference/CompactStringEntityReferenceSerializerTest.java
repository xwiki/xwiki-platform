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

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for {@link com.xpn.xwiki.internal.model.reference.CompactStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 */
public class CompactStringEntityReferenceSerializerTest extends AbstractBridgedComponentTestCase
{
    private EntityReferenceSerializer<EntityReference> serializer;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.serializer = getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING, "compact");
    }

    @Test
    public void testSerializeWhenNoContext() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        Assert.assertEquals("wiki:space.page", this.serializer.serialize(reference));
    }

    @Test
    public void testSerializeWhenNoContextDocument() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        Assert.assertEquals("wiki:space.page", this.serializer.serialize(reference));
    }

    @Test
    public void testSerializeDocumentReferenceWhenContextDocument() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        getContext().setDatabase("wiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        Assert.assertEquals("page", this.serializer.serialize(reference));

        getContext().setDatabase("wiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "otherpage")));
        Assert.assertEquals("page", this.serializer.serialize(reference));

        getContext().setDatabase("wiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "otherspace", "otherpage")));
        Assert.assertEquals("space.page", this.serializer.serialize(reference));

        getContext().setDatabase("otherwiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "otherpage")));
        Assert.assertEquals("wiki:space.page", this.serializer.serialize(reference));

        getContext().setDatabase("wiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "otherspace", "page")));
        Assert.assertEquals("space.page", this.serializer.serialize(reference));

        getContext().setDatabase("otherwiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "page")));
        Assert.assertEquals("wiki:space.page", this.serializer.serialize(reference));

        getContext().setDatabase("otherwiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "page")));
        Assert.assertEquals("wiki:space.page", this.serializer.serialize(reference));

        getContext().setDatabase("otherwiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "otherpage")));
        Assert.assertEquals("wiki:space.page", this.serializer.serialize(reference));
    }

    @Test
    public void testSerializeSpaceReferenceWhenHasChildren() throws Exception
    {
        AttachmentReference reference =
            new AttachmentReference("filename", new DocumentReference("wiki", "space", "page"));

        getContext().setDatabase("wiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        Assert.assertEquals("page", this.serializer.serialize(reference.getParent()));
        Assert.assertEquals("space", this.serializer.serialize(reference.getParent().getParent()));

        getContext().setDatabase("xwiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("xwiki", "xspace", "xpage")));
        Assert.assertEquals("wiki:space.page", this.serializer.serialize(reference.getParent()));
        Assert.assertEquals("wiki:space", this.serializer.serialize(reference.getParent().getParent()));

    }

    @Test
    public void testSerializeAttachmentReferenceWhenContextDocument() throws Exception
    {
        AttachmentReference reference =
            new AttachmentReference("filename", new DocumentReference("wiki", "space", "page"));

        getContext().setDatabase("wiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        Assert.assertEquals("filename", this.serializer.serialize(reference));

        getContext().setDatabase("wiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "otherpage")));
        Assert.assertEquals("page@filename", this.serializer.serialize(reference));

        getContext().setDatabase("otherwiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "page")));
        Assert.assertEquals("wiki:space.page@filename", this.serializer.serialize(reference));
    }

    @Test
    public void testSerializeEntityReferenceWithExplicit()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        getContext().setDatabase("wiki");
        getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        Assert.assertEquals("space.page",
            this.serializer.serialize(reference, new EntityReference("otherspace", EntityType.SPACE)));
    }
}
