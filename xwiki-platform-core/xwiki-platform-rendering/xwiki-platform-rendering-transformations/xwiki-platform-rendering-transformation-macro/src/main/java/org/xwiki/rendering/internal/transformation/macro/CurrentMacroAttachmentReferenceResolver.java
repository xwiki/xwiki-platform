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
package org.xwiki.rendering.internal.transformation.macro;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * A {@link CurrentMacroEntityReferenceResolver} specialized for {@link AttachmentReference}s.
 * 
 * @version $Id$
 * @since 5.0M1
 * @see CurrentMacroEntityReferenceResolver
 */
@Component
@Named("macro")
@Singleton
public class CurrentMacroAttachmentReferenceResolver implements AttachmentReferenceResolver<String>
{
    /**
     * The generic resolver.
     */
    @Inject
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @Override
    public AttachmentReference resolve(String attachmentReferenceRepresentation, Object... parameters)
    {
        return new AttachmentReference(macroEntityReferenceResolver.resolve(attachmentReferenceRepresentation,
            EntityType.ATTACHMENT, parameters));
    }
}
