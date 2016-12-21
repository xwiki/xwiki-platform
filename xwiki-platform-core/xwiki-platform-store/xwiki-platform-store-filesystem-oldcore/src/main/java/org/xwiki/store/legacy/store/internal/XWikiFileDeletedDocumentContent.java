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
package org.xwiki.store.legacy.store.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.xwiki.filter.input.DefaultFileInputSource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocumentContent;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The stored content of the deleted document.
 *
 * @version $Id$
 * @since 9.0RC1
 */
public class XWikiFileDeletedDocumentContent implements XWikiDeletedDocumentContent
{
    private final File content;

    private final Charset charset;

    /**
     * @param file the serialized document as XML
     * @param charset the charset of the file
     */
    public XWikiFileDeletedDocumentContent(File file, Charset charset)
    {
        this.content = file;
        this.charset = charset;
    }

    @Override
    public String getContentAsString() throws IOException
    {
        return FileUtils.readFileToString(this.content, this.charset);
    }

    /**
     * @param document the document to write to or null to create a new one
     * @return restored document
     * @throws XWikiException if error in {@link XWikiDocument#fromXML(String)}
     * @throws IOException when failing to read the content
     */
    @Override
    public XWikiDocument getXWikiDocument(XWikiDocument document) throws XWikiException, IOException
    {
        XWikiDocument result = document;
        if (result == null) {
            result = new XWikiDocument();
        }

        result.fromXML(new DefaultFileInputSource(this.content), true);

        return result;
    }
}
