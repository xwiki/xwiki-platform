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
package org.xwiki.platform.wiki.creationjob;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Step to be executed during the wiki creation job.
 *
 * @version $Id$
 * @since 7.0M2
 */
@Role
@Unstable
public interface WikiCreationStep
{
    /**
     * Execute the steps.
     * @param request wiki creation request with all information about the wiki to create
     * @throws WikiCreationException if problem occurs
     */
    void execute(WikiCreationRequest request) throws WikiCreationException;

    /**
     * The creation steps are sorted in ascending order before being executed so this method returns the one of the
     * current steps.
     * @return the order of the steps
     */
    int getOrder();
}
