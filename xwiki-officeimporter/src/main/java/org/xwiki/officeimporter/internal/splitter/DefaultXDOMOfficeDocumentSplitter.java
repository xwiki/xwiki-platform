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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameFactory;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.splitter.TargetPageDescriptor;
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
public class DefaultXDOMOfficeDocumentSplitter implements XDOMOfficeDocumentSplitter
{
    /**
     * Document access bridge.
     */
    @Requirement
    private DocumentAccessBridge docBridge;

    /**
     * Plain text renderer used for rendering heading names.
     */
    @Requirement("plain/1.0")
    private BlockRenderer plainTextRenderer;

    /**
     * Document name serializer used for serializing document names into strings.
     */
    @Requirement
    private DocumentNameSerializer nameSerializer;
    
    /**
     * Requierd for converting string document names to {@link DocumentName} instances.
     */
    @Requirement
    private DocumentNameFactory nameFactory;
    
    /**
     * The {@link DocumentSplitter} used for splitting wiki documents.
     */
    @Requirement
    private DocumentSplitter documentSplitter;
    
    /**
     * Used by {@link TargetPageDescriptor}.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     */
    public Map<TargetPageDescriptor, XDOMOfficeDocument> split(XDOMOfficeDocument officeDocument,
        int[] headingLevelsToSplit, String namingCriterionHint, DocumentName baseDocumentName)
        throws OfficeImporterException
    {
        // TODO: This code needs to be refactored along with the xwiki-refactoring module code.
        String strBaseDoc = nameSerializer.serialize(baseDocumentName);
        Map<TargetPageDescriptor, XDOMOfficeDocument> result = new HashMap<TargetPageDescriptor, XDOMOfficeDocument>();
        
        // Create splitting and naming criterion for refactoring.
        SplittingCriterion splittingCriterion = new HeadingLevelSplittingCriterion(headingLevelsToSplit);
        NamingCriterion namingCriterion = getNamingCriterion(namingCriterionHint, strBaseDoc);
        
        // Create the root document required by refactoring module.
        WikiDocument rootDoc = new WikiDocument(strBaseDoc, officeDocument.getContentDocument(), null);
        List<WikiDocument> documents = documentSplitter.split(rootDoc, splittingCriterion, namingCriterion);                        
        
        for (WikiDocument doc : documents) {
            // Initialize a target page descriptor.
            DocumentName targetName = nameFactory.createDocumentName(doc.getFullName());            
            TargetPageDescriptor targetPageDescriptor = new TargetPageDescriptor(targetName, this.componentManager);                        
            if (doc.getParent() != null) {
                DocumentName targetParent = nameFactory.createDocumentName(doc.getParent().getFullName());
                targetPageDescriptor.setParentName(targetParent);
            }
            
            // Rewire artifacts.
            Map<String, byte[]> artifacts = new HashMap<String, byte[]>();
            List<ImageBlock> imageBlocks = doc.getXdom().getChildrenByType(ImageBlock.class, true);
            for (ImageBlock imageBlock : imageBlocks) {
                String imageName = imageBlock.getImage().getName();
                artifacts.put(imageName, officeDocument.getArtifacts().remove(imageName));
            }
            
            // Create the resulting XDOMOfficeDocument.
            XDOMOfficeDocument splitDocument = new XDOMOfficeDocument(doc.getXdom(), artifacts, this.componentManager);
            result.put(targetPageDescriptor, splitDocument);
        }
        
        return result;
    }

    /**
     * Utility method for building a {@link NamingCriterion} based on the parameters provided.
     * 
     * @param targetWikiDocument name of the master wiki page.
     * @param params parameters provided for the splitting function.
     * @return a {@link NamingCriterion} based on the parameters provided.
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
