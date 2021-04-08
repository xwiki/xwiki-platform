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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * Used to simulate a live table request and access the live table results.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component(roles = LiveTableRequestHandler.class)
@Singleton
public class LiveTableRequestHandler
{
    static final String TEMPLATE = "template";

    static final String RESULT_PAGE = "resultPage";

    static final String CONTEXT_DOC = "$doc";

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
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * Converts the given live data query into a fake live table request and executes the given live table results
     * supplier in the context of this fake live table request.
     * 
     * @param liveDataQuery the live data query to convert into a live table request
     * @param liveTableResultsSupplier the actual code that produces the live table results
     * @return the live table results JSON
     */
    public String getLiveTableResults(LiveDataQuery liveDataQuery, Supplier<String> liveTableResultsSupplier)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        String originalAction = xcontext.getAction();
        xcontext.setAction("get");

        XWikiDocument originalDoc = maybeSetContextDocument(xcontext, liveDataQuery);

        XWikiRequest originalRequest = xcontext.getRequest();
        xcontext.setRequest(new LiveTableRequest(originalRequest, getRequestParameters(liveDataQuery)));

        XWikiResponse originalResponse = xcontext.getResponse();
        LiveTableResponse liveTableResponse = new LiveTableResponse(originalResponse);
        xcontext.setResponse(liveTableResponse);

        boolean finished = xcontext.isFinished();
        try {
            String liveTableResultsJSON = liveTableResultsSupplier.get();
            // The supplier can write directly to the response, e.g. using the #jsonResponse Velocity macro, in which
            // case the response should be already committed.
            return liveTableResponse.isCommitted() ? liveTableResponse.getContent() : liveTableResultsJSON;
        } finally {
            xcontext.setAction(originalAction);
            xcontext.setDoc(originalDoc);
            xcontext.setRequest(originalRequest);
            xcontext.setResponse(originalResponse);
            xcontext.setFinished(finished);
        }
    }

    private XWikiDocument maybeSetContextDocument(XWikiContext xcontext, LiveDataQuery liveDataQuery)
    {
        XWikiDocument originalDoc = xcontext.getDoc();

        Source source = liveDataQuery.getSource();
        String contextDocRefString = (String) (source != null ? source : new Source()).getParameters().get(CONTEXT_DOC);
        if (contextDocRefString != null) {
            DocumentReference contextDocRef = this.currentDocumentReferenceResolver.resolve(contextDocRefString);
            try {
                XWikiDocument contextDoc = xcontext.getWiki().getDocument(contextDocRef, xcontext);
                xcontext.setDoc(contextDoc);
            } catch (XWikiException e) {
                this.logger.debug("Failed to set context document [{}] for live table results.", contextDocRefString,
                    e);
            }
        }

        return originalDoc;
    }

    private Map<String, String[]> getRequestParameters(LiveDataQuery query)
    {
        Map<String, String[]> requestParams = new HashMap<>();

        // Make sure we output plain syntax.
        requestParams.put("outputSyntax", new String[] {"plain"});

        // Add source parameters.
        addSourceRequestParameters(query, requestParams);

        // Remove internal source parameters.
        Stream.of(TEMPLATE, RESULT_PAGE, CONTEXT_DOC).forEach(requestParams::remove);

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

        // The live table widget is sending this parameter (in order to avoid handling a response for an obsolete
        // request) and some live table sources are expecting it.
        requestParams.put("reqNo", new String[] {"1"});

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

            List<String> matchType = filter.getConstraints().stream()
                .map(constraint -> constraint == null ? null : constraint.getOperator())
                .map(operator -> MATCH_TYPE.getOrDefault(operator, StringUtils.defaultString(operator)))
                .collect(Collectors.toList());
            requestParams.put(filter.getProperty() + "_match", matchType.toArray(new String[matchType.size()]));
        }
    }
}
