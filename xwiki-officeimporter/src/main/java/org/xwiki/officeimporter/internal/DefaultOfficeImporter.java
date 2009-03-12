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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.openoffice.OpenOfficeDocumentConverter;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;

/**
 * Default implementation of the office importer component.
 * 
 * @version $Id$
 * @since 1.8M1
 */
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
    private DocumentAccessBridge docBridge;

    /**
     * OpenOffice document converter.
     */
    private OpenOfficeDocumentConverter ooConverter;

    /**
     * OpenOffice html cleaner.
     */
    private HTMLCleaner ooHtmlCleaner;

    /**
     * XHTML/1.0 syntax parser.
     */
    private Parser xHtmlParser;

    /**
     * XWiki/2.0 syntax parser.
     */
    private Parser xwikiParser;

    /**
     * Factory to get the XHTML renderer to output XHTML.
     */
    private PrintRendererFactory rendererFactory;

    /**
     * {@inheritDoc}
     */
    public void importStream(InputStream documentStream, String documentFormat, String targetWikiDocument,
        Map<String, String> params) throws OfficeImporterException
    {
        params.put("targetDocument", targetWikiDocument);
        OfficeImporterFileStorage storage =
            new OfficeImporterFileStorage("xwiki-office-importer-" + docBridge.getCurrentUser());
        try {
            Map<String, InputStream> artifacts = ooConverter.convert(documentStream, storage);
            docBridge.setDocumentSyntaxId(targetWikiDocument, XWIKI_20.toIdString());
            if (isPresentation(documentFormat)) {
                byte[] archive = buildPresentationArchive(artifacts);
                docBridge.setAttachmentContent(targetWikiDocument, PRESENTATION_ARCHIVE_NAME, archive);
                String xwikiPresentationCode = buildPresentationFrameCode(PRESENTATION_ARCHIVE_NAME, "output.html");
                docBridge.setDocumentContent(targetWikiDocument, xwikiPresentationCode,
                    "Content updated by office importer", false);
            } else {
                InputStreamReader reader = new InputStreamReader(artifacts.remove("output.html"), "UTF-8");
                Document xhtmlDoc = ooHtmlCleaner.clean(reader, params);
                XMLUtils.stripHTMLEnvelope(xhtmlDoc);
                String xwikiCode = convert(new StringReader(XMLUtils.toString(xhtmlDoc)), xHtmlParser, XWIKI_20);
                docBridge
                    .setDocumentContent(targetWikiDocument, xwikiCode, "Content updated by office importer", false);
                attachArtifacts(targetWikiDocument, artifacts);
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
                return convert(new StringReader(xwikiPresentationCode), xwikiParser, XHTML_10);
            } else {
                InputStreamReader reader = new InputStreamReader(artifacts.remove("output.html"), "UTF-8");
                attachArtifacts(documentName, artifacts);
                Document xhtmlDoc = ooHtmlCleaner.clean(reader, params);
                XMLUtils.stripHTMLEnvelope(xhtmlDoc);
                return XMLUtils.toString(xhtmlDoc);
            }
        } catch (Exception ex) {
            throw new OfficeImporterException(ex.getMessage(), ex);
        } finally {
            storage.cleanUp();
        }
    }

    /**
     * Converts the given code into targetSyntax.
     * 
     * @param inputReader the input code.
     * @param parser parser to be used for parsing the input.
     * @param targetSyntax expected syntax.
     * @return the output code in target syntax.
     * @throws OfficeImporterException if a parsing error occurs.
     */
    private String convert(Reader inputReader, Parser parser, Syntax targetSyntax) throws OfficeImporterException
    {
        try {
            XDOM xdom = parser.parse(inputReader);
            WikiPrinter printer = new DefaultWikiPrinter();
            Listener listener = this.rendererFactory.createRenderer(targetSyntax, printer);
            xdom.traverse(listener);
            return printer.toString();
        } catch (ParseException ex) {
            throw new OfficeImporterException("Internal error while parsing content.", ex);
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
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(bos);
            for (String artifactName : artifacts.keySet()) {
                ZipEntry entry = new ZipEntry(artifactName);
                zos.putNextEntry(entry);
                zos.write(readStream(artifacts.get(artifactName)));
                zos.closeEntry();
            }
            zos.close();
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new OfficeImporterException("Error while creating presentation archive.", ex);
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
        for (String artifactName : artifacts.keySet()) {
            try {
                docBridge.setAttachmentContent(documentName, artifactName, readStream(artifacts.get(artifactName)));
            } catch (Exception ex) {
                getLogger().error("Error while attaching artifact.", ex);
                // Skip the artifact.
            }
        }
    }

    /**
     * Utility method for extracting bytes from a stream.
     * 
     * @param stream the input stream.
     * @return collected bytes.
     * @throws IOException if an I/O error occurs.
     */
    private byte[] readStream(InputStream stream) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = stream.read(buf)) > 0) {
            bos.write(buf, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }
}
