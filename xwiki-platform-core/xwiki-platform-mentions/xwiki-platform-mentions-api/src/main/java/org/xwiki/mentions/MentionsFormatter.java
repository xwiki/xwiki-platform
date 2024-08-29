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
package org.xwiki.mentions;

import org.xwiki.component.annotation.Role;
import org.xwiki.mentions.internal.MentionFormatterProvider;

/**
 * Provides services to format the user mentions.
 * <p>
 * Unless you know statically the type of actor to format, {@link MentionFormatterProvider} should be used to resolve
 * the formatter to use according to the type of the actor.
 *
 * @since 12.10
 * @see MentionFormatterProvider
 * @version $Id$
 */
@Role
public interface MentionsFormatter
{
    /**
     * Format a actor mention.
     * <p>
     * The formatter takes the serialized reference of an actor and returns an user readable string.
     * The kind of actor handled is specific to a {@code MentionFormatter} implementation.
     *
     * @param actorReference the actor reference
     * @param style the display style of the mention
     * @return the formatted mention
     */
    String formatMention(String actorReference, DisplayStyle style);
}
