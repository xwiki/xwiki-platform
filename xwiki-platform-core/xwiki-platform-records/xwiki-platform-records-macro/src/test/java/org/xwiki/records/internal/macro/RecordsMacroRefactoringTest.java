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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;
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

    private static final String LOCATION_VALUE = "Books.Archive";

    private static final SpaceReference LOCATION = new SpaceReference("wiki", "Books", "Archive");

    private static final DocumentReference LOCATION_HOME = new DocumentReference("WebHome", LOCATION);

    private static final SpaceReference MOVED_LOCATION = new SpaceReference("wiki", "Books", "Old");

    @InjectMockComponents
    private RecordsMacroRefactoring refactoring;

    @MockComponent
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @BeforeEach
    void beforeEach()
    {
        when(this.entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
    }

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
    void replaceReferenceWhenTheLocationSpaceIsRenamed()
    {
        MacroBlock macroBlock = macroBlock("newEntriesLocation", LOCATION_VALUE);
        resolveSpace(macroBlock, LOCATION_VALUE, PAGE, LOCATION);
        when(this.compactWikiEntityReferenceSerializer.serialize(MOVED_LOCATION, PAGE.getWikiReference()))
            .thenReturn("Books.Old");

        Optional<MacroBlock> result = this.refactoring.replaceReference(macroBlock, PAGE, LOCATION_HOME,
            new DocumentReference("WebHome", MOVED_LOCATION), false);

        assertEquals("Books.Old", result.orElseThrow().getParameter("newEntriesLocation"));
    }

    @Test
    void replaceReferenceKeepsThePartialObjectTypeAndLocationOfAPageMovedToAnotherWiki()
    {
        // A space value names every space, so only its wiki is completed from the page holding the macro.
        MacroBlock macroBlock =
            new MacroBlock(RecordsMacro.ID, Map.of("class", "BookClass", "newEntriesLocation", "Archive"), true);
        DocumentReference otherWikiPage = new DocumentReference("other", "Shelf", "WebHome");
        SpaceReference archive = new SpaceReference("wiki", "Archive");
        resolve(macroBlock, "BookClass", PAGE, CLASS);
        resolve(macroBlock, "BookClass", otherWikiPage, new DocumentReference("other", "Shelf", "BookClass"));
        resolveSpace(macroBlock, "Archive", PAGE, archive);
        resolveSpace(macroBlock, "Archive", otherWikiPage, new SpaceReference("other", "Archive"));
        when(this.compactWikiEntityReferenceSerializer.serialize(CLASS, otherWikiPage.getWikiReference()))
            .thenReturn("wiki:Books.BookClass");
        when(this.compactWikiEntityReferenceSerializer.serialize(archive, otherWikiPage.getWikiReference()))
            .thenReturn("wiki:Archive");

        MacroBlock result = this.refactoring.replaceReference(macroBlock, otherWikiPage, PAGE, otherWikiPage, true,
            Map.of(PAGE, otherWikiPage)).orElseThrow();

        assertEquals("wiki:Books.BookClass", result.getParameter("class"));
        assertEquals("wiki:Archive", result.getParameter("newEntriesLocation"));
    }

    @Test
    void replaceReferenceFollowsALocationMovedWithThePage()
    {
        MacroBlock macroBlock = macroBlock("newEntriesLocation", "Archive");
        SpaceReference archive = new SpaceReference("wiki", "Archive");
        resolveSpace(macroBlock, "Archive", PAGE, archive);
        resolveSpace(macroBlock, "Archive", MOVED_PAGE, archive);
        SpaceReference movedArchive = new SpaceReference("wiki", "Library", "Archive");
        when(this.compactWikiEntityReferenceSerializer.serialize(movedArchive, MOVED_PAGE.getWikiReference()))
            .thenReturn("Library.Archive");

        Optional<MacroBlock> result = this.refactoring.replaceReference(macroBlock, MOVED_PAGE, PAGE, MOVED_PAGE, true,
            Map.of(PAGE, MOVED_PAGE, new DocumentReference("WebHome", archive),
                new DocumentReference("WebHome", movedArchive)));

        assertEquals("Library.Archive", result.orElseThrow().getParameter("newEntriesLocation"));
    }

    @Test
    void replaceReferenceKeepsTheLocationWhenItsHomeBecomesARegularPage()
    {
        MacroBlock macroBlock = macroBlock("newEntriesLocation", LOCATION_VALUE);
        resolveSpace(macroBlock, LOCATION_VALUE, PAGE, LOCATION);

        assertEquals(Optional.empty(), this.refactoring.replaceReference(macroBlock, PAGE, LOCATION_HOME,
            new DocumentReference("wiki", "Books", "Archive"), false));
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
        MacroBlock macroBlock = new MacroBlock(RecordsMacro.ID,
            Map.of("class", CLASS_VALUE, "newEntriesLocation", LOCATION_VALUE), true);
        assertEquals(Set.of(new DocumentResourceReference(CLASS_VALUE), new SpaceResourceReference(LOCATION_VALUE)),
            this.refactoring.extractReferences(macroBlock));
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

    private void resolveSpace(MacroBlock macroBlock, String value, DocumentReference base, SpaceReference result)
    {
        when(this.macroEntityReferenceResolver.resolve(eq(value), eq(EntityType.SPACE), same(macroBlock),
            eq(base))).thenReturn(result);
    }
}
