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
package org.xwiki.refactoring.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Interface used to refactor document links.
 * 
 * @version $Id$
 * @since 7.4M2
 */
@Role
public interface LinkRefactoring
{
    /**
     * Renames the links from the content of the specified document.
     * 
     * @param documentReference the document whose content needs to be updated
     * @param oldLinkTarget the link target that needs to be replaced
     * @param newLinkTarget the new link target
     */
    void renameLinks(DocumentReference documentReference, DocumentReference oldLinkTarget,
        DocumentReference newLinkTarget);

    /**
     * Updates the relative links from the content of a document after it has been renamed or moved. This ensures that
     * the links from the content of the renamed/moved document are relative to the new reference.
     * 
     * @param oldReference the document reference before the rename/move
     * @param newReference the document reference after the rename/move
     */
    void updateRelativeLinks(DocumentReference oldReference, DocumentReference newReference);
}
