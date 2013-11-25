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
package org.xwiki.wikistream.xml.internal.output;

import java.io.IOException;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.stax.StAXResult;

import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.output.OutputStreamOutputTarget;
import org.xwiki.wikistream.output.OutputTarget;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.WriterOutputTarget;
import org.xwiki.wikistream.xml.output.ResultOutputTarget;
import org.xwiki.wikistream.xml.output.XMLOutputProperties;

/**
 * @param <P>
 * @version $Id$
 * @since 5.2M2
 */
public abstract class AbstractXMLOutputWikiStream<P extends XMLOutputProperties> implements OutputWikiStream
{
    protected final P properties;

    protected final Result result;

    protected Object filter;

    public AbstractXMLOutputWikiStream(P properties) throws WikiStreamException, XMLStreamException, IOException
    {
        this.properties = properties;
        this.result = createResult(this.properties);
    }

    protected Result createResult(P properties) throws WikiStreamException, XMLStreamException, IOException
    {
        OutputTarget target = properties.getTarget();

        Result result;

        if (target instanceof ResultOutputTarget) {
            result = ((ResultOutputTarget) target).getResult();
        } else {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();

            XMLStreamWriter xmlStreamWriter;

            if (target instanceof WriterOutputTarget) {
                xmlStreamWriter = factory.createXMLStreamWriter(((WriterOutputTarget) target).getWriter());
            } else if (target instanceof OutputStreamOutputTarget) {
                xmlStreamWriter =
                    factory.createXMLStreamWriter(((OutputStreamOutputTarget) target).getOutputStream(),
                        properties.getEncoding());
            } else {
                throw new WikiStreamException("Unknown target type [" + target.getClass() + "]");
            }

            if (properties.isFormat()) {
                xmlStreamWriter = new IndentingXMLStreamWriter(xmlStreamWriter);
            }

            result = new StAXResult(xmlStreamWriter);
        }

        return result;
    }

    @Override
    public Object getFilter() throws WikiStreamException
    {
        if (this.filter == null) {
            try {
                this.filter = createFilter(this.properties);
            } catch (Exception e) {
                throw new WikiStreamException("Failed to create filter", e);
            }
        }

        return this.filter;
    }

    protected abstract Object createFilter(P parameters) throws XMLStreamException, FactoryConfigurationError,
        WikiStreamException;

    @Override
    public void close() throws IOException
    {
        this.properties.getTarget().close();
    }
}
