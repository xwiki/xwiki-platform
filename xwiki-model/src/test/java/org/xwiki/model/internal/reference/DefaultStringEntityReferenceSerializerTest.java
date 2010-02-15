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
package org.xwiki.model.internal.reference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Unit tests for {@link DefaultStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultStringEntityReferenceSerializerTest
{
    private static final String DEFAULT_WIKI = "xwiki";

    private static final String DEFAULT_SPACE = "XWiki";

    private static final String DEFAULT_PAGE = "WebHome";

    private static final String DEFAULT_ATTACHMENT = "filename";

    private EntityReferenceSerializer serializer;

    private EntityReferenceResolver resolver;

    private Mockery mockery = new Mockery();

    private ModelConfiguration mockModelConfiguration;

    @Before
    public void setUp()
    {
        this.serializer = new DefaultStringEntityReferenceSerializer();
        
        this.resolver = new DefaultStringEntityReferenceResolver();
        this.mockModelConfiguration = this.mockery.mock(ModelConfiguration.class);
        ReflectionUtils.setFieldValue(this.resolver, "configuration", this.mockModelConfiguration);

        this.mockery.checking(new Expectations() {{
            allowing(mockModelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue(DEFAULT_WIKI));
            allowing(mockModelConfiguration).getDefaultReferenceValue(EntityType.SPACE);
                will(returnValue(DEFAULT_SPACE));
            allowing(mockModelConfiguration).getDefaultReferenceValue(EntityType.DOCUMENT);
                will(returnValue(DEFAULT_PAGE));
            allowing(mockModelConfiguration).getDefaultReferenceValue(EntityType.ATTACHMENT);
                will(returnValue(DEFAULT_ATTACHMENT));
        }});
    }

    @Test
    public void testSerializeDocumentReference() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
        Assert.assertEquals("wiki:space.page", serializer.serialize(reference));

        reference = resolver.resolve("wiki:space.", EntityType.DOCUMENT);
        Assert.assertEquals("wiki:space.WebHome", serializer.serialize(reference));

        reference = resolver.resolve("space.", EntityType.DOCUMENT);
        Assert.assertEquals("xwiki:space.WebHome", serializer.serialize(reference));

        reference = resolver.resolve("page", EntityType.DOCUMENT);
        Assert.assertEquals("xwiki:XWiki.page", serializer.serialize(reference));

        reference = resolver.resolve(".", EntityType.DOCUMENT);
        Assert.assertEquals("xwiki:XWiki.WebHome", serializer.serialize(reference));

        reference = resolver.resolve(null, EntityType.DOCUMENT);
        Assert.assertEquals("xwiki:XWiki.WebHome", serializer.serialize(reference));

        reference = resolver.resolve("", EntityType.DOCUMENT);
        Assert.assertEquals("xwiki:XWiki.WebHome", serializer.serialize(reference));

        reference = resolver.resolve("wiki1.wiki2:wiki3:some.space.page", EntityType.DOCUMENT);
        Assert.assertEquals("wiki1.wiki2:wiki3:some\\.space.page", serializer.serialize(reference));

        reference = resolver.resolve("some.space.page", EntityType.DOCUMENT);
        Assert.assertEquals("xwiki:some\\.space.page", serializer.serialize(reference));

        reference = resolver.resolve("wiki:page", EntityType.DOCUMENT);
        Assert.assertEquals("xwiki:XWiki.wiki:page", serializer.serialize(reference));

        // Verify that passing null doesn't throw a NPE
        Assert.assertNull(serializer.serialize(null));
        
        // Test escapes

        reference = resolver.resolve("\\.:@\\.", EntityType.DOCUMENT);
        Assert.assertEquals("xwiki:XWiki.\\.:@\\.", serializer.serialize(reference));
    }

    @Test
    public void testSerializeSpaceReference() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space1.space2", EntityType.SPACE);
        Assert.assertEquals("wiki:space1\\.space2", serializer.serialize(reference));
    }

    @Test
    public void testSerializeAttachmentReference() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space.page@filename", EntityType.ATTACHMENT);
        Assert.assertEquals("wiki:space.page@filename", serializer.serialize(reference));

        reference = resolver.resolve("", EntityType.ATTACHMENT);
        Assert.assertEquals("xwiki:XWiki.WebHome@filename", serializer.serialize(reference));

        reference = resolver.resolve("wiki:space.page@my.png", EntityType.ATTACHMENT);
        Assert.assertEquals("wiki:space.page@my.png", serializer.serialize(reference));

        reference = resolver.resolve("some:file.name", EntityType.ATTACHMENT);
        Assert.assertEquals("xwiki:XWiki.WebHome@some:file.name", serializer.serialize(reference));

        // Test escapes

        reference = resolver.resolve(":.\\@", EntityType.ATTACHMENT);
        Assert.assertEquals("xwiki:XWiki.WebHome@:.\\@", serializer.serialize(reference));
    }
    
    @Test
    public void testSerializeReferenceWithChild() 
    {
        EntityReference reference = resolver.resolve("wiki:Space.Page", EntityType.DOCUMENT);
        Assert.assertEquals("wiki:Space", serializer.serialize(reference.getParent()));
        
        Assert.assertEquals("wiki", serializer.serialize(reference.getParent().getParent()));
    }
}