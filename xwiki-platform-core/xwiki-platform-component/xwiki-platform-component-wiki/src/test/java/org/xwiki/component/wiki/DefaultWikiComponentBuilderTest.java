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
package org.xwiki.component.wiki;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.component.wiki.internal.DefaultWikiComponentBuilder;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.component.wiki.internal.bridge.WikiComponentBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultWikiComponentBuilder}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultWikiComponentBuilderTest implements WikiComponentConstants
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    @InjectMockComponents
    private DefaultWikiComponentBuilder builder;

    @MockComponent
    private WikiComponentBridge bridge;

    @Test
    void buildComponentsWithoutProgrammingRights() throws Exception
    {
        WikiComponentException expected = assertThrows(WikiComponentException.class, () -> this.builder.buildComponents(DOC_REFERENCE));

        assertEquals("Registering wiki components requires programming rights", expected.getMessage());
    }

    @Test
    void buildComponents() throws Exception
    {
        when(this.bridge.getAuthorReference(DOC_REFERENCE)).thenReturn(AUTHOR_REFERENCE);
        when(this.bridge.getRoleType(DOC_REFERENCE)).thenReturn(TestRole.class);
        when(this.bridge.getRoleHint(DOC_REFERENCE)).thenReturn("test");
        when(this.bridge.getScope(DOC_REFERENCE)).thenReturn(WikiComponentScope.WIKI);
        when(this.bridge.getSyntax(DOC_REFERENCE)).thenReturn(Syntax.XWIKI_2_1);
        when(this.bridge.hasProgrammingRights(DOC_REFERENCE)).thenReturn(true);

        List<WikiComponent> components = this.builder.buildComponents(DOC_REFERENCE);

        assertEquals(1, components.size());
    }
}
