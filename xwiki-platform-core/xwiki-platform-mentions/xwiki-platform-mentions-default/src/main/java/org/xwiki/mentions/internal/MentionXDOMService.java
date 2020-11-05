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
package org.xwiki.mentions.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * A service for manipulating XDOM trees in the context of the mentions.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Role
public interface MentionXDOMService
{
    /**
     * Search for the mentions macro inside an {@link XDOM}.
     * @param xdom The xdom.
     * @return The list of mentions macro found in the XDOM.
     */
    List<MacroBlock> listMentionMacros(XDOM xdom);

    /**
     * Groups the mention anchors by user references.
     * An anchor is the unique identifier of a mention inside a document.
     * If the anchor is not specified for a given user reference (the mentioned user), then the corresponding list
     * contains an empty value (null or empty string).
     *
     *
     * @param mentions the list of mention macros in the content
     * @return the map of the anchors grouped by user references
     */
    Map<MentionedActorReference, List<String>> groupAnchorsByUserReference(List<MacroBlock> mentions);

    /**
     *
     * @param payload the string to parse
     * @param syntax the syntax of the payload
     * @return The result of the parsing. Empty if the parsing failed
     */
    Optional<XDOM> parse(String payload, Syntax syntax);
}
