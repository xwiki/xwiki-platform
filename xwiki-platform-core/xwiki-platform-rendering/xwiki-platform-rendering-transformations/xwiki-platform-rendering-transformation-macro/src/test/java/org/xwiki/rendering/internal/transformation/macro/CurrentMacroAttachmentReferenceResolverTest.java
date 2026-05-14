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
package org.xwiki.rendering.internal.transformation.macro;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CurrentMacroAttachmentReferenceResolver}.
 *
 * @version $Id$
 * @since 5.0M1
 */
@ComponentTest
class CurrentMacroAttachmentReferenceResolverTest
{
    @InjectMockComponents
    private CurrentMacroAttachmentReferenceResolver resolver;

    @MockComponent
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @Test
    void resolve()
    {
        EntityReference result = new AttachmentReference("file", new DocumentReference("wiki", "Space", "Page"));
        when(this.macroEntityReferenceResolver.resolve("reference", EntityType.ATTACHMENT, "parameter"))
            .thenReturn(result);
        assertEquals(result, this.resolver.resolve("reference", "parameter"));
    }
}
