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
package org.xwiki.wikistream.xml.input;

import java.io.IOException;

import javax.xml.transform.Source;

/**
 * Default implementation of {@link SourceInputSource}.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
public class DefaultSourceInputSource implements SourceInputSource
{
    /**
     * @see #getSource()
     */
    private Source source;

    /**
     * @param source the {@link Source}
     */
    public DefaultSourceInputSource(Source source)
    {
        this.source = source;
    }

    @Override
    public boolean restartSupported()
    {
        return false;
    }

    @Override
    public void close() throws IOException
    {
        // Source is not closable
    }

    @Override
    public Source getSource()
    {
        return this.source;
    }
}
