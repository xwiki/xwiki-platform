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

import java.util.Collections;
import java.util.Map;

import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Job request for deleting a page.
 *
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class DeleteRequest extends EntityRequest
{
    /**
     * Key of the optional property that indicates whether the document should be sent to the recycle bin or removed
     * permanently.
     */
    public static final String SHOULD_SKIP_RECYCLE_BIN = "shouldSkipRecycleBin";

    /**
     * @see #getNewBacklinkTargets()
     */
    public static final String NEW_BACKLINK_TARGETS = "newBacklinkTargets";

    /**
     * @see #isUpdateLinks()
     */
    public static final String UPDATE_LINKS = "updateLinks";

    /**
     * @see #isUpdateLinksOnFarm()
     * @deprecated not taken into account anymore
     */
    @Deprecated(since = "14.8RC1")
    public static final String UPDATE_LINKS_ON_FARM = "updateLinksOnFarm";

    /**
     * @see #isAutoRedirect()
     */
    public static final String AUTO_REDIRECT = "autoRedirect";

    /**
     * @see #isWaitForIndexing()
     */
    private static final String PROPERTY_WAIT_FOR_INDEXING = "waitForIndexing";

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public DeleteRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public DeleteRequest(Request request)
    {
        super(request);
    }

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
     *            should be sent to recycle bin
     */
    public void setShouldSkipRecycleBin(boolean shouldSkipRecycleBin)
    {
        setProperty(SHOULD_SKIP_RECYCLE_BIN, shouldSkipRecycleBin);
    }

    /**
     * @return a Map where the keys are the deleted documents and the values are the new target documents to be used
     *         after delete
     */
    public Map<DocumentReference, DocumentReference> getNewBacklinkTargets()
    {
        return getProperty(NEW_BACKLINK_TARGETS, Collections.emptyMap());
    }

    /**
     * Sets the new backlink target for documents after delete.
     *
     * @param newBacklinkTargets a Map where the keys are the deleted documents and the values are new target documents
     */
    public void setNewBacklinkTargets(Map<DocumentReference, DocumentReference> newBacklinkTargets)
    {
        setProperty(NEW_BACKLINK_TARGETS, newBacklinkTargets);
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
     * @return {@code true} if, when {@link #isUpdateLinks()} is {@code true}, the refactoring shall wait for link
     * indexing to complete. This ensures that accurate information about links is available, this is particularly
     * relevant when multiple documents with links between them are moved.
     *
     * @since 16.8.0
     * @since 16.4.4
     * @since 15.10.13
     */
    @Unstable
    public boolean isWaitForIndexing()
    {
        return getProperty(PROPERTY_WAIT_FOR_INDEXING, true);
    }

    /**
     * Sets whether the refactoring job should wait for links to be indexed before updating them.
     *
     * @param waitForIndexing if the refactoring job should wait for links to be indexed before updating them
     * @since 16.8.0
     * @since 16.4.4
     * @since 15.10.13
     */
    @Unstable
    public void setWaitForIndexing(boolean waitForIndexing)
    {
        setProperty(PROPERTY_WAIT_FOR_INDEXING, waitForIndexing);
    }

    /**
     * @return {@code true} if the job should update the links that target the old entity reference (before the delete)
     *         from anywhere on the farm, {@code false} if the job should update only the links from the wiki where the
     *         entity was located before the delete
     * @deprecated not taken into account anymore
     */
    @Deprecated(since = "14.8RC1")
    public boolean isUpdateLinksOnFarm()
    {
        return true;
    }

    /**
     * Sets whether the job should update the links that target the old entity reference (before the delete) from
     * anywhere on the farm, or only from the wiki where the entity was located before the delete.
     * <p>
     * Note that this parameter has no effect if {@link #isUpdateLinks()} is {@code false}.
     *
     * @param updateLinksOnFarm {@code true} to update the links from anywhere on the farm, {@code false} to update only
     *            the links from the wiki where the entity is located
     * @deprecated not taken into account anymore
     */
    @Deprecated(since = "14.8RC1")
    public void setUpdateLinksOnFarm(boolean updateLinksOnFarm)
    {
        // Ignored
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
