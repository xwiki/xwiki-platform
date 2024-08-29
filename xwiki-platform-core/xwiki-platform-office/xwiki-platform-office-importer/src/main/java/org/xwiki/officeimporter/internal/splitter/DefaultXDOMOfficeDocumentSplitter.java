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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.document.OfficeDocumentArtifact;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.splitter.OfficeDocumentSplitterParameters;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.officeimporter.splitter.XDOMOfficeDocumentSplitter;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.splitter.DocumentSplitter;
import org.xwiki.refactoring.splitter.criterion.HeadingLevelSplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.match.ClassBlockMatcher;

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
     * The {@link DocumentSplitter} used for splitting wiki documents.
     */
    @Inject
    private DocumentSplitter documentSplitter;

    /**
     * Used by {@link org.xwiki.officeimporter.splitter.TargetDocumentDescriptor}.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public Map<TargetDocumentDescriptor, XDOMOfficeDocument> split(XDOMOfficeDocument officeDocument,
        OfficeDocumentSplitterParameters parameters) throws OfficeImporterException
    {
        ComponentManager componentManager = this.componentManagerProvider.get();
        Map<TargetDocumentDescriptor, XDOMOfficeDocument> result =
            new HashMap<TargetDocumentDescriptor, XDOMOfficeDocument>();

        // Create splitting and naming criterion for refactoring.
        SplittingCriterion splittingCriterion =
            new HeadingLevelSplittingCriterion(parameters.getHeadingLevelsToSplit());
        NamingCriterion namingCriterion;
        try {
            namingCriterion = componentManager.getInstance(NamingCriterion.class, parameters.getNamingCriterionHint());
        } catch (ComponentLookupException e) {
            throw new OfficeImporterException("Failed to create the naming criterion.", e);
        }
        namingCriterion.getParameters().setBaseDocumentReference(parameters.getBaseDocumentReference());
        namingCriterion.getParameters().setUseTerminalPages(parameters.isUseTerminalPages());

        // Create the root document required by refactoring module.
        WikiDocument rootDoc =
            new WikiDocument(parameters.getBaseDocumentReference(), officeDocument.getContentDocument(), null);
        List<WikiDocument> documents = this.documentSplitter.split(rootDoc, splittingCriterion, namingCriterion);

        for (WikiDocument doc : documents) {
            // Initialize a target page descriptor.
            TargetDocumentDescriptor targetDocumentDescriptor =
                new TargetDocumentDescriptor(doc.getDocumentReference(), componentManager);
            if (doc.getParent() != null) {
                targetDocumentDescriptor.setParentReference(doc.getParent().getDocumentReference());
            }

            // Rewire artifacts.
            Map<String, OfficeDocumentArtifact> artifactsMap = relocateArtifacts(doc, officeDocument);

            // Create the resulting XDOMOfficeDocument.
            XDOMOfficeDocument splitDocument = new XDOMOfficeDocument(doc.getXdom(), artifactsMap, componentManager,
                officeDocument.getConverterResult());
            result.put(targetDocumentDescriptor, splitDocument);
        }

        return result;
    }

    /**
     * Copy artifacts (i.e. embedded images) from the original office document to a specific wiki document corresponding
     * to a section. Only the artifacts from that section are copied.
     * 
     * @param sectionDoc the newly created wiki document corresponding to a section of the original office document
     * @param officeDocument the office document being splitted into wiki documents
     * @return the relocated artifacts
     */
    private Map<String, OfficeDocumentArtifact> relocateArtifacts(WikiDocument sectionDoc,
        XDOMOfficeDocument officeDocument)
    {
        Map<String, OfficeDocumentArtifact> artifacts = officeDocument.getArtifactsMap();
        Map<String, OfficeDocumentArtifact> result = new HashMap<>();
        List<ImageBlock> imageBlocks =
            sectionDoc.getXdom().getBlocks(new ClassBlockMatcher(ImageBlock.class), Axes.DESCENDANT);
        for (ImageBlock imageBlock : imageBlocks) {
            String imageReference = imageBlock.getReference().getReference();
            OfficeDocumentArtifact artifact = artifacts.get(imageReference);
            if (artifact != null) {
                result.put(imageReference, artifact);
            }
        }
        return result;
    }
}
