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
import com.xpn.xwiki.doc.DeletedAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Filesystem based implementation of DeletedAttachmentContent.
 *
 * @version $Id$
 * @since 9.9RC1
 */
public class FileDeletedAttachmentContent implements DeletedAttachmentContent
{
    private final File content;

    private final Charset charset;

    /**
     * @param file the serialized document as XML
     * @param charset the charset of the file
     */
    public FileDeletedAttachmentContent(File file, Charset charset)
    {
        this.content = file;
        this.charset = charset;
    }

    @Override
    public String getContentAsString() throws IOException
    {
        return FileUtils.readFileToString(this.content, this.charset);
    }

    @Override
    public XWikiAttachment getXWikiAttachment(XWikiAttachment attachment) throws XWikiException, IOException
    {
        XWikiAttachment result = attachment;
        if (result == null) {
            result = new XWikiAttachment();
        }

        result.fromXML(new DefaultFileInputSource(this.content));

        return result;
    }
}
