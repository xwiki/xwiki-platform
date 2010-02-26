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
package org.xwiki.officeimporter.internal;

import groovy.lang.GroovyClassLoader;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.OfficeImporterFilter;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.officeimporter.splitter.XDOMOfficeDocumentSplitter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.xml.html.HTMLUtils;

/**
 * Default implementation of the office importer component.
 * 
 * @version $Id$
 * @since 1.8M1
 * @deprecated use individual document builder components instead since 2.2M1
 */
@Component
@Deprecated
public class DefaultOfficeImporter extends AbstractLogEnabled implements OfficeImporter
{
    /**
     * File extensions corresponding to slide presentations.
     */
    public static final List<String> PRESENTATION_FORMAT_EXTENSIONS = Arrays.asList("ppt", "odp");

    /**
     * Error message to be used for indicating invalid splitting criterion.
     */
    private static final String ERROR_INVALID_SPLITTING_CRITERION = "Unable to determine splitting criterion.";

    /**
     * Document access bridge used to access wiki documents.
     */
    @Requirement
    private DocumentAccessBridge docBridge;

    /**
     * Used to dynamically lookup for components.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used for parsing document name strings.
     */
    @Requirement("currentmixed")
    private DocumentReferenceResolver currentMixedDocumentReferenceResolver;

    /**
     * Used for importing office documents.
     */
    @Requirement
    private XHTMLOfficeDocumentBuilder xhtmlOfficeDocumentBuilder;

    /**
     * Used for importing office documents.
     */
    @Requirement
    private XDOMOfficeDocumentBuilder xdomOfficeDocumentBuilder;

    /**
     * Used for importing (presentation) office documents.
     */
    @Requirement
    private PresentationBuilder presentationBuilder;

    /**
     * Used for splitting office imports.
     */
    @Requirement
    private XDOMOfficeDocumentSplitter xdomOfficeDocumentSplitter;

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void importStream(InputStream documentStream, String documentFormat, String targetWikiDocument,
        Map<String, String> params) throws OfficeImporterException
    {
        // Deduct an office file name from documentFormat argument.
        String extension = documentFormat.substring(documentFormat.lastIndexOf('.') + 1);
        String officeFileName = "input." + extension;

        DocumentReference baseDocument = this.currentMixedDocumentReferenceResolver.resolve(targetWikiDocument);
        if (isPresentation(documentFormat)) {
            XDOMOfficeDocument presentation = presentationBuilder.build(documentStream, officeFileName, baseDocument);
            saveDocument(presentation, new TargetDocumentDescriptor(baseDocument, this.componentManager), null,
                isAppendRequest(params), false);
        } else {
            OfficeImporterFilter importerFilter = getImporterFilter(params);
            XHTMLOfficeDocument xhtmlDoc =
                xhtmlOfficeDocumentBuilder.build(documentStream, officeFileName, baseDocument,
                    shouldFilterStyles(params));
            importerFilter.filter(targetWikiDocument, xhtmlDoc.getContentDocument());
            XDOMOfficeDocument xdomDoc = xdomOfficeDocumentBuilder.build(xhtmlDoc);
            importerFilter.filter(targetWikiDocument, xdomDoc.getContentDocument(), false);
            if (!isSplitRequest(params)) {
                saveDocument(xdomDoc, new TargetDocumentDescriptor(baseDocument, this.componentManager),
                    importerFilter, isAppendRequest(params), false);
            } else {
                int[] headingLevels = getHeadingLevelsToSplit(params);
                String namingCriterionHint = params.get("childPagesNamingMethod");
                Map<TargetDocumentDescriptor, XDOMOfficeDocument> results =
                    xdomOfficeDocumentSplitter.split(xdomDoc, headingLevels, namingCriterionHint, baseDocument);
                for (Map.Entry<TargetDocumentDescriptor, XDOMOfficeDocument> result : results.entrySet()) {
                    boolean append = result.getKey().getDocumentReference().equals(baseDocument);
                    saveDocument(result.getValue(), result.getKey(), importerFilter, append, true);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public String importAttachment(String strDocumentName, String strAttachmentFileName, Map<String, String> params)
        throws OfficeImporterException
    {
        DocumentReference documentReference = this.currentMixedDocumentReferenceResolver.resolve(strDocumentName);
        AttachmentReference attachmentReference = new AttachmentReference(strAttachmentFileName, documentReference);

        InputStream attachmentStream;
        try {
            attachmentStream = this.docBridge.getAttachmentContent(attachmentReference);
        } catch (Exception ex) {
            throw new OfficeImporterException("Error while reading attachment", ex);
        }

        String result = null;
        if (isPresentation(strAttachmentFileName)) {
            XDOMOfficeDocument presentation =
                presentationBuilder.build(attachmentStream, strAttachmentFileName, documentReference);
            // Annotated xhtml renderer is required here because WYSIWYG needs these annotations to interpret the macros
            // used in the presentation XDOM. Fortunately WYSIWYG module is the only module which depends on this
            // OfficeImporter#importAttachment() deprecated API so we don't have any side effects of this. However we
            // need to update xwiki-web-gwt-wysiwyg-server module so that it does not depend on this deprecated API
            // anymore and instead directly use PresentaionBuilder component.
            result = presentation.getContentAsString("annotatedxhtml/1.0");
            attachArtifacts(strDocumentName, presentation.getArtifacts());
        } else {
            XHTMLOfficeDocument xhtmlDoc =
                xhtmlOfficeDocumentBuilder.build(attachmentStream, strAttachmentFileName, documentReference,
                    shouldFilterStyles(params));
            HTMLUtils.stripHTMLEnvelope(xhtmlDoc.getContentDocument());
            result = xhtmlDoc.getContentAsString();
            attachArtifacts(strDocumentName, xhtmlDoc.getArtifacts());
        }
        return result;
    }

    /**
     * Utility method for saving an {@link XDOMOfficeDocument} into a wiki page.
     * 
     * @param document the xdom office document.
     * @param targetDescriptor where to save the office document.
     * @param importerFilter filter to be used on the final content just before saving.
     * @param append whether content should be appended if the target document already exists.
     * @param isSplit if this document is a newly split one.
     * @throws OfficeImporterException if an error occurs while saving into the xwiki page.
     */
    private void saveDocument(XDOMOfficeDocument document, TargetDocumentDescriptor targetDescriptor,
        OfficeImporterFilter importerFilter, boolean append, boolean isSplit) throws OfficeImporterException
    {
        String target = targetDescriptor.getDocumentReferenceAsString();
        String parent = targetDescriptor.getParentReferenceAsString();
        String title = document.getTitle();
        String content = document.getContentAsString();
        content = (null != importerFilter) ? importerFilter.filter(target, content, isSplit) : content;
        try {
            if (docBridge.exists(target) && append) {
                // Must make sure that the target document is in xwiki 2.0 syntax.
                if (!docBridge.getDocumentSyntaxId(target).equals(Syntax.XWIKI_2_0.toIdString())) {
                    throw new OfficeImporterException("Target document is not an xwiki/2.0 document.");
                }
                String oldContent = docBridge.getDocumentContent(target);
                // Insert a newline between the old content and the appended content.
                docBridge.setDocumentContent(target, oldContent + "\n" + content, "Updated by office importer", false);
            } else {
                docBridge.setDocumentSyntaxId(target, Syntax.XWIKI_2_0.toIdString());

                if (title != null) {
                    docBridge.setDocumentTitle(targetDescriptor.getDocumentReference(), title);
                }

                if (parent != null) {
                    docBridge.setDocumentParentReference(targetDescriptor.getDocumentReference(),
                        targetDescriptor.getParentReference());
                }

                docBridge.setDocumentContent(target, content, "Created by office importer", false);
            }
            // Attach the artifacts.
            attachArtifacts(target, document.getArtifacts());
        } catch (Exception ex) {
            throw new OfficeImporterException("Error while saving office document.", ex);
        }
    }

    /**
     * Utility method for attaching artifacts into a wiki page.
     * 
     * @param documentName name of the document.
     * @param artifacts artifacts.
     */
    private void attachArtifacts(String documentName, Map<String, byte[]> artifacts)
    {
        for (Map.Entry<String, byte[]> artifact : artifacts.entrySet()) {
            try {
                docBridge.setAttachmentContent(documentName, artifact.getKey(), artifact.getValue());
            } catch (Exception ex) {
                // Log the error and skip the artifact.
                getLogger().error("Error while attaching artifact.", ex);
            }
        }
    }

    /**
     * Utility method for building a {@link OfficeImporterFilter} suitable for this import operation. If no external
     * filter has been specified a {@link DefaultOfficeImporterFilter} will be used.
     * 
     * @param params additional parameters provided for this import operation.
     * @return an {@link OfficeImporterFilter}.
     */
    private OfficeImporterFilter getImporterFilter(Map<String, String> params)
    {
        OfficeImporterFilter importerFilter = null;
        String groovyFilterParam = params.get("groovyFilter");
        if (null != groovyFilterParam && !groovyFilterParam.equals("")) {
            try {
                String groovyScript = docBridge.getDocumentContent(groovyFilterParam);
                GroovyClassLoader gcl = new GroovyClassLoader();
                importerFilter = (OfficeImporterFilter) gcl.parseClass(groovyScript).newInstance();
            } catch (Throwable t) {
                getLogger().warn("Could not build the groovy filter.", t);
            }
        }
        importerFilter = (importerFilter == null) ? new DefaultOfficeImporterFilter() : importerFilter;
        importerFilter.setDocBridge(docBridge);
        return importerFilter;
    }

    /**
     * Utility method for extracting heading levels used for splitting.
     * 
     * @param params additional parameters passed in for the import operation.
     * @return heading levels array.
     * @throws OfficeImporterException if an error occurs while extracting heading levels.
     */
    private int[] getHeadingLevelsToSplit(Map<String, String> params) throws OfficeImporterException
    {
        String headingLevelsParam = params.get("headingLevelsToSplit");
        if (headingLevelsParam != null) {
            try {
                String[] headingLevelsStringArray = headingLevelsParam.split(",");
                int[] headingLevelsIntArray = new int[headingLevelsStringArray.length];
                for (int i = 0; i < headingLevelsStringArray.length; i++) {
                    headingLevelsIntArray[i] = Integer.parseInt(headingLevelsStringArray[i]);
                }
                return headingLevelsIntArray;
            } catch (NumberFormatException ex) {
                throw new OfficeImporterException(ERROR_INVALID_SPLITTING_CRITERION);
            }
        }
        throw new OfficeImporterException(ERROR_INVALID_SPLITTING_CRITERION);
    }

    /**
     * Utility method for checking if a file name corresponds to an office presentation.
     * 
     * @param format file name or the extension.
     * @return true if the file name / extension represents an office presentation format.
     */
    private boolean isPresentation(String format)
    {
        String extension = format.substring(format.lastIndexOf('.') + 1);
        return PRESENTATION_FORMAT_EXTENSIONS.contains(extension);
    }

    /**
     * Utility method for checking if a request is made to split a document while importing.
     * 
     * @param params additional parameters passed in for the import operation.
     * @return true if the params indicate that this is a import & split request.
     */
    private boolean isSplitRequest(Map<String, String> params)
    {
        String splitDocumentParam = params.get("splitDocument");
        return (splitDocumentParam != null) ? splitDocumentParam.equals(Boolean.toString(true)) : false;
    }

    /**
     * Utility method for checking if a request is made to append the importer result to an existing page.
     * 
     * @param params additional parameters passed in for the import operation.
     * @return true if the params indicate that this is an append request.
     */
    private boolean isAppendRequest(Map<String, String> params)
    {
        String appendParam = params.get("appendContent");
        return (appendParam != null) ? appendParam.equals(Boolean.toString(true)) : false;
    }

    /**
     * Utility method for checking if a request specifies styles be filtered from the import.
     * 
     * @param params additional parameters passed in for the import operation.
     * @return whether styles should be filtered or not.
     */
    private boolean shouldFilterStyles(Map<String, String> params)
    {
        String filterStylesParam = params.get("filterStyles");
        return (filterStylesParam != null) ? filterStylesParam.equals("strict") : false;
    }
}
