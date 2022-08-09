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

/**
 * A job request that can be used to permanently delete a list of deleted documents and/or an entire batch of
 * deleted documents from the recycle bin.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public class PermanentlyDeleteRequest extends AbstractDeletedDocumentsRequest
{
    private static final long serialVersionUID = 5272462199708765090L;

    /**
     * Default constructor.
     */
    public PermanentlyDeleteRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public PermanentlyDeleteRequest(Request request)
    {
        super(request);
    }
}
