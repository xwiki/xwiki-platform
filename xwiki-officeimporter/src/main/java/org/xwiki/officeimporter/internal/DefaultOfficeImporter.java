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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.OfficeImporterFilter;
import org.xwiki.officeimporter.openoffice.OpenOfficeDocumentConverter;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.splitter.DocumentSplitter;
import org.xwiki.refactoring.splitter.criterion.HeadingLevelSplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.HeadingNameNamingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.PageIndexNamingCriterion;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BlockFilter;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLUtils;

/**
 * Default implementation of the office importer component.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component
public class DefaultOfficeImporter extends AbstractLogEnabled implements OfficeImporter
{
    /**
     * File extensions corresponding to slide presentations.
     */
    public static final List<String> PRESENTATION_FORMAT_EXTENSIONS = Arrays.asList("ppt", "odp");

    /**
     * Name of the presentation archive.
     */
    public static final String PRESENTATION_ARCHIVE_NAME = "presentation.zip";

    /**
     * Document access bridge used to access wiki documents.
     */
    @Requirement
    private DocumentAccessBridge docBridge;

    /**
     * OpenOffice document converter.
     */
    @Requirement
    private OpenOfficeDocumentConverter ooConverter;

    /**
     * OpenOffice html cleaner.
     */
    @Requirement("openoffice")
    private HTMLCleaner ooHtmlCleaner;

    /**
     * XHTML/1.0 syntax parser.
     */
    @Requirement("xhtml/1.0")
    private Parser xHtmlParser;

    /**
     * XWiki/2.0 syntax parser.
     */
    @Requirement("xwiki/2.0")
    private Parser xwikiParser;

    /**
     * Factory to get various syntax renderers.
     */
    @Requirement
    private PrintRendererFactory rendererFactory;

    /**
     * The {@link DocumentSplitter} used for splitting documents.
     */
    @Requirement
    private DocumentSplitter documentSplitter;

    /**
     * {@inheritDoc}
     */
    public void importStream(InputStream documentStream, String documentFormat, String targetWikiDocument,
        Map<String, String> params) throws OfficeImporterException
    {
        params.put("targetDocument", targetWikiDocument);
        OfficeImporterFileStorage storage =
            new OfficeImporterFileStorage("xwiki-office-importer-" + docBridge.getCurrentUser());
        OfficeImporterFilter importerFilter = getImporterFilter(params);
        try {
            Map<String, InputStream> artifacts = ooConverter.convert(documentStream, storage);
            if (isPresentation(documentFormat)) {
                byte[] archive = buildPresentationArchive(artifacts);
                docBridge.setAttachmentContent(targetWikiDocument, PRESENTATION_ARCHIVE_NAME, archive);
                String xwikiPresentationCode = buildPresentationFrameCode(PRESENTATION_ARCHIVE_NAME, "output.html");
                saveDocument(targetWikiDocument, null, xwikiPresentationCode, isAppendRequest(params));
            } else {
                InputStreamReader reader = new InputStreamReader(artifacts.remove("output.html"), "UTF-8");
                HTMLCleanerConfiguration configuration = this.ooHtmlCleaner.getDefaultConfiguration();
                configuration.setParameters(params);
                Document xhtmlDoc = this.ooHtmlCleaner.clean(reader, configuration);
                importerFilter.filter(targetWikiDocument, xhtmlDoc);
                HTMLUtils.stripHTMLEnvelope(xhtmlDoc);
                XDOM xdom = xHtmlParser.parse(new StringReader(XMLUtils.toString(xhtmlDoc)));
                importerFilter.filter(targetWikiDocument, xdom, false);
                if (!isSplitRequest(params)) {
                    saveDocument(targetWikiDocument, extractTitle(xdom), importerFilter.filter(targetWikiDocument,
                        renderXdom(xdom, XWIKI_20), false), isAppendRequest(params));
                    attachArtifacts(targetWikiDocument, artifacts);
                } else {
                    splitImport(targetWikiDocument, xdom, artifacts, params, importerFilter);
                }
            }
        } catch (Exception ex) {
            throw new OfficeImporterException(ex.getMessage(), ex);
        } finally {
            storage.cleanUp();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String importAttachment(String documentName, String attachmentName, Map<String, String> params)
        throws OfficeImporterException
    {
        params.put("targetDocument", documentName);
        OfficeImporterFileStorage storage =
            new OfficeImporterFileStorage("xwiki-office-importer-" + docBridge.getCurrentUser());
        try {
            ByteArrayInputStream bis =
                new ByteArrayInputStream(docBridge.getAttachmentContent(documentName, attachmentName));
            Map<String, InputStream> artifacts = ooConverter.convert(bis, storage);
            if (isPresentation(attachmentName)) {
                byte[] archive = buildPresentationArchive(artifacts);
                docBridge.setAttachmentContent(documentName, PRESENTATION_ARCHIVE_NAME, archive);
                String xwikiPresentationCode = buildPresentationFrameCode(PRESENTATION_ARCHIVE_NAME, "output.html");
                XDOM xdom = xwikiParser.parse(new StringReader(xwikiPresentationCode));
                return renderXdom(xdom, XHTML_10);
            } else {
                InputStreamReader reader = new InputStreamReader(artifacts.remove("output.html"), "UTF-8");
                attachArtifacts(documentName, artifacts);
                HTMLCleanerConfiguration configuration = this.ooHtmlCleaner.getDefaultConfiguration();
                configuration.setParameters(params);
                Document xhtmlDoc = this.ooHtmlCleaner.clean(reader, configuration);
                HTMLUtils.stripHTMLEnvelope(xhtmlDoc);
                return XMLUtils.toString(xhtmlDoc);
            }
        } catch (Exception ex) {
            throw new OfficeImporterException(ex.getMessage(), ex);
        } finally {
            storage.cleanUp();
        }
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
        return (splitDocumentParam != null) ? splitDocumentParam.equals("true") : false;
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
        return (appendParam != null) ? appendParam.equals("true") : false;
    }

    /**
     * Utility method for rendering a given xdom into target syntax.
     * 
     * @param xdom the {@link XDOM}.
     * @param targetSyntax target syntax.
     * @return the rendered content in target syntax.
     */
    private String renderXdom(XDOM xdom, Syntax targetSyntax)
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        Listener listener = this.rendererFactory.createRenderer(targetSyntax, printer);
        xdom.traverse(listener);
        return printer.toString();
    }

    /**
     * Generates a suitable title for the document represented by newDoc.
     * 
     * @param xdom the {@link XDOM} of the newly split document.
     * @return a string representing the title for the new document or null.
     */
    private String extractTitle(XDOM xdom)
    {
        String title = null;
        // Filter all top-level header blocks.
        List<HeaderBlock> headerBlocks = xdom.getChildrenByType(HeaderBlock.class, false);
        // Filter all top-level section blocks.
        List<SectionBlock> sectionBlocks = xdom.getChildrenByType(SectionBlock.class, false);
        // If no top-level header blocks are present, search inside top-level section blocks.
        if (headerBlocks.isEmpty() && !sectionBlocks.isEmpty()) {
            headerBlocks = sectionBlocks.get(0).getChildrenByType(HeaderBlock.class, true);
        }
        // If a suitable header block is found, filter it's content and generate the title.
        if (!headerBlocks.isEmpty()) {
            Block firstHeader = headerBlocks.get(0);
            // Clone the header block and remove any unwanted stuff
            Block clonedHeaderBlock = firstHeader.clone(new BlockFilter()
            {
                public List<Block> filter(Block block)
                {
                    List<Block> blocks = new ArrayList<Block>();
                    if (block instanceof WordBlock || block instanceof SpaceBlock
                        || block instanceof SpecialSymbolBlock) {
                        blocks.add(block);
                    }
                    return blocks;
                }
            });
            title = renderXdom(new XDOM(clonedHeaderBlock.getChildren()), XWIKI_20);
            // Strip line-feed and new-line characters if present.
            title = title.replaceAll("[\n\r]", "");
            // Truncate long titles.
            if (title.length() > 255) {
                title = title.substring(0, 255);
            }
        }
        return title;
    }

    /**
     * Splits the document represented by inputReader into multiple documents.
     * 
     * @param documentName name of the master document, this name will be used as a basis for naming the resulting
     *            documents.
     * @param xdom the {@link XDOM} of the master document that needs to be split.
     * @param params additional parameters for the split operation (split criterion, naming criterion etc.)
     * @param importerFilter the {@link OfficeImporterFilter} to be used for filtering split documents.
     * @throws OfficeImporterException if a parsing error occurs.
     */
    private void splitImport(String documentName, XDOM xdom, Map<String, InputStream> artifacts,
        Map<String, String> params, OfficeImporterFilter importerFilter) throws OfficeImporterException
    {
        SplittingCriterion splittingCriterion = getSplittingCriterion(documentName, params);
        NamingCriterion namingCriterion = getNamingCriterion(documentName, params);
        try {
            WikiDocument rootDoc = new WikiDocument(documentName, xdom, null);
            List<WikiDocument> documents = documentSplitter.split(rootDoc, splittingCriterion, namingCriterion);
            for (WikiDocument doc : documents) {
                XDOM childXdom = doc.getXdom();
                // Apply extended filtering.
                importerFilter.filter(doc.getFullName(), childXdom, true);
                // Set parent document (if possible).
                if (doc.getParent() != null) {
                    DocumentName parentName = docBridge.getDocumentName(doc.getParent().getFullName());
                    docBridge.getDocument(doc.getFullName()).setParent(parentName);
                }
                // Extract all image artifacts & attach them to the current document.
                Map<String, InputStream> tempArtifacts = new HashMap<String, InputStream>();
                List<ImageBlock> imageBlocks = childXdom.getChildrenByType(ImageBlock.class, true);
                for (ImageBlock imageBlock : imageBlocks) {
                    String imageName = imageBlock.getImage().getName();
                    tempArtifacts.put(imageName, artifacts.remove(imageName));
                }
                attachArtifacts(doc.getFullName(), tempArtifacts);
                String content = renderXdom(childXdom, XWIKI_20);
                // Check if this is an append request (only root doc can be appended).
                boolean append = doc.equals(rootDoc) ? isAppendRequest(params) : false;
                saveDocument(doc.getFullName(), extractTitle(doc.getXdom()), importerFilter.filter(doc.getFullName(),
                    content, true), append);
            }
        } catch (Exception ex) {
            throw new OfficeImporterException("Internal error while importing document.", ex);
        }
    }

    /**
     * Utility method for building a {@link SplittingCriterion} based on the parameters provided.
     * 
     * @param targetWikiDocument name of the master wiki page.
     * @param params parameters provided for the splitting function.
     * @return a {@link DocumentSplitter} based on the parameters provided.
     * @throws OfficeImporterException if it's not possible to determine / build the splitting criterion.
     */
    private SplittingCriterion getSplittingCriterion(String targetWikiDocument, Map<String, String> params)
        throws OfficeImporterException
    {
        String headingLevelsParam = params.get("headingLevelsToSplit");
        if (headingLevelsParam != null) {
            try {
                String[] headingLevelsStringArray = headingLevelsParam.split(",");
                int[] headingLevelsIntArray = new int[headingLevelsStringArray.length];
                for (int i = 0; i < headingLevelsStringArray.length; i++) {
                    headingLevelsIntArray[i] = Integer.parseInt(headingLevelsStringArray[i]);
                }
                return new HeadingLevelSplittingCriterion(headingLevelsIntArray);
            } catch (NumberFormatException ex) {
                throw new OfficeImporterException("Unable to determine splitting criterion.");
            }
        }
        throw new OfficeImporterException("Unable to determine splitting criterion.");
    }

    /**
     * Utility method for building a {@link NamingCriterion} based on the parameters provided.
     * 
     * @param targetWikiDocument name of the master wiki page.
     * @param params parameters provided for the splitting function.
     * @return a {@link NamingCriterion} based on the parameters provided.
     */
    private NamingCriterion getNamingCriterion(String targetWikiDocument, Map<String, String> params)
        throws OfficeImporterException
    {
        String namingMethodParam = params.get("childPagesNamingMethod");
        if (namingMethodParam == null || namingMethodParam.equals("")) {
            throw new OfficeImporterException("Unable to determine child pages naming method.");
        } else if (namingMethodParam.equals("headingNames")) {
            return new HeadingNameNamingCriterion(targetWikiDocument, docBridge, rendererFactory, false);
        } else if (namingMethodParam.equals("mainPageNameAndHeading")) {
            return new HeadingNameNamingCriterion(targetWikiDocument, docBridge, rendererFactory, true);
        } else if (namingMethodParam.equals("mainPageNameAndNumbering")) {
            return new PageIndexNamingCriterion(targetWikiDocument, docBridge);
        } else {
            throw new OfficeImporterException("The specified naming criterion is not implemented yet.");
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
     * Utility method for saving xwiki/2.0 content generated from the import operation.
     * 
     * @param documentName name of the xwiki document where the content should be saved into.
     * @param title title to be set for the document, should be null if not required.
     * @param content the string content (result of the import operation)
     * @param append if the content should be appended in case of an existing document.
     * @throws OfficeImporterException if an error occurs while saving the document.
     */
    private void saveDocument(String documentName, String title, String content, boolean append)
        throws OfficeImporterException
    {
        try {
            if (docBridge.exists(documentName) && append) {
                // Must make sure that the target document is in xwiki 2.0 syntax.
                if (!docBridge.getDocumentSyntaxId(documentName).equals(XWIKI_20.toIdString())) {
                    throw new OfficeImporterException("Target document is not an xwiki 2.0 document.");
                }
                String oldContent = docBridge.getDocumentContent(documentName);
                // Append the new content.
                docBridge.setDocumentSyntaxId(documentName, XWIKI_20.toIdString());
                // Insert a newline between the old content and the appended content.
                docBridge.setDocumentContent(documentName, oldContent + "\n" + content, "Updated by office importer",
                    false);
            } else {
                if (null != title) {
                    docBridge.getDocument(documentName).setTitle(title);
                }
                docBridge.setDocumentSyntaxId(documentName, XWIKI_20.toIdString());
                docBridge.setDocumentContent(documentName, content, "Updated by office importer", false);
            }
        } catch (Exception ex) {
            throw new OfficeImporterException(ex.getMessage(), ex);
        }
    }

    /**
     * Attached the given artifacts into target wiki document.
     * 
     * @param documentName target wiki document name.
     * @param artifacts artifacts collected during the document conversion.
     */
    private void attachArtifacts(String documentName, Map<String, InputStream> artifacts)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len = 0;
        for (String artifactName : artifacts.keySet()) {
            try {
                InputStream stream = artifacts.get(artifactName);
                while ((len = stream.read(buf)) > 0) {
                    bos.write(buf, 0, len);
                }
                docBridge.setAttachmentContent(documentName, artifactName, bos.toByteArray());
                bos.reset();
            } catch (Exception ex) {
                getLogger().error("Error while attaching artifact.", ex);
                // Skip the artifact.
            }
        }
    }

    /**
     * Utility method for building xwiki 2.0 code required for displaying an office presentation.
     * 
     * @param zipFilename name of the presentation zip archive.
     * @param index the html file (in the archive) to begin the presentation.
     * @return the xwiki code for displaying the presentation.
     */
    private String buildPresentationFrameCode(String zipFilename, String index)
    {
        return "{{velocity}}#set($url=$xwiki.zipexplorer.getFileLink($doc, \"" + zipFilename + "\", \"" + index
            + "\")){{html}}<iframe src=\"$url\" frameborder=0 width=800px height=600px></iframe>{{/html}}{{/velocity}}";
    }

    /**
     * Utility method for building a zip archive for presentation imports.
     * 
     * @param artifacts artifacts collected during the document conversion.
     * @return the byte[] containing the zip archive.
     * @throws OfficeImporterException if an I/O exception is encountered.
     */
    private byte[] buildPresentationArchive(Map<String, InputStream> artifacts) throws OfficeImporterException
    {
        try {
            ByteArrayOutputStream zipBos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(zipBos);
            ByteArrayOutputStream tempBos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len = 0;
            for (String artifactName : artifacts.keySet()) {
                ZipEntry entry = new ZipEntry(artifactName);
                zos.putNextEntry(entry);
                InputStream stream = artifacts.get(artifactName);
                while ((len = stream.read(buf)) > 0) {
                    tempBos.write(buf, 0, len);
                }
                zos.write(tempBos.toByteArray());
                zos.closeEntry();
                tempBos.reset();
            }
            zos.close();
            return zipBos.toByteArray();
        } catch (IOException ex) {
            throw new OfficeImporterException("Error while creating presentation archive.", ex);
        }
    }
}
