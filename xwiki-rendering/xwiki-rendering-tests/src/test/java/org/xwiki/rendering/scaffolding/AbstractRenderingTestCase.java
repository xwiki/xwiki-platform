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
package org.xwiki.rendering.scaffolding;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;

import java.util.List;
import java.io.StringWriter;

import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

public abstract class AbstractRenderingTestCase extends AbstractBridgedXWikiComponentTestCase
{
    protected void assertBlocks(String expected, List<Block> blocks)
    {
        // Assert the result by parsing it through the TestEventsListener to generate easily
        // assertable events.
        XDOM dom = new XDOM(blocks);
        StringWriter sw = new StringWriter();
        dom.traverse(new TestEventsListener(sw));
        assertEquals(expected, sw.toString());
    }
}
