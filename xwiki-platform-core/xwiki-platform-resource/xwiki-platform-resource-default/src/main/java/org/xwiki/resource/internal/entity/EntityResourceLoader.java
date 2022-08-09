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
package org.xwiki.resource.internal.entity;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.ResourceLoader;
import org.xwiki.resource.entity.EntityResourceReference;

/**
 * Load the content of an EntityResourceReference. Only Attachments are supported at the moment and all other
 * resource reference types return null.
 *
 * @version $Id$
 * @since 14.7RC1
 */
@Component
@Singleton
public class EntityResourceLoader implements ResourceLoader<EntityResourceReference>
{
    @Inject
    private Logger logger;

    @Inject
    private DocumentAccessBridge dab;

    @Override
    public InputStream load(EntityResourceReference reference)
    {
        InputStream result = null;

        EntityReference entityReference = reference.getEntityReference();
        if (EntityType.ATTACHMENT.equals(entityReference.getType())
            || EntityType.PAGE_ATTACHMENT.equals(entityReference.getType()))
        {
            // Return the attachment's content
            try {
                result = this.dab.getAttachmentContent(entityReference);
            } catch (Exception e) {
                // Failed to get the document's content, consider the resource doesn't exist but log a debug error
                // in case it's not normal, and we need to debug it.
                this.logger.debug("Failed to get attachment's content for [{}]", entityReference, e);
            }
        }

        return result;
    }
}
