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
package org.xwiki.uiextension.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides {@link org.xwiki.uiextension.UIExtension} components from definitions stored in XObjects.
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Singleton
@Named("uiextension")
public class WikiUIExtensionComponentBuilder implements WikiComponentBuilder, WikiUIExtensionConstants
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The {@link org.xwiki.context.Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * Used to transform the wiki page reference where the UI Extension is defined into a Component Role Hint.
     * We use a compact serializer since UI Extensions are registered for a given wiki only.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    /**
     * Used to get the parser to transform the extension content to a XDOM.
     */
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    /**
     * Parse the data provided by the extension.
     * The data is provided in a LargeString property of the extension object. In the future it would be better
     * to have a Map<String, String> XClass property.
     *
     * @param rawData the string to parse
     * @return a map of data
     */
    private Map<String, String> parseData(String rawData)
    {
        Map<String, String> data = new HashMap<String, String>();
        for (String line : rawData.split("[\\r\\n]+")) {
            String[] pair = line.split("=", 2);
            if (pair.length == 2 && !"".equals(pair[0]) && !"".equals(pair[1])) {
                data.put(pair[0], pair[1]);
            }
        }

        return data;
    }

    @Override
    public List<WikiComponent> buildComponents(DocumentReference reference) throws WikiComponentException
    {
        List<WikiComponent> extensions = new ArrayList<WikiComponent>();
        XWikiDocument doc = null;

        try {
            doc = getXWikiContext().getWiki().getDocument(reference, getXWikiContext());

            if (!getXWikiContext().getWiki().getRightService().hasAccessLevel("admin", doc.getContentAuthor(),
                "XWiki.XWikiPreferences", getXWikiContext())) {
                throw new WikiComponentException("Registering UI extensions requires admin rights at the wiki level");
            }
        } catch (XWikiException e) {
            throw new WikiComponentException("Failed to create UI Extension(s)", e);
        }

        // Check whether this document contains a listener definition.
        List<BaseObject> extensionDefinitions = doc.getXObjects(UI_EXTENSION_CLASS);

        if (extensionDefinitions.size() == 0) {
            throw new WikiComponentException(String.format("No UI extension object could be found in document [%s]",
                doc.getPrefixedFullName()));
        }

        for (BaseObject extensionDefinition : extensionDefinitions) {
            // Extract extension definition.
            String id = extensionDefinition.getStringValue(ID_PROPERTY);
            String extensionPointId = extensionDefinition.getStringValue(EXTENSION_POINT_ID_PROPERTY);
            String content = extensionDefinition.getStringValue(CONTENT_PROPERTY);
            Map<String, String> data = parseData(extensionDefinition.getStringValue(DATA_PROPERTY));

            try {
                Parser parser = componentManager.getInstance(Parser.class, doc.getSyntax().toIdString());

                try {
                    XDOM xdom = parser.parse(new StringReader(content));
                    WikiUIExtension extension =
                        new WikiUIExtension(reference, id, extensionPointId, xdom, doc.getSyntax(), data,
                            componentManager);
                    extensions.add(extension);
                } catch (ParseException e) {
                    throw new WikiComponentException(
                        String.format("Failed to find parse content of extension [{}]", id));
                }
            } catch (ComponentLookupException e) {
                throw new WikiComponentException(String.format("Failed to find a parser for syntax [{}]",
                    doc.getSyntax().toIdString()));
            }
        }

        return extensions;
    }

    /**
     * @return list of document references to documents containing a UI extension object.
     */
    public List<DocumentReference> getDocumentReferences()
    {
        List<DocumentReference> results = new ArrayList<DocumentReference>();
        String query = ", BaseObject as obj, StringProperty as epId where obj.className=? "
            + "and obj.name=doc.fullName and epId.id.id=obj.id and epId.id.name=? and epId.value <>''";
        List<String> parameters = new ArrayList<String>();
        parameters.add(this.compactWikiSerializer.serialize(UI_EXTENSION_CLASS));
        parameters.add(EXTENSION_POINT_ID_PROPERTY);

        try {
            results.addAll(
                getXWikiContext().getWiki().getStore().searchDocumentReferences(query, parameters, getXWikiContext()));
        } catch (XWikiException e) {
            this.logger.warn("Search for UI extensions failed: [{}]", e.getMessage());
        }

        return results;
    }

    /**
     * Utility method for accessing XWikiContext.
     *
     * @return the XWikiContext.
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
