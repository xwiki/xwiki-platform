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
import java.util.Map;
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
import org.xwiki.model.reference.EntityReference;
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
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @MockComponent
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void replaceDocumentReferenceWhenNoReferenceParameterSet() throws Exception
    {
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        assertEquals(Optional.empty(), this.includeMacroRefactoring.replaceReference(block, null, null,
            (DocumentReference) null, false, Map.of()));
    }

    @Test
    void replaceDocumentReferenceWhenEmptyReferenceParameterSet() throws Exception
    {
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "");
        assertEquals(Optional.empty(), this.includeMacroRefactoring.replaceReference(block, null, null,
            (DocumentReference) null, false, Map.of()));
    }

    @Test
    void replaceDocumentReferenceWhenEmptyPageParameterSet() throws Exception
    {
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("page", "");
        assertEquals(Optional.empty(), this.includeMacroRefactoring.replaceReference(block, null, null,
            (DocumentReference) null, false, Map.of()));
    }

    @Test
    void replaceDocumentReferenceWhenIncludedReferenceIsRenamedUsingReferenceParameterAndNoBASEReference()
        throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("sourcewiki", "sourcespace", "foo");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");

        // This is what tells IncludeMacroRefactoring that it's the included reference that is being renamed
        // (as macro parameter reference == passed source reference).
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block, sourceReference))
            .thenReturn(sourceReference);

        DocumentReference targetReference = mock(DocumentReference.class);
        DocumentReference currentReference = new DocumentReference("foowiki", "foospace", "foo");
        when(this.compactEntityReferenceSerializer.serialize(targetReference, currentReference))
            .thenReturn("targetwiki:targetspace.targetfoo");

        when(this.compactWikiEntityReferenceSerializer.serialize(sourceReference,
            new EntityReference("sourcewiki", EntityType.WIKI))).thenReturn("sourcespace.foo");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, currentReference,
            sourceReference, targetReference, false, Map.of());
        assertFalse(result.isEmpty());
        assertEquals("targetwiki:targetspace.targetfoo", result.get().getParameter("reference"));
    }

    @Test
    void replaceDocumentReferenceWhenIncludingDocumentRenamedUsingReferenceParameterAndNoBASEReference()
        throws Exception
    {
        // The source reference points to the including document original reference
        DocumentReference sourceReference = new DocumentReference("sourcewiki", "sourcespace", "sourcepage");
        // The target reference points to the including document new reference
        DocumentReference targetReference = new DocumentReference("targetwiki", "targetspace", "targetpage");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        // The reference was pointing to: sourcewiki:sourcespace.foo
        block.setParameter("reference", "foo");

        // This tells IncludeMacroRefactoring that the macro parameter reference isn't equal to the passed
        // source reference and thus that we are in the case of a including document rename.
        DocumentReference fooReference = new DocumentReference("sourcewiki", "sourcespace", "foo");
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block, sourceReference)).thenReturn(
            fooReference);

        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block, sourceReference)).thenReturn(
            new DocumentReference("sourcewiki", "sourcespace", "foo"));
        DocumentReference fooTargetReference = new DocumentReference("targetwiki", "targetspace", "foo");
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block, targetReference)).thenReturn(
            fooTargetReference);
        when(this.compactEntityReferenceSerializer.serialize(
            new DocumentReference("sourcewiki", "sourcespace", "foo"), fooTargetReference))
            .thenReturn("sourcewiki:sourcespace.foo");

        when(this.compactWikiEntityReferenceSerializer.serialize(fooReference,
            new EntityReference("sourcewiki", EntityType.WIKI))).thenReturn("sourcespace.foo");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null,
            sourceReference, targetReference, false, Map.of());
        assertFalse(result.isEmpty());
        assertEquals("sourcewiki:sourcespace.foo", result.get().getParameter("reference"));
    }

    @Test
    void replaceDocumentReferenceWhenIncludingDocumentRenamedUsingPageParameterAndNoBASEReference()
        throws Exception
    {
        // The source reference points to the including document original reference
        DocumentReference sourceReference = new DocumentReference("sourcewiki", "sourcespace", "sourcepage");
        // The target reference points to the including document new reference
        DocumentReference targetReference = new DocumentReference("targetwiki", "targetspace", "targetpage");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        // The reference was pointing to: sourcewiki:sourcespace.foo
        block.setParameter("page", "foo");

        // This tells IncludeMacroRefactoring that the macro parameter reference isn't equal to the passed
        // source reference and thus that we are in the case of a including document rename.
        PageReference fooReference = new PageReference("sourcewiki", "sourcespace", "foo");
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.PAGE, block, sourceReference)).thenReturn(
            fooReference);

        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.PAGE, block, sourceReference)).thenReturn(
            new PageReference("sourcewiki", "sourcespace", "foo"));
        PageReference footTargetPageReference = new PageReference("targetwiki", "targetspace", "foo");
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.PAGE, block, targetReference)).thenReturn(
            footTargetPageReference);
        when(this.compactEntityReferenceSerializer.serialize(
            new PageReference("sourcewiki", "sourcespace", "foo"), footTargetPageReference))
            .thenReturn("sourcewiki:sourcespace.foo");

        when(this.compactWikiEntityReferenceSerializer.serialize(fooReference,
            new EntityReference("sourcewiki", EntityType.WIKI))).thenReturn("sourcespace/foo");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null,
            sourceReference, targetReference, false, Map.of());
        assertFalse(result.isEmpty());
        assertEquals("sourcewiki:sourcespace.foo", result.get().getParameter("page"));
    }

    @Test
    void replaceDocumentReferenceWhenIncludedReferenceIsRenamedUsingPageParameterAndNoBASEReference()
        throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("sourcewiki", "sourcespace", "foo");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("page", "foo");

        // This is what tells IncludeMacroRefactoring that it's the included reference that is being renamed
        // (as macro parameter reference == passed source reference).
        PageReference pageReference = new PageReference("sourcewiki", "sourcespace", "foo");
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.PAGE, block, sourceReference))
            .thenReturn(pageReference);

        DocumentReference targetReference = new DocumentReference("targetwiki", "targetspace", "targetfoo");
        PageReference targetPageReference = new PageReference("targetwiki", "targetspace", "targetfoo");
        DocumentReference currentReference = new DocumentReference("foowiki", "foospace", "foo");
        when(this.compactEntityReferenceSerializer.serialize(targetPageReference, currentReference))
            .thenReturn("targetwiki:targetspace.targetfoo");

        when(this.compactWikiEntityReferenceSerializer.serialize(pageReference,
            new EntityReference("sourcewiki", EntityType.WIKI))).thenReturn("sourcespace/foo");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, currentReference,
            sourceReference, targetReference, false, Map.of());
        assertFalse(result.isEmpty());
        assertEquals("targetwiki:targetspace.targetfoo", result.get().getParameter("page"));
    }

    @Test
    void replaceDocumentReferenceWhenIncludingDocumentIsRenamedToSameReference() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("sourcewiki", "sourcespace", "sourcepage");
        DocumentReference targetReference = new DocumentReference("sourcewiki", "sourcespace", "sourcepage");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");

        // This tells IncludeMacroRefactoring that the macro parameter reference isn't equal to the passed
        // source reference and thus that we are in the case of a including document rename.
        DocumentReference fooReference = new DocumentReference("sourcewiki", "sourcespace", "foo");
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block, sourceReference))
            .thenReturn(fooReference);

        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block, sourceReference)).thenReturn(
            new DocumentReference("sourcewiki", "sourcespace", "foo"));
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.DOCUMENT, block, targetReference)).thenReturn(
            new DocumentReference("sourcewiki", "sourcespace", "foo"));

        when(this.compactWikiEntityReferenceSerializer.serialize(fooReference,
            new EntityReference("sourcewiki", EntityType.WIKI))).thenReturn("sourcespace.foo");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null, sourceReference,
            targetReference, false, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void replaceAttachmentReferenceWhenIncludedAttachmentReferenceIsRenamedUsingReferenceParameterAndNoBASEReference()
        throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        AttachmentReference sourceReference =
            new AttachmentReference("sourcefile", new DocumentReference("sourcewiki", "sourcespace", "foo"));
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foo");
        // Unsupported type ATM (by the include and display macros) but we still check that we can refactor the
        // parameter
        block.setParameter("type", "attachment");

        AttachmentReference targetAttachmentReference = new AttachmentReference("targetfile",
            new DocumentReference("targetwiki", "targetspace", "targetfoo"));
        when(this.macroEntityReferenceResolver.resolve("foo", EntityType.ATTACHMENT, block, sourceReference))
            .thenReturn(sourceReference);
        DocumentReference currentReference = new DocumentReference("foowiki", "foospace", "foo");
        when(this.compactEntityReferenceSerializer.serialize(targetAttachmentReference, currentReference))
            .thenReturn("targetwiki:targetspace.targetfoo@targetfile");

        when(this.compactWikiEntityReferenceSerializer.serialize(sourceReference,
            new EntityReference("sourcewiki", EntityType.WIKI))).thenReturn("sourcespace.foo");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, currentReference,
            sourceReference, targetAttachmentReference, false, Map.of());
        assertFalse(result.isEmpty());
        assertEquals("targetwiki:targetspace.targetfoo@targetfile", result.get().getParameter("reference"));
    }

    @Test
    void replaceDocumentReferenceWhenIncludingDocumentIsRenamedUsingAttachmentReferenceParameterAndNoBASEReference()
        throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        // The source reference points to the including document original reference
        DocumentReference sourceReference = new DocumentReference("sourcewiki", "sourcespace", "sourcepage");
        // The target reference points to the including document new reference
        DocumentReference targetReference = new DocumentReference("targetwiki", "targetspace", "targetpage");
        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("reference", "foopage@foofile");
        // Unsupported type ATM (by the include and display macros) but we still check that we can refactor the
        // parameter
        block.setParameter("type", "attachment");

        // This tells IncludeMacroRefactoring that the macro parameter reference isn't equal to the passed
        // source reference and thus that we are in the case of a including document rename.
        AttachmentReference fooReference = new AttachmentReference("foofile",
            new DocumentReference("sourcewiki", "sourcespace", "foopage"));
        when(this.macroEntityReferenceResolver.resolve("foopage@foofile", EntityType.ATTACHMENT, block,
            sourceReference)).thenReturn(fooReference);
        when(this.macroEntityReferenceResolver.resolve("foopage@foofile", EntityType.ATTACHMENT, block,
            sourceReference)).thenReturn(new AttachmentReference("foofile",
                new DocumentReference("sourcewiki", "sourcespace", "foopage")));
        AttachmentReference fooTargetReference = new AttachmentReference("foofile",
            new DocumentReference("targetwiki", "targetspace", "foopage"));
        when(this.macroEntityReferenceResolver.resolve("foopage@foofile", EntityType.ATTACHMENT, block,
            targetReference)).thenReturn(fooTargetReference);

        when(this.compactEntityReferenceSerializer.serialize(new AttachmentReference("foofile",
            new DocumentReference("sourcewiki", "sourcespace", "foopage")), fooTargetReference))
            .thenReturn("sourcewiki:sourcespace.foopage@foofile");

        when(this.compactWikiEntityReferenceSerializer.serialize(fooReference,
            new EntityReference("sourcewiki", EntityType.WIKI))).thenReturn("sourcespace.foopage@foofile");

        Optional<MacroBlock> result = this.includeMacroRefactoring.replaceReference(block, null,
            sourceReference, targetReference, false, Map.of());
        assertFalse(result.isEmpty());
        assertEquals("sourcewiki:sourcespace.foopage@foofile", result.get().getParameter("reference"));
    }

    @Test
    void replaceDocumentReferenceWhenParametersPopulateError() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        MacroBlock block = new MacroBlock("include", Collections.emptyMap(), false);
        block.setParameter("type", "invalid");

        Throwable exception = assertThrows(MacroRefactoringException.class,
            () -> this.includeMacroRefactoring.replaceReference(block, null, null, (DocumentReference) null, false,
                Map.of()));
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
