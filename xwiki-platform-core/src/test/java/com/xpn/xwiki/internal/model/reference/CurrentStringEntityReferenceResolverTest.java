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
import org.junit.Before;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link CurrentMixedStringDocumentReferenceResolver}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class CurrentStringEntityReferenceResolverTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String CURRENT_WIKI = "currentwiki";

    private static final String CURRENT_SPACE = "currentspace";

    private static final String CURRENT_PAGE = "currentpage";

    private EntityReferenceResolver<String> resolver;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.resolver = getComponentManager().lookup(EntityReferenceResolver.class, "current");
    }

    @org.junit.Test
    public void testResolveAttachmentReferenceWhenMissingParentsAndNoContextDocument()
    {
        EntityReference reference = resolver.resolve("filename", EntityType.ATTACHMENT);

        Assert.assertEquals("WebHome", reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals("Main", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals("xwiki", reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @org.junit.Test
    public void testResolveAttachmentReferenceWhenMissingParentsAndContextDocument()
    {
        getContext().setDatabase(CURRENT_WIKI);
        getContext().setDoc(new XWikiDocument(new DocumentReference("docwiki", CURRENT_SPACE, CURRENT_PAGE)));

        EntityReference reference = resolver.resolve("filename", EntityType.ATTACHMENT);

        Assert.assertEquals(CURRENT_PAGE, reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals(CURRENT_SPACE, reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals(CURRENT_WIKI, reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }
}
