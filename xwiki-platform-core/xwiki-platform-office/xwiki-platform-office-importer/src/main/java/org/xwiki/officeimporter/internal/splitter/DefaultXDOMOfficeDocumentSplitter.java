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
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.officeimporter.splitter.XDOMOfficeDocumentSplitter;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.splitter.DocumentSplitter;
import org.xwiki.refactoring.splitter.criterion.HeadingLevelSplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.HeadingNameNamingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.PageIndexNamingCriterion;
import org.xwiki.rendering.block.ImageBlock;
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
     * Document name serializer used for serializing document names into strings.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Required for converting string document names to {@link org.xwiki.model.reference.DocumentReference} instances.
     */
    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

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
        int[] headingLevelsToSplit, String namingCriterionHint, DocumentReference baseDocumentReference)
        throws OfficeImporterException
    {
        // TODO: This code needs to be refactored along with the xwiki-refactoring module code.
        String strBaseDoc = this.entityReferenceSerializer.serialize(baseDocumentReference);
        Map<TargetDocumentDescriptor, XDOMOfficeDocument> result =
            new HashMap<TargetDocumentDescriptor, XDOMOfficeDocument>();

        // Create splitting and naming criterion for refactoring.
        SplittingCriterion splittingCriterion = new HeadingLevelSplittingCriterion(headingLevelsToSplit);
        NamingCriterion namingCriterion = getNamingCriterion(namingCriterionHint, strBaseDoc);

        // Create the root document required by refactoring module.
        WikiDocument rootDoc = new WikiDocument(strBaseDoc, officeDocument.getContentDocument(), null);
        List<WikiDocument> documents = documentSplitter.split(rootDoc, splittingCriterion, namingCriterion);

        for (WikiDocument doc : documents) {
            // Initialize a target page descriptor.
            DocumentReference targetReference = this.currentMixedDocumentReferenceResolver.resolve(doc.getFullName());
            TargetDocumentDescriptor targetDocumentDescriptor =
                new TargetDocumentDescriptor(targetReference, this.componentManager);
            if (doc.getParent() != null) {
                DocumentReference targetParent =
                    this.currentMixedDocumentReferenceResolver.resolve(doc.getParent().getFullName());
                targetDocumentDescriptor.setParentReference(targetParent);
            }

            // Rewire artifacts.
            Map<String, byte[]> artifacts = new HashMap<String, byte[]>();
            List<ImageBlock> imageBlocks = doc.getXdom().getChildrenByType(ImageBlock.class, true);
            for (ImageBlock imageBlock : imageBlocks) {
                String imageReference = imageBlock.getReference().getReference();
                artifacts.put(imageReference, officeDocument.getArtifacts().remove(imageReference));
            }

            // Create the resulting XDOMOfficeDocument.
            XDOMOfficeDocument splitDocument = new XDOMOfficeDocument(doc.getXdom(), artifacts, this.componentManager);
            result.put(targetDocumentDescriptor, splitDocument);
        }

        return result;
    }

    /**
     * Utility method for building a {@link NamingCriterion} based on the parameters provided.
     * 
     * @param namingCriterionId naming criterion identifier.
     * @param baseDocument reference document name to be used when generating names.
     * @return a {@link NamingCriterion} based on the parameters provided.
     * @throws OfficeImporterException if there is no naming criterion matching the given naming criterion id.
     */
    private NamingCriterion getNamingCriterion(String namingCriterionId, String baseDocument)
        throws OfficeImporterException
    {
        // TODO: This code needs to be refactored along with the xwiki-refactoring module code.
        if (namingCriterionId.equals("headingNames")) {
            return new HeadingNameNamingCriterion(baseDocument, this.docBridge, this.plainTextRenderer, false);
        } else if (namingCriterionId.equals("mainPageNameAndHeading")) {
            return new HeadingNameNamingCriterion(baseDocument, this.docBridge, this.plainTextRenderer, true);
        } else if (namingCriterionId.equals("mainPageNameAndNumbering")) {
            return new PageIndexNamingCriterion(baseDocument, this.docBridge);
        } else {
            throw new OfficeImporterException("The specified naming criterion is not implemented yet.");
        }
    }
}
