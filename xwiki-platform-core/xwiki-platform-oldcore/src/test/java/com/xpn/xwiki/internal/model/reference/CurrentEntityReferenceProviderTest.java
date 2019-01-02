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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CurrentEntityReferenceProvider}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ComponentList(DefaultEntityReferenceProvider.class)
public class CurrentEntityReferenceProviderTest
{
    private static final String CONTEXT_WIKI = "contextwiki";

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

    private static final EntityReference DEFAULT_WIKI_REFERENCE = new EntityReference(DEFAULT_WIKI, EntityType.WIKI);

    private static final EntityReference DEFAULT_SPACE_REFERENCE = new EntityReference(DEFAULT_SPACE, EntityType.SPACE);

    private static final EntityReference DEFAULT_DOCUMENT_REFERENCE =
        new EntityReference(DEFAULT_DOCUMENT, EntityType.DOCUMENT);

    private static final EntityReference DEFAULT_ATTACHMENT_REFERENCE =
        new EntityReference(DEFAULT_ATTACHMENT, EntityType.ATTACHMENT);

    private static final EntityReference DEFAULT_OBJECT_REFERENCE =
        new EntityReference(DEFAULT_OBJECT, EntityType.OBJECT);

    private static final EntityReference DEFAULT_OBJECT_PROPERTY_REFERENCE =
        new EntityReference(DEFAULT_OBJECT_PROPERTY, EntityType.OBJECT_PROPERTY);

    private static final EntityReference DEFAULT_CLASS_PROPERTY_REFERENCE =
        new EntityReference(DEFAULT_CLASS_PROPERTY, EntityType.CLASS_PROPERTY);

    private static final EntityReference DEFAULT_PAGE_REFERENCE = new EntityReference(DEFAULT_PAGE, EntityType.PAGE);

    private static final EntityReference DEFAULT_PAGE_ATTACHMENT_REFERENCE =
        new EntityReference(DEFAULT_PAGE_ATTACHMENT, EntityType.PAGE_ATTACHMENT);

    private static final EntityReference DEFAULT_PAGE_OBJECT_REFERENCE =
        new EntityReference(DEFAULT_PAGE_OBJECT, EntityType.PAGE_OBJECT);

    private static final EntityReference DEFAULT_PAGE_OBJECT_PROPERTY_REFERENCE =
        new EntityReference(DEFAULT_PAGE_OBJECT_PROPERTY, EntityType.PAGE_OBJECT_PROPERTY);

    private static final EntityReference DEFAULT_PAGE_CLASS_PROPERTY_REFERENCE =
        new EntityReference(DEFAULT_PAGE_CLASS_PROPERTY, EntityType.PAGE_CLASS_PROPERTY);

    @MockComponent
    private ModelConfiguration configuration;

    @InjectMockitoOldcore
    private MockitoOldcore mockitoOldcore;

    @InjectMockComponents
    private CurrentEntityReferenceProvider provider;

    @BeforeEach
    public void beforeEach()
    {
        this.mockitoOldcore.getXWikiContext().setWikiId(null);

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
    public void getDefaultReferenceWithoutContextDocument()
    {
        assertEquals(DEFAULT_WIKI_REFERENCE, this.provider.getDefaultReference(EntityType.WIKI));

        assertEquals(DEFAULT_SPACE_REFERENCE, this.provider.getDefaultReference(EntityType.SPACE));
        assertEquals(DEFAULT_DOCUMENT_REFERENCE, this.provider.getDefaultReference(EntityType.DOCUMENT));
        assertEquals(DEFAULT_ATTACHMENT_REFERENCE, this.provider.getDefaultReference(EntityType.ATTACHMENT));
        assertEquals(DEFAULT_OBJECT_REFERENCE, this.provider.getDefaultReference(EntityType.OBJECT));
        assertEquals(DEFAULT_OBJECT_PROPERTY_REFERENCE, this.provider.getDefaultReference(EntityType.OBJECT_PROPERTY));
        assertEquals(DEFAULT_CLASS_PROPERTY_REFERENCE, this.provider.getDefaultReference(EntityType.CLASS_PROPERTY));
        assertEquals(DEFAULT_PAGE_REFERENCE, this.provider.getDefaultReference(EntityType.PAGE));
        assertEquals(DEFAULT_PAGE_ATTACHMENT_REFERENCE, this.provider.getDefaultReference(EntityType.PAGE_ATTACHMENT));
        assertEquals(DEFAULT_PAGE_OBJECT_REFERENCE, this.provider.getDefaultReference(EntityType.PAGE_OBJECT));
        assertEquals(DEFAULT_PAGE_OBJECT_PROPERTY_REFERENCE,
            this.provider.getDefaultReference(EntityType.PAGE_OBJECT_PROPERTY));
        assertEquals(DEFAULT_PAGE_CLASS_PROPERTY_REFERENCE,
            this.provider.getDefaultReference(EntityType.PAGE_CLASS_PROPERTY));
    }

    @Test
    public void getDefaultReferenceWithContextDocument() throws IllegalAccessException
    {
        this.mockitoOldcore.getXWikiContext().setWikiId(CONTEXT_WIKI);

        XWikiDocument document =
            new XWikiDocument(new DocumentReference("docwiki", Arrays.asList("docspace1", "docspace2"), "docname"));
        FieldUtils.writeDeclaredField(document, "pageReferenceCache",
            new PageReference("docwiki", "docspace1", "docspace2", "docname"), true);
        this.mockitoOldcore.getXWikiContext().setDoc(document);

        assertEquals(new EntityReference(CONTEXT_WIKI, EntityType.WIKI),
            this.provider.getDefaultReference(EntityType.WIKI));

        assertEquals(
            new EntityReference("docspace2", EntityType.SPACE, new EntityReference("docspace1", EntityType.SPACE)),
            this.provider.getDefaultReference(EntityType.SPACE));
        assertEquals(new EntityReference("docname", EntityType.DOCUMENT),
            this.provider.getDefaultReference(EntityType.DOCUMENT));
        assertEquals(
            new EntityReference("docname", EntityType.PAGE,
                new EntityReference("docspace2", EntityType.PAGE, new EntityReference("docspace1", EntityType.PAGE))),
            this.provider.getDefaultReference(EntityType.PAGE));

        assertEquals(DEFAULT_ATTACHMENT_REFERENCE, this.provider.getDefaultReference(EntityType.ATTACHMENT));
        assertEquals(DEFAULT_OBJECT_REFERENCE, this.provider.getDefaultReference(EntityType.OBJECT));
        assertEquals(DEFAULT_OBJECT_PROPERTY_REFERENCE, this.provider.getDefaultReference(EntityType.OBJECT_PROPERTY));
        assertEquals(DEFAULT_CLASS_PROPERTY_REFERENCE, this.provider.getDefaultReference(EntityType.CLASS_PROPERTY));
        assertEquals(DEFAULT_PAGE_ATTACHMENT_REFERENCE, this.provider.getDefaultReference(EntityType.PAGE_ATTACHMENT));
        assertEquals(DEFAULT_PAGE_OBJECT_REFERENCE, this.provider.getDefaultReference(EntityType.PAGE_OBJECT));
        assertEquals(DEFAULT_PAGE_OBJECT_PROPERTY_REFERENCE,
            this.provider.getDefaultReference(EntityType.PAGE_OBJECT_PROPERTY));
        assertEquals(DEFAULT_PAGE_CLASS_PROPERTY_REFERENCE,
            this.provider.getDefaultReference(EntityType.PAGE_CLASS_PROPERTY));
    }
}
