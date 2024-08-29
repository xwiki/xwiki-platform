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
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeManagerResult;

/**
 * Allow to define some behaviors of the merge.
 *
 * @version $Id$
 * @since 3.2M1
 */
public class MergeConfiguration
{
    /**
     * Versions to use as fallback in case of conflicts.
     *
     * @version $Id$
     * @since 15.2RC1
     * @since 14.10.7
     */
    @Unstable
    public enum ConflictFallbackVersion
    {
        /**
         * Current version. Default value.
         */
        CURRENT,

        /**
         * Next version.
         */
        NEXT
    };

    /**
     * @see #isProvidedVersionsModifiables()
     */
    private boolean providedVersionsModifiables = true;

    private DocumentReference concernedDocument;

    private EntityReference userReference;

    private ConflictFallbackVersion conflictFallbackVersion = ConflictFallbackVersion.CURRENT;

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

    /**
     * Get the version to use as fallback in case of conflict: this is the version that is used in
     * {@link MergeManagerResult#getMergeResult()} whenever a conflict is found.
     * When not set the default value is {@link ConflictFallbackVersion#CURRENT}.
     *
     * @return the version to use as fallback
     * @since 15.2RC1
     * @since 14.10.7
     */
    @Unstable
    public ConflictFallbackVersion getConflictFallbackVersion()
    {
        return conflictFallbackVersion;
    }

    /**
     * Specify the version to use as fallback in case of conflict.
     *
     * @param conflictFallbackVersion the fallback version to use
     * @see #getConflictFallbackVersion()
     * @since 15.2RC1
     * @since 14.10.7
     */
    @Unstable
    public void setConflictFallbackVersion(ConflictFallbackVersion conflictFallbackVersion)
    {
        this.conflictFallbackVersion = conflictFallbackVersion;
    }
}
