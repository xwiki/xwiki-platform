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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * Unit tests for {@link DefaultStringEntityReferenceResolver}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultStringEntityReferenceResolverTest
{
    private static final String DEFAULT_WIKI = "defwiki";
    
    private static final String DEFAULT_SPACE = "defspace";
    
    private static final String DEFAULT_PAGE = "defpage";
        
    private static final String DEFAULT_ATTACHMENT = "deffilename";

    private static final String DEFAULT_OBJECT = "defobject";

    private static final String DEFAULT_OBJECT_PROPERTY = "defproperty";

    private EntityReferenceResolver<String> resolver;

    private Mockery mockery = new Mockery();

    @Before
    public void setUp()
    {
        this.resolver = new DefaultStringEntityReferenceResolver();
        final EntityReferenceValueProvider mockValueProvider = this.mockery.mock(EntityReferenceValueProvider.class);
        ReflectionUtils.setFieldValue(this.resolver, "provider", mockValueProvider);
        
        this.mockery.checking(new Expectations() {{
            allowing(mockValueProvider).getDefaultValue(EntityType.WIKI);
                will(returnValue(DEFAULT_WIKI));
            allowing(mockValueProvider).getDefaultValue(EntityType.SPACE);
                will(returnValue(DEFAULT_SPACE));
            allowing(mockValueProvider).getDefaultValue(EntityType.DOCUMENT);
                will(returnValue(DEFAULT_PAGE));
            allowing(mockValueProvider).getDefaultValue(EntityType.ATTACHMENT);
                will(returnValue(DEFAULT_ATTACHMENT));
            allowing(mockValueProvider).getDefaultValue(EntityType.OBJECT);
                will(returnValue(DEFAULT_OBJECT));
            allowing(mockValueProvider).getDefaultValue(EntityType.OBJECT_PROPERTY);
                will(returnValue(DEFAULT_OBJECT_PROPERTY));
        }});
    }

    @Test
    public void testResolveDocumentReference() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());

        reference = resolver.resolve("wiki:space.", EntityType.DOCUMENT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.getName());

        reference = resolver.resolve("space.", EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.getName());

        reference = resolver.resolve("page", EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());

        reference = resolver.resolve(".", EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.getName());

        reference = resolver.resolve(null, EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.getName());

        reference = resolver.resolve("", EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.getName());

        reference = resolver.resolve("wiki1.wiki2:wiki3:some.space.page", EntityType.DOCUMENT);
        Assert.assertEquals("wiki1.wiki2:wiki3", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("some.space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());

        reference = resolver.resolve("some.space.page", EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("some.space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());

        reference = resolver.resolve("wiki:page", EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("wiki:page", reference.getName());

        // Test escapes

        reference = resolver.resolve("\\\\\\.:@\\.", EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("\\.:@.", reference.getName());

        reference = resolver.resolve("some\\.space.page", EntityType.DOCUMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("some.space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());

        // Escaping characters are escaped
        reference = resolver.resolve("\\\\:\\\\.\\\\", EntityType.DOCUMENT);
        Assert.assertEquals("\\", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("\\", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("\\", reference.getName());

        reference = resolver.resolve("\\wiki:\\space.\\page", EntityType.DOCUMENT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());
    }

    @Test
    public void testResolveAttachmentReference() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space.page@filename.ext", EntityType.ATTACHMENT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("filename.ext", reference.getName());

        reference = resolver.resolve("", EntityType.ATTACHMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(DEFAULT_ATTACHMENT, reference.getName());

        reference = resolver.resolve("wiki:space.page@my.png", EntityType.ATTACHMENT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("my.png", reference.getName());

        reference = resolver.resolve("some:file.name", EntityType.ATTACHMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("some:file.name", reference.getName());

        // Test escapes

        reference = resolver.resolve(":.\\@", EntityType.ATTACHMENT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(":.@", reference.getName());
    }

    /**
     * Tests resolving object references.
     */
    @Test
    public void testResolveObjectReference()
    {
        EntityReference reference = resolver.resolve("wiki:space.page^Object", EntityType.OBJECT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.getName());

        // default values
        reference = resolver.resolve("", EntityType.OBJECT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(DEFAULT_OBJECT, reference.getName());

        // without some of the parents
        reference = resolver.resolve("space.page^Object", EntityType.OBJECT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.getName());

        reference = resolver.resolve("page^Object", EntityType.OBJECT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.getName());

        reference = resolver.resolve("XWiki.Class", EntityType.OBJECT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("XWiki.Class", reference.getName());

        // property without object
        reference = resolver.resolve("wiki:space.page.property", EntityType.OBJECT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("wiki:space.page.property", reference.getName());

        // object with no name
        reference = resolver.resolve("wiki:space.page^", EntityType.OBJECT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(DEFAULT_OBJECT, reference.getName());

        // test separator escape
        reference = resolver.resolve("wiki:space.page^obje\\^ct", EntityType.OBJECT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("obje^ct", reference.getName());
        
        // and that separators don't need to be escaped other than in the object name        
        reference = resolver.resolve("wiki:spa^ce.page^Object", EntityType.OBJECT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("spa^ce", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.getName());

        reference = resolver.resolve(":.\\^@", EntityType.OBJECT);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(":.^@", reference.getName());
    }

    /**
     * Tests resolving object references.
     */
    @Test
    public void testResolvePropertyReference()
    {
        EntityReference reference = resolver.resolve("wiki:space.page^object.prop", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("object", reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals("prop", reference.getName());

        // default values
        reference = resolver.resolve("", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(DEFAULT_OBJECT, reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals(DEFAULT_OBJECT_PROPERTY, reference.getName());

        // without some of the parents
        reference = resolver.resolve("space.page^Object.prop", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals("prop", reference.getName());

        reference = resolver.resolve("page^Object.prop", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals("prop", reference.getName());

        reference = resolver.resolve("Object.prop", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals("prop", reference.getName());

        reference = resolver.resolve("FooBar", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(DEFAULT_OBJECT, reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals("FooBar", reference.getName());

        // object without property, parsed as property
        reference = resolver.resolve("page^Object", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(DEFAULT_OBJECT, reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals("page^Object", reference.getName());

        // empty prop
        reference = resolver.resolve("wiki:space.page^Object.", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals(DEFAULT_OBJECT_PROPERTY, reference.getName());

        // test separator escape
        reference = resolver.resolve("wiki:space.page^Object.prop\\.erty", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("Object", reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals("prop.erty", reference.getName());

        // and that separators don't need to be escaped other than in the property name
        reference = resolver.resolve("wiki:space.page^x.wiki.class[0].prop", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("x.wiki.class[0]", reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals("prop", reference.getName());

        reference = resolver.resolve(":^\\.@", EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(DEFAULT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(DEFAULT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_PAGE, reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals(DEFAULT_OBJECT, reference.extractReference(EntityType.OBJECT).getName());
        Assert.assertEquals(":^.@", reference.getName());
    }

    @Test
    public void testResolveDocumentReferenceWithExplicitReference()
    {
        EntityReference reference = resolver.resolve("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)));
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());
    }
}
