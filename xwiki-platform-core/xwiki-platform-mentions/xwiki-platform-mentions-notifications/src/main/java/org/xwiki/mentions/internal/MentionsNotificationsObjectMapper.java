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

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.mentions.events.MentionEventParams;
import org.xwiki.stability.Unstable;

/**
 * Provides the operations to serialize and unserializa objects that holds mentions notifications datas.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Role
@Unstable
public interface MentionsNotificationsObjectMapper
{
    /**
     * Unserializes the data to {@link MentionEventParams}.
     *
     * @param data The content to unserialize.
     * @return The content serialized. {@link Optional#empty()} if the unserialization failed.
     */
    Optional<MentionEventParams> unserialize(String data);

    /**
     * Serializez a {@link MentionEventParams} into a json string.
     * @param params The params.
     * @return The resulting json string. {@link Optional#empty()} in case of error.
     */
    Optional<String> serialize(MentionEventParams params);
}
