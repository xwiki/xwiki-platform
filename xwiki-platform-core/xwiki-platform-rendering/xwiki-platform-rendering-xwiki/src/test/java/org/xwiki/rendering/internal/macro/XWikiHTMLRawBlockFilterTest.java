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
package org.xwiki.rendering.internal.macro;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.transformation.macro.RawBlockFilterParameters;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XWikiHTMLRawBlockFilter}.
 *
 * @version $Id$
 * @since 14.8RC1
 */
@ComponentTest
class XWikiHTMLRawBlockFilterTest
{
    @InjectMockComponents
    private XWikiHTMLRawBlockFilter filter;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @Test
    void filter() throws MacroExecutionException
    {
        RawBlock block = mock(RawBlock.class);
        Syntax blockSyntax = mock(Syntax.class);
        when(block.getSyntax()).thenReturn(blockSyntax);

        RawBlockFilterParameters parameters = mock(RawBlockFilterParameters.class);

        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);
        when(blockSyntax.getType()).thenReturn(SyntaxType.HTML);

        assertSame(block, this.filter.filter(block, parameters));
        verify(parameters, never()).setRestricted(true);

        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);
        when(blockSyntax.getType()).thenReturn(SyntaxType.EVENT);

        assertSame(block, this.filter.filter(block, parameters));
        verify(parameters, never()).setRestricted(true);

        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(false);
        when(blockSyntax.getType()).thenReturn(SyntaxType.EVENT);

        assertSame(block, this.filter.filter(block, parameters));
        verify(parameters, never()).setRestricted(true);

        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(false);
        when(blockSyntax.getType()).thenReturn(SyntaxType.HTML);

        // We don't check the behaviour of super.filter there, only the fact that the parameter is set to restricted.
        assertSame(block, this.filter.filter(block, parameters));
        verify(parameters).setRestricted(true);
    }
}