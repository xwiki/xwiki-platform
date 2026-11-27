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
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backward compatibility for the mappings of properties with the {@code text} type and a length lower than
 * {@link #TEXT_LENGTH}: their columns keep the large character type of the database (for example {@code longtext}
 * with MySQL and {@code text} with PostgreSQL), whatever their length.
 * <p>
 * Hibernate 7 handles {@link SqlTypes#LONGVARCHAR} like {@link SqlTypes#VARCHAR}, so a property mapped with
 * {@code type="text" length="60000"} would get a {@code text} column (limited to 65535 bytes) with MySQL and a
 * {@code varchar(60000)} one with PostgreSQL, and a smaller length would give a {@code varchar} column, which counts in
 * the maximum size of a MySQL row. The {@code text} type is often used with such lengths precisely to get a large
 * character type, and the existing databases contain this type.
 * <p>
 * The mappings using these lengths are deprecated (a warning is logged for each column when the session factory is
 * created): the properties which are not meant to be limited should use the length {@link #TEXT_LENGTH}, which gives
 * the large character types with both Hibernate 5 and Hibernate 7.
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
// TODO: decide on the forum whether this backward compatibility is kept, and until when
public class LongTextTypeContributor implements TypeContributor, Integrator
{
    /**
     * The length of the {@code text} properties which are not meant to be limited ({@link Length#LONG32}, documented
     * by Hibernate as giving the large character types of the databases).
     */
    public static final long TEXT_LENGTH = Length.LONG32;

    private static final Logger LOGGER = LoggerFactory.getLogger(LongTextTypeContributor.class);

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

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
        SessionFactoryImplementor sessionFactory)
    {
        if (metadata.getDatabase().getTypeConfiguration().getDdlTypeRegistry()
            .getDescriptor(SqlTypes.LONGVARCHAR) instanceof LongTextDdlType) {
            for (Table table : metadata.collectTableMappings()) {
                for (Column column : table.getColumns()) {
                    if (isDeprecatedTextColumn(column, metadata)) {
                        LOGGER.warn("Deprecated length [{}] for the column [{}] of the table [{}] mapped with the type"
                            + " [text]: the column gets the large character type of the database whatever its length."
                            + " Use the length [{}] for a property which is not meant to be limited.",
                            column.getLength(), column.getName(), table.getName(), TEXT_LENGTH);
                    }
                }
            }
        }
    }

    private static boolean isDeprecatedTextColumn(Column column, Metadata metadata)
    {
        try {
            return column.getSqlTypeCode(metadata) == SqlTypes.LONGVARCHAR && isDeprecatedLength(column.getLength());
        } catch (MappingException e) {
            // Not a column with a basic type
            return false;
        }
    }

    private static boolean isDeprecatedLength(Long length)
    {
        return length == null || length < TEXT_LENGTH;
    }

    /**
     * The {@link SqlTypes#VARCHAR} type of the dialect, with the length {@link LongTextTypeContributor#TEXT_LENGTH}
     * when the length is lower.
     */
    private static final class LongTextDdlType implements DdlType
    {
        private final DdlType varchar;

        private final String typeName;

        LongTextDdlType(DdlType varchar)
        {
            this.varchar = varchar;
            this.typeName = varchar.getTypeName(TEXT_LENGTH, null, null);
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
            return isDeprecatedLength(size) ? this.typeName : this.varchar.getTypeName(size, precision, scale);
        }

        @Override
        public boolean isLob(Size size)
        {
            return this.varchar.isLob(isDeprecatedLength(size.getLength()) ? Size.length(TEXT_LENGTH) : size);
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
