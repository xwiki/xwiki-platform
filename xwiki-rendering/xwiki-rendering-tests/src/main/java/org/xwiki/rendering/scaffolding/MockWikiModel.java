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

import org.apache.commons.lang.StringUtils;
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
        DefaultComponentDescriptor<WikiModel> componentDescriptor = 
            new DefaultComponentDescriptor<WikiModel>();

        componentDescriptor.setRole(WikiModel.class);
        componentDescriptor.setImplementation(MockWikiModel.class);

        return componentDescriptor;
    }
    
    /**
     * {@inheritDoc}
     * @see WikiModel#getAttachmentURL(String, String)
     */
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        return "/xwiki/bin/download/" + (documentName == null ? "currentdoc" : documentName.replace(".", "/")) + "/"
            + attachmentName;
    }

    /**
     * {@inheritDoc}
     * @see WikiModel#getDocumentEditURL(String, String, String)
     */
    public String getDocumentEditURL(String documentName, String anchor, String queryString)
    {
        String result =
            "/xwiki/bin/edit/" + (StringUtils.isBlank(documentName) ? "currentdoc" : documentName.replace(".", "/"));
        if (anchor != null) {
            result = result + "#" + anchor;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        } else {
            result = result + "?parent=xwiki%3ASpace.Page";
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @see WikiModel#getDocumentViewURL(String, String, String)
     */
    public String getDocumentViewURL(String documentName, String anchor, String queryString)
    {
        String result =
            "/xwiki/bin/view/" + (StringUtils.isBlank(documentName) ? "currentdoc" : documentName.replace(".", "/"));
        if (anchor != null) {
            result = result + "#" + anchor;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @see WikiModel#isDocumentAvailable(String)
     */
    public boolean isDocumentAvailable(String documentName)
    {
        return documentName.equals("Space.ExistingPage");
    }
}
