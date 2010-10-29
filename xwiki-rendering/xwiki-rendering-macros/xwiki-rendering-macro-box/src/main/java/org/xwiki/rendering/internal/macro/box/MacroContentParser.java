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
package org.xwiki.rendering.internal.macro.box;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Parses content of a macro field (parameter, macro content) in a given syntax.
 *
 * @version $Id$
 * @since 2.6RC1
 */
@ComponentRole
public interface MacroContentParser
{
    /**
     * Parses content of a macro field (parameter, macro content) in a given syntax and optionally remove the top level
     * paragraph.
     *
     * @param content the content to parse
     * @param syntax the syntax in which the content is written in
     * @param removeTopLevelParagraph whether the top level paragraph should be removed after parsing
     * @return the result as a {@link org.xwiki.rendering.block.Block}s
     * @throws MacroExecutionException in case of a parsing error
     */
    List<Block> parse(String content, Syntax syntax, boolean removeTopLevelParagraph) throws MacroExecutionException;
}
