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
package org.xwiki.officeimporter.internal.splitter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.splitter.OfficeDocumentSplitterParameters;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.officeimporter.splitter.XDOMOfficeDocumentSplitter;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.splitter.DocumentSplitter;
import org.xwiki.refactoring.splitter.criterion.HeadingLevelSplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.rendering.renderer.BlockRenderer;

/**
 * Default implementation of {@link XDOMOfficeDocumentSplitter}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Component
@Singleton
public class DefaultXDOMOfficeDocumentSplitter implements XDOMOfficeDocumentSplitter
{
    /**
     * Document access bridge.
     */
    @Inject
    private DocumentAccessBridge docBridge;

    /**
     * Plain text renderer used for rendering heading names.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextRenderer;

    /**
     * The {@link DocumentSplitter} used for splitting wiki documents.
     */
    @Inject
    private DocumentSplitter documentSplitter;

    /**
     * Used by {@link org.xwiki.officeimporter.splitter.TargetDocumentDescriptor}.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public Map<TargetDocumentDescriptor, XDOMOfficeDocument> split(XDOMOfficeDocument officeDocument,
        OfficeDocumentSplitterParameters parameters) throws OfficeImporterException
    {
        Map<TargetDocumentDescriptor, XDOMOfficeDocument> result =
            new HashMap<TargetDocumentDescriptor, XDOMOfficeDocument>();

        // Create splitting and naming criterion for refactoring.
        SplittingCriterion splittingCriterion =
            new HeadingLevelSplittingCriterion(parameters.getHeadingLevelsToSplit());
        NamingCriterion namingCriterion =
            DocumentSplitterUtils.getNamingCriterion(parameters, this.docBridge, this.plainTextRenderer);

        // Create the root document required by refactoring module.
        WikiDocument rootDoc =
            new WikiDocument(parameters.getBaseDocumentReference(), officeDocument.getContentDocument(), null);
        List<WikiDocument> documents = this.documentSplitter.split(rootDoc, splittingCriterion, namingCriterion);

        for (WikiDocument doc : documents) {
            // Initialize a target page descriptor.
            TargetDocumentDescriptor targetDocumentDescriptor =
                new TargetDocumentDescriptor(doc.getDocumentReference(), this.componentManager);
            if (doc.getParent() != null) {
                targetDocumentDescriptor.setParentReference(doc.getParent().getDocumentReference());
            }

            // Rewire artifacts.
            Set<File> artifactsFiles = DocumentSplitterUtils.relocateArtifacts(doc, officeDocument);

            // Create the resulting XDOMOfficeDocument.
            XDOMOfficeDocument splitDocument = new XDOMOfficeDocument(doc.getXdom(), artifactsFiles,
                this.componentManager, officeDocument.getConverterResult());
            result.put(targetDocumentDescriptor, splitDocument);
        }

        return result;
    }
}
