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
package org.xwiki.query.hql.internal;

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.query.QueryException;

/**
 * A component in charge of validating a passed HQL statement.
 * 
 * @version $Id$
 * @since 17.0.0RC1
 * @since 16.10.2
 * @since 15.10.16
 * @since 16.4.6
 */
@Role
public interface HQLCompleteStatementValidator
{
    /**
     * @param statement the HQL statement to validate
     * @return {@link Boolean#TRUE} if the passed query is safe, {@link Boolean#FALSE} if it's not and
     *         {@link Optional#empty()} if unknown.
     * @throws QueryException when failing the validate the query
     */
    Optional<Boolean> isSafe(String statement) throws QueryException;
}
