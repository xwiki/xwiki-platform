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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for {@link CurrentMixedStringDocumentReferenceResolver}.
 * 
 * @version $Id$
 */
public class CurrentMixedStringDocumentReferenceResolverTest extends AbstractBridgedComponentTestCase
{
    private static final String CURRENT_SPACE = "currentspace";

    private EntityReferenceResolver<String> resolver;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.resolver = getComponentManager().getInstance(EntityReferenceResolver.TYPE_STRING, "currentmixed");
    }

    @Test
    public void testResolveDocumentReferenceWhenContextDocument() throws Exception
    {
        getContext().setDoc(new XWikiDocument(new DocumentReference("not used", CURRENT_SPACE, "notused")));

        getContext().setDatabase("currentwiki");

        EntityReference reference = this.resolver.resolve("", EntityType.DOCUMENT);
        Assert.assertEquals("currentwiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(CURRENT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("WebHome", reference.getName());
    }

    @Test
    public void testResolveDocumentReferenceForDefaultWikiWhenNoContextDocument() throws Exception
    {
        getContext().setDatabase("currentwiki");

        EntityReference reference = this.resolver.resolve("space.page", EntityType.DOCUMENT);

        // Make sure the resolved wiki is the current wiki and not the wiki from the current document (since that
        // doc isn't set).
        Assert.assertEquals("currentwiki", reference.extractReference(EntityType.WIKI).getName());

        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());
    }
}
