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
package org.xwiki.officeimporter.internal.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Default implementation of {@link PresentationBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Component
public class DefaultPresentationBuilder implements PresentationBuilder
{
    /**
     * XWiki/2.0 syntax parser used for building the presentation XDOM.
     */
    @Requirement("xwiki/2.0")
    private Parser xwikiParser;

    /**
     * Used to transform the XDOM.
     */
    @Requirement
    private TransformationManager transformationManager;

    /**
     * Component manager used by {@link XDOMOfficeDocument}.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used to obtain document converter.
     */
    @Requirement
    private OpenOfficeManager officeManager;
    
    /**
     * Used to serialize {@link DocumentReference} instances into strings.
     */
    @Requirement
    private EntityReferenceSerializer<String> serializer;
    
    /**
     * Used to access current context document.
     */
    @Requirement
    private DocumentAccessBridge docBridge;

    /**
     * {@inheritDoc}    
     */
    public XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName, DocumentReference reference)
        throws OfficeImporterException
    {
        // Invoke openoffice document converter.
        Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();
        inputStreams.put(officeFileName, officeFileStream);
        Map<String, byte[]> artifacts;
        try {
            artifacts = this.officeManager.getConverter().convert(inputStreams, officeFileName, "output.html");
        } catch (OpenOfficeConverterException ex) {
            String message = "Error while converting document [%s] into html.";
            throw new OfficeImporterException(String.format(message, officeFileName), ex);
        }

        // Create presentation archive.
        byte[] presentationArchive = buildPresentationArchive(artifacts);
        artifacts.clear();
        artifacts.put("presentation.zip", presentationArchive);

        // Build presentation XDOM.
        String stringReference = serializer.serialize(reference);
        return new XDOMOfficeDocument(buildPresentationXDOM(stringReference), artifacts, this.componentManager);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.builder.PresentationBuilder#build(java.io.InputStream, java.lang.String)
     */
    @Deprecated
    public XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName) throws OfficeImporterException
    {
        return build(officeFileStream, officeFileName, docBridge.getCurrentDocumentReference());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.builder.PresentationBuilder#build(byte[])
     */
    @Deprecated
    public XDOMOfficeDocument build(byte[] officeFileData) throws OfficeImporterException
    {
        return build(new ByteArrayInputStream(officeFileData), "input.tmp");
    }

    /**
     * Utility method for building a zip archive from artifacts.
     * 
     * @param artifacts artifacts collected during document import operation.
     * @return the byte[] containing the zip archive off all the artifacts.
     * @throws OfficeImporterException if an I/O exception is encountered.
     */
    private byte[] buildPresentationArchive(Map<String, byte[]> artifacts) throws OfficeImporterException
    {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(bos);
            for (String artifactName : artifacts.keySet()) {
                ZipEntry entry = new ZipEntry(artifactName);
                zos.putNextEntry(entry);
                zos.write(artifacts.get(artifactName));
                zos.closeEntry();
            }
            zos.close();
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new OfficeImporterException("Error while creating presentation archive.", ex);
        }
    }

    /**
     * Utility method for building the presentation XDOM.
     * 
     * @param stringDocumentReference name of the target wiki page.
     * @return presentation XDOM.
     * @throws OfficeImporterException if an error occurs while building the presentation xdom.
     */
    private XDOM buildPresentationXDOM(String stringDocumentReference) throws OfficeImporterException
    {
        // TODO: the XDOM should be generated in pure java and not depends on velocity, html macros and xwiki/2.0
        // parser. This is slow and wrong. Before we need to convert the zip plugin into component to be able to use it
        // directly.
        StringBuffer buffer = new StringBuffer();
        buffer.append("{{velocity}}");
        buffer.append(String.format("#set($mdoc = $xwiki.getDocument(\"%s\"))", stringDocumentReference));
        buffer.append("#set($url = $xwiki.zipexplorer.getFileLink($mdoc, 'presentation.zip', 'output.html'))");
        buffer.append("{{html wiki=\"false\" clean=\"false\"}}");
        buffer.append("<iframe src=\"$url\" frameborder=0 width=800px height=600px></iframe>");
        buffer.append("{{/html}}");
        buffer.append("{{/velocity}}");

        XDOM xdom;
        try {
            xdom = this.xwikiParser.parse(new StringReader(buffer.toString()));

            // Transform XDOM
            this.transformationManager.performTransformations(xdom, Syntax.XWIKI_2_0);

            return xdom;
        } catch (ParseException e) {
            throw new OfficeImporterException(
                "Error while building presentation: failed to parse presentation content", e);
        } catch (TransformationException e) {
            throw new OfficeImporterException("Error while building presentation: failed to transform XDOM", e);
        }
    }
}
