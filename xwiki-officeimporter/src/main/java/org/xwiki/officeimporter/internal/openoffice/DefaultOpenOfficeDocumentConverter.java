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
package org.xwiki.officeimporter.internal.openoffice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.sf.jodconverter.DefaultDocumentFormatRegistry;
import net.sf.jodconverter.DocumentFormat;
import net.sf.jodconverter.DocumentFormatRegistry;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.internal.OfficeImporterFileStorage;
import org.xwiki.officeimporter.openoffice.OpenOfficeDocumentConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;

/**
 * Default implementation of {@link OpenOfficeDocumentConverter}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class DefaultOpenOfficeDocumentConverter extends AbstractLogEnabled implements OpenOfficeDocumentConverter,
    Initializable
{
    /**
     * The {@link OpenOfficeManager} component.
     */
    private OpenOfficeManager ooManager;

    /**
     * Output format of this transformer.
     */
    private DocumentFormat htmlFormat;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        DocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
        htmlFormat = formatRegistry.getFormatByExtension("html");
    }
    
    /**
     * {@inheritDoc}
     */
    public Map<String, InputStream> convert(InputStream in, OfficeImporterFileStorage storage)
        throws OfficeImporterException
    {
        // Make sure the openoffice server is connected.    
        if (ooManager.getState() != OpenOfficeManager.ManagerState.CONNECTED) {
            throw new OfficeImporterException("openoffice server not found.");
        }   
        // Prepare the result.
        Map<String, InputStream> result = new HashMap<String, InputStream>();
        // Copy bytes from the input stream into temporary input file.
        try {            
            FileOutputStream fos = new FileOutputStream(storage.getInputFile());
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.close();      
        } catch (IOException ex) {
            throw new OfficeImporterException("Error while creating temporary files.", ex);
        }
        // Make the conversion.
        ooManager.getDocumentConverter().convert(storage.getInputFile(), storage.getOutputFile(), htmlFormat);
        // Collect the resulting artifact streams
        File[] artifacts = storage.getOutputDir().listFiles();
        for (File artifact : artifacts) {
            try {
                FileInputStream fis = new FileInputStream(artifact);
                result.put(artifact.getName(), fis);
            } catch (IOException ex) {
                getLogger().error("Internal error while reading artifact : " + artifact.getName(), ex);
                // Skip the artifact.
            }
        }
        return result;
    }
}
