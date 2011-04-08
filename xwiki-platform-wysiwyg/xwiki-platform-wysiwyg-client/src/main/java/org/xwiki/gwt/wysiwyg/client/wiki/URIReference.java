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
package org.xwiki.gwt.wysiwyg.client.wiki;

import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType;

/**
 * A reference to an external entity identified by an URI.
 * 
 * @version $Id$
 */
public class URIReference
{
    /**
     * The component that stores the URI that identifies the reference entity.
     */
    public static final String URI = "uri";

    /**
     * The underlying, untyped, entity reference.
     */
    private final EntityReference entityReference;

    /**
     * Default constructor.
     */
    public URIReference()
    {
        this(new EntityReference());
        entityReference.setType(EntityType.EXTERNAL);
    }

    /**
     * Creates a typed URI reference.
     * 
     * @param entityReference an untyped entity reference
     */
    public URIReference(EntityReference entityReference)
    {
        this.entityReference = entityReference;
    }

    /**
     * Creates a new typed reference that points to the external entity identified by the given URI.
     * 
     * @param uri an URI
     */
    public URIReference(String uri)
    {
        this();
        setURI(uri);
    }

    /**
     * @return the underlying, untyped, entity reference
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    /**
     * @return the URI that identifies the referenced entity
     */
    public String getURI()
    {
        return entityReference.getComponent(URI);
    }

    /**
     * Sets the URI that identifies the referenced entity.
     * 
     * @param uri an URI
     */
    public void setURI(String uri)
    {
        entityReference.setComponent(URI, uri);
    }
}
