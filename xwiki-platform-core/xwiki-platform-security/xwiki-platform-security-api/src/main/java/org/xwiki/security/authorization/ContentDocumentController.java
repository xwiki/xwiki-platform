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
 * Interface for changing the security context document from which the content author will be resolved in the
 * authorization context.
 *
 * The contetn document controller must always be used with a try-finally statement to ensure that the document is
 * correctly popped of the security stack.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface ContentDocumentController
{

    /**
     * Set a new document from where the content author will be extracted and set in the authorization context, while
     * maintaing a stack of previous security context entries.
     *
     * @param document The new content document.
     */
    void pushContentDocument(DocumentModelBridge document);

    /**
     * Remove the current content document in the authorization context and restore the previous security stack entry.
     */
    void popContentDocument();

}
