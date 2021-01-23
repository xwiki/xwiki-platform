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
package org.xwiki.livedata.internal.livetable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.WithParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xpn.xwiki.XWikiContext;

/**
 * {@link LiveDataEntryStore} implementation that reuses existing live table data.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Named(LiveTableLiveDataEntryStore.ROLE_HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class LiveTableLiveDataEntryStore extends WithParameters implements LiveDataEntryStore
{
    /**
     * The hint of this component implementation.
     */
    public static final String ROLE_HINT = "liveTable";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private LiveTableRequestHandler liveTableRequestHandler;

    @Override
    public Optional<Map<String, Object>> get(Object entryId)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        try {
            // We need to allow backslash escaping because some live table sources are generating the JSON by hand
            // instead of serializing a map.
            ObjectMapper objectMapper =
                JsonMapper.builder().enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER).build();
            ObjectNode liveTableResults = getLiveTableResultsJSON(query, objectMapper);
            LiveData liveData = new LiveData();
            liveData.setCount(liveTableResults.path("totalrows").asLong());
            JsonNode rows = liveTableResults.path("rows");
            if (rows.isArray()) {
                liveData.getEntries().addAll(convertLiveTableRowsToLiveDataEntries((ArrayNode) rows, objectMapper));
            }
            return liveData;
        } catch (Exception e) {
            throw new LiveDataException("Failed to execute the live data query.", e);
        }
    }

    private ObjectNode getLiveTableResultsJSON(LiveDataQuery query, ObjectMapper objectMapper) throws Exception
    {
        // Merge the parameters of this live data source with the parameters from the given query.
        Source originalSource = query.getSource();
        query.setSource(new Source(ROLE_HINT));
        query.getSource().getParameters().putAll(getParameters());
        if (originalSource != null) {
            query.getSource().getParameters().putAll(originalSource.getParameters());
        }

        try {
            Object template = query.getSource().getParameters().get(LiveTableRequestHandler.TEMPLATE);
            Object resultPage = query.getSource().getParameters().get(LiveTableRequestHandler.RESULT_PAGE);
            String liveTableResultsJSON;
            if (template instanceof String) {
                liveTableResultsJSON = getLiveTableResultsFromTemplate((String) template, query);
            } else if (resultPage instanceof String) {
                liveTableResultsJSON = getLiveTableResultsFromPage((String) resultPage, query);
            } else {
                liveTableResultsJSON = getLiveTableResultsFromPage("XWiki.LiveTableResults", query);
            }
            return (ObjectNode) objectMapper.readTree(liveTableResultsJSON);
        } finally {
            // Restore the original query source.
            query.setSource(originalSource);
        }
    }

    private String getLiveTableResultsFromTemplate(String template, LiveDataQuery query) throws Exception
    {
        return this.liveTableRequestHandler.getLiveTableResults(query, () -> {
            try {
                return this.templateManager.render(template);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String getLiveTableResultsFromPage(String page, LiveDataQuery query) throws Exception
    {
        DocumentReference documentReference = this.currentDocumentReferenceResolver.resolve(page);
        this.authorization.checkAccess(Right.VIEW, documentReference);

        return this.liveTableRequestHandler.getLiveTableResults(query, () -> {
            try {
                // The live table results page may use "global" variables.
                this.templateManager.render("xwikivars.vm");
                XWikiContext xcontext = this.xcontextProvider.get();
                return xcontext.getWiki().getDocument(documentReference, xcontext).getRenderedContent(Syntax.PLAIN_1_0,
                    xcontext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<Map<String, Object>> convertLiveTableRowsToLiveDataEntries(ArrayNode rows, ObjectMapper objectMapper)
        throws Exception
    {
        List<Map<String, Object>> entries = new LinkedList<>();
        for (JsonNode row : rows) {
            if (row.isObject()) {
                Map<String, Object> entry = objectMapper.readerForMapOf(Object.class).readValue(row);
                // The special "doc.*" columns appear as "doc_*" inside the live table row. We need to fix this because
                // the live data expects the entry (row) properties to match the properties (columns) specified in the
                // live data query.
                Set<String> keysToRename =
                    entry.keySet().stream().filter(key -> key.startsWith("doc_")).collect(Collectors.toSet());
                keysToRename.forEach(key -> {
                    entry.put("doc." + key.substring(4), entry.remove(key));
                });
                entries.add(entry);
            }
        }
        return entries;
    }
}
