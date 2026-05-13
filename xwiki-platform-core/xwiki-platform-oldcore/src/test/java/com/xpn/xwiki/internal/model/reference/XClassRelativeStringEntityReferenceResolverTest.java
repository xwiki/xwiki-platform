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

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link XClassRelativeStringEntityReferenceResolver}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    DefaultSymbolScheme.class
})
class XClassRelativeStringEntityReferenceResolverTest
{
    @InjectMockComponents
    private XClassRelativeStringEntityReferenceResolver resolver;

    @Test
    void resolve()
    {
        EntityReference reference = this.resolver.resolve("page", EntityType.DOCUMENT);
        assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        assertEquals("XWiki", reference.extractReference(EntityType.SPACE).getName());
        assertNull(reference.extractReference(EntityType.WIKI));
    }

    @Test
    void resolveWhenExplicitParameterAndNoPageInStringRepresentation()
    {
        EntityReference reference =
            this.resolver.resolve("", EntityType.DOCUMENT, new DocumentReference("dummy", "dummy", "page"));
        assertEquals("page", reference.extractReference(EntityType.DOCUMENT).getName());
        assertEquals("XWiki", reference.extractReference(EntityType.SPACE).getName());
        assertNull(reference.extractReference(EntityType.WIKI));
    }

    @Test
    void resolveWhenNoPageReferenceSpecified()
    {
        Exception expected = assertThrows(IllegalArgumentException.class,
            () -> this.resolver.resolve("", EntityType.DOCUMENT));
        assertEquals("A Reference to a page must be passed as a parameter when the string to resolve "
            + "doesn't specify a page", expected.getMessage());
    }
}
