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

import java.util.Set;

import org.xwiki.component.annotation.Role;

/**
 * This interface encapsulates methods for the management of search queries and is similar to JCR's
 * <a href="http://www.day.com/maven/jsr170/javadocs/jcr-2.0/javax/jcr/query/QueryManager.html">QueryManager</a>.
 *
 * @version $Id$
 * @since 1.6M1
 */
@Role
public interface QueryManager
{
    /**
     * Create query for given statement and language. Use createQuery("statement", Query.LANGUAGE). For example:
     * createQuery("select doc.name from XWikiDocument doc", Query.HQL).
     *
     * @param statement query statement.
     * @param language language of the query. Must be one of {@link #getLanguages()}. Use {@link Query}.LANGUAGE for
     * indication.
     * @return a Query object.
     * @throws QueryException if language is not supported
     */
    Query createQuery(String statement, String language) throws QueryException;

    /**
     * @param queryName name of named query.
     * @return Query object.
     * @throws QueryException if there is no query with that name
     */
    Query getNamedQuery(String queryName) throws QueryException;

    /**
     * @return supported languages.
     */
    Set<String> getLanguages();

    /**
     * @param language language to check.
     * @return is language supported.
     */
    boolean hasLanguage(String language);
}
