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
package com.xpn.xwiki.doc.merge;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Allow to define some behaviors of the merge.
 *
 * @version $Id$
 * @since 3.2M1
 */
public class MergeConfiguration
{
    /**
     * @see #isProvidedVersionsModifiables()
     */
    private boolean providedVersionsModifiables = true;

    private DocumentReference concernedDocument;

    private EntityReference userReference;

    /**
     * @param providedVersionsModifiables true if the merge is allowed to modify input elements
     */
    public void setProvidedVersionsModifiables(boolean providedVersionsModifiables)
    {
        this.providedVersionsModifiables = providedVersionsModifiables;
    }

    /**
     * Indicate of the provided previous/new document can be modified.
     * <p>
     * By default merge can modify provided documents for performances reasons. For example when the new version of the
     * document contains a new object it's directly moved without being cloned.
     *
     * @return true if merge is allowed to modify provided versions
     */
    public boolean isProvidedVersionsModifiables()
    {
        return this.providedVersionsModifiables;
    }

    /**
     * @return the reference of the document concerned by this merge.
     */
    public DocumentReference getConcernedDocument()
    {
        return concernedDocument;
    }

    /**
     * Specify which document is currently merged.
     * @param concernedDocument the reference to the document that is merged.
     * @since 11.8RC1
     */
    public void setConcernedDocument(DocumentReference concernedDocument)
    {
        this.concernedDocument = concernedDocument;
    }

    /**
     * @return the reference of the user performing the merge.
     * @since 11.8RC1
     */
    public EntityReference getUserReference()
    {
        return userReference;
    }

    /**
     * Specify which user is performing the merge.
     * @param userReference the reference to the user doing the merge.
     * @since 11.8RC1
     */
    public void setUserReference(EntityReference userReference)
    {
        this.userReference = userReference;
    }
}
