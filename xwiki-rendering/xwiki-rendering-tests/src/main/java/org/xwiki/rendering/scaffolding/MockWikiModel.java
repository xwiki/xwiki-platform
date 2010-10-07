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
     * @see WikiModel#getAttachmentURL(String, String)
     * @deprecated since 2.5RC1 use {@link #getAttachmentURL(String)} instead
     */
    @Deprecated
    public String getAttachmentURL(String documentReference, String attachmentName)
    {
        return "attachmenturl";
    }

    /**
     * {@inheritDoc}
     *
     * @see WikiModel#getAttachmentURL(String)
     * @since 2.5RC1
     */
    public String getAttachmentURL(String attachmentReference)
    {
        return "attachmenturl";
    }

    /**
     * {@inheritDoc}
     *
     * @see WikiModel#getImageURL(String, java.util.Map)
     */
    public String getImageURL(String attachmentReference, Map<String, String> parameters)
    {
        return getAttachmentURL(attachmentReference);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#getDocumentEditURL(String, String, String)
     */
    public String getDocumentEditURL(String documentReference, String anchor, String queryString)
    {
        return "editurl";
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#getDocumentViewURL(String, String, String)
     */
    public String getDocumentViewURL(String documentReference, String anchor, String queryString)
    {
        return "viewurl" + (queryString != null ? "?" + queryString : "") + (anchor != null ? "#" + anchor : "");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#isDocumentAvailable(String)
     */
    public boolean isDocumentAvailable(String documentReference)
    {
        return "Space.ExistingPage".equals(documentReference);
    }
}
