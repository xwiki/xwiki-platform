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
package org.xwiki.wysiwyg.internal.plugin.alfresco.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.wysiwyg.plugin.alfresco.server.NodeReference;
import org.xwiki.wysiwyg.plugin.alfresco.server.NodeReferenceParser;
import org.xwiki.wysiwyg.plugin.alfresco.server.StoreReference;

/**
 * Extracts the node reference from an Alfresco URL.
 * 
 * @version $Id$
 */
@Component
@Named("url")
public class UrlNodeReferenceParser implements NodeReferenceParser
{
    /**
     * The regular expression used to extract the node reference from the URL path.
     */
    private static final Pattern NODE_REF_PATH = Pattern.compile(
        "/([^/]+)/([^/]+)/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})", Pattern.CASE_INSENSITIVE);

    /**
     * The object used to parse node references.
     */
    @Inject
    private NodeReferenceParser nodeReferenceParser;

    @Override
    public NodeReference parse(String url)
    {
        String nodeReference = null;
        try {
            // First look at the query string for the nodeRef parameter.
            for (NameValuePair parameter : URLEncodedUtils.parse(new URI(url), "UTF-8")) {
                if ("nodeRef".equals(parameter.getName())) {
                    nodeReference = parameter.getValue();
                    break;
                }
            }
        } catch (URISyntaxException e) {
            // Ignore.
        }
        if (nodeReference == null) {
            // Look in the URL path.
            Matcher matcher = NODE_REF_PATH.matcher(url);
            if (matcher.find()) {
                return new NodeReference(matcher.group(3), new StoreReference(matcher.group(1), matcher.group(2)));
            }
        }
        return nodeReferenceParser.parse(nodeReference);
    }
}
