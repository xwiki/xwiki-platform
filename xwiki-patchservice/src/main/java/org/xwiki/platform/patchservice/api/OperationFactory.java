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
 *
 */
package org.xwiki.platform.patchservice.api;

import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;

/**
 * A helper class for managing implementations for different {@link Operation} implementations.
 * 
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface OperationFactory
{
    /**
     * Retrieve a new {@link RWOperation} object implementing a specific operation type.
     * 
     * @param type The operation type, should be one of the constants defined in {@link Operation}.
     * @return An object implementing that operation type.
     * @throws XWikiException If no class is registered for that operation type, or if the registered class cannot be
     *             instantiated.
     */
    RWOperation newOperation(String type) throws XWikiException;

    /**
     * Load an Operation object from an XML export.
     * 
     * @param e The XML DOM Element containing an exported {@link Operation}.
     * @return An Operation object.
     * @throws XWikiException If the XML does not contain a valid object, or the object cannot be properly created.
     */
    Operation loadOperation(Element e) throws XWikiException;
}
