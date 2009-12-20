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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

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
     * {@inheritDoc}    
     */
    public XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName) throws OfficeImporterException
    {
        // Invoke openoffice document converter.
        Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();
        inputStreams.put(officeFileName, officeFileStream);
        Map<String, byte[]> artifacts;
        try {
            artifacts = officeManager.getConverter().convert(inputStreams, officeFileName, "output.html");
        } catch (OpenOfficeConverterException ex) {
            String message = "Error while converting document [%s] into html.";
            throw new OfficeImporterException(String.format(message, officeFileName), ex);
        }
        
        // Create presentation archive.
        byte[] presentationArchive = buildPresentationArchive(artifacts);
        artifacts.clear();
        artifacts.put("presentation.zip", presentationArchive);
        
        // Build presentation XDOM.
        return new XDOMOfficeDocument(buildPresentationXDOM(), artifacts, componentManager);
    }

    /**
     * {@inheritDoc}
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
     * @return presentation XDOM.
     * @throws OfficeImporterException if an error occurs while building the presentation xdom.
     */
    private XDOM buildPresentationXDOM() throws OfficeImporterException
    {
        String newLine = "\n";
        StringBuffer buffer = new StringBuffer();
        buffer.append("{{velocity}}").append(newLine);
        buffer.append("#set($url=$xwiki.zipexplorer.getFileLink($doc, 'presentation.zip', 'output.html'))");
        buffer.append(newLine).append(newLine);
        buffer.append("{{html}}").append(newLine);
        buffer.append("<iframe src=\"$url\" frameborder=0 width=800px height=600px></iframe>").append(newLine);
        buffer.append("{{/html}}").append(newLine).append(newLine);
        buffer.append("{{/velocity}}");
        try {
            return xwikiParser.parse(new StringReader(buffer.toString()));
        } catch (ParseException ex) {
            throw new OfficeImporterException("Error while building presentation.", ex);
        }
    }
}
