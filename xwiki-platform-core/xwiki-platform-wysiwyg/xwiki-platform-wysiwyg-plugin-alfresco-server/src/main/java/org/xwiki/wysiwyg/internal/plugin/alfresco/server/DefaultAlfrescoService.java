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

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.gwt.wysiwyg.client.plugin.alfresco.AlfrescoEntity;
import org.xwiki.gwt.wysiwyg.client.plugin.alfresco.AlfrescoService;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;
import org.xwiki.gwt.wysiwyg.client.wiki.URIReference;
import org.xwiki.wysiwyg.plugin.alfresco.server.AlfrescoConfiguration;
import org.xwiki.wysiwyg.plugin.alfresco.server.AlfrescoResponseParser;
import org.xwiki.wysiwyg.plugin.alfresco.server.NodeReference;
import org.xwiki.wysiwyg.plugin.alfresco.server.NodeReferenceParser;
import org.xwiki.wysiwyg.plugin.alfresco.server.SimpleHttpClient;
import org.xwiki.wysiwyg.plugin.alfresco.server.SimpleHttpClient.ResponseHandler;

/**
 * Default {@link AlfrescoService} implementation based on the REST service.
 * 
 * @version $Id$
 */
@Component
public class DefaultAlfrescoService implements AlfrescoService
{
    /**
     * The request parameter used to filter the list of CMIS properties retrieved.
     */
    private static final String FILTER_PARAM = "filter";

    /**
     * The URL path prefix used to access the Alfresco repository node API.
     */
    private static final String NODE_API_URL_PATH_PREFIX = "/alfresco/service/api/node/";

    /**
     * The component that controls the Alfresco access configuration.
     */
    @Inject
    private AlfrescoConfiguration configuration;

    /**
     * The object used to parse the responses received for Alfresco REST requests.
     */
    @Inject
    private AlfrescoResponseParser responseParser;

    /**
     * The HTTP client used to make REST requests to Alfresco.
     */
    @Inject
    private SimpleHttpClient httpClient;

    /**
     * The object used to parse node references.
     */
    @Inject
    private NodeReferenceParser nodeReferenceParser;

    /**
     * The object used to extract the node reference out of an Alfresco URL.
     */
    @Inject
    @Named("url")
    private NodeReferenceParser urlNodeReferenceParser;

    @Override
    public List<AlfrescoEntity> getChildren(final EntityReference parentReference)
    {
        String parentPath = createNodeReference(parentReference).asPath();
        String childrenURL = configuration.getServerURL() + NODE_API_URL_PATH_PREFIX + parentPath + "/children";

        List<Entry<String, String>> parameters =
            Collections.<Entry<String, String>> singletonList(new SimpleEntry<String, String>(FILTER_PARAM,
                "cmis:name,cmis:path,cmis:objectId,cmis:contentStreamMimeType"));
        try {
            return httpClient.doGet(childrenURL, parameters, new ResponseHandler<List<AlfrescoEntity>>()
            {
                public List<AlfrescoEntity> read(InputStream content)
                {
                    return responseParser.parseChildren(content);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to request the list of children.", e);
        }
    }

    @Override
    public AlfrescoEntity getParent(final EntityReference childReference)
    {
        String childPath = createNodeReference(childReference).asPath();
        String parentURL = configuration.getServerURL() + NODE_API_URL_PATH_PREFIX + childPath + "/parent";

        List<Entry<String, String>> parameters =
            Collections.<Entry<String, String>> singletonList(new SimpleEntry<String, String>(FILTER_PARAM,
                "cmis:name,cmis:path,cmis:objectId"));
        try {
            return httpClient.doGet(parentURL, parameters, new ResponseHandler<AlfrescoEntity>()
            {
                public AlfrescoEntity read(InputStream content)
                {
                    AlfrescoEntity parent = responseParser.parseParent(content);
                    if (parent == null) {
                        // If the given child reference points to an Alfresco entity that has no parent then we return
                        // an Alfresco entity that wraps the given child reference.
                        parent = new AlfrescoEntity();
                        parent.setReference(childReference);
                        parent.setPath("/");
                    }
                    return parent;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to request the parent", e);
        }
    }

    /**
     * Creates a node reference from the given entity reference.
     * 
     * @param entityReference an entity reference
     * @return the corresponding entity reference
     */
    private NodeReference createNodeReference(EntityReference entityReference)
    {
        String entityURL = new URIReference(entityReference).getURI();
        return entityURL != null ? urlNodeReferenceParser.parse(entityURL) : nodeReferenceParser.parse(configuration
            .getDefaultNodeReference());
    }
}
