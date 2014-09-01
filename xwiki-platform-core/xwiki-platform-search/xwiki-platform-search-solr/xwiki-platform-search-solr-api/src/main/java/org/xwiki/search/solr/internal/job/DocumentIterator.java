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
package org.xwiki.search.solr.internal.job;

import java.util.Iterator;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Iterates the documents from a store.
 * 
 * @version $Id$
 * @param <T> the type of data used to determine if a document is up to date
 * @since 5.4.5
 */
@Role
public interface DocumentIterator<T> extends Iterator<Pair<DocumentReference, T>>
{
    /**
     * Limit the iterator to the specified entity (e.g. a wiki or a space). If the passed reference is {@code null} (or
     * if you don't call this method) then all the documents from the underlying store are iterated. Otherwise, only the
     * documents that correspond to the specified entity are returned.
     * 
     * @param rootReference specifies the root entity (e.g. a wiki or a space) whose documents should be iterated
     */
    void setRootReference(EntityReference rootReference);

    /**
     * @return estimate the size of the iterated store for showing progress information
     */
    long size();
}
