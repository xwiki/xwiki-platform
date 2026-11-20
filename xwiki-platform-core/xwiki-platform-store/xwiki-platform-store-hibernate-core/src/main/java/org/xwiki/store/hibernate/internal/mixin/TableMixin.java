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

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link Table} APIs restored in the patched Hibernate.
 *
 * @version $Id$
 */
@Mixin(target = Table.class)
@SuppressWarnings({"rawtypes", "deprecation", "removal"})
public final class TableMixin
{
    private TableMixin()
    {
    }

    /**
     * @param self the instance
     * @return an iterator over the result of {@code getColumns()}
     * @deprecated use {@code getColumns()} instead
     */
    @Deprecated
    public static Iterator<Column> getColumnIterator(Table self)
    {
        return self.getColumns().iterator();
    }
    /**
     * @param self the instance
     * @return an iterator over the result of {@code getIndexes().values()}
     * @deprecated use {@code getIndexes().values()} instead
     */
    @Deprecated
    public static Iterator<Index> getIndexIterator(Table self)
    {
        return self.getIndexes().values().iterator();
    }
    /**
     * @param self the instance
     * @return an iterator over the result of {@code getForeignKeys().values()}
     * @deprecated use {@code getForeignKeys().values()} instead
     */
    @Deprecated
    public static Iterator<ForeignKey> getForeignKeyIterator(Table self)
    {
        return self.getForeignKeys().values().iterator();
    }
    /**
     * @param self the instance
     * @return an iterator over the result of {@code getUniqueKeys().values()}
     * @deprecated use {@code getUniqueKeys().values()} instead
     */
    @Deprecated
    public static Iterator<UniqueKey> getUniqueKeyIterator(Table self)
    {
        return self.getUniqueKeys().values().iterator();
    }
}
