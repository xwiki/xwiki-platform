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
package org.xwiki.crypto.store;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

/**
 * Store and retrieve signatures of entities.
 *
 * @version $Id$
 * @since 6.0
 */
@Role
public interface SignatureStore
{
    /**
     * Store a provided signature for a given entity.
     *
     * @param entity the entity that the signature relate to.
     * @param signature the signature to store.
     * @throws SignatureStoreException on error.
     */
    void store(EntityReference entity, byte[] signature) throws SignatureStoreException;

    /**
     * Retrieve the signature for a given entity.
     *
     * @param entity the entity for which a signature is requested.
     * @return the signature corresponding to the entity, or null if none have been found.
     * @throws SignatureStoreException on error.
     */
    byte[] retrieve(EntityReference entity) throws SignatureStoreException;
}
