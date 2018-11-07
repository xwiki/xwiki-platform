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

import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Define a request that can be processed by a user.
 *
 * @version $Id$
 * @since 10.10RC1
 */
@Unstable
public interface UserOrientedRequest extends Request
{
    /**
     * @return {@code true} in case the job should check if the user specified by {@link #getUserReference()} is
     *         authorized to perform the actions implied by this request, {@code false} otherwise
     */
    boolean isCheckRights();

    /**
     * Sets whether the job should check or not if the user specified by {@link #getUserReference()} is authorized to
     * perform the actions implied by this request.
     *
     * @param checkRights {@code true} to check if {@link #getUserReference()} is authorized to perform this request,
     *            {@code false} to perform this request without checking rights
     */
    void setCheckRights(boolean checkRights);

    /**
     * @return the user that should be used to perform this request; this user must be authorized to perform the actions
     *         implied by this request if {@link #isCheckRights()} is {@code true}.
     */
    DocumentReference getUserReference();

    /**
     * Sets the user that should be used to perform this request. This user must be authorized to perform the actions
     * implied by this request if {@link #isCheckRights()} is {@code true}.
     *
     * @param userReference the user reference
     */
    void setUserReference(DocumentReference userReference);
}
