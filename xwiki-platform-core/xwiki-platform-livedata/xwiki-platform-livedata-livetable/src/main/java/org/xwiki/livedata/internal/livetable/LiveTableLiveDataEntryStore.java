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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.WithParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AccessDeniedException;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.xpn.xwiki.XWikiException;

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

    private static final String CLASS_NAME_PARAMETER = "className";

    private static final String DOC_PREFIX = "doc.";

    private static final String UNDEFINED_CLASS_ERROR_MESSAGE =
        "Can't update object properties if the object type (class name) is undefined.";

    @Inject
    private LiveTableLiveDataResultsRenderer resultsRenderer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    @Named(ROLE_HINT)
    private Provider<LiveDataConfiguration> liveDataConfigurationProvider;

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
            JsonNode liveTableResults = getLiveTableResultsJSON(query, objectMapper);
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

    private JsonNode getLiveTableResultsJSON(LiveDataQuery query, ObjectMapper objectMapper) throws Exception
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
                liveTableResultsJSON = this.resultsRenderer.getLiveTableResultsFromTemplate((String) template, query);
            } else if (resultPage instanceof String) {
                liveTableResultsJSON = this.resultsRenderer.getLiveTableResultsFromPage((String) resultPage, query);
            } else {
                liveTableResultsJSON =
                    this.resultsRenderer.getLiveTableResultsFromPage("XWiki.LiveTableResults", query);
            }

            return objectMapper.readTree(liveTableResultsJSON);
        } finally {
            // Restore the original query source.
            query.setSource(originalSource);
        }
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
                keysToRename.forEach(key -> entry.put(DOC_PREFIX + key.substring(4), entry.remove(key)));
                entries.add(entry);
            }
        }
        return entries;
    }

    @Override
    public Optional<Object> update(Object entryId, String property, Object value) throws LiveDataException
    {
        String className = (String) this.getParameters().get(CLASS_NAME_PARAMETER);

        // We can't update an object property if the object type is undefined.
        if (className == null && !StringUtils.defaultIfEmpty(property, "").startsWith(DOC_PREFIX)) {
            throw new LiveDataException(UNDEFINED_CLASS_ERROR_MESSAGE);
        }

        DocumentReference documentReference = this.currentDocumentReferenceResolver.resolve((String) entryId);
        DocumentReference classReference;
        if (className != null) {
            classReference = this.currentDocumentReferenceResolver.resolve(className);
        } else {
            classReference = null;
        }
        try {
            return this.modelBridge.update(property, value, documentReference, classReference);
        } catch (AccessDeniedException | XWikiException | LiveDataException e) {
            throw new LiveDataException(e);
        }
    }

    @Override
    public Optional<Object> save(Map<String, Object> entry) throws LiveDataException
    {
        String className = (String) this.getParameters().get(CLASS_NAME_PARAMETER);

        // We can't save the entry if one of its properties maps to an object property and the object type is unknown.
        boolean hasXObjectPropertiesToUpdate = entry.keySet()
            .stream()
            .anyMatch(it -> !StringUtils.defaultIfEmpty(it, "").startsWith(DOC_PREFIX));
        if (className == null && hasXObjectPropertiesToUpdate) {
            throw new LiveDataException(UNDEFINED_CLASS_ERROR_MESSAGE);
        }

        String entryId = this.liveDataConfigurationProvider.get().getMeta().getEntryDescriptor().getIdProperty();
        String fullName = (String) entry.get(entryId);
        if (fullName == null) {
            throw new LiveDataException(
                String.format("Entry id [%s] missing. Can't load the document to update.", entryId));
        }

        DocumentReference documentReference = this.currentDocumentReferenceResolver.resolve(fullName);

        DocumentReference classReference;
        if (className != null) {
            classReference = this.currentDocumentReferenceResolver.resolve(className);
        } else {
            classReference = null;
        }

        try {
            this.modelBridge.updateAll(entry, documentReference, classReference);
        } catch (AccessDeniedException | XWikiException e) {
            throw new LiveDataException(e);
        }

        return Optional.of(fullName);
    }
   
}
