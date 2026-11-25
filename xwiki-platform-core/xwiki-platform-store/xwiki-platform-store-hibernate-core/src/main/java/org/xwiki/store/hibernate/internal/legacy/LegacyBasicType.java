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
package org.xwiki.store.hibernate.internal.legacy;

import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Base class of the Hibernate 5 types restored in the patched Hibernate (like {@code org.hibernate.type.StringType}),
 * each one associated with the corresponding constant of {@link org.hibernate.type.StandardBasicTypes}.
 * <p>
 * The restored types are working types on their own, but the APIs accepting them resolve the actual type from the
 * type configuration of the session factory (see {@link #resolve(TypeConfiguration)}), since some of them rely on a
 * converter (like {@code YesNoType}).
 *
 * @param <J> the Java type of the values
 * @version $Id$
 * @since 19.0.0RC1
 */
public abstract class LegacyBasicType<J> extends BasicTypeImpl<J>
{
    private static final long serialVersionUID = 1L;

    /**
     * Used to resolve the types outside of any session factory (when the restored type constants are initialized).
     */
    private static final TypeConfiguration TYPE_CONFIGURATION = new TypeConfiguration();

    private final transient BasicTypeReference<J> reference;

    /**
     * @param reference the Hibernate 7 reference of the type
     */
    protected LegacyBasicType(BasicTypeReference<J> reference)
    {
        this(reference, TYPE_CONFIGURATION.getBasicTypeRegistry().resolve(reference));
    }

    private LegacyBasicType(BasicTypeReference<J> reference, BasicType<J> type)
    {
        super(type.getJavaTypeDescriptor(), type.getJdbcType());

        this.reference = reference;
    }

    /**
     * @return the Hibernate 7 reference of the type
     */
    public BasicTypeReference<J> getReference()
    {
        return this.reference;
    }

    /**
     * @param typeConfiguration the type configuration of the session factory
     * @return the type registered in the passed configuration for the reference of this type
     */
    public BasicType<J> resolve(TypeConfiguration typeConfiguration)
    {
        return typeConfiguration.getBasicTypeRegistry().resolve(this.reference);
    }
}
