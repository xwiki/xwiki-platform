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

import org.xwiki.store.hibernate.internal.legacy.LegacyBasicType;

/**
 * The Hibernate 5 type of {@link StandardBasicTypes#TRUE_FALSE}, restored for the code compiled against Hibernate 5
 * which passes it to the APIs accepting a {@link Type} (like {@code Query#setParameter(String, Object, Type)} or
 * {@code NativeQuery#addScalar(String, Type)}).
 *
 * @version $Id$
 * @since 19.0.0RC1
 * @deprecated use {@link StandardBasicTypes#TRUE_FALSE} instead
 */
@Deprecated(since = "19.0.0RC1")
public class TrueFalseType extends LegacyBasicType<Boolean>
{
    /**
     * The unique instance of the type.
     */
    public static final TrueFalseType INSTANCE = new TrueFalseType();

    /**
     * Use {@link #INSTANCE}.
     */
    public TrueFalseType()
    {
        super(StandardBasicTypes.TRUE_FALSE);
    }
}
