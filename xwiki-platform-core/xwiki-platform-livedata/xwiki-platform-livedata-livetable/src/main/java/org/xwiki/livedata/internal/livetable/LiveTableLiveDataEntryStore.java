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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.WithParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * {@link LiveDataEntryStore} implementation that reuses existing live table data.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component
@Named("liveTable")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class LiveTableLiveDataEntryStore extends WithParameters implements LiveDataEntryStore
{
    private static final String TEMPLATE = "template";

    private static final String RESULT_PAGE = "resultPage";

    @SuppressWarnings("serial")
    private static final Map<String, String> MATCH_TYPE = new HashMap<String, String>()
    {
        {
            put("equals", "exact");
            put("contains", "partial");
            put("startsWith", "prefix");
        }
    };

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Override
    public Optional<Object> add(Map<String, Object> entry)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Map<String, Object>> get(Object entryId)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
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

    @Override
    public Optional<Object> update(Map<String, Object> entry)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Map<String, Object>> remove(Object entryId)
    {
        throw new UnsupportedOperationException();
    }

    private ObjectNode getLiveTableResultsJSON(LiveDataQuery query, ObjectMapper objectMapper) throws Exception
    {
        Object template = query.getSource().getParameters().get(TEMPLATE);
        Object resultPage = query.getSource().getParameters().get(RESULT_PAGE);
        String liveTableResultsJSON;
        if (template instanceof String) {
            liveTableResultsJSON = getLiveTableResultsFromTemplate((String) template, query);
        } else if (resultPage instanceof String) {
            liveTableResultsJSON = getLiveTableResultsFromPage((String) resultPage, query);
        } else {
            liveTableResultsJSON = getLiveTableResultsFromPage("XWiki.LiveTableResults", query);
        }
        return (ObjectNode) objectMapper.readTree(liveTableResultsJSON);
    }

    private String getLiveTableResultsFromTemplate(String template, LiveDataQuery query) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiRequest originalRequest = wrapRequest(getRequestParameters(query));
        try {
            return this.templateManager.render(template);
        } finally {
            xcontext.setRequest(originalRequest);
        }
    }

    private String getLiveTableResultsFromPage(String page, LiveDataQuery query) throws Exception
    {
        DocumentReference documentReference = this.currentDocumentReferenceResolver.resolve(page);
        this.authorization.checkAccess(Right.VIEW, documentReference);

        XWikiContext xcontext = this.xcontextProvider.get();
        String originalAction = xcontext.getAction();
        xcontext.setAction("get");
        Map<String, String[]> requestParams = new HashMap<>();
        requestParams.put("outputSyntax", new String[] {"plain"});
        requestParams.putAll(getRequestParameters(query));
        XWikiRequest originalRequest = wrapRequest(requestParams);
        try {
            return xcontext.getWiki().getDocument(documentReference, xcontext).getRenderedContent(Syntax.PLAIN_1_0,
                xcontext);
        } finally {
            xcontext.setAction(originalAction);
            xcontext.setRequest(originalRequest);
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
                keysToRename.forEach(key -> {
                    entry.put("doc." + key.substring(4), entry.remove(key));
                });
                entries.add(entry);
            }
        }
        return entries;
    }

    private Map<String, String[]> getRequestParameters(LiveDataQuery query)
    {
        Map<String, String[]> requestParams = new HashMap<>();

        // Add source parameters.
        addSourceRequestParameters(query, requestParams);

        // Remove internal source parameters.
        Stream.of(TEMPLATE, RESULT_PAGE).forEach(requestParams::remove);

        // Rename the className parameter.
        String[] className = requestParams.remove("className");
        if (className != null) {
            requestParams.put("classname", className);
        }

        // Rename the translationPrefix parameter.
        String[] translationPrefix = requestParams.remove("translationPrefix");
        if (translationPrefix != null) {
            requestParams.put("transprefix", translationPrefix);
        }

        // Add the list of columns.
        if (query.getProperties() != null) {
            requestParams.put("collist", new String[] {StringUtils.join(query.getProperties(), ",")});
        }

        // Add the sort and direction.
        addSortRequestParameters(query, requestParams);

        // Add the filters.
        if (query.getFilters() != null) {
            query.getFilters().stream().forEach(filter -> addFilterRequestParameters(filter, requestParams));
        }

        // Add offset and limit. Note that the default live table results expects the offset to start from 1.
        if (query.getOffset() != null) {
            requestParams.put("offset", new String[] {String.valueOf(query.getOffset() + 1)});
        }
        if (query.getLimit() != null) {
            requestParams.put("limit", new String[] {String.valueOf(query.getLimit())});
        }

        return requestParams;
    }

    @SuppressWarnings("unchecked")
    private void addSourceRequestParameters(LiveDataQuery query, Map<String, String[]> requestParams)
    {
        if (query.getSource() != null) {
            for (Map.Entry<String, Object> entry : query.getSource().getParameters().entrySet()) {
                Stream<Object> stream;
                if (entry.getValue() instanceof Collection) {
                    stream = ((Collection<Object>) entry.getValue()).stream();
                } else {
                    // This should work for both single value and arrays.
                    stream = Stream.of(entry.getValue());
                }
                List<String> values =
                    stream.filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
                if (!values.isEmpty()) {
                    requestParams.put(entry.getKey(), values.toArray(new String[values.size()]));
                }
            }
        }
    }

    private void addSortRequestParameters(LiveDataQuery query, Map<String, String[]> requestParams)
    {
        if (query.getSort() != null && !query.getSort().isEmpty()) {
            List<String> sortList =
                query.getSort().stream().map(sortEntry -> sortEntry.getProperty()).collect(Collectors.toList());
            requestParams.put("sort", sortList.toArray(new String[sortList.size()]));
            List<String> dirList = query.getSort().stream().map(sortEntry -> sortEntry.isDescending() ? "desc" : "asc")
                .collect(Collectors.toList());
            requestParams.put("dir", dirList.toArray(new String[dirList.size()]));
        }
    }

    private void addFilterRequestParameters(Filter filter, Map<String, String[]> requestParams)
    {
        List<String> values = filter.getConstraints().stream().filter(Objects::nonNull).map(Constraint::getValue)
            .filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
        if (!values.isEmpty()) {
            requestParams.put(filter.getProperty(), values.toArray(new String[values.size()]));
            requestParams.put(filter.getProperty() + "/join_mode", new String[] {filter.isMatchAll() ? "AND" : "OR"});

            // The default live table results page supports a single filter operator (match type) per column.
            Set<String> operators = filter.getConstraints().stream().filter(Objects::nonNull)
                .map(Constraint::getOperator).filter(Objects::nonNull).collect(Collectors.toSet());
            if (operators.size() == 1) {
                String operator = operators.iterator().next();
                String matchType = MATCH_TYPE.getOrDefault(operator, operator);
                requestParams.put(filter.getProperty() + "_match", new String[] {matchType});
            }
        }
    }

    private XWikiRequest wrapRequest(Map<String, String[]> parameters)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiRequest originalRequest = xcontext.getRequest();
        xcontext.setRequest(new LiveTableRequest(originalRequest, parameters));
        return originalRequest;
    }
}
