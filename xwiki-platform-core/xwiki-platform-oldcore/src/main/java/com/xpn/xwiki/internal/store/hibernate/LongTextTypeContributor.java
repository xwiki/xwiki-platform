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
package com.xpn.xwiki.internal.store.hibernate;

import org.hibernate.Length;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Make the columns of the properties mapped with the {@code text} type use the long character type of the database
 * (for example {@code longtext} with MySQL and {@code text} with PostgreSQL), whatever their length.
 * <p>
 * Hibernate 7 handles {@link SqlTypes#LONGVARCHAR} like {@link SqlTypes#VARCHAR}, so a property mapped with
 * {@code type="text" length="60000"} would get a {@code mediumtext} column with MySQL and a {@code varchar(60000)} one
 * with PostgreSQL. Since the length of these columns was ignored until Hibernate 6, the existing databases contain the
 * long type, and the lengths in the mappings are not meant to limit the size of the columns (several of them could
 * not even fit in a MySQL row as {@code varchar}).
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
public class LongTextTypeContributor implements TypeContributor
{
    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry)
    {
        DdlTypeRegistry registry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

        // Only when the dialect does not have its own type for LONGVARCHAR
        DdlType varchar = registry.getDescriptor(SqlTypes.VARCHAR);
        if (varchar != null && registry.getDescriptor(SqlTypes.LONGVARCHAR) == varchar) {
            registry.addDescriptor(new LongTextDdlType(varchar));
        }
    }

    /**
     * The {@link SqlTypes#VARCHAR} type of the dialect, always with the longest length.
     */
    private static final class LongTextDdlType implements DdlType
    {
        private static final long LENGTH = Length.LONG32;

        private final DdlType varchar;

        private final String typeName;

        LongTextDdlType(DdlType varchar)
        {
            this.varchar = varchar;
            this.typeName = varchar.getTypeName(LENGTH, null, null);
        }

        @Override
        public int getSqlTypeCode()
        {
            return SqlTypes.LONGVARCHAR;
        }

        @Override
        public String getRawTypeName()
        {
            return this.typeName;
        }

        @Override
        public String[] getRawTypeNames()
        {
            return new String[] { this.typeName };
        }

        @Override
        public String getTypeName(Long size, Integer precision, Integer scale)
        {
            return this.typeName;
        }

        @Override
        public boolean isLob(Size size)
        {
            return this.varchar.isLob(Size.length(LENGTH));
        }

        @Override
        public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType)
        {
            return this.varchar.getCastTypeName(jdbcType, javaType);
        }

        @Override
        public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType, Long length, Integer precision,
            Integer scale)
        {
            return this.varchar.getCastTypeName(jdbcType, javaType, length, precision, scale);
        }
    }
}
