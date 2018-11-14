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
package org.xwiki.job.api;

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Define a request that have rights to check.
 *
 * @version $Id$
 * @since 10.10RC1
 * @since 10.8.2
 * @since 9.11.9
 */
@Unstable
public abstract class AbstractCheckRightsRequest extends AbstractRequest
{
    /**
     * @see #getUserReference()
     */
    private static final String PROPERTY_USER_REFERENCE = "user.reference";

    /**
     * @see #getAuthorReference()
     */
    private static final String PROPERTY_CALLER_REFERENCE = "caller.reference";

    /**
     * @see #isCheckRights()
     */
    private static final String PROPERTY_CHECK_RIGHTS = "checkrights";

    /**
     * @see #isCheckAuthorRights()
     */
    private static final String PROPERTY_CHECK_AUTHOR_RIGHTS = "checkAuthorRights";

    /**
     * @return {@code true} in case the job should check if the author specified by {@link #getAuthorReference()} is
     *          authorized to perform the actions implied by this request, {@code false} otherwise
     */
    public boolean isCheckAuthorRights()
    {
        return getProperty(PROPERTY_CHECK_AUTHOR_RIGHTS, true);
    }

    /**
     * Sets whether the job should check or not if the user specified by {@link #getAuthorReference()} is authorized to
     * perform the actions implied by this request.
     *
     * @param checkAuthorRights {@code true} to check if {@link #getAuthorReference()} is authorized to perform this
     *      request, {@code false} to perform this request without checking rights
     */
    public void setCheckAuthorRights(boolean checkAuthorRights)
    {
        setProperty(PROPERTY_CHECK_AUTHOR_RIGHTS, checkAuthorRights);
    }

    /**
     * @return {@code true} in case the job should check if the user specified by {@link #getUserReference()} is
     *         authorized to perform the actions implied by this request, {@code false} otherwise
     */
    public boolean isCheckRights()
    {
        return getProperty(PROPERTY_CHECK_RIGHTS, true);
    }

    /**
     * Sets whether the job should check or not if the user specified by {@link #getUserReference()} is authorized to
     * perform the actions implied by this request.
     *
     * @param checkRights {@code true} to check if {@link #getUserReference()} is authorized to perform this request,
     *            {@code false} to perform this request without checking rights
     */
    public void setCheckRights(boolean checkRights)
    {
        setProperty(PROPERTY_CHECK_RIGHTS, checkRights);
    }

    /**
     * @return the user that should be used to perform this request; this user must be authorized to perform the actions
     *         implied by this request if {@link #isCheckRights()} is {@code true}.
     */
    public DocumentReference getUserReference()
    {
        return getProperty(PROPERTY_USER_REFERENCE);
    }

    /**
     * Sets the user that should be used to perform this request. This user must be authorized to perform the actions
     * implied by this request if {@link #isCheckRights()} is {@code true}.
     *
     * @param userReference the user reference
     */
    public void setUserReference(DocumentReference userReference)
    {
        setProperty(PROPERTY_USER_REFERENCE, userReference);
    }

    /**
     * @return the author of the script which is performing the request; this user must be authorized to perform the
     * actions implied by this request if {@link #isCheckRights()} is {@code true}.
     */
    public DocumentReference getAuthorReference()
    {
        return getProperty(PROPERTY_CALLER_REFERENCE);
    }

    /**
     * Sets the author of the script which is performing the request. This user must be authorized to perform the
     * actions implied by this request if {@link #isCheckRights()} is {@code true}.
     *
     * @param authorReference the author reference
     */
    public void setAuthorReference(DocumentReference authorReference)
    {
        setProperty(PROPERTY_CALLER_REFERENCE, authorReference);
    }
}
