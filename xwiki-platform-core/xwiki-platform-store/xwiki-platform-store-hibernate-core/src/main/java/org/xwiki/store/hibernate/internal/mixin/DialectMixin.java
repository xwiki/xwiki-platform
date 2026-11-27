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

import org.hibernate.dialect.Dialect;
import org.xwiki.store.hibernate.internal.legacy.LegacyDdlTypes;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link Dialect} APIs restored in the patched Hibernate.
 *
 * @version $Id$
 */
@Mixin(target = Dialect.class)
public final class DialectMixin
{
    private DialectMixin()
    {
    }

    /**
     * @param self the instance
     * @param code the JDBC type code (see {@link java.sql.Types})
     * @return the database type associated with the type code, whatever its size
     * @deprecated use the {@code DdlTypeRegistry} of the {@code TypeConfiguration} instead
     */
    @Deprecated
    public static String getTypeName(Dialect self, int code)
    {
        return LegacyDdlTypes.getTypeName(self, code);
    }

    /**
     * @param self the instance
     * @param code the JDBC type code (see {@link java.sql.Types})
     * @param length the length of the column
     * @param precision the precision of the column
     * @param scale the scale of the column
     * @return the database type associated with the type code and the size
     * @deprecated use the {@code DdlTypeRegistry} of the {@code TypeConfiguration} instead
     */
    @Deprecated
    public static String getTypeName(Dialect self, int code, long length, int precision, int scale)
    {
        return LegacyDdlTypes.getTypeName(self, code, length, precision, scale);
    }
}
