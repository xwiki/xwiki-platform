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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link IncludeMacroRefactoring}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@ComponentTest
class IncludeMacroRefactoringTest
{
    @InjectMockComponents
    private IncludeMacroRefactoring includeMacroRefactoring;

    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private MacroBlock macroBlock;

    @Mock
    private DocumentReference sourceReference;

    @Mock
    private DocumentReference targetReference;

    @Mock
    private DocumentReference currentDocumentReference;

    @Test
    void replaceReferenceNoOrEmptyParameter() throws MacroRefactoringException
    {
        assertEquals(Optional.empty(), this.includeMacroRefactoring.replaceReference(this.macroBlock,
            this.currentDocumentReference, this.sourceReference, this.targetReference, false));
        when(this.macroBlock.getParameter(IncludeMacroRefactoring.REFERENCE_MACRO_PARAMETER))
            .thenReturn("");
        when(this.macroBlock.getParameter(IncludeMacroRefactoring.DOCUMENT_MACRO_PARAMETER))
            .thenReturn("");
        assertEquals(Optional.empty(), this.includeMacroRefactoring.replaceReference(this.macroBlock,
            this.currentDocumentReference, this.sourceReference, this.targetReference, false));
    }

    @Test
    void replaceReferenceReferenceParameter() throws MacroRefactoringException
    {
        String referenceParam = "foo";
        when(this.macroBlock.getParameter(IncludeMacroRefactoring.REFERENCE_MACRO_PARAMETER))
            .thenReturn(referenceParam);
        when(this.documentReferenceResolver.resolve(referenceParam)).thenReturn(this.sourceReference);

        String targetReference = "foobar42";
        when(this.stringEntityReferenceSerializer.serialize(this.targetReference, this.currentDocumentReference))
            .thenReturn(targetReference);
        MacroBlock expectedBlock = mock(MacroBlock.class);
        when(this.macroBlock.clone()).thenReturn(expectedBlock);
        assertEquals(Optional.of(expectedBlock), this.includeMacroRefactoring.replaceReference(this.macroBlock,
            this.currentDocumentReference, this.sourceReference, this.targetReference, false));
        verify(expectedBlock).setParameter(IncludeMacroRefactoring.REFERENCE_MACRO_PARAMETER, targetReference);
    }

    @Test
    void replaceReferenceDocumentParameter() throws MacroRefactoringException
    {
        String referenceParam = "foo";
        when(this.macroBlock.getParameter(IncludeMacroRefactoring.DOCUMENT_MACRO_PARAMETER))
            .thenReturn(referenceParam);
        when(this.documentReferenceResolver.resolve(referenceParam)).thenReturn(this.sourceReference);

        String targetReference = "foobar42";
        when(this.stringEntityReferenceSerializer.serialize(this.targetReference, this.currentDocumentReference))
            .thenReturn(targetReference);
        MacroBlock expectedBlock = mock(MacroBlock.class);
        when(this.macroBlock.clone()).thenReturn(expectedBlock);
        assertEquals(Optional.of(expectedBlock), this.includeMacroRefactoring.replaceReference(this.macroBlock,
            this.currentDocumentReference, this.sourceReference, this.targetReference, false));
        verify(expectedBlock).setParameter(IncludeMacroRefactoring.DOCUMENT_MACRO_PARAMETER, targetReference);
    }

    @Test
    void extractReferences() throws MacroRefactoringException
    {
        assertEquals(Collections.emptySet(), this.includeMacroRefactoring.extractReferences(this.macroBlock));
        String referenceParam = "foo";
        when(this.macroBlock.getParameter(IncludeMacroRefactoring.DOCUMENT_MACRO_PARAMETER))
            .thenReturn(referenceParam);
        assertEquals(Collections.singleton(new DocumentResourceReference(referenceParam)),
            this.includeMacroRefactoring.extractReferences(this.macroBlock));

        String otherReferenceParam = "bar";
        when(this.macroBlock.getParameter(IncludeMacroRefactoring.REFERENCE_MACRO_PARAMETER))
            .thenReturn(otherReferenceParam);
        assertEquals(Collections.singleton(new DocumentResourceReference(otherReferenceParam)),
            this.includeMacroRefactoring.extractReferences(this.macroBlock));
    }
}
