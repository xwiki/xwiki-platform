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
package org.xwiki.test.escaping.framework;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.http.entity.ContentType;

public class URLContent
{
    private ContentType type;

    private byte[] content;

    public URLContent(String typeHeader, byte[] content)
    {
        this.type = typeHeader != null ? ContentType.parse(typeHeader) : null;
        this.content = content;
    }

    public ContentType getType()
    {
        return this.type;
    }

    public byte[] getContent()
    {
        return this.content;
    }

    public Reader getContentReader()
    {
        Charset charset;
        if (this.type != null && this.type.getCharset() != null) {
            charset = this.type.getCharset();
        } else {
            charset = StandardCharsets.UTF_8;
        }

        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(getContent()), charset));
    }
}
