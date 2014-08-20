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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Reference to a cryptographic store in the wiki.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Unstable
public class WikiStoreReference implements StoreReference
{
    private EntityReference reference;

    /**
     * Wrap a document reference or a space reference as a store reference.
     * @param reference the reference to a document or a space.
     */
    public WikiStoreReference(EntityReference reference) {
        if (reference.getType() != EntityType.DOCUMENT && reference.getType() != EntityType.SPACE) {
            throw new IllegalArgumentException("Certificates could only be stored into document or space.");
        }
        this.reference = reference;
    }

    /**
     * @return the wrapped entity reference.
     */
    public EntityReference getReference() {
        return reference;
    }
}
