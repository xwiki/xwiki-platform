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
package org.xwiki.model.reference;

import org.xwiki.model.EntityType;

/**
 * Represents a reference to a document in the current wiki.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class LocalDocumentReference extends EntityReference
{
    /**
     * Create a new Document reference in the current wiki.
     *
     * @param spaceName the name of the space containing the document, must not be null
     * @param pageName the name of the document, must not be null
     */
    public LocalDocumentReference(String spaceName, String pageName)
    {
        super(pageName, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE));
    }
}
