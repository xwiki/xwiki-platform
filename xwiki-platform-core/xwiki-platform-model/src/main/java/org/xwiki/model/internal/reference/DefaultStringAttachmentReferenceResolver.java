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
package org.xwiki.model.internal.reference;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Specialized version of {@link org.xwiki.model.reference.EntityReferenceResolver} which can be considered a helper
 * component to resolve {@link AttachmentReference} objects from their string representation. This implementation uses
 * fixed default values when parts of the Reference are missing in the string representation. Default values are
 * retrieved from the {@link org.xwiki.model.ModelConfiguration} class.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@Component
@Singleton
public class DefaultStringAttachmentReferenceResolver implements AttachmentReferenceResolver<String>
{
    /**
     * Default entity reference resolver used for resolution.
     */
    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Override
    public AttachmentReference resolve(String attachmentReferenceRepresentation, Object... parameters)
    {
        return new AttachmentReference(this.entityReferenceResolver.resolve(
            attachmentReferenceRepresentation, EntityType.ATTACHMENT, parameters));
    }
}
