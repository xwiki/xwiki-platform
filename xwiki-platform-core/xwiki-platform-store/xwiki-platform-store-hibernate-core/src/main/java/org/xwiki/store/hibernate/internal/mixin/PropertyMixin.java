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
package org.xwiki.store.hibernate.internal.mixin;

import java.util.Iterator;

import org.hibernate.mapping.Property;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link Property} APIs restored in the patched Hibernate.
 *
 * @version $Id$
 */
@Mixin(target = Property.class)
@SuppressWarnings({"rawtypes", "deprecation", "removal"})
public final class PropertyMixin
{
    private PropertyMixin()
    {
    }

    /**
     * @param self the instance
     * @return an iterator over the result of {@code getSelectables()}
     * @deprecated use {@code getSelectables()} instead
     */
    @Deprecated
    public static Iterator getColumnIterator(Property self)
    {
        return self.getSelectables().iterator();
    }
}
