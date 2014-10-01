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
package org.xwiki.office.viewer.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.office.viewer.OfficeResourceViewer;
import org.xwiki.office.viewer.OfficeViewer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;

/**
 * Default implementation of {@link org.xwiki.office.viewer.OfficeViewer}.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultOfficeViewer implements OfficeViewer
{
    @Inject
    private OfficeResourceViewer officeViewer;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public XDOM createView(AttachmentReference attachmentReference, Map<String, String> parameters) throws Exception
    {
        String reference = this.serializer.serialize(attachmentReference);

        return this.officeViewer.createView(new AttachmentResourceReference(reference), parameters);
    }
}
