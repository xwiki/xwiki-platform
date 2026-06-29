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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link CurrentMixedStringDocumentReferenceResolver}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class CurrentMixedStringDocumentReferenceResolverTest
{
    private static final String CURRENT_SPACE = "currentspace";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private EntityReferenceResolver<String> resolver;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.resolver = this.oldcore.getMocker().getInstance(EntityReferenceResolver.TYPE_STRING, "currentmixed");
    }

    @Test
    void resolveDocumentReferenceWhenContextDocument()
    {
        this.oldcore.getXWikiContext()
            .setDoc(new XWikiDocument(new DocumentReference("not used", CURRENT_SPACE, "notused")));
        this.oldcore.getXWikiContext().setWikiId("currentwiki");

        EntityReference reference = this.resolver.resolve("", EntityType.DOCUMENT);
        assertEquals("currentwiki", reference.extractReference(EntityType.WIKI).getName());
        assertEquals(CURRENT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        assertEquals("WebHome", reference.getName());
    }

    @Test
    void resolveDocumentReferenceForDefaultWikiWhenNoContextDocument()
    {
        this.oldcore.getXWikiContext().setWikiId("currentwiki");

        EntityReference reference = this.resolver.resolve("space.page", EntityType.DOCUMENT);

        // Make sure the resolved wiki is the current wiki and not the wiki from the current document (since that
        // doc isn't set).
        assertEquals("currentwiki", reference.extractReference(EntityType.WIKI).getName());

        assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        assertEquals("page", reference.getName());
    }
}
