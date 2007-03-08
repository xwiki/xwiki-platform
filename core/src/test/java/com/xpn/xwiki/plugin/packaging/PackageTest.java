/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import org.jmock.Mock;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Unit tests for the {@link com.xpn.xwiki.plugin.packaging.Package} class.
 *
 * @version $Id: $
 */
public class PackageTest extends org.jmock.cglib.MockObjectTestCase
{
    private Package pack;

    private XWikiContext context;

    private Mock mockXWiki;

    protected void setUp() throws XWikiException
    {
        this.pack = new Package();
        this.context = new XWikiContext();

        this.mockXWiki = mock(XWiki.class, new Class[]{XWikiConfig.class, XWikiContext.class},
            new Object[]{new XWikiConfig(), this.context});
        mockXWiki.stubs().method("getEncoding").will(returnValue("UTF-8"));
        mockXWiki.stubs().method("checkAccess").will(returnValue(true));
        this.context.setWiki((XWiki) this.mockXWiki.proxy());
    }

    public void testImportWithHeterogeneousEncodingInFiles() throws Exception
    {
        String docTitle = "Un \u00e9t\u00e9 36";
        String docContent =
            "\u00e0\u00e7\u00e9\u00e8\u00c0\u00c7\u00c9\u00c8\u00ef\u00f6\u00eb\u00fc";

        XWikiDocument doc1 = new XWikiDocument("Main", "Document1");
        doc1.setTitle(docTitle);
        doc1.setContent(docContent);

        XWikiDocument doc2 = new XWikiDocument("Main", "Document2");
        doc2.setTitle(docTitle);
        doc2.setContent(docContent);

        XWikiDocument docs[] = {doc1, doc2};

        this.pack.Import(this.createZipFile(docs, new String[] {"ISO-8859-1", "UTF-8"}),
            this.context);

        assertEquals(2, this.pack.getFiles().size());
        assertEquals(((DocumentInfo) this.pack.getFiles().get(0)).getDoc().getTitle(),
            ((DocumentInfo) this.pack.getFiles().get(1)).getDoc().getTitle());
        assertEquals(((DocumentInfo) this.pack.getFiles().get(0)).getDoc().getContent(),
            ((DocumentInfo) this.pack.getFiles().get(1)).getDoc().getContent());
    }

    private String getPackageXML(XWikiDocument docs[])
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        sb.append("<package>\n").append("<infos>\n").append("<name>Backup</name>\n");
        sb.append("<description>on Mon Jan 01 01:44:32 CET 2007 by XWiki.Admin</description>\n");
        sb.append("<licence></licence>\n");
        sb.append("<author>XWiki.Admin</author>\n");
        sb.append("<version></version>\n");
        sb.append("<backupPack>true</backupPack>\n");
        sb.append("</infos>\n");
        sb.append("<files>\n");
        for (int i = 0; i < docs.length; i++) {
            sb.append(
                "<file defaultAction=\"0\" language=\"\">" + docs[i].getFullName() + "</file>\n");
        }
        sb.append("</files></package>\n");
        return sb.toString();
    }

    private byte[] getEncodedByteArray(String content, String charset) throws IOException
    {
        StringReader rdr = new StringReader(content);
        BufferedReader bfr = new BufferedReader(rdr);
        ByteArrayOutputStream ostr = new ByteArrayOutputStream();
        OutputStreamWriter os = new OutputStreamWriter(ostr, charset);

        // Voluntarily ignore the first line... as it's the xml declaration
        String line = bfr.readLine();
        os.append(MessageFormat.format("<?xml version=\"1.0\" encoding=\"{0}\"?>\n", charset));

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

    private byte[] createZipFile(XWikiDocument docs[], String[] encodings) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry zipp = new ZipEntry("package.xml");
        zos.putNextEntry(zipp);
        zos.write(getEncodedByteArray(getPackageXML(docs), "ISO-8859-1"));
        for (int i = 0; i < docs.length; i++) {
            ZipEntry zipe = new ZipEntry(docs[i].getSpace() + "/" + docs[i].getName());
            zos.putNextEntry(zipe);
            String xmlCode = docs[i].toXML(false, false, false, false, context);
            zos.write(getEncodedByteArray(xmlCode, encodings[i]));
        }
        zos.closeEntry();
        return baos.toByteArray();
    }
}
