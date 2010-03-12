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

import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import org.junit.*;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Unit tests for {@link XClassRelativeStringEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2.3
 */
public class XClassRelativeStringEntityReferenceResolverTest extends AbstractBridgedXWikiComponentTestCase
{
    private EntityReferenceResolver<String> resolver;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.resolver = getComponentManager().lookup(EntityReferenceResolver.class, "xclass");
    }

    @Test
    public void testResolve()
    {
        EntityReference reference = this.resolver.resolve("page", EntityType.DOCUMENT);
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("XWiki", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertNull(reference.extractReference(EntityType.WIKI));
    }

    @Test
    public void testResolveWhenExplicitParameterAndNoPageInStringRepresentation()
    {
        EntityReference reference = this.resolver.resolve("", EntityType.DOCUMENT,
            new DocumentReference("dummy", "dummy", "page"));
        Assert.assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("XWiki", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertNull(reference.extractReference(EntityType.WIKI));
    }

    @Test
    public void testResolveWhenNoPageReferenceSpecified()
    {
        try {
            this.resolver.resolve("", EntityType.DOCUMENT);
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("A Reference to a page must be passed as a parameter when the string to resolve "
                + "doesn't specify a page", expected.getMessage());
        }
    }
}
