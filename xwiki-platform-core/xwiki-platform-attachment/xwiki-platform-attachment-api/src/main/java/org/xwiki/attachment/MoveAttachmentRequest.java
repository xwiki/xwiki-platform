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
package org.xwiki.attachment;

import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.stability.Unstable;

/**
 * Job request for moving an attachment to a new location. A redirection can be persisted at the old location.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Unstable
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
}
