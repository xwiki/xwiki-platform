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
package org.xwiki.search.solr.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.solr.client.solrj.FastStreamingDocsCallback;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.BinaryResponseWriter.Resolver;
import org.apache.solr.search.SolrReturnFields;
import org.apache.solr.servlet.SolrRequestParsers;

/**
 * Extends {@link EmbeddedSolrServer} to workaround problems in standard embedded Solr server.
 * 
 * @version $Id$
 * @since 17.10.4
 * @since 18.1.0RC1
 */
public class EmbeddedClient extends EmbeddedSolrServer
{
    private static final long serialVersionUID = 1L;

    private final SolrCore core;

    private final SolrRequestParsers parser;

    /**
     * A wrapper around a {@link StreamingResponseCallback} to workaround a bug in Solr. See
     * https://issues.apache.org/jira/browse/SOLR-10198.
     */
    private final class WrappingStreamingResponseCallback extends StreamingResponseCallback
    {
        private final StreamingResponseCallback callback;

        private final Resolver resolver;

        private WrappingStreamingResponseCallback(SolrParams params, StreamingResponseCallback callback)
            throws Exception
        {
            this.callback = callback;

            SolrQueryRequest req = parser.buildRequestFrom(core, params, List.of());

            this.resolver = new Resolver(req, new SolrReturnFields());
        }

        private SolrDocument convert(SolrDocument doc) throws IOException
        {
            // Serialize and unserialize the document (which is what happen in #query())
            // Code copied from BinaryResponseWriter#getParsedResponse
            try (var out = new ByteArrayOutputStream()
            {
                ByteArrayInputStream toInputStream()
                {
                    return new ByteArrayInputStream(buf, 0, count);
                }
            }) {
                serialize(doc, out);

                return deserialize(out.toInputStream());
            }
        }

        private void serialize(SolrDocument doc, ByteArrayOutputStream out) throws IOException
        {
            try (JavaBinCodec jbc = new JavaBinCodec(this.resolver)) {
                jbc.setWritableDocFields(this.resolver).marshal(doc, out);
            }
        }

        private SolrDocument deserialize(ByteArrayInputStream stream) throws IOException
        {
            try (InputStream in = stream) {
                try (JavaBinCodec jbc = new JavaBinCodec(this.resolver)) {
                    return (SolrDocument) jbc.unmarshal(in);
                }
            }
        }

        @Override
        public void streamSolrDocument(SolrDocument doc)
        {
            SolrDocument convertedDoc;
            try {
                convertedDoc = convert(doc);
            } catch (IOException e) {
                throw new RuntimeException("Failed to convert streamed Solr document", e);
            }

            this.callback.streamSolrDocument(convertedDoc);
        }

        @Override
        public void streamDocListInfo(long numFound, long start, Float maxScore)
        {
            this.callback.streamDocListInfo(numFound, start, maxScore);
        }
    }

    /**
     * @param core the embedded Solr core
     */
    public EmbeddedClient(SolrCore core)
    {
        super(core.getCoreContainer(), core.getName(), RequestWriterSupplier.JavaBin);

        this.core = core;
        this.parser = new SolrRequestParsers(null);
    }

    private WrappingStreamingResponseCallback createWrappingCallback(SolrParams params,
        StreamingResponseCallback callback)
    {
        try {
            return new WrappingStreamingResponseCallback(params, callback);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create wrapping streaming response callback", e);
        }
    }

    @Override
    public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback)
        throws SolrServerException, IOException
    {
        return super.queryAndStreamResponse(params, createWrappingCallback(params, callback));
    }

    @Override
    public QueryResponse queryAndStreamResponse(String collection, SolrParams params,
        StreamingResponseCallback callback) throws SolrServerException, IOException
    {
        return super.queryAndStreamResponse(collection, params, createWrappingCallback(params, callback));
    }

    @Override
    // TODO: might also need some workaround here, if FastStreamingDocsCallback is affected by the same bug.
    public QueryResponse queryAndStreamResponse(String collection, SolrParams params,
        FastStreamingDocsCallback callback) throws SolrServerException, IOException
    {
        return super.queryAndStreamResponse(collection, params, callback);
    }
}
