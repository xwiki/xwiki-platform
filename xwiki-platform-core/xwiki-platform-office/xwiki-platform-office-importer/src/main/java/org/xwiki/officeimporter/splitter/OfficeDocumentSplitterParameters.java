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
package org.xwiki.officeimporter.splitter;

import org.xwiki.model.reference.DocumentReference;

/**
 * Parameters that control how an office document is split.
 * 
 * @version $Id$
 * @since 14.10.2
 * @since 15.0RC1
 */
public class OfficeDocumentSplitterParameters
{
    private int[] headingLevelsToSplit = new int[] {1};

    private String namingCriterionHint = "headingNames";

    private boolean useTerminalPages;

    private DocumentReference baseDocumentReference;

    /**
     * @return the heading levels (1..6) used as boundaries
     * @see #setHeadingLevelsToSplit(int[])
     */
    public int[] getHeadingLevelsToSplit()
    {
        return headingLevelsToSplit;
    }

    /**
     * Set the heading levels (1..6) to be used as boundaries. The split process is recursive. If there are multiple
     * heading levels specified, the original document will be split from the highest heading level
     * ({@code lowest value >= 1}) first and then the resulting office documents will be re-split from the next highest
     * heading level.
     * 
     * @param headingLevelsToSplit the heading levels (1..6) to use as boundaries
     */
    public void setHeadingLevelsToSplit(int[] headingLevelsToSplit)
    {
        this.headingLevelsToSplit = headingLevelsToSplit;
    }

    /**
     * @return the naming criterion used when producing target (child) pages names
     * @see #setNamingCriterionHint(String)
     */
    public String getNamingCriterionHint()
    {
        return namingCriterionHint;
    }

    /**
     * Set the naming criterion to be used when producing target page names for the newly split documents. Currently
     * three schemes are supported:
     * <ul>
     * <li>headingNames: uses the first heading name as target document name</li>
     * <li>mainPageNameAndHeading: base document name followed by heading name</li>
     * <li>mainPageNameAndNumbering: base document name followed by index</li>
     * </ul>
     * 
     * @param namingCriterionHint the naming criterion to be used when producing target (child) pages names
     */
    public void setNamingCriterionHint(String namingCriterionHint)
    {
        this.namingCriterionHint = namingCriterionHint;
    }

    /**
     * @return {@code true} if the split parts are saved as terminal pages, {@code false} otherwise (when nested pages
     *         are used instead)
     * @see #setUseTerminalPages(boolean)
     */
    public boolean isUseTerminalPages()
    {
        return useTerminalPages;
    }

    /**
     * Set whether to use terminal pages or nested pages for storing the newly split documents.
     * 
     * @param useTerminalPages whether to use terminal pages or nested pages
     */
    public void setUseTerminalPages(boolean useTerminalPages)
    {
        this.useTerminalPages = useTerminalPages;
    }

    /**
     * @return the base document reference used for computing the child document references
     * @see #setBaseDocumentReference(DocumentReference)
     */
    public DocumentReference getBaseDocumentReference()
    {
        return baseDocumentReference;
    }

    /**
     * Set the base (root) document reference to be used when generating target document references for child (newly
     * split) documents.
     * 
     * @param baseDocumentReference the base document reference used for computing the child document references
     */
    public void setBaseDocumentReference(DocumentReference baseDocumentReference)
    {
        this.baseDocumentReference = baseDocumentReference;
    }
}
