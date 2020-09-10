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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link org.xwiki.model.internal.reference.ExplicitReferenceDocumentReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2.3
 */
@ComponentTest
// @formatter:off
@ComponentList({
    DefaultSymbolScheme.class
})
// @formatter:on
class ExplicitReferenceDocumentReferenceResolverTest
{
    @InjectMockComponents
    private DefaultStringEntityReferenceResolver resolver;

    private DocumentReferenceResolver<EntityReference> documentResolver;

    @BeforeEach
    void setUp()
    {
        this.documentResolver = new ExplicitReferenceDocumentReferenceResolver();
        ReflectionUtils.setFieldValue(this.documentResolver, "entityReferenceResolver", this.resolver);
    }

    @Test
    void resolveWithExplicitDocumentReference()
    {
        DocumentReference reference =
            this.documentResolver.resolve(null, new DocumentReference("wiki", "space", "page"));

        assertEquals("page", reference.getName());
        assertEquals("space", reference.getLastSpaceReference().getName());
        assertEquals("wiki", reference.getWikiReference().getName());
    }
}
