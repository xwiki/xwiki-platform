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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CurrentReferenceEntityReferenceResolver}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(DefaultEntityReferenceProvider.class)
public class CurrentReferenceEntityReferenceResolverTest
{
    private static final String CURRENT_WIKI = "currentwiki";

    private static final String CURRENT_SPACE = "currentspace";

    private static final String CURRENT_DOCUMENT = "currentdocument";

    private static final String CURRENT_PAGE = "currentpage";

    private static final String DEFAULT_WIKI = "defwiki";

    private static final String DEFAULT_SPACE = "defspace";

    private static final String DEFAULT_DOCUMENT = "defdocument";

    private static final String DEFAULT_PAGE = "defpage";

    private static final String DEFAULT_ATTACHMENT = "deffilename";

    private static final String DEFAULT_OBJECT = "defobject";

    private static final String DEFAULT_OBJECT_PROPERTY = "defobjproperty";

    private static final String DEFAULT_CLASS_PROPERTY = "defclassproperty";

    private static final String DEFAULT_PAGE_ATTACHMENT = "defpagefilename";

    private static final String DEFAULT_PAGE_OBJECT = "defpageobject";

    private static final String DEFAULT_PAGE_OBJECT_PROPERTY = "defpageobjproperty";

    private static final String DEFAULT_PAGE_CLASS_PROPERTY = "defpageclassproperty";

    private static final EntityReference CURRENT_WIKI_REFERENCE = new EntityReference(CURRENT_WIKI, EntityType.WIKI);

    private static final EntityReference CURRENT_SPACE_REFERENCE = new EntityReference(CURRENT_SPACE, EntityType.SPACE);

    private static final EntityReference CURRENT_DOCUMENT_REFERENCE =
        new EntityReference(CURRENT_DOCUMENT, EntityType.DOCUMENT);

    private static final EntityReference CURRENT_PAGE_REFERENCE = new EntityReference(CURRENT_PAGE, EntityType.PAGE);

    @MockComponent
    @Named("current")
    private EntityReferenceProvider currentProvider;

    @MockComponent
    private ModelConfiguration configuration;

    @InjectMockComponents
    private CurrentReferenceEntityReferenceResolver resolver;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.currentProvider.getDefaultReference(EntityType.WIKI)).thenReturn(CURRENT_WIKI_REFERENCE);
        when(this.currentProvider.getDefaultReference(EntityType.SPACE)).thenReturn(CURRENT_SPACE_REFERENCE);
        when(this.currentProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(CURRENT_DOCUMENT_REFERENCE);
        when(this.currentProvider.getDefaultReference(EntityType.PAGE)).thenReturn(CURRENT_PAGE_REFERENCE);

        when(this.configuration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn(DEFAULT_WIKI);
        when(this.configuration.getDefaultReferenceValue(EntityType.SPACE)).thenReturn(DEFAULT_SPACE);
        when(this.configuration.getDefaultReferenceValue(EntityType.DOCUMENT)).thenReturn(DEFAULT_DOCUMENT);
        when(this.configuration.getDefaultReferenceValue(EntityType.OBJECT)).thenReturn(DEFAULT_OBJECT);
        when(this.configuration.getDefaultReferenceValue(EntityType.OBJECT_PROPERTY))
            .thenReturn(DEFAULT_OBJECT_PROPERTY);
        when(this.configuration.getDefaultReferenceValue(EntityType.CLASS_PROPERTY)).thenReturn(DEFAULT_CLASS_PROPERTY);
        when(this.configuration.getDefaultReferenceValue(EntityType.ATTACHMENT)).thenReturn(DEFAULT_ATTACHMENT);
        when(this.configuration.getDefaultReferenceValue(EntityType.PAGE)).thenReturn(DEFAULT_PAGE);
        when(this.configuration.getDefaultReferenceValue(EntityType.PAGE_OBJECT)).thenReturn(DEFAULT_PAGE_OBJECT);
        when(this.configuration.getDefaultReferenceValue(EntityType.PAGE_OBJECT_PROPERTY))
            .thenReturn(DEFAULT_PAGE_OBJECT_PROPERTY);
        when(this.configuration.getDefaultReferenceValue(EntityType.PAGE_ATTACHMENT))
            .thenReturn(DEFAULT_PAGE_ATTACHMENT);
        when(this.configuration.getDefaultReferenceValue(EntityType.PAGE_CLASS_PROPERTY))
            .thenReturn(DEFAULT_PAGE_CLASS_PROPERTY);
    }

    @Test
    public void testResolveAttachmentReferenceWhenMissingParents()
    {
        EntityReference reference =
            resolver.resolve(new EntityReference("filename", EntityType.ATTACHMENT), EntityType.ATTACHMENT);

        assertEquals(CURRENT_DOCUMENT, reference.getParent().getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        assertEquals(CURRENT_SPACE, reference.getParent().getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        assertEquals(CURRENT_WIKI, reference.getParent().getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void resolvePageReferenceWhenTypeIsDocument()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("document", EntityType.DOCUMENT), EntityType.PAGE);

        assertEquals(new PageReference(CURRENT_WIKI, CURRENT_SPACE, "document"), reference);

        reference = this.resolver.resolve(new EntityReference(DEFAULT_DOCUMENT, EntityType.DOCUMENT), EntityType.PAGE);

        assertEquals(new PageReference(CURRENT_WIKI, CURRENT_SPACE), reference);
    }

    @Test
    public void resolvePageReferenceWhenTypeIsSpace()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("space", EntityType.SPACE), EntityType.PAGE);

        assertEquals(new PageReference(CURRENT_WIKI, "space"), reference);
    }

    @Test
    public void resolveDocumentReferenceWhenTypeIsPage()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("page", EntityType.PAGE), EntityType.DOCUMENT);

        assertEquals(new DocumentReference(CURRENT_WIKI, "page", DEFAULT_DOCUMENT), reference);

        reference = this.resolver.resolve(
            new EntityReference("page1", EntityType.PAGE, new EntityReference("page2", EntityType.PAGE)),
            EntityType.DOCUMENT);

        assertEquals(new DocumentReference(CURRENT_WIKI, Arrays.asList("page2", "page1"), DEFAULT_DOCUMENT), reference);
    }

    @Test
    public void resolveSpaceReferenceWhenTypeIsPage()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("page", EntityType.PAGE), EntityType.SPACE);

        assertEquals(new SpaceReference(CURRENT_WIKI, Arrays.asList("page")), reference);

        reference = this.resolver.resolve(
            new EntityReference("page1", EntityType.PAGE, new EntityReference("page2", EntityType.PAGE)),
            EntityType.SPACE);

        assertEquals(new SpaceReference(CURRENT_WIKI, Arrays.asList("page2", "page1")), reference);
    }
}
