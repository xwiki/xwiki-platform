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
 *
 */
package org.xwiki.security.internal;

import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReference;

import org.xwiki.component.annotation.Component;

/**
 * A serializer used by the right cache for generating keys.
 * @version $Id: $
 */
@Component("rightcachekey")
public class RightCacheKeyEntityReferenceSerializer implements EntityReferenceSerializer<String>
{
    @Override
    public String serialize(EntityReference reference, Object... parameters)
    {
        StringBuilder builder = new StringBuilder();
        EntityReference mainWiki = reference.getRoot();
        for (EntityReference ref = reference.getRoot(); ref != null; ref = ref.getChild()) {
            builder.append(ref.getName());
            switch (ref.getType()) {
                case WIKI:
                    builder.append(':');
                    break;
                case SPACE:
                    builder.append('.');
                    break;
                case DOCUMENT:
                    break;
                default:
                    throw new IllegalArgumentException("Unsopported entity type: " + ref.getType());
            }
        }
        return builder.toString();
    }
}
