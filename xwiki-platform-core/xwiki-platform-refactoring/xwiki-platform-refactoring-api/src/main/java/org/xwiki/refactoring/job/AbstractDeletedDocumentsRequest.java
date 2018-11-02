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

import java.util.List;

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

/**
 * A job request that can be used for handling deleted documents from the recycle bin.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractDeletedDocumentsRequest extends AbstractRequest
{
    private static final String BATCH_ID = "batchId";

    private static final String DELETED_DOCUMENT_IDS = "deletedDocumentIds";

    private static final String CHECK_RIGHTS = "checkRights";

    private static final String USER_REFERENCE = "userReference";

    private static final String WIKI_REFERENCE = "wikiReference";

    /**
     * @return the ID of the batch of deleted documents to handle. If {@link #setDeletedDocumentIds(List)} is also
     *         specified, the two will be merged when the operation is executed
     */
    public String getBatchId()
    {
        return getProperty(BATCH_ID);
    }

    /**
     * @param batchId the ID of the batch of deleted documents to handle. If {@link #setDeletedDocumentIds(List)} is
     *            also specified, the two will be merged when the operation is executed
     */
    public void setBatchId(String batchId)
    {

        setProperty(BATCH_ID, batchId);
    }

    /**
     * @return the list of IDs of the deleted documents to handle. If {@link #setBatchId(String)} is also specified,
     *         the two will be merged when the operation is executed
     */
    public List<Long> getDeletedDocumentIds()
    {
        return getProperty(DELETED_DOCUMENT_IDS);
    }

    /**
     * @param deletedDocumentIds the list of IDs of the deleted documents to handle. If {@link #setBatchId(String)} is
     *            also specified, the two will be merged when the operation is executed
     */
    public void setDeletedDocumentIds(List<Long> deletedDocumentIds)
    {
        setProperty(DELETED_DOCUMENT_IDS, deletedDocumentIds);
    }

    /**
     * @return whether or not to check the rights of the user executing the operation
     */
    public boolean isCheckRights()
    {
        return getProperty(CHECK_RIGHTS, true);
    }

    /**
     * @param checkRights whether or not to check the rights of the user executing the operation
     */
    public void setCheckRights(boolean checkRights)
    {
        setProperty(CHECK_RIGHTS, checkRights);

    }

    /**
     * @return the user executing the handle operation. This is also the user for which any rights might be checked if
     *         {@link #setCheckRights(boolean)} is enabled
     */
    public DocumentReference getUserReference()
    {
        return getProperty(USER_REFERENCE);
    }

    /**
     * @param userReference the user executing the handle operation. This is also the user for which any rights
     *            might be checked if {@link #setCheckRights(boolean)} is enabled
     */
    public void setUserReference(DocumentReference userReference)
    {
        setProperty(USER_REFERENCE, userReference);
    }

    /**
     * @return the wiki on which the handle operation is performed
     */
    public WikiReference getWikiReference()
    {
        return getProperty(WIKI_REFERENCE);
    }

    /**
     * @param wikiReference the wiki on which the handle operation is performed
     */
    public void setWikiReference(WikiReference wikiReference)
    {
        setProperty(WIKI_REFERENCE, wikiReference);
    }
}
