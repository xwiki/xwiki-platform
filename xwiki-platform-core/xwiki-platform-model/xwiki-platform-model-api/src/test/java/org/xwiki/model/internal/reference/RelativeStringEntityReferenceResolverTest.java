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

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link RelativeStringEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2.3
 */
@ComponentTest
@ComponentList({
    DefaultSymbolScheme.class
})
class RelativeStringEntityReferenceResolverTest
{
    @InjectMockComponents
    private RelativeStringEntityReferenceResolver resolver;

    @Test
    void resolveDocumentReference()
    {
        EntityReference reference = this.resolver.resolve("", EntityType.DOCUMENT);
        assertNull(reference);

        reference = this.resolver.resolve("space.page", EntityType.DOCUMENT);
        assertNull(reference.extractReference(EntityType.WIKI));
        EntityReference spaceReference = reference.extractReference(EntityType.SPACE);
        assertEquals("space", spaceReference.getName());
        assertEquals(EntityType.SPACE, spaceReference.getParentType());
        assertEquals("page", reference.getName());

        reference = this.resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
        assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        assertEquals("page", reference.getName());
    }

    @Test
    void resolveDocumentReferenceWithBaseReference()
    {
        EntityReference reference =
            this.resolver.resolve("", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE));

        assertNull(reference.extractReference(EntityType.WIKI));
        assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        assertNull(reference.extractReference(EntityType.DOCUMENT));
    }
}
