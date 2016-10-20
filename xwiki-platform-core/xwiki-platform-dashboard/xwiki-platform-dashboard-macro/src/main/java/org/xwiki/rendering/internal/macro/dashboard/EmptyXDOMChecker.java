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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroMarkerBlock;

/**
 * Checks if the passed Blocks represent some visually empty content or not. We consider it's empty when:
 * <ul>
 *  <li>There are no blocks</li>
 *  <li>There's only 1 block, it's a {@link org.xwiki.rendering.block.MacroMarkerBlock} and it doesn't have
 *      children</li>
 * </ul>
 *
 * @version $Id$
 * @since 8.4RC1
 */
@Component
@Named("empty")
@Singleton
public class EmptyXDOMChecker implements XDOMChecker
{
    @Override
    public boolean check(List<Block> blocks)
    {
        return isContentEmpty(blocks);
    }

    private boolean isContentEmpty(List<Block> blocks)
    {
        boolean result = false;

        if (blocks.isEmpty()) {
            result = true;
        } else {
            result = true;
            for (Block block : blocks) {
                if (!isMacroMarkerBlockAndEmpty(block)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    private boolean isMacroMarkerBlockAndEmpty(Block block)
    {
        return block instanceof MacroMarkerBlock && block.getChildren().isEmpty();
    }
}
