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
package org.xwiki.attachment.internal.job;

import javax.inject.Named;

import org.xwiki.attachment.MoveAttachmentRequest;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.internal.job.AbstractEntityJobWithChecks;
import org.xwiki.refactoring.job.EntityJobStatus;

/**
 * TODO: document me.
 *
 * @version $Id$
 * @since X.Y.X
 */
@Component
@Named(MoveAttachmentJob.HINT)
public class MoveAttachmentJob
    extends AbstractEntityJobWithChecks<MoveAttachmentRequest, EntityJobStatus<MoveAttachmentRequest>>
{
    /**
     * The hint for this job.
     */
    public static final String HINT = "refactoring/attachment/move";

    @Override
    public String getType()
    {
        return HINT;
    }

    @Override
    protected void process(EntityReference entityReference)
    {
        // TODO: implement me
    }
}
