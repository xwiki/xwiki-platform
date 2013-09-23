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
package com.xpn.xwiki.internal.model.reference;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;

/**
 * Generate an entity reference string that doesn't contain the wiki reference part if the passed reference matches the
 * current wiki. The space reference and page references are always printed.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@Component("compactwiki")
public class CompactWikiStringEntityReferenceSerializer extends CompactStringEntityReferenceSerializer
{
    @Override
    protected String resolveDefaultValue(EntityType type, Object... parameters)
    {
        if (type == EntityType.WIKI) {
            return super.resolveDefaultValue(type, parameters);
        }

        return null;
    }
}
