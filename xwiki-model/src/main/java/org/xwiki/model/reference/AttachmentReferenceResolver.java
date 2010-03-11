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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Resolve an Attachment reference defined in a given representation into a validated {@link AttachmentReference}
 * object, ie with valid values and a valid hierarchy (eg an Attachment reference must have a parent which is a
 * document reference, reference values must not be null, etc).
 *
 * @version $Id$
 * @since 2.2M1
 * @param <T> the type of the representation (eg a String)
 */
@ComponentRole
public interface AttachmentReferenceResolver<T>
{
    /**
     * @param attachmentReferenceRepresentation the representation of an attachment reference (eg as a String)
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the valid resolved attachment reference as an Object
     */
    AttachmentReference resolve(T attachmentReferenceRepresentation, Object... parameters);
}
