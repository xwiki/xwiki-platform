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
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * The Hibernate 5 type mapping an enum, removed from Hibernate 7 and restored for the {@code hbm.xml} mappings which
 * declare it as a custom type, for example:
 *
 * <pre>
 * {@code
 * <property name="state" column="STATE">
 *   <type name="org.hibernate.type.EnumType">
 *     <param name="enumClass">org.example.State</param>
 *   </type>
 * </property>
 * }
 * </pre>
 * <p>
 * Supported parameters, with the same meaning as in Hibernate 5:
 * <ul>
 * <li>{@value #ENUM} (required): the fully qualified name of the enum class</li>
 * <li>{@value #NAMED}: {@code true} to store the name of the enum constant in a {@code VARCHAR} column, {@code false}
 * (the default) to store its ordinal in an {@code INTEGER} column</li>
 * <li>{@value #TYPE}: the {@link Types JDBC type} of the column, a character type to store the name and a numeric
 * type to store the ordinal; ignored when {@value #NAMED} is set</li>
 * </ul>
 *
 * @param <T> the type of the enum
 * @version $Id$
 * @since 19.0.0RC1
 * @deprecated let Hibernate infer the mapping of the enum from the type of the property, with {@code type="string"}
 *             (or the {@link jakarta.persistence.Enumerated} annotation) to store the name instead of the ordinal
 */
@Deprecated(since = "19.0.0RC1")
public class EnumType<T extends Enum<T>> implements UserType<T>, ParameterizedType, Serializable
{
    /**
     * The name of the parameter indicating the enum class.
     */
    public static final String ENUM = "enumClass";

    /**
     * The name of the parameter indicating if the name of the enum constant is stored instead of its ordinal.
     */
    public static final String NAMED = "useNamed";

    /**
     * The name of the parameter indicating the JDBC type of the column.
     */
    public static final String TYPE = "type";

    private static final long serialVersionUID = 1L;

    private Class<T> enumClass;

    private boolean named;

    private int sqlType = Types.INTEGER;

    @Override
    @SuppressWarnings("unchecked")
    public void setParameterValues(Properties parameters)
    {
        String enumClassName = parameters.getProperty(ENUM);
        if (enumClassName == null) {
            throw new MappingException("Missing the [" + ENUM + "] parameter of the enum type");
        }
        try {
            this.enumClass = (Class<T>) Class.forName(enumClassName, false, getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new MappingException("Enum class not found: " + enumClassName, e);
        }

        String namedParameter = parameters.getProperty(NAMED);
        String typeParameter = parameters.getProperty(TYPE);
        if (namedParameter != null) {
            this.named = Boolean.parseBoolean(namedParameter);
            this.sqlType = this.named ? Types.VARCHAR : Types.INTEGER;
        } else if (typeParameter != null) {
            this.sqlType = Integer.decode(typeParameter);
            this.named = isCharacterType(this.sqlType);
            if (!this.named && !isNumericType(this.sqlType)) {
                throw new MappingException(
                    "Unsupported JDBC type [" + this.sqlType + "] for the enum type of [" + enumClassName + "]");
            }
        }
    }

    private static ClassLoader getClassLoader()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        return classLoader != null ? classLoader : EnumType.class.getClassLoader();
    }

    private static boolean isCharacterType(int jdbcType)
    {
        return switch (jdbcType) {
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> true;
            default -> false;
        };
    }

    private static boolean isNumericType(int jdbcType)
    {
        return switch (jdbcType) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT, Types.NUMERIC, Types.DECIMAL -> true;
            default -> false;
        };
    }

    @Override
    public int getSqlType()
    {
        return this.sqlType;
    }

    @Override
    public Class<T> returnedClass()
    {
        return this.enumClass;
    }

    @Override
    public T nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException
    {
        if (this.named) {
            String name = rs.getString(position);

            return name != null ? Enum.valueOf(this.enumClass, name.trim()) : null;
        }

        int ordinal = rs.getInt(position);

        return rs.wasNull() ? null : this.enumClass.getEnumConstants()[ordinal];
    }

    @Override
    public void nullSafeSet(PreparedStatement st, T value, int index, WrapperOptions options) throws SQLException
    {
        if (value == null) {
            st.setNull(index, this.sqlType);
        } else if (this.named) {
            st.setString(index, value.name());
        } else {
            st.setInt(index, value.ordinal());
        }
    }

    @Override
    public boolean equals(T x, T y)
    {
        return x == y;
    }

    @Override
    public int hashCode(T x)
    {
        return Objects.hashCode(x);
    }

    @Override
    public T deepCopy(T value)
    {
        // Enum constants are immutable
        return value;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }
}
