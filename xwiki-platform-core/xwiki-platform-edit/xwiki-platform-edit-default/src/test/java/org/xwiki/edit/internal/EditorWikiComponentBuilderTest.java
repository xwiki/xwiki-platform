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
package org.xwiki.edit.internal;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EditorWikiComponentBuilder}.
 *
 * @version $Id$
 */
@ComponentTest
class EditorWikiComponentBuilderTest
{
    private static final DocumentReference EDITOR_REFERENCE = new DocumentReference("wiki", "Space", "Editor");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("wiki", "XWiki", "Author");

    // Raw type to match the role of the injected Provider<EditorWikiComponent>.
    @MockComponent
    @SuppressWarnings("rawtypes")
    private EditorWikiComponent editorWikiComponent;

    @MockComponent
    private DocumentAuthorizationManager authorization;

    @InjectMockComponents
    private EditorWikiComponentBuilder builder;

    @BeforeEach
    void setUp()
    {
        when(this.editorWikiComponent.getAuthorReference()).thenReturn(AUTHOR_REFERENCE);
    }

    @Test
    void buildUserEditor() throws Exception
    {
        when(this.editorWikiComponent.getScope()).thenReturn(WikiComponentScope.USER);

        assertEquals(List.of(this.editorWikiComponent), this.builder.buildComponents(EDITOR_REFERENCE));

        verify(this.editorWikiComponent).initialize(EDITOR_REFERENCE);
        verify(this.authorization, never()).hasAccess(any(), any(), any(), any());
    }

    @Test
    void buildWikiEditor() throws Exception
    {
        when(this.editorWikiComponent.getScope()).thenReturn(WikiComponentScope.WIKI);
        when(this.authorization.hasAccess(Right.ADMIN, EntityType.WIKI, AUTHOR_REFERENCE, EDITOR_REFERENCE))
            .thenReturn(true);

        assertEquals(List.of(this.editorWikiComponent), this.builder.buildComponents(EDITOR_REFERENCE));
    }

    @Test
    void buildGlobalEditor() throws Exception
    {
        when(this.editorWikiComponent.getScope()).thenReturn(WikiComponentScope.GLOBAL);
        when(this.authorization.hasAccess(Right.PROGRAM, null, AUTHOR_REFERENCE, EDITOR_REFERENCE)).thenReturn(true);

        assertEquals(List.of(this.editorWikiComponent), this.builder.buildComponents(EDITOR_REFERENCE));
    }

    @ParameterizedTest
    @EnumSource(value = WikiComponentScope.class, names = { "WIKI", "GLOBAL" })
    void buildEditorWithoutRight(WikiComponentScope scope) throws Exception
    {
        when(this.editorWikiComponent.getScope()).thenReturn(scope);
        // Having the right on another level isn't enough.
        when(this.authorization.hasAccess(eq(scope == WikiComponentScope.WIKI ? Right.PROGRAM : Right.ADMIN), any(),
            any(), any())).thenReturn(true);

        WikiComponentException exception =
            assertThrows(WikiComponentException.class, () -> this.builder.buildComponents(EDITOR_REFERENCE));

        assertEquals(scope == WikiComponentScope.WIKI
            ? "Registering editors at wiki level requires wiki administration rights"
            : "Registering global editors requires programming rights", exception.getMessage());
    }
}
