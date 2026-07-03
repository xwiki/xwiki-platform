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
package org.xwiki.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * A simple {@link String} implementation of {@link StreamProvider}.
 * 
 * @version $Id$
 * @since 16.4.0RC1
 */
public class StringStreamProvider implements StreamProvider
{
    private final String content;

    private final Charset charset;

    /**
     * @param content the {@link String} to read
     * @param charset the {@linkplain java.nio.charset.Charset} to be used to encode the {@code String}
     */
    public StringStreamProvider(String content, Charset charset)
    {
        this.content = content;
        this.charset = charset;
    }

    @Override
    public InputStream getStream() throws Exception
    {
        return new ByteArrayInputStream(this.content.getBytes(this.charset));
    }
}
