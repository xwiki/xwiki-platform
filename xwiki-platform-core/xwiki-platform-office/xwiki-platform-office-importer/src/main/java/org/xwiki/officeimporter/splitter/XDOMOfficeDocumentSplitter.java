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

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;

/**
 * Component responsible for splitting office imports.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Role
public interface XDOMOfficeDocumentSplitter
{
    /**
     * Splits an {@link XDOMOfficeDocument} into multiple {@link XDOMOfficeDocument} instances using the provided
     * heading levels as boundaries. The naming Criterion and the base name determines the target wiki pages for the
     * newly split documents.
     * 
     * @param xdomOfficeDocument {@link XDOMOfficeDocument} to be split
     * @param headingLevelsToSplit heading levels (1..6) to be used as boundaries. The split process is recursive, if
     *            there are multiple heading levels specified, the original document will be split from the highest
     *            heading level ({@code lowest value >= 1}) first and then the resulting office documents will be
     *            re-split from the next highest heading level
     * @param namingCriterionHint naming criterion to be used when producing target page names for the newly split
     *            documents. Currently three schemes are supported:
     *            <ul>
     *            <li>headingNames - Uses the first heading name as target document name</li>
     *            <li>mainPageNameAndHeading - Base document name followed by heading name</li>
     *            <li>mainPageNameAndNumbering - Base document name followed by index</li>
     *            </ul>
     * @param baseDocumentReference base (root) page name to be used when generating target page names for child (newly
     *            split) documents
     * @return a map of page descriptors vs. xdom office documents; each page descriptor describes the target wiki page
     *         name for the corresponding xdom office document
     * @throws OfficeImporterException if an error occurs while splitting
     * @since 2.2M1
     * @deprecated since 14.10.2 / 15.0RC1 use {@link #split(XDOMOfficeDocument, OfficeDocumentSplitterParameters)}
     *             instead
     */
    @Deprecated
    default Map<TargetDocumentDescriptor, XDOMOfficeDocument> split(XDOMOfficeDocument xdomOfficeDocument,
        int[] headingLevelsToSplit, String namingCriterionHint, DocumentReference baseDocumentReference)
        throws OfficeImporterException
    {
        OfficeDocumentSplitterParameters parameters = new OfficeDocumentSplitterParameters();
        parameters.setHeadingLevelsToSplit(headingLevelsToSplit);
        parameters.setNamingCriterionHint(namingCriterionHint);
        parameters.setBaseDocumentReference(baseDocumentReference);
        parameters.setUseTerminalPages(!"WebHome".equals(baseDocumentReference.getName()));
        return split(xdomOfficeDocument, parameters);
    }

    /**
     * Splits an {@link XDOMOfficeDocument} into multiple {@link XDOMOfficeDocument} instances based on the provided
     * parameters.
     *
     * @param xdomOfficeDocument {@link XDOMOfficeDocument} to be split
     * @param parameters the split parameters
     * @return a map of page descriptors vs. xdom office documents; each page descriptor describes the target wiki page
     *         name for the corresponding xdom office document
     * @throws OfficeImporterException if an error occurs while splitting
     * @since 14.10.2
     * @since 15.0RC1
     */
    Map<TargetDocumentDescriptor, XDOMOfficeDocument> split(XDOMOfficeDocument xdomOfficeDocument,
        OfficeDocumentSplitterParameters parameters) throws OfficeImporterException;
}
