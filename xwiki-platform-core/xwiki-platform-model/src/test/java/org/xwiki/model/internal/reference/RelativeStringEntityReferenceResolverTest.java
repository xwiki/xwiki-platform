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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link RelativeStringEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2.3
 */
@ComponentList({
    DefaultSymbolScheme.class
})
public class RelativeStringEntityReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<RelativeStringEntityReferenceResolver> mocker =
        new MockitoComponentMockingRule<>(RelativeStringEntityReferenceResolver.class);

    private EntityReferenceResolver<String> resolver;

    @Before
    public void setUp() throws Exception
    {
        this.resolver = this.mocker.getComponentUnderTest();
    }

    @Test
    public void resolveDocumentReference() throws Exception
    {
        EntityReference reference = this.resolver.resolve("", EntityType.DOCUMENT);
        Assert.assertNull(reference);

        reference = this.resolver.resolve("space.page", EntityType.DOCUMENT);
        Assert.assertNull(reference.extractReference(EntityType.WIKI));
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());

        reference = this.resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
        Assert.assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());
    }

    @Test
    public void resolveDocumentReferenceWithBaseReference() throws Exception
    {
        EntityReference reference =
            this.resolver.resolve("", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE));

        Assert.assertNull(reference.extractReference(EntityType.WIKI));
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertNull(reference.extractReference(EntityType.DOCUMENT));
    }
}
