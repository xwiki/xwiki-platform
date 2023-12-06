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
package org.xwiki.attachment.refactoring;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.job.Request;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.refactoring.job.EntityRequest;

/**
 * Job request for moving an attachment to a new location. A redirection can be persisted at the old location.
 *
 * @version $Id$
 * @since 14.0RC1
 */
public class MoveAttachmentRequest extends EntityRequest
{
    /**
     * Destination property name.
     */
    public static final String DESTINATION = "destination";

    /**
     * Auto-redirection property name.
     */
    public static final String AUTO_REDIRECT = "autoRedirect";

    /**
     * Update references refactoring property name.
     *
     * @since 14.2RC1
     */
    public static final String UPDATE_REFERENCES = "updateReferences";

    /**
     * Default constructor.
     */
    public MoveAttachmentRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public MoveAttachmentRequest(Request request)
    {
        super(request);
    }

    /**
     * @return the destination of the move
     */
    public AttachmentReference getDestination()
    {
        return getProperty(DESTINATION);
    }

    /**
     * @return {@code true} if a redirection must be created from the old location to the new one, {@code false}
     *     otherwise
     */
    public boolean isAutoRedirect()
    {
        return getProperty(AUTO_REDIRECT, true);
    }

    /**
     * @return {@code true} if the references must be updated, {@code false} otherwise
     * @since 14.2RC1
     */
    public boolean isUpdateReferences()
    {
        return getProperty(UPDATE_REFERENCES, true);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("entityReferences", getEntityReferences())
            .append("id", getId())
            .append("interactive", isInteractive())
            .append("properties", getProperties())
            .append("userReference", getUserReference())
            .append("authorReference", getAuthorReference())
            .toString();
    }
}
