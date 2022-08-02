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

package com.xpn.xwiki.plugin.packaging;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.xwiki.extension.ExtensionId;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

/**
 * Utility functions used by ImportTest and PackageTest.
 *
 * @since 4.1M2
 * @version $Id$
 */
@OldcoreTest
@AllComponents
public abstract class AbstractPackageTest
{
    @InjectMockitoOldcore
    MockitoOldcore oldcore;

    /**
     * Create a XAR file using java.util.zip.
     *
     * @param docs The documents to include.
     * @param encodings The charset for each document.
     * @param packageXmlEncoding The encoding of package.xml
     * @param extension the extension id and version or null if none should be added
     * @return the XAR file as a byte array.
     */
    protected byte[] createZipFile(XWikiDocument docs[], String[] encodings, String packageXmlEncoding,
        ExtensionId extension) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry zipp = new ZipEntry("package.xml");
        zos.putNextEntry(zipp);
        zos.write(getEncodedByteArray(getPackageXML(docs, packageXmlEncoding, extension), packageXmlEncoding));
        for (int i = 0; i < docs.length; i++) {
            String zipEntryName = docs[i].getSpace() + "/" + docs[i].getName();
            if (docs[i].getTranslation() != 0) {
                zipEntryName += "." + docs[i].getLanguage();
            }
            ZipEntry zipe = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipe);
            String xmlCode = docs[i].toXML(false, false, false, false, this.oldcore.getXWikiContext());
            zos.write(getEncodedByteArray(xmlCode, encodings[i]));
        }
        zos.finish();
        zos.close();
        return baos.toByteArray();
    }

    /**
     * Create a XAR file using java.util.zip.
     *
     * @param docs The documents to include.
     * @param encodings The charset for each document.
     * @param extension the extension id and version or null if none should be added
     * @return the XAR file as a byte array.
     */
    protected byte[] createZipFile(XWikiDocument docs[], String[] encodings, ExtensionId extension) throws Exception
    {
        return createZipFile(docs, encodings, "ISO-8859-1", extension);
    }

    /**
     * Create a XAR file using commons compress.
     *
     * @param docs The documents to include.
     * @param encodings The charset for each document.
     * @param packageXmlEncoding The encoding of package.xml
     * @return the XAR file as a byte array.
     */
    protected byte[] createZipFileUsingCommonsCompress(XWikiDocument docs[], String[] encodings,
        String packageXmlEncoding, ExtensionId extension) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos);
        ZipArchiveEntry zipp = new ZipArchiveEntry("package.xml");
        zos.putArchiveEntry(zipp);
        zos.write(getEncodedByteArray(getPackageXML(docs, packageXmlEncoding, extension), packageXmlEncoding));
        for (int i = 0; i < docs.length; i++) {
            String zipEntryName = docs[i].getSpace() + "/" + docs[i].getName();
            if (docs[i].getTranslation() != 0) {
                zipEntryName += "." + docs[i].getLanguage();
            }
            ZipArchiveEntry zipe = new ZipArchiveEntry(zipEntryName);
            zos.putArchiveEntry(zipe);
            String xmlCode = docs[i].toXML(false, false, false, false, this.oldcore.getXWikiContext());
            zos.write(getEncodedByteArray(xmlCode, encodings[i]));
            zos.closeArchiveEntry();
        }
        zos.finish();
        zos.close();
        return baos.toByteArray();
    }

    /**
     * Create a XAR file using commons compress.
     *
     * @param docs The documents to include.
     * @param encodings The charset for each document.
     * @param extension the extension id and version or null if none should be added
     * @return the XAR file as a byte array.
     */
    protected byte[] createZipFileUsingCommonsCompress(XWikiDocument docs[], String[] encodings, ExtensionId extension)
        throws Exception
    {
        return createZipFileUsingCommonsCompress(docs, encodings, "ISO-8859-1", extension);
    }

    private byte[] getEncodedByteArray(String content, String charset) throws IOException
    {
        StringReader rdr = new StringReader(content);
        BufferedReader bfr = new BufferedReader(rdr);
        ByteArrayOutputStream ostr = new ByteArrayOutputStream();
        OutputStreamWriter os = new OutputStreamWriter(ostr, charset);

        // Voluntarily ignore the first line... as it's the xml declaration
        String line = bfr.readLine();
        os.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\n");

        line = bfr.readLine();
        while (null != line) {
            os.append(line);
            os.append("\n");
            line = bfr.readLine();
        }
        os.flush();
        os.close();
        return ostr.toByteArray();
    }

    private String getPackageXML(XWikiDocument docs[], String encoding, ExtensionId extension)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
        sb.append("<package>\n").append("<infos>\n").append("<name>Backup</name>\n");
        sb.append("<description>on Mon Jan 01 01:44:32 CET 2007 by XWiki.Admin</description>\n");
        sb.append("<licence></licence>\n");
        sb.append("<author>XWiki.Admin</author>\n");
        sb.append("<backupPack>true</backupPack>\n");

        // Add extension related informations
        if (extension != null) {
            sb.append("<extensionId>");
            sb.append(extension.getId());
            sb.append("</extensionId>\n");
            sb.append("<version>");
            sb.append(extension.getVersion());
            sb.append("</version>\n");
        } else {
            sb.append("<version></version>\n");
        }

        sb.append("</infos>\n");

        sb.append("<files>\n");
        for (int i = 0; i < docs.length; i++) {

            sb.append("<file defaultAction=\"0\" language=\"" + docs[i].getLanguage() + "\">" + docs[i].getFullName()
                + "</file>\n");
        }
        sb.append("</files></package>\n");
        return sb.toString();
    }

}
