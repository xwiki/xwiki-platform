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
package org.xwiki.component.wiki.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of a wiki component builder, that is using the legacy XWiki core module.
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Singleton
public class DefaultWikiComponentBuilder implements WikiComponentBuilder, WikiComponentConstants
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to retrieve parsers dynamically depending on documents syntax.
     */
    @Inject
    private ComponentManager componentManager;
    
    /**
     * Execution context, needed to access the XWiki context map.
     */
    @Inject
    private Execution execution;

    @Override
    public List<DocumentReference> getDocumentReferences()
    {
        List<DocumentReference> results = new ArrayList<DocumentReference>();
        String query = ", BaseObject as obj, StringProperty as role where obj.className=? and obj.name=doc.fullName "
                + "and role.id.id=obj.id and role.id.name=? and role.value <>''";
        List<String> parameters = new ArrayList<String>();
        parameters.add(COMPONENT_CLASS);
        parameters.add(COMPONENT_ROLE_TYPE_FIELD);

        try {
            results.addAll(getXWikiContext().getWiki().getStore().searchDocumentReferences(query, parameters,
                getXWikiContext()));
        } catch (XWikiException e) {
            this.logger.error("Failed to search for existing wiki components [{}]", e.getMessage());
        }

        return results;
    }

    @Override
    public List<WikiComponent> buildComponents(DocumentReference reference) throws WikiComponentException
    {
        List<WikiComponent> components = new ArrayList<WikiComponent>();

        try {
            XWikiDocument componentDocument = getXWikiContext().getWiki().getDocument(reference, getXWikiContext());
            BaseObject componentObject = componentDocument.getObject(COMPONENT_CLASS);

            if (!getXWikiContext().getWiki().getRightService().hasProgrammingRights(componentDocument,
                getXWikiContext())) {
                throw new WikiComponentException("Registering wiki components requires programming rights");
            }

            if (componentObject == null) {
                throw new WikiComponentException("No component object could be found");
            }

            String role = componentObject.getStringValue(COMPONENT_ROLE_TYPE_FIELD);

            if (StringUtils.isBlank(role)) {
                throw new WikiComponentException("No role were precised in the component");
            }

            Class< ? > roleAsClass;
            try {
                roleAsClass = Class.forName(role);
            } catch (ClassNotFoundException e) {
                throw new WikiComponentException("The role class could not be found", e);
            }

            String roleHint = StringUtils.defaultIfEmpty(componentObject.getStringValue("roleHint"), "default");

            DefaultWikiComponent component = new DefaultWikiComponent(reference, roleAsClass, roleHint);
            component.setHandledMethods(this.getHandledMethods(componentDocument));
            component.setImplementedInterfaces(this.getDeclaredInterfaces(componentDocument));

            components.add(component);
        } catch (XWikiException e) {
            throw new WikiComponentException("Failed to build wiki component for document " + reference.toString());
        }

        return components;
    }
    
    /**
     * @param componentDocument the document holding the component description
     * @return the map of component handled methods/method body
     */
    private Map<String, XDOM> getHandledMethods(XWikiDocument componentDocument)
    {
        Map<String, XDOM> handledMethods = new HashMap<String, XDOM>();
        if (componentDocument.getObjectNumbers(METHOD_CLASS) > 0) {
            for (BaseObject method : componentDocument.getObjects(METHOD_CLASS)) {
                if (!StringUtils.isBlank(method.getStringValue(METHOD_NAME_FIELD))) {
                    try {
                        Parser parser =
                            componentManager.getInstance(Parser.class, componentDocument.getSyntax().toIdString());
                        XDOM xdom = parser.parse(new StringReader(method.getStringValue(METHOD_CODE_FIELD)));
                        handledMethods.put(method.getStringValue(METHOD_NAME_FIELD), xdom);
                    } catch (Exception e) {
                        this.logger.error("Failed to execute code for component method [{}] in document [{}] ",
                            method.getNumber(), componentDocument.getPrefixedFullName());
                    }
                }
            }
        }
        return handledMethods;
    }

    /**
     * @param componentDocument the document holding the component description
     * @return the array of interfaces declared (and actually existing) by the document
     */
    private List<Class< ? >> getDeclaredInterfaces(XWikiDocument componentDocument)
    {
        List<Class< ? >> interfaces = new ArrayList<Class< ? >>();
        if (componentDocument.getObjectNumbers(INTERFACE_CLASS) > 0) {
            for (BaseObject iface : componentDocument.getObjects(INTERFACE_CLASS)) {
                if (!StringUtils.isBlank(iface.getStringValue(INTERFACE_NAME_FIELD))) {
                    try {
                        Class< ? > implemented = Class.forName(iface.getStringValue(INTERFACE_NAME_FIELD));
                        interfaces.add(implemented);
                    } catch (ClassNotFoundException e) {
                        this.logger.warn("Interface [{}] not found, declared for wiki component [{}]",
                            iface.getStringValue(INTERFACE_NAME_FIELD), componentDocument.getPrefixedFullName());
                    }
                }
            }
        }
        return interfaces;
    }

    /**
     * @return the XWikiContext extracted from the execution.
     */
    public XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
