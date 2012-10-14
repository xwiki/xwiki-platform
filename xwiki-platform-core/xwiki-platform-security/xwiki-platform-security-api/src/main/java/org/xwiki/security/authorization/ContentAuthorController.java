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
package org.xwiki.security.authorization;

import org.xwiki.bridge.DocumentModelBridge;

import org.xwiki.component.annotation.Role;

/**
 * Interface for changing the content author in the authorization context.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Role
public interface ContentAuthorController
{

    /**
     * Set a new document from where the content author will be extracted and set in the authorization context, while
     * maintaing a stack of previous content authors.
     *
     * @param document The new content document.
     */
    void pushContentDocument(DocumentModelBridge document);

    /**
     * Remove the current content document in the authorization context and restore the previous one.
     *
     * @return The content document that was removed from the authorization context.
     */
    DocumentModelBridge popContentDocument();

}
