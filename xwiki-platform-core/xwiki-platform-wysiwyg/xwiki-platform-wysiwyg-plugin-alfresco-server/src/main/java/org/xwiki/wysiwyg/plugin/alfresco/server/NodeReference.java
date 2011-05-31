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
package org.xwiki.wysiwyg.plugin.alfresco.server;

/**
 * A reference to an Alfresco repository node.
 * 
 * @version $Id$
 */
public class NodeReference
{
    /** A reference to the store holding this node. */
    private StoreReference storeReference;

    /** The node identifier. */
    private String id;

    /**
     * Default constructor.
     */
    public NodeReference()
    {
    }

    /**
     * Creates a new instance.
     * 
     * @param id the node identifier
     * @param storeReference the store reference
     */
    public NodeReference(String id, StoreReference storeReference)
    {
        this.id = id;
        this.storeReference = storeReference;
    }

    /**
     * @return the store reference
     */
    public StoreReference getStoreReference()
    {
        return storeReference;
    }

    /**
     * Sets the store reference.
     * 
     * @param storeReference the new store reference
     */
    public void setStoreReference(StoreReference storeReference)
    {
        this.storeReference = storeReference;
    }

    /**
     * @return the node identifier
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the node identifier.
     * 
     * @param id the new node identifier
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return storeReference.getProtocol() + "://" + storeReference.getId() + '/' + id;
    }

    /**
     * @return this node reference as path, to be used in URLs
     */
    public String asPath()
    {
        return storeReference.getProtocol() + '/' + storeReference.getId() + '/' + id;
    }
}
