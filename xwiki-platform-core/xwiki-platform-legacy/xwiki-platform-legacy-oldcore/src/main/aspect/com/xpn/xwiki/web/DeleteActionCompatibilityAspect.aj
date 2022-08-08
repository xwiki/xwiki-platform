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
package com.xpn.xwiki.web;

import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import org.xwiki.stability.Unstable;

/**
 * Add a backward compatibility layer to the {@link DeleteAction} class.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public privileged aspect DeleteActionCompatibilityAspect
{
    // deleteToRecycleBin and deleteDocument, while previously {@code protected} are now {@code public} due to some
    // technical limitations of AspectJ.
    // See https://doanduyhai.wordpress.com/2011/12/12/advanced-aspectj-part-ii-inter-type-declaration/
    // "If their is one thing to remember from access modifier, it’s that their semantic applies with respect to the
    // declaring aspect, and not to the target."
    // This method must still be used as if it is {@code protected} and only be overloaded by sub-classes of
    // DeleteAction.
    /**
     * Create a job to delete an entity.
     * If the recycle bin is active, the entity is moved to the recycle bin.
     * Otherwise, the entity is removed permanently.
     *
     * @param entityReference the entity to delete
     * @param context the current context, used to access the user's request
     * @return {@code true} if the user is redirected, {@code false} otherwise
     * @throws XWikiException if anything goes wrong during the document deletion
     * @deprecated since 12.8RC1, use {@link #deleteDocument(EntityReference, XWikiContext, boolean)} instead
     * @since 12.8RC1
     */
    @Deprecated
    @Unstable
    public boolean DeleteAction.deleteToRecycleBin(EntityReference entityReference, XWikiContext context)
        throws XWikiException
    {
        return this.deleteDocument(entityReference, context, false, null, false, false);
    }
    
    /**
     * Create a job to delete an entity.
     *
     * An entity can either be deleted permanently or moved to the recycle bin.
     * The preference of the user deleting the entity is stored in the {@code shouldSkipRecycleBin} parameter.
     * When {@code shouldSkipRecycleBin} is {@code true} the entity is preferably permanently deleted.
     * Otherwise, the entity is preferably moved to the recycle bin.
     * Note that it only express a choice made by the user.
     * If the user does not have the right to remove an entity permanently, the entity might still be saved in the
     * recycle bin.
     * If the wiki does not have access to a recycle bin, the entity might be permanently removed, regardless of the
     * user's preferences.
     *
     * @param entityReference the entity to delete
     * @param context the current context, used to access the user's request
     * @param shouldSkipRecycleBin if {@code false} the entity is preferably sent to the recycle bin, if {@code true},
     *            the entity is preferably deleted permanently
     * @return {@code true} if the user is redirected, {@code false} otherwise
     * @throws XWikiException if anything goes wrong during the document deletion
     * @deprecated since 14.4.2, 14.5, use {@link #deleteDocument(EntityReference, XWikiContext, boolean,
     *             DocumentReference, boolean, boolean)} instead
     * @since 12.8RC1
     */
    @Deprecated
    public boolean DeleteAction.deleteDocument(EntityReference entityReference, XWikiContext context,
        boolean shouldSkipRecycleBin) throws XWikiException
    {
    	return deleteDocument(entityReference, context, shouldSkipRecycleBin, null, false, false);
    }
}
