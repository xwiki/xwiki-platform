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
package org.xwiki.vfs.internal.attach;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.url.ExtendedURL;
import org.xwiki.vfs.internal.AbstractVfsResourceReferenceSerializer;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Converts a {@link VfsResourceReference} using the {@code attach} scheme into a relative {@link ExtendedURL}
 * (with the Context Path added). The attachment reference is made absolute.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Named("attach")
@Singleton
public class AttachVfsResourceReferenceSerializer extends AbstractVfsResourceReferenceSerializer
{
    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentResolver;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    @Override
    protected URI makeAbsolute(URI uri)
    {
        AttachmentReference attachmentReference = this.attachmentResolver.resolve(uri.getSchemeSpecificPart());
        String scheme = uri.getScheme();
        return URI.create(String.format("%s:%s", scheme, this.entitySerializer.serialize(attachmentReference)));
    }
}
