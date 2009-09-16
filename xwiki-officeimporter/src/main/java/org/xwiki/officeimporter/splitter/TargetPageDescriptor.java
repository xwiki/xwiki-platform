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
package org.xwiki.officeimporter.splitter;

import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * Descriptor for specifying a wiki page into which an office document is going to be saved.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class TargetPageDescriptor
{
    /**
     * Name of the target wiki page.
     */
    private DocumentName pageName;

    /**
     * Name of the parent wiki page.
     */
    private DocumentName parentName;

    /**
     * Component manager used to lookup for various name serializers.
     */
    private ComponentManager componentManager;

    /**
     * Creates a new {@link TargetPageDescriptor} instance.
     * 
     * @param pageName name of the target wiki page.
     * @param componentManager used to lookup for various name serializers.
     */
    public TargetPageDescriptor(DocumentName pageName, ComponentManager componentManager)
    {
        this.pageName = pageName;
        this.componentManager = componentManager;
    }

    /**
     * @return target page name.
     */
    public DocumentName getPageName()
    {
        return this.pageName;
    }
    
    /**
     * @return target page name as a string.
     */
    public String getPageNameAsString()
    {
        return serializeDocumentName(pageName, "default");
    }

    /**
     * @return target parent page name.
     */
    public DocumentName getParentName()
    {
       return this.parentName; 
    }
    
    /**
     * @return name of the parent wiki page.
     */
    public String getParentNameAsString()
    {
        return (null != parentName) ? serializeDocumentName(parentName, "default") : null;
    }

    /**
     * Sets the name of the parent wiki page.
     * 
     * @param parentName parent wiki page name.
     */
    public void setParentName(DocumentName parentName)
    {
        this.parentName = parentName;
    }

    /**
     * Utility method for serializing a {@link DocumentName}.
     * 
     * @param documentName document name.
     * @param serializerHint which serializer to use.
     * @return documentName parameter serialized into a string.
     */
    private String serializeDocumentName(DocumentName documentName, String serializerHint)
    {
        try {
            DocumentNameSerializer serializer =
                this.componentManager.lookup(DocumentNameSerializer.class, serializerHint);
            return serializer.serialize(documentName);
        } catch (ComponentLookupException ex) {
            // TODO: put a descriptive comment.
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj)
    {
        boolean equals = false;
        if (obj instanceof TargetPageDescriptor) {
            TargetPageDescriptor other = (TargetPageDescriptor) obj;
            equals = other.getPageName().equals(getPageName());
        }
        return equals;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return getPageName().hashCode();
    }
}
