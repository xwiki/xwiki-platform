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
package org.xwiki.livedata.internal.macro;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.internal.script.LiveDataConfigHelper;
import org.xwiki.livedata.macro.LiveDataMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Display dynamic lists of data.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@Component
@Named("liveData")
@Singleton
public class LiveDataMacro extends AbstractMacro<LiveDataMacroParameters>
{
    private static final Pattern PATTERN_COMMA = Pattern.compile("\\s*,\\s*");

    private static final String UTF8 = "UTF-8";

    private static final String ID = "id";

    @Inject
    private LiveDataConfigHelper configHelper;

    /**
     * Default constructor.
     */
    public LiveDataMacro()
    {
        super("Live Data", "Display dynamic lists of data.", LiveDataMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    @Override
    public List<Block> execute(LiveDataMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // TODO: Load the JavaScript and CSS.
        GroupBlock output = new GroupBlock();
        output.setParameter("class", "liveData");
        if (!StringUtils.isEmpty(parameters.getId())) {
            output.setParameter(ID, parameters.getId());
        }
        try {
            String liveDataConfigJSON = getLiveDataConfigJSON(parameters, content);
            output.setParameter("data-config", this.configHelper.effectiveConfig(liveDataConfigJSON));
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to generate live data configuration from macro parameters.", e);
        }
        return Collections.singletonList(output);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    private String getLiveDataConfigJSON(LiveDataMacroParameters parameters, String content)
        throws JsonProcessingException, MalformedURLException, UnsupportedEncodingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
        ObjectNode liveDataConfig = objectMapper.createObjectNode();
        if (!StringUtils.isEmpty(parameters.getId())) {
            liveDataConfig.put(ID, parameters.getId());
        }
        liveDataConfig.set("query", getQuery(parameters, objectMapper));
        JsonNode data = objectMapper.valueToTree(getLiveData(content));
        if (!data.isEmpty()) {
            liveDataConfig.set("data", data);
        }
        return objectMapper.writeValueAsString(liveDataConfig);
    }

    private ObjectNode getQuery(LiveDataMacroParameters parameters, ObjectMapper objectMapper)
        throws MalformedURLException, UnsupportedEncodingException
    {
        ObjectNode query = objectMapper.valueToTree(getLiveDataQuery(parameters));
        List<Filter> hiddenFilters = getFilters(parameters.getHiddenFilters());
        if (!hiddenFilters.isEmpty()) {
            query.set("hiddenFilters", objectMapper.valueToTree(hiddenFilters));
        }
        return query;
    }

    private LiveDataQuery getLiveDataQuery(LiveDataMacroParameters parameters)
        throws MalformedURLException, UnsupportedEncodingException
    {
        LiveDataQuery query = new LiveDataQuery();
        query.getProperties().addAll(getProperties(parameters.getProperties()));
        query.getSource().setId(parameters.getSource());
        query.getSource().putAll(getSourceParameters(parameters.getSourceParameters()));
        query.getSort().addAll(getSortEntries(parameters.getSort()));
        query.getFilters().addAll(getFilters(parameters.getFilters()));
        query.setLimit(parameters.getLimit());
        query.setOffset(parameters.getOffset());
        return query;
    }

    private List<String> getProperties(String properties)
    {
        if (StringUtils.isEmpty(properties)) {
            return Collections.emptyList();
        } else {
            return Stream.of(PATTERN_COMMA.split(properties)).collect(Collectors.toList());
        }
    }

    private Map<String, Object> getSourceParameters(String sourceParametersString)
        throws MalformedURLException, UnsupportedEncodingException
    {
        if (StringUtils.isEmpty(sourceParametersString)) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> urlParams = getURLParameters('?' + sourceParametersString);
        Map<String, Object> sourceParams = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : urlParams.entrySet()) {
            if (entry.getValue().size() > 1) {
                sourceParams.put(entry.getKey(), entry.getValue());
            } else {
                sourceParams.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return sourceParams;
    }

    private List<SortEntry> getSortEntries(String sort)
    {
        if (StringUtils.isEmpty(sort)) {
            return Collections.emptyList();
        } else {
            return Stream.of(PATTERN_COMMA.split(sort)).filter(StringUtils::isNotEmpty).map(this::getSortEntry)
                .collect(Collectors.toList());
        }
    }

    private SortEntry getSortEntry(String sortEntryString)
    {
        SortEntry sortEntry = new SortEntry();
        sortEntry.setDescending(sortEntryString.endsWith(":desc"));
        if (sortEntry.isDescending() || sortEntryString.endsWith(":asc")) {
            sortEntry.setProperty(StringUtils.substringBeforeLast(sortEntryString, ":"));
        } else {
            sortEntry.setProperty(sortEntryString);
        }
        return sortEntry;
    }

    private List<Filter> getFilters(String filtersString) throws MalformedURLException, UnsupportedEncodingException
    {
        if (StringUtils.isEmpty(filtersString)) {
            return Collections.emptyList();
        }

        return getURLParameters('?' + filtersString).entrySet().stream().map(this::getFilter)
            .collect(Collectors.toList());
    }

    private Filter getFilter(Map.Entry<String, List<String>> entry)
    {
        Filter filter = new Filter();
        filter.setProperty(entry.getKey());
        filter.getConstraints().addAll(entry.getValue().stream().map(Constraint::new).collect(Collectors.toList()));
        return filter;
    }

    private LiveData getLiveData(String content)
    {
        // TODO: Read live data entries from the macro content.
        return new LiveData();
    }

    private Map<String, List<String>> getURLParameters(String url)
        throws MalformedURLException, UnsupportedEncodingException
    {
        URL baseURL = new URL("http://www.xwiki.org");
        String queryString = new URL(baseURL, url).getQuery();
        Map<String, List<String>> parameters = new HashMap<>();
        for (String entry : queryString.split("&")) {
            String[] parts = entry.split("=", 2);
            String key = URLDecoder.decode(parts[0], UTF8);
            if (key.isEmpty()) {
                continue;
            }
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], UTF8) : "";
            List<String> values = parameters.get(key);
            if (values == null) {
                values = new ArrayList<>();
                parameters.put(key, values);
            }
            values.add(value);
        }
        return parameters;
    }
}
