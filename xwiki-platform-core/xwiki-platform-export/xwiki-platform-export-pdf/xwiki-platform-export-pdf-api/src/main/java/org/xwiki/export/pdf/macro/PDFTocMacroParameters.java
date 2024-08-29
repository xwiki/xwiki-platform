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
package org.xwiki.export.pdf.macro;

import javax.validation.constraints.Min;

import org.xwiki.export.pdf.internal.macro.PDFTocMacro;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;

/**
 * The parameters supported by {@link PDFTocMacro}.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class PDFTocMacroParameters
{
    private String jobId;

    /**
     * The maximum section level. For example if 3 then all section levels from 4 will not be listed.
     */
    @Min(1)
    private int depth = 3;

    /**
     * @return the id of the PDF export job for which to generate the table of contents
     */
    public String getJobId()
    {
        return jobId;
    }

    /**
     * Specifies the PDF export job for which to generate the table of contents.
     * 
     * @param jobId the id of the PDF export job
     */
    @PropertyDescription("The id of the PDF export job for which to generate the table of contents.")
    @PropertyMandatory
    public void setJobId(String jobId)
    {
        this.jobId = jobId;
    }

    /**
     * @return the maximum section level; for example if 3 then all section levels from 4 will not be listed
     */
    public int getDepth()
    {
        return depth;
    }

    /**
     * @param depth the maximum section level; for example if 3 then all section levels from 4 will not be listed
     */
    @PropertyDescription("The maximum section level. "
        + "For example if 3 then all section levels from 4 will not be listed.")
    public void setDepth(int depth)
    {
        this.depth = depth;
    }
}
