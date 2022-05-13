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

import org.xwiki.model.reference.DocumentReference;

/**
 * Job request for deleting a page.
 * 
 * @version $Id$
 * @since 14.4RC1
 */
public class DeleteRequest extends EntityRequest
{
    /**
     * Key of the optional property that indicates whether the document should be send to the recycle bin or removed
     * permanently.
     */
    public static final String SHOULD_SKIP_RECYCLE_BIN = "shouldSkipRecycleBin";

    /**
     * @see #getNewTarget()
     */
    public static final String NEW_TARGET = "newTarget";

    /**
     * @see #isUpdateLinks()
     */
    public static final String UPDATE_LINKS = "updateLinks";

    /**
     * @see #isUpdateLinksOnFarm()
     */
    public static final String UPDATE_LINKS_ON_FARM = "updateLinksOnFarm";

    /**
     * @see #isAutoRedirect()
     */
    public static final String AUTO_REDIRECT = "autoRedirect";

    private static final long serialVersionUID = 1L;

    /**
     * @return {@code true} if the document will be removed permanently, {@code false} if it will be moved to recycle
     *         bin
     */
    public boolean shouldSkipRecycleBin()
    {
        return getProperty(SHOULD_SKIP_RECYCLE_BIN, true);
    }

    /**
     * Sets whether the document should be send to the recycle bin or removed permanently.
     * 
     * @param shouldSkipRecycleBin {@code true} if the document should be removed permanently, {@code false} if it
     *            should be send to recycle bin
     */
    public void setShouldSkipRecycleBin(boolean shouldSkipRecycleBin)
    {
        setProperty(SHOULD_SKIP_RECYCLE_BIN, shouldSkipRecycleBin);
    }

    /**
     * @return the document to be used as the new target after the delete
     */
    public DocumentReference getNewTarget()
    {
        return getProperty(NEW_TARGET);
    }

    /**
     * Sets a document to be used as the new target after the delete.
     *
     * @param newTarget reference to the new target document
     */
    public void setNewTarget(DocumentReference newTarget)
    {
        setProperty(NEW_TARGET, newTarget);
    }

    /**
     * @return {@code true} if the links that target the old entity reference (before the delete) should be updated to
     *         point to the new specified target, {@code false} to preserve the old link target
     */
    public boolean isUpdateLinks()
    {
        return getProperty(UPDATE_LINKS, false);
    }

    /**
     * Sets whether the links that target the old entity reference (before the delete) should be updated to point to the
     * new specified target or not.
     *
     * @param updateLinks {@code true} to update the links, {@code false} to preserve the old link target
     */
    public void setUpdateLinks(boolean updateLinks)
    {
        setProperty(UPDATE_LINKS, updateLinks);
    }

    /**
     * @return {@code true} if the job should update the links that target the old entity reference (before the delete)
     *         from anywhere on the farm, {@code false} if the job should update only the links from the wiki where the
     *         entity was located before the delete
     */
    public boolean isUpdateLinksOnFarm()
    {
        return getProperty(UPDATE_LINKS_ON_FARM, false);
    }

    /**
     * Sets whether the job should update the links that target the old entity reference (before the delete) from
     * anywhere on the farm, or only from the wiki where the entity was located before the delete.
     * <p>
     * Note that this parameter has no effect if {@link #isUpdateLinks()} is {@code false}.
     *
     * @param updateLinksOnFarm {@code true} to update the links from anywhere on the farm, {@code false} to update only
     *            the links from the wiki where the entity is located
     */
    public void setUpdateLinksOnFarm(boolean updateLinksOnFarm)
    {
        setProperty(UPDATE_LINKS_ON_FARM, updateLinksOnFarm);
    }

    /**
     * @return {@code true} if the original pages should be redirected automatically to the new specified location when
     *         accessed by the user, in order to preserve external links, {@code false} otherwise
     */
    public boolean isAutoRedirect()
    {
        return getProperty(AUTO_REDIRECT, false);
    }

    /**
     * Sets whether the original pages should be redirected automatically to the new specified location when accessed by
     * the user, in order to preserve external links.
     * 
     * @param autoRedirect {@code true} to automatically redirect the old pages to the new target, {@code false}
     *            otherwise
     */
    public void setAutoRedirect(boolean autoRedirect)
    {
        setProperty(AUTO_REDIRECT, autoRedirect);
    }
}
