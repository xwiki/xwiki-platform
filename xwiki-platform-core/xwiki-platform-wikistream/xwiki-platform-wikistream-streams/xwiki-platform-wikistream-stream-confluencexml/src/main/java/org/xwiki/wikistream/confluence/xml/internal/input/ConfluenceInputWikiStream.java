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
package org.xwiki.wikistream.confluence.xml.internal.input;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.confluence.xml.internal.ConfluenceFilter;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.input.InputStreamInputSource;
import org.xwiki.wikistream.internal.input.AbstractBeanInputWikiStream;

/**
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Named(ConfluenceInputWikiStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConfluenceInputWikiStream extends AbstractBeanInputWikiStream<ConfluenceInputProperties, ConfluenceFilter>
{
    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    @Override
    protected void read(Object filter, ConfluenceFilter proxyFilter) throws WikiStreamException
    {
        InputSource inputSource = this.properties.getSource();

        if (inputSource instanceof InputStreamInputSource) {
            InputStream stream;
            try {
                stream = ((InputStreamInputSource) inputSource).getInputStream();
            } catch (IOException e) {
                throw new WikiStreamException("Failed to get input stream", e);
            }

            // TODO: Read
        } else {
            throw new WikiStreamException(
                String.format("Unsupported input source of type [%s]", inputSource.getClass()));
        }
    }
}
