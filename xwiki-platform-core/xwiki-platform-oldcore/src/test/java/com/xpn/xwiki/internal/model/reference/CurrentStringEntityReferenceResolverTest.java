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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.ExplicitReferenceEntityReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitReferencePageReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link CurrentStringEntityReferenceResolver}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ComponentList(value = { CurrentEntityReferenceProvider.class, CurrentStringEntityReferenceResolver.class,
DefaultModelConfiguration.class, DefaultSymbolScheme.class, ExplicitReferencePageReferenceResolver.class,
ExplicitReferenceEntityReferenceResolver.class })
public class CurrentStringEntityReferenceResolverTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private static final String CURRENT_WIKI = "currentwiki";

    private static final String CURRENT_SPACE = "currentspace";

    private static final String CURRENT_DOCUMENT = "currentdocument";

    private static final String CURRENT_PAGE = "currentpage";

    private static final String CURRENTDOC_SPACE = "currentdocspace";

    private static final String CURRENTDOC_DOCUMENT = "currentdocdocument";

    @InjectMockComponents
    private CurrentStringEntityReferenceResolver resolver;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.oldcore.getXWikiContext().setWikiId(CURRENT_WIKI);
    }

    @Test
    public void testResolveDocumentReferenceWhenNoContextWiki() throws Exception
    {
        this.oldcore.getXWikiContext().setWikiId(null);

        EntityReference reference = resolver.resolve("", EntityType.DOCUMENT);

        assertEquals("xwiki", reference.extractReference(EntityType.WIKI).getName());
        assertEquals("Main", reference.extractReference(EntityType.SPACE).getName());
        assertEquals("WebHome", reference.getName());
    }

    @Test
    public void testResolveDocumentReferenceWhenNoContextDocument() throws Exception
    {
        this.oldcore.getXWikiContext().setWikiId(null);
        this.oldcore.getXWikiContext().setDoc(null);

        EntityReference reference = resolver.resolve("", EntityType.DOCUMENT);

        assertEquals("xwiki", reference.extractReference(EntityType.WIKI).getName());
        assertEquals("Main", reference.extractReference(EntityType.SPACE).getName());
        assertEquals("WebHome", reference.getName());
    }

    @Test
    public void testResolveDocumentReferenceWhenContextDocument() throws Exception
    {
        this.oldcore.getXWikiContext()
            .setDoc(new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENTDOC_SPACE, CURRENTDOC_DOCUMENT)));

        EntityReference reference = resolver.resolve("", EntityType.DOCUMENT);

        assertEquals(CURRENT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        assertEquals(CURRENTDOC_SPACE, reference.extractReference(EntityType.SPACE).getName());
        assertEquals(CURRENTDOC_DOCUMENT, reference.getName());
    }

    @Test
    public void testResolveAttachmentReference() throws Exception
    {
        this.oldcore.getXWikiContext()
            .setDoc(new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENTDOC_SPACE, CURRENTDOC_DOCUMENT)));

        EntityReference reference = resolver.resolve("", EntityType.ATTACHMENT);

        assertEquals(CURRENT_WIKI, reference.extractReference(EntityType.WIKI).getName());
        assertEquals(CURRENTDOC_SPACE, reference.extractReference(EntityType.SPACE).getName());
        assertEquals(CURRENTDOC_DOCUMENT, reference.extractReference(EntityType.DOCUMENT).getName());
        assertEquals("filename", reference.getName());
    }

    @Test
    public void testResolveAttachmentReferenceWhenMissingParentsAndNoContextDocument()
    {
        EntityReference reference = resolver.resolve("filename", EntityType.ATTACHMENT);

        assertEquals("WebHome", reference.getParent().getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        assertEquals("Main", reference.getParent().getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        assertEquals(CURRENT_WIKI, reference.getParent().getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void testResolveAttachmentReferenceWhenMissingParentsAndContextDocument()
    {
        this.oldcore.getXWikiContext()
            .setDoc(new XWikiDocument(new DocumentReference("docwiki", CURRENT_SPACE, CURRENT_DOCUMENT)));

        EntityReference reference = resolver.resolve("filename", EntityType.ATTACHMENT);

        assertEquals(CURRENT_DOCUMENT, reference.getParent().getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        assertEquals(CURRENT_SPACE, reference.getParent().getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        assertEquals(CURRENT_WIKI, reference.getParent().getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void testResolvePageReferenceKeywords() throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("docwiki", CURRENT_SPACE, CURRENT_PAGE));
        FieldUtils.writeDeclaredField(document, "pageReferenceCache",
            new PageReference("docwiki", CURRENT_SPACE, CURRENT_PAGE), true);
        this.oldcore.getXWikiContext().setDoc(document);

        EntityReference reference = this.resolver.resolve(".", EntityType.PAGE);
        assertEquals(new PageReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE), reference);

        reference = this.resolver.resolve("page/.", EntityType.PAGE);
        assertEquals(new PageReference(CURRENT_WIKI, "page"), reference);

        reference = this.resolver.resolve("page/./.", EntityType.PAGE);
        assertEquals(new PageReference(CURRENT_WIKI, "page"), reference);

        reference = this.resolver.resolve("..", EntityType.PAGE);
        assertEquals(new PageReference(CURRENT_WIKI, CURRENT_SPACE), reference);

        reference = this.resolver.resolve("page/..", EntityType.PAGE);
        assertEquals(new WikiReference(CURRENT_WIKI), reference);

        reference = this.resolver.resolve("page1/page2/..", EntityType.PAGE);
        assertEquals(new PageReference(CURRENT_WIKI, "page1"), reference);
    }
}
