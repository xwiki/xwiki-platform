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
package org.xwiki.rendering.internal.macro.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link EmptyXDOMChecker}.
 *
 * @version $Id$
 * @since 8.4RC1
 */
public class EmptyXDOMCheckerTest
{
    @Rule
    public MockitoComponentMockingRule<EmptyXDOMChecker> mocker =
        new MockitoComponentMockingRule<>(EmptyXDOMChecker.class);

    @Test
    public void checkWhenSingleMacroMarkerBlock() throws Exception
    {
        List<Block> blocks = Collections.singletonList(
            new MacroMarkerBlock("macro", Collections.emptyMap(), Collections.emptyList(), false));

        assertTrue(this.mocker.getComponentUnderTest().check(blocks));
    }

    @Test
    public void checkWhenTwoMacroMarkerBlock() throws Exception
    {
        List<Block> blocks = Arrays.asList(
            new MacroMarkerBlock("macro", Collections.emptyMap(), Collections.emptyList(), false),
            new MacroMarkerBlock("macro2", Collections.emptyMap(), Collections.emptyList(), false)
        );

        assertTrue(this.mocker.getComponentUnderTest().check(blocks));
    }

    @Test
    public void checkWhenNoBlock() throws Exception
    {
        List<Block> blocks = Collections.emptyList();
        assertTrue(this.mocker.getComponentUnderTest().check(blocks));
    }
}
