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

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.type.MappingContext;
import org.xwiki.store.hibernate.internal.patcher.BinaryOnly;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link Column} APIs restored in the patched Hibernate.
 *
 * @version $Id$
 */
@Mixin(target = Column.class)
@SuppressWarnings({"rawtypes", "deprecation", "removal"})
public final class ColumnMixin
{
    private ColumnMixin()
    {
    }

    /**
     * Hibernate 5 {@code Column#getLength()} returned an {@code int}.
     *
     * @param self the instance
     * @return the length of the column
     */
    @BinaryOnly("getLength")
    public static int getIntLength(Column self)
    {
        Long length = self.getLength();

        return length != null ? length.intValue() : (int) Size.DEFAULT_LENGTH;
    }

    /**
     * Hibernate 5 {@code Column#getPrecision()} returned an {@code int}.
     *
     * @param self the instance
     * @return the precision of the column
     */
    @BinaryOnly("getPrecision")
    public static int getIntPrecision(Column self)
    {
        Integer precision = self.getPrecision();

        return precision != null ? precision : Size.DEFAULT_PRECISION;
    }

    /**
     * Hibernate 5 {@code Column#getScale()} returned an {@code int}.
     *
     * @param self the instance
     * @return the scale of the column
     */
    @BinaryOnly("getScale")
    public static int getIntScale(Column self)
    {
        Integer scale = self.getScale();

        return scale != null ? scale : Size.DEFAULT_SCALE;
    }

    /**
     * Hibernate 5 {@code Column#setLength(int)}.
     *
     * @param self the instance
     * @param length the length of the column
     */
    @BinaryOnly("setLength")
    public static void setIntLength(Column self, int length)
    {
        self.setLength(Long.valueOf(length));
    }

    /**
     * Hibernate 5 {@code Column#setPrecision(int)}.
     *
     * @param self the instance
     * @param precision the precision of the column
     */
    @BinaryOnly("setPrecision")
    public static void setIntPrecision(Column self, int precision)
    {
        self.setPrecision(Integer.valueOf(precision));
    }

    /**
     * Hibernate 5 {@code Column#setScale(int)}.
     *
     * @param self the instance
     * @param scale the scale of the column
     */
    @BinaryOnly("setScale")
    public static void setIntScale(Column self, int scale)
    {
        self.setScale(Integer.valueOf(scale));
    }

    /**
     * Hibernate 5 {@code Column#getSqlTypeCode(Mapping)}.
     *
     * @param self the instance
     * @param mapping the mapping information
     * @return the JDBC type code of the column
     */
    @BinaryOnly("getSqlTypeCode")
    public static int getMappingSqlTypeCode(Column self, Mapping mapping)
    {
        return self.getSqlTypeCode((MappingContext) mapping);
    }

    /**
     * @param self the instance
     * @param dialect the dialect (the one of the metadata is used)
     * @param mapping the mapping information, a {@link Metadata}
     * @return the database type of the column
     * @deprecated use {@code getSqlType(Metadata)} instead
     */
    @Deprecated
    public static String getSqlType(Column self, Dialect dialect, Mapping mapping)
    {
        return self.getSqlType((Metadata) mapping);
    }
}
