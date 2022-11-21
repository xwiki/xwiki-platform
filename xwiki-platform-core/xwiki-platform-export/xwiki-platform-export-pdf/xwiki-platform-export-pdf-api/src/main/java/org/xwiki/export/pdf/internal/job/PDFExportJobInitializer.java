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
package org.xwiki.export.pdf.internal.job;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.job.GroupedJobInitializer;
import org.xwiki.job.JobGroupPath;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named("PDFExport")
public class PDFExportJobInitializer implements GroupedJobInitializer
{
    @Inject
    private PDFExportConfiguration configuration;

    /**
     * @return a lower priority than {@link Thread#NORM_PRIORITY} since PDF export is resource consuming and we want
     *         other threads to have the priority.
     */
    @Override
    public int getDefaultPriority()
    {
        return Thread.NORM_PRIORITY - 1;
    }

    @Override
    public JobGroupPath getId()
    {
        return new JobGroupPath(Arrays.asList("export", "pdf"));
    }

    @Override
    public int getPoolSize()
    {
        return this.configuration.getThreadPoolSize();
    }
}
