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
package org.xwiki.wikistream.xar.internal.input;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.input.InputStreamInputSource;
import org.xwiki.wikistream.input.ReaderInputSource;
import org.xwiki.wikistream.internal.input.AbstractBeanInputWikiStream;
import org.xwiki.wikistream.xar.internal.XARFilter;
import org.xwiki.wikistream.xar.internal.XARUtils;
import org.xwiki.wikistream.xml.input.source.SourceInputSource;

/**
 * @version $Id$
 * @since 5.2RC1
 */
@Component
@Named(XARUtils.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XARInputWikiStream extends AbstractBeanInputWikiStream<XARInputProperties, XARFilter>
{
    @Inject
    private SyntaxFactory syntaxFactory;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeResolver;

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    @Override
    protected void read(Object filter, XARFilter proxyFilter) throws WikiStreamException
    {
        InputSource inputSource = this.properties.getSource();

        if (inputSource instanceof ReaderInputSource || inputSource instanceof SourceInputSource) {
            readDocument(filter, proxyFilter);
        } else if (inputSource instanceof InputStreamInputSource) {
            InputStream stream;
            try {
                stream = ((InputStreamInputSource) inputSource).getInputStream();
            } catch (IOException e) {
                throw new WikiStreamException("Failed to get input stream", e);
            }

            boolean iszip;
            try {
                iszip = isZip(stream);
            } catch (IOException e) {
                throw new WikiStreamException("Failed to read input stream", e);
            } finally {
                try {
                    inputSource.close();
                } catch (IOException e) {
                    throw new WikiStreamException("Failed to close the source", e);
                }
            }

            if (iszip) {
                readDocument(filter, proxyFilter);
            } else {
                readXAR(filter, proxyFilter);
            }
        } else {
            throw new WikiStreamException(
                String.format("Unsupported input source of type [%s]", inputSource.getClass()));
        }
    }

    private void readXAR(Object filter, XARFilter proxyFilter) throws WikiStreamException
    {
        WikiReader wikiReader = new WikiReader(this.syntaxFactory, this.relativeResolver);

        try {
            wikiReader.read(filter, proxyFilter, this.properties);
        } catch (Exception e) {
            throw new WikiStreamException("Failed to read XAR package", e);
        }
    }

    protected void readDocument(Object filter, XARFilter proxyFilter) throws WikiStreamException
    {
        DocumentLocaleReader documentReader = new DocumentLocaleReader(this.syntaxFactory, this.relativeResolver);

        try {
            documentReader.read(filter, proxyFilter, this.properties);
        } catch (Exception e) {
            throw new WikiStreamException("Failed to read XAR XML document", e);
        }

        // Close last space
        if (documentReader.getCurrentSpace() != null) {
            proxyFilter.endWikiSpace(documentReader.getCurrentSpace(), documentReader.getCurrentSpaceParameters());
        }
    }

    private boolean isZip(InputStream stream) throws IOException
    {
        if (!stream.markSupported()) {
            // ZIP by default
            return false;
        }

        final byte[] signature = new byte[12];
        stream.mark(signature.length);
        int signatureLength = stream.read(signature);
        stream.reset();

        return ZipArchiveInputStream.matches(signature, signatureLength);
    }
}
