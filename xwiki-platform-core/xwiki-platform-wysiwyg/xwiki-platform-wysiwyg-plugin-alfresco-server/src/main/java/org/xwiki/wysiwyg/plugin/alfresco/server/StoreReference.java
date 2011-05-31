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
 * A reference to an Alfresco repository store.
 * 
 * @version $Id$
 */
public class StoreReference
{
    /** The store identifier. */
    private String id;

    /** The store protocol. E.g. workspace, archive. */
    private String protocol;

    /**
     * Default constructor.
     */
    public StoreReference()
    {
    }

    /**
     * Creates a new instance.
     * 
     * @param protocol the store protocol
     * @param id the store identifier
     */
    public StoreReference(String protocol, String id)
    {
        this.protocol = protocol;
        this.id = id;
    }

    /**
     * @return the store identifier
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the store identifier.
     * 
     * @param id the new store identifier
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the store protocol
     */
    public String getProtocol()
    {
        return protocol;
    }

    /**
     * Sets the store protocol.
     * 
     * @param protocol the new store protocol
     */
    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }
}
