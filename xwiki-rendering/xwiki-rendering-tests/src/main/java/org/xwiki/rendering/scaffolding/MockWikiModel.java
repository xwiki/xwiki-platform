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
package org.xwiki.rendering.scaffolding;

import java.util.Map;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Mock WikiModel implementation for integration tests.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class MockWikiModel implements WikiModel
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor<WikiModel> getComponentDescriptor()
    {
        DefaultComponentDescriptor<WikiModel> componentDescriptor = new DefaultComponentDescriptor<WikiModel>();

        componentDescriptor.setRole(WikiModel.class);
        componentDescriptor.setImplementation(MockWikiModel.class);

        return componentDescriptor;
    }

    /**
     * {@inheritDoc}
     *
     * @see WikiModel#getAttachmentURL(org.xwiki.rendering.listener.reference.ResourceReference)
     * @since 2.5RC1
     */
    public String getAttachmentURL(ResourceReference attachmentReference)
    {
        String queryString = attachmentReference.getParameter(AttachmentResourceReference.QUERY_STRING);
        return "attachmenturl" + (queryString != null ? "?" + queryString : "");
    }

    /**
     * {@inheritDoc}
     *
     * @see WikiModel#getImageURL(org.xwiki.rendering.listener.reference.ResourceReference , java.util.Map)
     */
    public String getImageURL(ResourceReference attachmentReference, Map<String, String> parameters)
    {
        return getAttachmentURL(attachmentReference);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#getDocumentEditURL(org.xwiki.rendering.listener.reference.ResourceReference)
     */
    public String getDocumentEditURL(ResourceReference documentReference)
    {
        return "editurl";
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#getDocumentViewURL(org.xwiki.rendering.listener.reference.ResourceReference)
     */
    public String getDocumentViewURL(ResourceReference documentReference)
    {
        String queryString = documentReference.getParameter(DocumentResourceReference.QUERY_STRING);
        String anchor = documentReference.getParameter(DocumentResourceReference.ANCHOR);
        return "viewurl" + (queryString != null ? "?" + queryString : "") + (anchor != null ? "#" + anchor : "");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#isDocumentAvailable(org.xwiki.rendering.listener.reference.ResourceReference)
     */
    public boolean isDocumentAvailable(ResourceReference documentReference)
    {
        return "Space.ExistingPage".equals(documentReference.getReference());
    }
}
