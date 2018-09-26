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
package org.xwiki.user.internal.group;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

/**
 * Manipulate the cache of groups's members.
 * 
 * @version $Id$
 * @since 10.8RC1
 */
@Component(roles = MembersCache.class)
@Singleton
public class MembersCache extends AbstractGroupCache
{
    /**
     * Set the id.
     */
    public MembersCache()
    {
        super("user.membership.members");
    }

    private String toKey(DocumentReference reference)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(this.serializer.serialize(reference));

        return builder.toString();
    }

    /**
     * @param reference the reference of the entity for which to get the cache entry
     * @param create true if an entry should be created if none exist
     * @return the cache entry
     */
    public GroupCacheEntry getCacheEntry(DocumentReference reference, boolean create)
    {
        String key = toKey(reference);

        return getCacheEntry(key, reference, create);
    }

}
