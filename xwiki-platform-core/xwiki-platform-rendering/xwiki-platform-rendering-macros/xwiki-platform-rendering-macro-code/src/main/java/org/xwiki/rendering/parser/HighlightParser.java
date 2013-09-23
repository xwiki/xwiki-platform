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
package org.xwiki.rendering.parser;

import java.io.Reader;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;

/**
 * Convert provided source to an highlighted version.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
@Role
public interface HighlightParser extends Parser
{
    /**
     * Return a highlighted version of the provided content.
     * 
     * @param syntaxId the identifier of the source syntax.
     * @param source the content to highlight.
     * @return the highlighted version of the provided source.
     * @throws ParseException the highlighting failed.
     */
    List<Block> highlight(String syntaxId, Reader source) throws ParseException;
}
