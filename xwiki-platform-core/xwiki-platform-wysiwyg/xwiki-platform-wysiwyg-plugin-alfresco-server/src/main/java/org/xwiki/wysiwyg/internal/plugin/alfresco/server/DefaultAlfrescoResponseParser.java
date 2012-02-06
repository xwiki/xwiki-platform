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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.gwt.wysiwyg.client.plugin.alfresco.AlfrescoEntity;
import org.xwiki.gwt.wysiwyg.client.wiki.URIReference;
import org.xwiki.wysiwyg.plugin.alfresco.server.AlfrescoConfiguration;
import org.xwiki.wysiwyg.plugin.alfresco.server.AlfrescoResponseParser;
import org.xwiki.wysiwyg.plugin.alfresco.server.NodeReferenceParser;
import org.xwiki.xml.EntityResolver;

/**
 * Default implementation of {@link AlfrescoResponseParser}.
 * 
 * @version $Id$
 */
@Component
public class DefaultAlfrescoResponseParser implements AlfrescoResponseParser
{
    /**
     * The element that wraps the CMIS properties.
     */
    private static final String CMIS_PROPERTIES_TAG = "cmis:properties";

    /**
     * The name of the attribute that specified the CMIS property name.
     */
    private static final String PROPERTY_NAME = "propertyDefinitionId";

    /**
     * The object used to parse node references.
     */
    @Inject
    private NodeReferenceParser nodeReferenceParser;

    /**
     * The component used to get the Alfresco server URL.
     */
    @Inject
    private AlfrescoConfiguration configuration;

    /**
     * The component used to resolve XML entities.
     */
    @Inject
    private EntityResolver entityResolver;

    @Override
    public String parseAuthTicket(InputStream json)
    {
        try {
            return new JSONObject(new JSONTokener(new InputStreamReader(json))).getJSONObject("data").getString(
                "ticket");
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parse JSON response.", e);
        }
    }

    @Override
    public AlfrescoEntity parseParent(InputStream xml)
    {
        NodeList cmisPropertiesElements = parseXML(xml).getElementsByTagName(CMIS_PROPERTIES_TAG);
        return cmisPropertiesElements.getLength() > 0 ? createEntity(cmisPropertiesElements.item(0)) : null;
    }

    /**
     * Creates a new Alfresco entity based on the given CMIS properties.
     * 
     * @param cmisProperties the node that wraps the CMIS properties
     * @return a new Alfresco entity
     */
    private AlfrescoEntity createEntity(Node cmisProperties)
    {
        AlfrescoEntity entity = new AlfrescoEntity();
        Node property = cmisProperties.getFirstChild();
        String nodeRef = null;
        while (property != null) {
            if (property.getNodeType() == Node.ELEMENT_NODE && ((Element) property).hasAttribute(PROPERTY_NAME)) {
                String propertyName = ((Element) property).getAttribute(PROPERTY_NAME);
                if ("cmis:path".equals(propertyName)) {
                    entity.setPath(property.getFirstChild().getFirstChild().getNodeValue());
                } else if ("cmis:name".equals(propertyName)) {
                    entity.setName(property.getFirstChild().getFirstChild().getNodeValue());
                } else if ("cmis:objectId".equals(propertyName)) {
                    nodeRef = property.getFirstChild().getFirstChild().getNodeValue();
                } else if ("cmis:contentStreamMimeType".equals(propertyName)) {
                    entity.setMediaType(property.getFirstChild().getFirstChild().getNodeValue());
                }
            }
            property = property.getNextSibling();
        }
        String nodePath = nodeReferenceParser.parse(nodeRef).asPath();
        if (entity.getMediaType() != null) {
            if (entity.getMediaType().startsWith("image/")) {
                // Image URL.
                entity.setUrl(configuration.getServerURL() + "/alfresco/d/d/" + nodePath + '/' + entity.getName());
                // FIXME: The thumbnail service requires separate authentication.
                entity.setPreviewURL(configuration.getServerURL() + "/alfresco/service/api/node/" + nodePath
                    + "/content/thumbnails/doclib");
            } else {
                // Document URL.
                entity.setUrl(configuration.getServerURL() + "/alfresco/n/showDocDetails/" + nodePath);
            }
        } else {
            // Space URL.
            entity.setUrl(configuration.getServerURL() + "/alfresco/n/showSpaceDetails/" + nodePath);
        }
        entity.setReference(new URIReference(entity.getUrl()).getEntityReference());
        return entity;
    }

    @Override
    public List<AlfrescoEntity> parseChildren(InputStream xml)
    {
        List<AlfrescoEntity> children = new ArrayList<AlfrescoEntity>();
        NodeList cmisPropertiesElements = parseXML(xml).getElementsByTagName(CMIS_PROPERTIES_TAG);
        for (int i = 0; i < cmisPropertiesElements.getLength(); i++) {
            children.add(createEntity(cmisPropertiesElements.item(i)));
        }
        return children;
    }

    /**
     * Parses the given XML input stream.
     * 
     * @param xml the XML stream to be parsed
     * @return the DOM document corresponding to the XML input stream
     */
    private Document parseXML(InputStream xml)
    {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentBuilder.setEntityResolver(entityResolver);
            return documentBuilder.parse(xml);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML response.", e);
        }
    }
}
