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
package org.xwiki.refactoring.util;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.stability.Unstable;

/**
 * Utility tool to replace references during rename/move refactoring operations.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Unstable
@Role
public interface ReferenceRenamer
{
    /**
     * @param xdom the {@link XDOM} to modify
     * @param currentDocumentReference the current document reference
     * @param oldTarget the previous reference of the renamed document
     * @param newTarget the new reference of the renamed document
     * @param relative {@code true} if the link should be serialized relatively to the current document
     *      (see {@link org.xwiki.refactoring.internal.LinkRefactoring#updateRelativeLinks})
     * @return true if the passed {@link XDOM} was modified
     */
    boolean renameReferences(XDOM xdom, DocumentReference currentDocumentReference, DocumentReference oldTarget,
        DocumentReference newTarget, boolean relative);
}
