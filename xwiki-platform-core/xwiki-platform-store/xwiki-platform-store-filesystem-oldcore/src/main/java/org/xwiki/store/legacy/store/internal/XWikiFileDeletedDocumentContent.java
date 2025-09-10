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

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.xwiki.filter.input.DefaultStreamProviderInputSource;
import org.xwiki.store.blob.Blob;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocumentContent;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Filesystem implementation of XWikiDeletedDocumentContent.
 *
 * @version $Id$
 * @since 9.0RC1
 */
public class XWikiFileDeletedDocumentContent implements XWikiDeletedDocumentContent
{
    private final Blob content;

    private final Charset charset;

    /**
     * @param file the serialized document as XML
     * @param charset the charset of the file
     */
    public XWikiFileDeletedDocumentContent(Blob file, Charset charset)
    {
        this.content = file;
        this.charset = charset;
    }

    @Override
    public String getContentAsString() throws IOException
    {
        try {
            return IOUtils.toString(this.content.getStream(), this.charset);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to read the deleted document content.", e);
        }
    }

    @Override
    public XWikiDocument getXWikiDocument(XWikiDocument document) throws XWikiException, IOException
    {
        XWikiDocument result = document;
        if (result == null) {
            result = new XWikiDocument();
        }

        result.fromXML(new DefaultStreamProviderInputSource(this.content), true);

        return result;
    }
}
