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
package org.xwiki.refactoring.job;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * A job request that can be used to move a collection of entities to a specified destination. This request can also be
 * used to rename an entity.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Unstable
public class MoveRequest extends EntityRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getDestination()
     */
    private static final String PROPERTY_DESTINATION = "destination";

    /**
     * @see #isDeleteSource()
     */
    private static final String PROPERTY_DELETE_SOURCE = "deleteSource";

    /**
     * @see #isUpdateLinks()
     */
    private static final String PROPERTY_UPDATE_LINKS = "updateLinks";

    /**
     * @see #isAutoRedirect()
     */
    private static final String PROPERTY_AUTO_REDIRECT = "autoRedirect";

    /**
     * @see #isUpdateParentField()
     */
    private static final String PROPERTY_UPDATE_PARENT_FIELD = "updateParentField";

    /**
     * @return the destination entity, where to move the entities specified by {@link #getEntityReferences()}
     */
    public EntityReference getDestination()
    {
        return getProperty(PROPERTY_DESTINATION);
    }

    /**
     * Sets the destination entity, where to move the entities specified by {@link #getEntityReferences()}.
     * 
     * @param destination the destination entity
     */
    public void setDestination(EntityReference destination)
    {
        setProperty(PROPERTY_DESTINATION, destination);
    }

    /**
     * @return {@code true} if the source entities specified by {@link #getEntityReferences()} should be deleted,
     *         {@code false} otherwise; in a standard move operation the source is deleted but sometimes you may want to
     *         keep the source as a backup; this option can also be used to perform a copy instead of a move; note that
     *         the difference between a copy and a standard move without delete is that the back-links are not updated
     */
    public boolean isDeleteSource()
    {
        return getProperty(PROPERTY_DELETE_SOURCE, true);
    }

    /**
     * Sets whether the source entities specified by {@link #getEntityReferences()} should be deleted or not.
     * 
     * @param deleteSource {@code true} to delete the source, {@code false} to keep it as a backup
     */
    public void setDeleteSource(boolean deleteSource)
    {
        setProperty(PROPERTY_DELETE_SOURCE, deleteSource);
    }

    /**
     * @return {@code true} if the links that target the old entity reference (before the move) should be updated to
     *         target the new reference (after the move), {@code false} to preserve the old link target
     */
    public boolean isUpdateLinks()
    {
        return getProperty(PROPERTY_UPDATE_LINKS, true);
    }

    /**
     * Sets whether the links that target the old entity reference (before the move) should be updated to target the new
     * reference (after the move) or not.
     * 
     * @param updateLinks {@code true} to update the links, {@code false} to preserve the old link target
     */
    public void setUpdateLinks(boolean updateLinks)
    {
        setProperty(PROPERTY_UPDATE_LINKS, updateLinks);
    }

    /**
     * @return {@code true} if the original pages should be redirected automatically to the new location when accessed
     *         by the user, in order to preserve external links, {@code false} otherwise
     */
    public boolean isAutoRedirect()
    {
        return getProperty(PROPERTY_AUTO_REDIRECT, true);
    }

    /**
     * Sets whether the original pages should be redirected automatically to the new location when accessed by the user,
     * in order to preserve external links.
     * 
     * @param autoRedirect {@code true} to automatically redirect the old pages to the new location, {@code false}
     *            otherwise
     */
    public void setAutoRedirect(boolean autoRedirect)
    {
        setProperty(PROPERTY_AUTO_REDIRECT, autoRedirect);
    }

    /**
     * @return {@code true} if the parent-child relationship should be preserved by updating the {@code parent} field of
     *         the {@code source}'s child pages to use the {@code destination} as parent instead; {@code false}
     *         otherwise
     * @since 8.0M2
     * @since 7.4.2
     */
    public boolean isUpdateParentField()
    {
        return getProperty(PROPERTY_UPDATE_PARENT_FIELD, true);
    }

    /**
     * Sets whether the parent-child relationship should be preserved by updating the {@code parent} field of the
     * {@code source}'s child pages to use the {@code destination} as parent instead; {@code false} otherwise.
     *
     * @param updateParentField {@code true} to update the parent field of the {@code source}'s child pages and use the
     *            {@code destination} as parent instead, {@code false} otherwise
     * @since 8.0M2
     * @since 7.4.2
     */
    public void setUpdateParentField(boolean updateParentField)
    {
        setProperty(PROPERTY_UPDATE_PARENT_FIELD, updateParentField);
    }
}
