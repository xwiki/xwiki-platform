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
 *
 */
package org.xwiki.url;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultURLFactory implements URLFactory, Initializable
{
    private String pattern;

    private Map regexMappings;
    
    private Pattern regexPattern;

    /**
     * For performance reason compile the regex pattern.
     */
    public void initialize() throws InitializationException
    {
       this.regexPattern = Pattern.compile(this.pattern);
    }

    public XWikiURL createURL(String urlAsString) throws InvalidURLException
    {
        XWikiURL url = new XWikiURL();

        // Use a regex to parse the URL into its discrete parts:
        // <protocol>://<server>:<port>/<context>/<action>/<space>/<document>
        Matcher matcher = this.regexPattern.matcher(urlAsString);
        if (matcher.matches()) {

            // Find the action part in the URL
            String action = matcher.group(Integer.parseInt((String) regexMappings.get("action")));
            url.setAction(action);

            // Find the space part in the URL
            String space = matcher.group(Integer.parseInt((String) regexMappings.get("space")));
            url.setSpace(space);

            // Find the document part in the URL
            String document =
                matcher.group(Integer.parseInt((String) regexMappings.get("document")));
            url.setDocument(space);

            // Find the query string if any and transform it into a parameter Map for easy access
            String queryString =
                matcher.group(Integer.parseInt((String) regexMappings.get("queryString")));
            if (queryString != null) {
                StringTokenizer st = new StringTokenizer(queryString, "&");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    int pos = token.indexOf("=");
                    if (pos == -1) {
                        throw new InvalidURLException("Invalid Query String for token [" + token
                            + "]. Parameter values need an '=' sign between name and value.");
                    }
                    url.addParameter(token.substring(0, pos), token.substring(pos + 1));
                }
            }

        } else {
            throw new InvalidURLException("Failed to parse URL [" + urlAsString
                + "] using pattern [" + pattern + "]");
        }

        return url;
    }
}
