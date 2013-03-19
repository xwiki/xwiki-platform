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

import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.xml.sax.ContentHandler;
import org.xwiki.rendering.internal.renderer.xml.SAXSerializer;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.internal.output.OuputTarget;
import org.xwiki.wikistream.internal.output.OutputStreamOuputTarget;
import org.xwiki.wikistream.internal.output.WriterOuputTarget;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.xml.internal.ContentHandlerOuputTarget;

public abstract class AbstractXMLOutputWikiStream<P extends XMLOuputParameters> implements OutputWikiStream
{
    protected final P parameters;

    protected final ContentHandler contentHandler;

    protected Object listener;

    public AbstractXMLOutputWikiStream(P parameters) throws WikiStreamException
    {
        this.parameters = parameters;
        this.contentHandler = createContentHandler(this.parameters);
    }

    protected ContentHandler createContentHandler(P parameters) throws WikiStreamException
    {
        OuputTarget target = parameters.getTarget();

        ContentHandler contentHandler;

        if (target instanceof ContentHandlerOuputTarget) {
            contentHandler = ((ContentHandlerOuputTarget) target).getContentHandler();
        } else if (target instanceof WriterOuputTarget) {
            contentHandler = new SAXSerializer(((WriterOuputTarget) target).getWriter());
        } else if (target instanceof OutputStreamOuputTarget) {
            Writer writer;
            try {
                writer =
                    new OutputStreamWriter(((OutputStreamOuputTarget) target).getOutputStream(),
                        parameters.getEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new WikiStreamException("Failed to create a XML serailizer", e);
            }
            contentHandler = new SAXSerializer(writer);
        } else {
            throw new WikiStreamException("Unknown target type [" + target.getClass() + "]");
        }

        return contentHandler;
    }

    @Override
    public Object getFilter()
    {
        if (this.listener != null) {
            this.listener = createListener();
        }

        return this.listener;
    }

    protected abstract Object createListener();
}
