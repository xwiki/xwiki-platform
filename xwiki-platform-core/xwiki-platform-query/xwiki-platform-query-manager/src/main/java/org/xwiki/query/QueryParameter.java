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
package org.xwiki.query;

/**
 * Represents a parameter of a Query. This is providing an API to represent a query parameter as made of several parts:
 * <ul>
 *   <li>literal parts: those are constructed by calling {@link #literal(String)} and their characters will be escaped
 *       when the Query will executed (i.e. {@code %}, {@code _} and {@code !} characters will get escaped)</li>
 *   <li>special SQL characters: constructed by calling {@link #anyChars()} and {@link #anychar()} which correspond
 *       respectively to the {@code %} and {@code _} SQL characters.</li>
 * </ul>
 * Example:
 * <pre>{@code
 * #set(xwql = "WHERE doc.fullName LIKE :space AND doc.fullName <> :fullName")
 * #set ($query = $services.query.xwql(xwql))
 * #set ($query = $query.bindValue('space').literal($doc.space).literal('.').anyChars().query())
 * }</pre>
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public interface QueryParameter
{
    /**
     * @param literal the characters that will get escaped at query execution time
     * @return this object, in order to make the API fluent
     */
    QueryParameter literal(String literal);

    /**
     * Append a {@code _} character that will not be escaped.
     *
     * @return this object, in order to make the API fluent
     */
    QueryParameter anychar();

    /**
     * Append a {@code %} character that will not be escaped.
     *
     * @return this object, in order to make the API fluent
     */
    QueryParameter anyChars();

    /**
     * @return the associated query to keep the API fluent
     */
    Query query();
}
