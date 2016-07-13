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
package org.xwiki.extension.xar.job.diff;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.extension.job.InstallRequest;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.stability.Unstable;

/**
 * The status of a job that computes the differences between the documents provided by a XAR extension and the documents
 * from the database.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
@Unstable
public class DiffXarJobStatus extends DefaultJobStatus<InstallRequest>
{
    /**
     * The list of documents that have differences.
     */
    private final List<DocumentUnifiedDiff> documentDiffs = new ArrayList<>();

    /**
     * Creates a new job status.
     * 
     * @param request the request provided when the job was started
     * @param observationManager the observation manager
     * @param loggerManager the logger manager
     */
    public DiffXarJobStatus(InstallRequest request, ObservationManager observationManager, LoggerManager loggerManager)
    {
        super(request, null, observationManager, loggerManager);
    }

    /**
     * @return the list of documents that have differences.
     */
    public List<DocumentUnifiedDiff> getDocumentDiffs()
    {
        return documentDiffs;
    }
}
