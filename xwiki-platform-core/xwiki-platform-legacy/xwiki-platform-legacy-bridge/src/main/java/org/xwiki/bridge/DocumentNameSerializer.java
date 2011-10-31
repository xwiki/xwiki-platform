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
package org.xwiki.bridge;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Generate a fully qualified document reference string (ie of the form
 * {@code wiki:space.page}) out of a {@link DocumentName}.
 * 
 * @version $Id$
 * @since 1.8.1
 * @deprecated use {@link org.xwiki.model.reference.EntityReferenceSerializer} instead since 2.2M1
 */
@ComponentRole
@Deprecated
public interface DocumentNameSerializer
{
    /**
     * @param documentName the document name to serialize
     * @return the fully qualified document reference string (ie of the form {@code wiki:space.page})
     */
    String serialize(DocumentName documentName);
}
