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
package org.xwiki.internal.document;

import org.apache.commons.lang3.function.FailableFunction;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

/**
 * A simple cache for documents.
 *
 * @param <C> the type of the cached value
 * @param <E> the type of the exception that can be thrown by the provider
 * @version $Id$
 */
// We need to use @ComponentRole here as @Role doesn't support generic component implementations, and there is no
// non-deprecated way to achieve this.
@SuppressWarnings({ "java:S1874", "deprecation" })
@ComponentRole
public interface SimpleDocumentCache<C, E extends Throwable>
{
    /**
     * Initialize the cache.
     *
     * @param cacheConfiguration the cache configuration
     * @throws CacheException if the cache couldn’t be created
     */
    void initializeCache(CacheConfiguration cacheConfiguration) throws CacheException;

    /**
     * Get the value associated with the provided document reference, or compute it when missing. The computed valued is
     * stored in the cache.
     *
     * @param documentReference the reference of the document
     * @param provider the provider to compute the value if it isn't in the cache. The document used to compute the
     *     cached value needs to be loaded inside the provider to ensure that no stale data is cached
     * @return the value
     */
    C get(DocumentReference documentReference, FailableFunction<DocumentReference, C, E> provider) throws E;
}
