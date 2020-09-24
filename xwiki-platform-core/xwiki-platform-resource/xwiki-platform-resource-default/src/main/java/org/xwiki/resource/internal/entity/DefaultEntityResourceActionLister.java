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
package org.xwiki.resource.internal.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.entity.EntityResourceAction;

/**
 * Takes into account both the old legacy way of defining Actions (in struts.xml) and the new way using
 * {@link EntityResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Singleton
public class DefaultEntityResourceActionLister implements EntityResourceActionLister, Initializable
{
    private static final String STRUTS_CONFIG_RESOURCE = "/struts.xml";

    private List<String> strutsActionNames;

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    protected SAXBuilder createSAXBuilder()
    {
        SAXBuilder builder = new SAXBuilder();

        // Make sure we don't require an Internet Connection to parse the Struts config file!
        builder.setEntityResolver(new EntityResolver()
        {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
            {
                return new InputSource(new StringReader(""));
            }
        });

        return builder;
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Parse the Struts config file (struts.xml) to extract all available actions
        List<String> actionNames = new ArrayList<>();

        // Step 1: Get a stream on the Struts config file if it exists
        try (InputStream strutsConfigStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(getStrutsConfigResource())) {
            if (strutsConfigStream != null) {
                // Step 2: Parse the Strust config file, looking for action names
                Document document = createSAXBuilder().build(strutsConfigStream);

                Element mappingElement = document.getRootElement().getChild("package");
                for (Element element : mappingElement.getChildren("action")) {
                    actionNames.add(element.getAttributeValue("name"));
                }
            }
        } catch (JDOMException | IOException e) {
            throw new InitializationException(
                String.format("Failed to parse Struts Config file [%s]", getStrutsConfigResource()), e);
        }

        this.strutsActionNames = actionNames;
    }

    @Override
    public List<String> listActions()
    {
        List<String> actionNames = new ArrayList<>(this.strutsActionNames);

        // Note: We don't cache the action names coming from Resource Reference Handlers as some extensions could
        // contribute some.
        try {
            Map<String, ResourceReferenceHandler<EntityResourceAction>> componentMap =
                this.contextComponentManagerProvider.get().getInstanceMap(
                    new DefaultParameterizedType(null, ResourceReferenceHandler.class, EntityResourceAction.class));
            for (String key : componentMap.keySet()) {
                actionNames.add(key);
            }
        } catch (ComponentLookupException e) {
            // This should not happen normally.
            throw new RuntimeException("Failed to locate Resource Reference Handlers", e);
        }

        return actionNames;
    }

    protected String getStrutsConfigResource()
    {
        return STRUTS_CONFIG_RESOURCE;
    }
}
