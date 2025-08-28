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

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @version $Id$
 */
@ComponentTest
@ComponentList(WithParametersSymbolScheme.class)
class WithParametersTest
{
    @InjectMockComponents
    private WithParametersStringEntityReferenceResolver resolver;

    @InjectMockComponents
    private WithParametersStringEntityReferenceSerializer serializer;

    @Test
    void serializeResolvePage()
    {
        PageReference pageReference =
            new PageReference("page1", new PageReference("page2", new WikiReference("wiki")), Locale.ENGLISH);

        String serialized = this.serializer.serialize(pageReference);
        EntityReference resolved = this.resolver.resolve(serialized, EntityType.PAGE);

        assertEquals(pageReference, resolved);
    }

    @Test
    void serializeResolveDocument()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page", Locale.ENGLISH);

        String serialized = this.serializer.serialize(documentReference);
        EntityReference resolved = this.resolver.resolve(serialized, EntityType.DOCUMENT);

        assertEquals(documentReference, resolved);

        EntityReference reference = resolver.resolve("wiki:space;param1=value2.page", EntityType.DOCUMENT);
        assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
        assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        assertEquals("page", reference.getName());
        assertEquals(Map.of("param1", "value2"), reference.extractReference(EntityType.SPACE).getParameters());
    }
}
