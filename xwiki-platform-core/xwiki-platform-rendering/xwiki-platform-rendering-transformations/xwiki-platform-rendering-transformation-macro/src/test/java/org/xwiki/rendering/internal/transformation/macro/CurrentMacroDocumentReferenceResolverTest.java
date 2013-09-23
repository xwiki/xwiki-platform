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

import static org.mockito.Mockito.*;
import org.junit.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link CurrentMacroDocumentReferenceResolver}.
 * 
 * @version $Id$
 * @since 5.0M1
 */
public class CurrentMacroDocumentReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentReferenceResolver<String>> mocker =
        new MockitoComponentMockingRule<DocumentReferenceResolver<String>>(CurrentMacroDocumentReferenceResolver.class);

    @Test
    public void resolve() throws Exception
    {
        EntityReference result = new DocumentReference("wiki", "Space", "Page");
        EntityReferenceResolver<String> macroEntityReferenceResolver =
            mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "macro");
        when(macroEntityReferenceResolver.resolve("reference", EntityType.DOCUMENT, "parameter")).thenReturn(result);
        Assert.assertEquals(result, mocker.getComponentUnderTest().resolve("reference", "parameter"));
    }
}
