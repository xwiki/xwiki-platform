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
package org.xwiki.rendering.internal.macro.include;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultReferenceEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultReferencePageReferenceResolver;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReference;
import org.xwiki.properties.internal.DefaultBeanManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.PageResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link IncludeMacroRefactoring}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@ComponentTest
@ComponentList({
    DefaultBeanManager.class,
    DefaultConverterManager.class,
    EnumConverter.class,
    ConvertUtilsConverter.class,
    DefaultReferencePageReferenceResolver.class,
    DefaultReferenceEntityReferenceResolver.class,
    DefaultEntityReferenceProvider.class,
    DefaultModelConfiguration.class
})
class IncludeMacroRefactoringTest
{
    @InjectMockComponents
    private IncludeMacroRefactoring includeMacroRefactoring;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @MockComponent
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void replaceReferenceWhenNoReferenceParameterSet() throws MacroRefactoringException
    {
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        assertEquals(Optional.empty(), this.includeMacroRefactoring.replaceReference(block, null, null,
            (DocumentReference) null, false));
    }

    @Test
    void replaceReferenceWhenEmptyReferenceParameterSet() throws MacroRefactoringException
    {
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "");
        assertEquals(Optional.empty(), this.includeMacroRefactoring.replaceReference(block, null, null,
            (DocumentReference) null, false));
    }

    @Test
    void replaceReferenceWhenEmptyPageParameterSet() throws MacroRefactoringException
    {
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("page", "");
        assertEquals(Optional.empty(), this.includeMacroRefactoring.replaceReference(block, null, null,
            (DocumentReference) null, false));
    }

    @Test
    void replaceReferenceWhenMatchingSourceReferenceTypeReference() throws MacroRefactoringException
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");

        DocumentReference targetReference = mock(DocumentReference.class);
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block)).thenReturn(sourceReference);
        when(this.compactEntityReferenceSerializer.serialize(targetReference)).thenReturn("foobar42");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null, sourceReference,
            targetReference, false);
        assertFalse(result.isEmpty());
        assertEquals("foobar42", result.get().getParameter("reference"));
    }

    @Test
    void replaceReferenceWhenNotMatchingSourceReferenceDifferentType() throws MacroRefactoringException
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");

        // Use a different reference type to verify it doesn't match
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block)).thenReturn(
            new PageReference("wiki", "space", "page"));

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null, sourceReference,
            null, false);
        assertTrue(result.isEmpty());
    }

    @Test
    void replaceReferenceWhenNotMatchingSourceReferenceDifferentReference() throws MacroRefactoringException
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");

        // Use a different reference content to verify it doesn't match
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block)).thenReturn(
            new DocumentReference("wiki", "space", "page2"));

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null, sourceReference,
            null, false);
        assertTrue(result.isEmpty());
    }

    @Test
    void replaceReferenceWhenMatchingSourceReferenceRelative() throws MacroRefactoringException
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");
        DocumentReference currentReference = new DocumentReference("currentwiki", "currentspace", "currentpage");
        DocumentReference targetReference = mock(DocumentReference.class);
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block)).thenReturn(sourceReference);
        when(this.compactEntityReferenceSerializer.serialize(targetReference, currentReference))
            .thenReturn("foobar42");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, currentReference,
            sourceReference, targetReference, true);
        assertFalse(result.isEmpty());
        assertEquals("foobar42", result.get().getParameter("reference"));
    }

    @Test
    void replaceReferenceWhenMatchingSourceReferenceTypePage() throws MacroRefactoringException
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("page", "foo");

        PageReference pageReference = new PageReference("wiki", "space", "page");
        DocumentReference targetReference = new DocumentReference("targetwiki", "targetspace", "targetpage");
        PageReference targetPageReference = new PageReference("targetwiki", "targetspace", "targetpage");
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.PAGE, block)).thenReturn(pageReference);
        when(this.compactEntityReferenceSerializer.serialize(targetPageReference)).thenReturn("foobar42");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null, sourceReference,
            targetReference, false);
        assertFalse(result.isEmpty());
        assertEquals("foobar42", result.get().getParameter("page"));
    }

    @Test
    void replaceReferenceWhenMatchingSourceReferenceTypeAttachment() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        AttachmentReference sourceReference =
            new AttachmentReference("file", new DocumentReference("wiki", "space", "page"));
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "test");
        // Unsupported type
        block.setParameter("type", "attachment");

        AttachmentReference targetAttachmentReference = new AttachmentReference("targetFile",
            new DocumentReference("targetwiki", "targetspace", "targetpage"));
        when(this.macroEntityReferenceResolver.resolve("test", EntityType.ATTACHMENT, block))
            .thenReturn(sourceReference);
        when(this.compactEntityReferenceSerializer.serialize(targetAttachmentReference)).thenReturn("foobar42");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null, sourceReference,
            targetAttachmentReference, false);
        assertFalse(result.isEmpty());
        assertEquals("foobar42", result.get().getParameter("reference"));
    }

    @Test
    void replaceReferenceWhenPopulateError() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("type", "invalid");

        Throwable exception = assertThrows(MacroRefactoringException.class, () -> {
            this.includeMacroRefactoring.replaceReference(block, null, null, (DocumentReference) null, false);
        });
        assertEquals("There's one or several invalid parameters for an [include] macro with parameters "
            + "[{type=invalid}]", exception.getMessage());
    }

    @Test
    void extractReferencesWhenReferenceParameter() throws MacroRefactoringException
    {
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");
        Set<ResourceReference> result = this.includeMacroRefactoring.extractReferences(block);
        assertEquals(1, result.size());
        assertEquals(DocumentResourceReference.class.getName(), result.iterator().next().getClass().getName());
        assertEquals("foo", result.iterator().next().getReference());
    }

    @Test
    void extractReferencesWhenPageParameter() throws MacroRefactoringException
    {
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("page", "foo");
        Set<ResourceReference> result = this.includeMacroRefactoring.extractReferences(block);
        assertEquals(1, result.size());
        assertEquals(PageResourceReference.class.getName(), result.iterator().next().getClass().getName());
        assertEquals("foo", result.iterator().next().getReference());
    }

    @Test
    void extractReferencesWhenAttachmentParameter() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");
        block.setParameter("type", "attachment");
        Set<ResourceReference> result = this.includeMacroRefactoring.extractReferences(block);
        assertEquals(1, result.size());
        assertEquals(AttachmentResourceReference.class.getName(), result.iterator().next().getClass().getName());
        assertEquals("foo", result.iterator().next().getReference());
    }

    @Test
    void extractReferencesWhenIncompatibleTypeParameter() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "test");
        block.setParameter("type", "object");
        Set<ResourceReference> result = this.includeMacroRefactoring.extractReferences(block);
        assertEquals(0, result.size());

        assertEquals("The reference type [OBJECT] is not currently supported. Not extracting the reference [test] for "
            + "it.", logCapture.getMessage(0));
    }
}
