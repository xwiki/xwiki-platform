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
package org.xwiki.filter.xar.internal.input;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.filter.input.ReaderInputSource;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARFilterUtils;
import org.xwiki.filter.xml.input.SourceInputSource;
import org.xwiki.model.reference.EntityReference;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component(hints = {
    XARFilterUtils.ROLEHINT_16,
    XARFilterUtils.ROLEHINT_15,
    XARFilterUtils.ROLEHINT_14,
    XARFilterUtils.ROLEHINT_13,
    XARFilterUtils.ROLEHINT_12,
    XARFilterUtils.ROLEHINT_11
})
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XARInputFilterStream extends AbstractBeanInputFilterStream<XARInputProperties, XARInputFilter>
{
    @Inject
    private Provider<WikiReader> wikiReaderProvider;

    @Inject
    private Provider<DocumentLocaleReader> documentLocaleReaderProvider;

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    @Override
    protected void read(Object filter, XARInputFilter proxyFilter) throws FilterException
    {
        InputSource inputSource = this.properties.getSource();

        if (this.properties.isForceDocument() || inputSource instanceof ReaderInputSource
            || inputSource instanceof SourceInputSource) {
            readDocument(filter, proxyFilter);
        } else if (inputSource instanceof InputStreamInputSource) {
            InputStream stream;
            try {
                stream = ((InputStreamInputSource) inputSource).getInputStream();
            } catch (IOException e) {
                throw new FilterException("Failed to get input stream", e);
            }

            try {
                // Check if it's a ZIP or not
                Boolean iszip = isZip(stream);

                if (iszip == Boolean.FALSE) {
                    readDocument(filter, proxyFilter);
                } else {
                    readXAR(filter, proxyFilter);
                }
            } catch (IOException e) {
                throw new FilterException("Failed to read input stream", e);
            } finally {
                try {
                    inputSource.close();
                } catch (IOException e) {
                    throw new FilterException("Failed to close the source", e);
                }
            }
        } else {
            throw new FilterException(String.format("Unsupported input source of type [%s]", inputSource.getClass()));
        }
    }

    private void readXAR(Object filter, XARInputFilter proxyFilter) throws FilterException
    {
        WikiReader wikiReader = this.wikiReaderProvider.get();
        wikiReader.setProperties(this.properties);

        try {
            wikiReader.read(filter, proxyFilter);
        } catch (Exception e) {
            throw new FilterException("Failed to read XAR package", e);
        }
    }

    protected void readDocument(Object filter, XARInputFilter proxyFilter) throws FilterException
    {
        DocumentLocaleReader documentReader = documentLocaleReaderProvider.get();
        documentReader.setProperties(this.properties);

        try {
            documentReader.read(filter, proxyFilter);
        } catch (Exception e) {
            throw new FilterException("Failed to read XAR XML document", e);
        }

        // Close remaining opened spaces
        if (documentReader.getSentSpaceReference() != null) {
            for (EntityReference space = documentReader.getSentSpaceReference(); space != null; space =
                space.getParent()) {
                proxyFilter.endWikiSpace(space.getName(), FilterEventParameters.EMPTY);
            }
        }
    }

    private Boolean isZip(InputStream stream) throws IOException
    {
        if (!stream.markSupported()) {
            // ZIP by default
            return null;
        }

        final byte[] signature = new byte[12];
        stream.mark(signature.length);
        int signatureLength = stream.read(signature);
        stream.reset();

        return ZipArchiveInputStream.matches(signature, signatureLength);
    }
}
