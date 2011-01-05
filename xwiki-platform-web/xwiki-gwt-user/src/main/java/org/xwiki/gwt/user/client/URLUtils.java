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
package org.xwiki.gwt.user.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.http.client.URL;

/**
 * Provides utility methods for manipulating URLs.
 * 
 * @version $Id$
 */
public class URLUtils
{
    /**
     * The query string component separator.
     */
    public static final String QUERY_STRING_COMPONENT_SEPARATOR = "&";

    /**
     * Constructor.
     */
    protected URLUtils()
    {
    }

    /**
     * Extracts the query string from the given URL.
     * 
     * @param url the URL to extract the query string from
     * @return the query string of the given URL, empty string if the given URL doesn't have a query string
     */
    public static String getQueryString(String url)
    {
        int beginIndex = url.indexOf('?');
        if (beginIndex < 0) {
            return "";
        } else {
            int endIndex = url.lastIndexOf('#');
            if (endIndex < 0) {
                return url.substring(beginIndex + 1);
            } else {
                return url.substring(beginIndex + 1, endIndex);
            }
        }
    }

    /**
     * Replaces the query string of the given URL and returns the result.
     * 
     * @param url the URL whose query string is changed
     * @param parameters the map of query string parameters
     * @return a new URL that has the specified query string
     */
    public static String setQueryString(String url, Map<String, ? > parameters)
    {
        String queryString = serializeQueryStringParameters(parameters);
        StringBuilder newURL = new StringBuilder(url);
        int beginIndex = url.indexOf('?');
        int endIndex = url.lastIndexOf('#');
        if (beginIndex < 0 && endIndex < 0) {
            // The given URL doesn't have a query string nor a fragment identifier.
            if (queryString.length() > 0) {
                newURL.append('?').append(queryString);
            }
        } else if (beginIndex < 0) {
            // The given URL doesn't have a query string but has a fragment identifier.
            if (queryString.length() > 0) {
                newURL.insert(endIndex, queryString).insert(endIndex, '?');
            }
        } else {
            // The given URL has a query string a possibly a fragment identifier.
            if (endIndex < 0) {
                endIndex = url.length();
            }
            newURL.replace(beginIndex + 1, endIndex, queryString);
        }
        return newURL.toString();
    }

    /**
     * Parses the given query string and decodes the parameter names and values.
     * 
     * @param queryString the query string to be parsed
     * @return the map of query string parameters
     */
    public static Map<String, List<String>> parseQueryString(String queryString)
    {
        Map<String, List<String>> parameters = new HashMap<String, List<String>>();
        String[] keyValuePairs = queryString.split(QUERY_STRING_COMPONENT_SEPARATOR);
        for (int i = 0; i < keyValuePairs.length; i++) {
            if (keyValuePairs[i].length() == 0) {
                continue;
            }
            String[] pair = keyValuePairs[i].split("=", 2);
            String name = pair[0];
            String value = pair.length == 1 ? "" : pair[1];
            name = URL.decodeQueryString(name);
            value = URL.decodeQueryString(value);
            List<String> values = parameters.get(name);
            if (values == null) {
                values = new ArrayList<String>();
                parameters.put(name, values);
            }
            values.add(value);
        }
        return parameters;
    }

    /**
     * Serializes the given parameters into a query string.
     * 
     * @param parameters the parameters to serialize
     * @return a query string that includes the given parameters
     */
    public static String serializeQueryStringParameters(Map<String, ? > parameters)
    {
        StringBuilder queryString = new StringBuilder();
        String separator = "";
        for (Map.Entry<String, ? > entry : parameters.entrySet()) {
            if (entry.getValue() instanceof Iterable< ? >) {
                for (Object value : (Iterable< ? >) entry.getValue()) {
                    queryString.append(separator).append(URL.encodeQueryString(entry.getKey())).append('=').append(
                        URL.encodeQueryString(value.toString()));
                    separator = QUERY_STRING_COMPONENT_SEPARATOR;
                }
            } else {
                queryString.append(separator).append(URL.encodeQueryString(entry.getKey())).append('=').append(
                    URL.encodeQueryString(entry.getValue().toString()));
                separator = QUERY_STRING_COMPONENT_SEPARATOR;
            }
        }
        return queryString.toString();
    }
}
