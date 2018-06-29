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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.test.TestConstants;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@ComponentTest
@ComponentList({ DefaultSymbolScheme.class })
public class DefaultStringEntityReferenceSerializerTest implements TestConstants
{
    @MockComponent
    private EntityReferenceProvider provider;

    @InjectMockComponents
    private DefaultStringEntityReferenceSerializer serializer;

    @InjectMockComponents
    private DefaultStringEntityReferenceResolver resolver;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.provider.getDefaultReference(EntityType.WIKI)).thenReturn(DEFAULT_WIKI_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.SPACE)).thenReturn(DEFAULT_SPACE_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(DEFAULT_DOCUMENT_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.ATTACHMENT)).thenReturn(DEFAULT_ATTACHMENT_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.OBJECT)).thenReturn(DEFAULT_OBJECT_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.OBJECT_PROPERTY))
            .thenReturn(DEFAULT_OBJECT_PROPERTY_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.CLASS_PROPERTY)).thenReturn(DEFAULT_CLASS_PROPERTY_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.PAGE)).thenReturn(DEFAULT_PAGE_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.PAGE_ATTACHMENT))
            .thenReturn(DEFAULT_PAGE_ATTACHMENT_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.PAGE_OBJECT)).thenReturn(DEFAULT_PAGE_OBJECT_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.PAGE_OBJECT_PROPERTY))
            .thenReturn(DEFAULT_PAGE_OBJECT_PROPERTY_REFERENCE);
        when(this.provider.getDefaultReference(EntityType.PAGE_CLASS_PROPERTY))
            .thenReturn(DEFAULT_PAGE_CLASS_PROPERTY_REFERENCE);
    }

    @Test
    public void serializeWikiReferences() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki", EntityType.WIKI);
        assertEquals("wiki", serializer.serialize(reference));
    }

    @Test
    public void serializeDocumentReferences() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
        assertEquals("wiki:space.page", serializer.serialize(reference));

        reference = resolver.resolve("wiki:space.", EntityType.DOCUMENT);
        assertEquals("wiki:space.defdocument", serializer.serialize(reference));

        reference = resolver.resolve("space.", EntityType.DOCUMENT);
        assertEquals("defwiki:space.defdocument", serializer.serialize(reference));

        reference = resolver.resolve("page", EntityType.DOCUMENT);
        assertEquals("defwiki:defspace.page", serializer.serialize(reference));

        reference = resolver.resolve(".", EntityType.DOCUMENT);
        assertEquals("defwiki:defspace.defdocument", serializer.serialize(reference));

        reference = resolver.resolve(null, EntityType.DOCUMENT);
        assertEquals("defwiki:defspace.defdocument", serializer.serialize(reference));

        reference = resolver.resolve("", EntityType.DOCUMENT);
        assertEquals("defwiki:defspace.defdocument", serializer.serialize(reference));

        reference = resolver.resolve("wiki1.wiki2:wiki3:some.space.page", EntityType.DOCUMENT);
        assertEquals("wiki1.wiki2:wiki3:some.space.page", serializer.serialize(reference));

        reference = resolver.resolve("some.space.page", EntityType.DOCUMENT);
        assertEquals("defwiki:some.space.page", serializer.serialize(reference));

        reference = resolver.resolve("wiki:page", EntityType.DOCUMENT);
        assertEquals("defwiki:defspace.wiki:page", serializer.serialize(reference));

        // Verify that passing null doesn't throw a NPE
        assertNull(serializer.serialize(null));

        // Test escapes

        reference = resolver.resolve("\\.:@\\.", EntityType.DOCUMENT);
        assertEquals("defwiki:defspace.\\.:@\\.", serializer.serialize(reference));

        reference = resolver.resolve("\\\\:\\\\.\\\\", EntityType.DOCUMENT);
        assertEquals("\\\\:\\\\.\\\\", serializer.serialize(reference));

        // The escaping here is not necessary but we want to test that it works
        reference = resolver.resolve("\\wiki:\\space.\\page", EntityType.DOCUMENT);
        assertEquals("wiki:space.page", serializer.serialize(reference));
    }

    @Test
    public void serializeSpaceReferences() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space1.space2", EntityType.SPACE);
        assertEquals("wiki:space1.space2", serializer.serialize(reference));
    }

    @Test
    public void serializeAttachmentReferences() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space.page@filename", EntityType.ATTACHMENT);
        assertEquals("wiki:space.page@filename", serializer.serialize(reference));

        reference = resolver.resolve("", EntityType.ATTACHMENT);
        assertEquals("defwiki:defspace.defdocument@deffilename", serializer.serialize(reference));

        reference = resolver.resolve("wiki:space.page@my.png", EntityType.ATTACHMENT);
        assertEquals("wiki:space.page@my.png", serializer.serialize(reference));

        reference = resolver.resolve("some:file.name", EntityType.ATTACHMENT);
        assertEquals("defwiki:defspace.defdocument@some:file.name", serializer.serialize(reference));

        // Test escapes

        reference = resolver.resolve(":.\\@", EntityType.ATTACHMENT);
        assertEquals("defwiki:defspace.defdocument@:.\\@", serializer.serialize(reference));
    }

    @Test
    public void serializeReferencesWithChild()
    {
        EntityReference reference = resolver.resolve("wiki:Space.Page", EntityType.DOCUMENT);
        assertEquals("wiki:Space", serializer.serialize(reference.getParent()));

        assertEquals("wiki", serializer.serialize(reference.getParent().getParent()));
    }

    /**
     * Tests resolving and re-serializing an object reference.
     */
    @Test
    public void serializeObjectReferences()
    {
        EntityReference reference = resolver.resolve("wiki:space.page^Object", EntityType.OBJECT);
        assertEquals("wiki:space.page^Object", serializer.serialize(reference));

        // default values
        reference = resolver.resolve("", EntityType.OBJECT);
        assertEquals("defwiki:defspace.defdocument^defobject", serializer.serialize(reference));

        // property reference with no object
        reference = resolver.resolve("wiki:space.page.property", EntityType.OBJECT);
        assertEquals("defwiki:defspace.defdocument^wiki:space.page.property", serializer.serialize(reference));

        // test escaping character
        reference = resolver.resolve("wiki:space.page^Obje\\^ct", EntityType.OBJECT);
        assertEquals("wiki:space.page^Obje\\^ct", serializer.serialize(reference));

        reference = resolver.resolve("wiki:spa^ce.page^Obje\\^ct", EntityType.OBJECT);
        assertEquals("wiki:spa^ce.page^Obje\\^ct", serializer.serialize(reference));

        reference = resolver.resolve(":.\\^@", EntityType.OBJECT);
        assertEquals("defwiki:defspace.defdocument^:.\\^@", serializer.serialize(reference));
    }

    /**
     * Tests resolving and re-serializing an object reference.
     */
    @Test
    public void serializeObjectPropertyReferences()
    {
        EntityReference reference = resolver.resolve("wiki:space.page^xwiki.class[0].prop", EntityType.OBJECT_PROPERTY);
        assertEquals("wiki:space.page^xwiki.class[0].prop", serializer.serialize(reference));

        // default values
        reference = resolver.resolve("", EntityType.OBJECT_PROPERTY);
        assertEquals("defwiki:defspace.defdocument^defobject.defobjproperty", serializer.serialize(reference));

        // using separators
        reference = resolver.resolve("space^page@attachment", EntityType.OBJECT_PROPERTY);
        assertEquals("defwiki:defspace.defdocument^defobject.space^page@attachment", serializer.serialize(reference));

        reference = resolver.resolve("wiki:space^object", EntityType.OBJECT_PROPERTY);
        assertEquals("defwiki:defspace.defdocument^defobject.wiki:space^object", serializer.serialize(reference));

        // test escaping character
        reference = resolver.resolve("wiki:space.page^xwiki.class[0].prop\\.erty", EntityType.OBJECT_PROPERTY);
        assertEquals("wiki:space.page^xwiki.class[0].prop\\.erty", serializer.serialize(reference));

        reference = resolver.resolve(":\\.^@", EntityType.OBJECT_PROPERTY);
        assertEquals("defwiki:defspace.defdocument^defobject.:\\.^@", serializer.serialize(reference));
    }

    /**
     * Tests resolving and re-serializing an object reference.
     */
    @Test
    public void serializeClassPropertyReferences()
    {
        EntityReference reference = resolver.resolve("wiki:space.page^ClassProperty", EntityType.CLASS_PROPERTY);
        assertEquals("wiki:space.page^ClassProperty", serializer.serialize(reference));

        // default values
        reference = resolver.resolve("", EntityType.CLASS_PROPERTY);
        assertEquals("defwiki:defspace.defdocument^defclassproperty", serializer.serialize(reference));

        // property reference with no object
        reference = resolver.resolve("wiki:space.page.property", EntityType.CLASS_PROPERTY);
        assertEquals("defwiki:defspace.defdocument^wiki:space.page.property", serializer.serialize(reference));

        // test escaping character
        reference = resolver.resolve("wiki:space.page^Obje\\^ct", EntityType.CLASS_PROPERTY);
        assertEquals("wiki:space.page^Obje\\^ct", serializer.serialize(reference));

        reference = resolver.resolve("wiki:spa^ce.page^Obje\\^ct", EntityType.CLASS_PROPERTY);
        assertEquals("wiki:spa^ce.page^Obje\\^ct", serializer.serialize(reference));

        reference = resolver.resolve(":.\\^@", EntityType.CLASS_PROPERTY);
        assertEquals("defwiki:defspace.defdocument^:.\\^@", serializer.serialize(reference));
    }

    @Test
    public void serializeRelativeReferences()
    {
        EntityReference reference = new EntityReference("page", EntityType.DOCUMENT);
        assertEquals("page", serializer.serialize(reference));

        reference = new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE));
        assertEquals("space.page", serializer.serialize(reference));

        // Ensure that the page part is not displayed in the serialized result
        assertEquals("space", serializer.serialize(reference.extractReference(EntityType.SPACE)));
    }

    @Test
    public void serializePageReferences() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space/page", EntityType.PAGE);
        assertEquals("wiki:space/page", serializer.serialize(reference));

        reference = resolver.resolve("wiki:space/", EntityType.PAGE);
        assertEquals("wiki:space/defpage", serializer.serialize(reference));

        reference = resolver.resolve("space/", EntityType.PAGE);
        assertEquals("defwiki:space/defpage", serializer.serialize(reference));

        reference = resolver.resolve("page", EntityType.PAGE);
        assertEquals("defwiki:page", serializer.serialize(reference));

        reference = resolver.resolve("/", EntityType.PAGE);
        assertEquals("defwiki:defpage/defpage", serializer.serialize(reference));

        reference = resolver.resolve(null, EntityType.PAGE);
        assertEquals("defwiki:defpage", serializer.serialize(reference));

        reference = resolver.resolve("", EntityType.PAGE);
        assertEquals("defwiki:defpage", serializer.serialize(reference));

        reference = resolver.resolve("wiki1/wiki2:wiki3:some/space/page", EntityType.PAGE);
        assertEquals("wiki1/wiki2:wiki3:some/space/page", serializer.serialize(reference));

        reference = resolver.resolve("some/space/page", EntityType.PAGE);
        assertEquals("defwiki:some/space/page", serializer.serialize(reference));

        // Verify that passing null doesn't throw a NPE
        assertNull(serializer.serialize(null));

        // Test escapes

        reference = resolver.resolve("\\\\:\\\\/\\\\", EntityType.PAGE);
        assertEquals("\\\\:\\\\/\\\\", serializer.serialize(reference));

        // The escaping here is not necessary but we want to test that it works
        reference = resolver.resolve("\\wiki:\\space/\\page", EntityType.PAGE);
        assertEquals("wiki:space/page", serializer.serialize(reference));
    }

    @Test
    public void serializePageReferencesParameters() throws Exception
    {
        EntityReference reference =
            resolver.resolve("wiki:page1;param1=value1;param2=value2/page2;en_US", EntityType.PAGE);
        assertEquals("wiki:page1;param1=value1;param2=value2/page2;en_US", serializer.serialize(reference));

        reference = resolver.resolve("wiki:page1;pa\\;ram1=val\\;ue1;param2=value2/page2;en_\\;US", EntityType.PAGE);
        assertEquals("wiki:page1;pa\\;ram1=val\\;ue1;param2=value2/page2;en_\\;US", serializer.serialize(reference));
    }
}
