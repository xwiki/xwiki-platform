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
package org.xwiki.extension.repository.xwiki.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.methods.GetMethod;
import org.xwiki.extension.ExtensionFile;
import org.xwiki.extension.ExtensionId;

public class XWikiExtensionFile implements ExtensionFile
{
    private XWikiExtensionRepository repository;

    private ExtensionId id;

    public XWikiExtensionFile(XWikiExtensionRepository repository, ExtensionId id)
    {
        this.repository = repository;
        this.id = id;
    }

    @Override
    public long getLength()
    {
        GetMethod getMethod;
        try {
            getMethod =
                this.repository.getRESTResource(this.repository.getExtensionFileUriBuider(), this.id.getId(),
                    this.id.getVersion().getValue());
        } catch (IOException e) {
            throw new RuntimeException("Failed to acess extension [" + this + "]");
        }

        long size = getMethod.getResponseContentLength();

        getMethod.abort();

        return size;
    }

    @Override
    public InputStream openStream() throws IOException
    {
        return this.repository.getRESTResourceAsStream(this.repository.getExtensionFileUriBuider(), this.id.getId(),
            this.id.getVersion().getValue());
    }
}
