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
package org.xwiki.records.internal.macro;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecordsMacroRefactoring}.
 *
 * @version $Id$
 */
@ComponentTest
class RecordsMacroRefactoringTest
{
    private static final String CLASS_VALUE = "Books.BookClass";

    private static final DocumentReference CLASS = new DocumentReference("wiki", "Books", "BookClass");

    private static final DocumentReference MOVED_CLASS = new DocumentReference("wiki", "Library", "BookClass");

    private static final DocumentReference PAGE = new DocumentReference("wiki", "Books", "WebHome");

    private static final DocumentReference MOVED_PAGE = new DocumentReference("wiki", "Shelf", "WebHome");

    @InjectMockComponents
    private RecordsMacroRefactoring refactoring;

    @MockComponent
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @Test
    void replaceReferenceWhenTheObjectTypeMoves()
    {
        MacroBlock macroBlock = macroBlock("class", CLASS_VALUE);
        resolve(macroBlock, CLASS_VALUE, PAGE, CLASS);
        when(this.compactWikiEntityReferenceSerializer.serialize(MOVED_CLASS, PAGE.getWikiReference()))
            .thenReturn("Library.BookClass");

        Optional<MacroBlock> result =
            this.refactoring.replaceReference(macroBlock, PAGE, CLASS, MOVED_CLASS, false, Map.of(CLASS, MOVED_CLASS));

        assertEquals("Library.BookClass", result.orElseThrow().getParameter("class"));
        assertNotSame(macroBlock, result.get());
        assertEquals(CLASS_VALUE, macroBlock.getParameter("class"));
    }

    @Test
    void replaceReferenceWhenAnotherPageMoves()
    {
        MacroBlock macroBlock = macroBlock("class", CLASS_VALUE);
        resolve(macroBlock, CLASS_VALUE, PAGE, CLASS);
        DocumentReference otherPage = new DocumentReference("wiki", "Other", "Page");

        assertEquals(Optional.empty(), this.refactoring.replaceReference(macroBlock, PAGE, otherPage,
            new DocumentReference("wiki", "Other", "Moved"), false));
    }

    @Test
    void replaceReferenceKeepsThePartialObjectTypeOfAMovedPage()
    {
        // The value names the object type by its page name only, so it would point to another page once the page
        // holding the macro has moved.
        MacroBlock macroBlock = macroBlock("class", "BookClass");
        resolve(macroBlock, "BookClass", PAGE, CLASS);
        resolve(macroBlock, "BookClass", MOVED_PAGE, new DocumentReference("wiki", "Shelf", "BookClass"));
        when(this.compactWikiEntityReferenceSerializer.serialize(CLASS, MOVED_PAGE.getWikiReference()))
            .thenReturn(CLASS_VALUE);

        Optional<MacroBlock> result =
            this.refactoring.replaceReference(macroBlock, MOVED_PAGE, PAGE, MOVED_PAGE, true, Map.of(PAGE, MOVED_PAGE));

        assertEquals(CLASS_VALUE, result.orElseThrow().getParameter("class"));
    }

    @Test
    void replaceReferenceKeepsTheFullObjectTypeOfAMovedPage()
    {
        MacroBlock macroBlock = macroBlock("class", CLASS_VALUE);
        resolve(macroBlock, CLASS_VALUE, PAGE, CLASS);
        resolve(macroBlock, CLASS_VALUE, MOVED_PAGE, CLASS);

        assertEquals(Optional.empty(),
            this.refactoring.replaceReference(macroBlock, MOVED_PAGE, PAGE, MOVED_PAGE, true));
    }

    @Test
    void replaceReferenceFollowsAnObjectTypeMovedWithThePage()
    {
        MacroBlock macroBlock = macroBlock("class", "BookClass");
        resolve(macroBlock, "BookClass", PAGE, CLASS);
        DocumentReference movedClass = new DocumentReference("wiki", "Shelf", "BookClass");
        resolve(macroBlock, "BookClass", MOVED_PAGE, movedClass);

        assertEquals(Optional.empty(), this.refactoring.replaceReference(macroBlock, MOVED_PAGE, PAGE, MOVED_PAGE,
            true, Map.of(PAGE, MOVED_PAGE, CLASS, movedClass)));
    }

    @Test
    void replaceReferenceAcceptsTheParameterInAnyCase()
    {
        MacroBlock macroBlock = macroBlock("Class", CLASS_VALUE);
        resolve(macroBlock, CLASS_VALUE, PAGE, CLASS);
        when(this.compactWikiEntityReferenceSerializer.serialize(MOVED_CLASS, PAGE.getWikiReference()))
            .thenReturn("Library.BookClass");

        Optional<MacroBlock> result = this.refactoring.replaceReference(macroBlock, PAGE, CLASS, MOVED_CLASS, false);

        assertEquals("Library.BookClass", result.orElseThrow().getParameter("Class"));
    }

    @Test
    void replaceReferenceWithoutObjectType()
    {
        assertEquals(Optional.empty(),
            this.refactoring.replaceReference(macroBlock("id", "books"), PAGE, CLASS, MOVED_CLASS, false));
        assertEquals(Optional.empty(),
            this.refactoring.replaceReference(macroBlock("class", " "), PAGE, CLASS, MOVED_CLASS, false));
    }

    @Test
    void replaceAttachmentReference()
    {
        assertEquals(Optional.empty(), this.refactoring.replaceReference(macroBlock("class", CLASS_VALUE), PAGE,
            new AttachmentReference("a.png", PAGE), new AttachmentReference("b.png", PAGE), false));
    }

    @Test
    void extractReferences()
    {
        assertEquals(Set.of(new DocumentResourceReference(CLASS_VALUE)),
            this.refactoring.extractReferences(macroBlock("class", CLASS_VALUE)));
        assertEquals(Set.of(), this.refactoring.extractReferences(macroBlock("id", "books")));
    }

    private MacroBlock macroBlock(String parameterName, String value)
    {
        return new MacroBlock(RecordsMacro.ID, Map.of(parameterName, value), true);
    }

    private void resolve(MacroBlock macroBlock, String value, DocumentReference base, DocumentReference result)
    {
        when(this.macroEntityReferenceResolver.resolve(eq(value), eq(EntityType.DOCUMENT), same(macroBlock),
            eq(base))).thenReturn(result);
    }
}
