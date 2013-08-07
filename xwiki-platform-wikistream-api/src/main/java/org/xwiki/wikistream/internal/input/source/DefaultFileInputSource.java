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
package org.xwiki.wikistream.internal.input.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xwiki.wikistream.input.source.FileInputSource;
import org.xwiki.wikistream.input.source.InputStreamInputSource;

public class DefaultFileInputSource extends AbstractInputStreamInputSourceInputSource implements FileInputSource, InputStreamInputSource
{
    private final File file;

    public DefaultFileInputSource(File file)
    {
        this.file = file;
    }

    public File getFile()
    {
        return this.file;
    }

    @Override
    protected InputStream openStream() throws IOException
    {
        return new FileInputStream(this.file);
    }
}
