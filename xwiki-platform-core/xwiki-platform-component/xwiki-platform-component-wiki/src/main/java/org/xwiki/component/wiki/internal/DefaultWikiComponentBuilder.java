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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.WikiComponentBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

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
    private ComponentManager rootComponentManager;

    /**
     * Used to access the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Bridge to isolate the old model.
     */
    @Inject
    private WikiComponentBridge componentBridge;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public List<DocumentReference> getDocumentReferences()
    {
        List<DocumentReference> results = new ArrayList<DocumentReference>();
        // Note that the query is made to work with Oracle which treats empty strings as null.
        String query = ", BaseObject as obj, StringProperty as role where obj.className=? and obj.name=doc.fullName "
            + "and role.id.id=obj.id and role.id.name=? "
            + "and  (role.value <> '' or (role.value is not null and '' is null))";
        List<String> parameters = new ArrayList<String>();
        parameters.add(COMPONENT_CLASS);
        parameters.add(COMPONENT_ROLE_TYPE_FIELD);

        try {
            XWikiContext xcontext = xcontextProvider.get();
            results.addAll(xcontext.getWiki().getStore().searchDocumentReferences(query, parameters, xcontext));
        } catch (XWikiException e) {
            this.logger.error("Failed to search for existing wiki components [{}]", e.getMessage());
        }

        return results;
    }

    @Override
    public List<WikiComponent> buildComponents(DocumentReference reference) throws WikiComponentException
    {
        List<WikiComponent> components = new ArrayList<WikiComponent>();

        if (!this.authorization.hasAccess(Right.PROGRAM, reference)) {
            throw new WikiComponentException("Registering wiki components requires programming rights");
        }

        DefaultWikiComponent rawComponent =
            new DefaultWikiComponent(reference, componentBridge.getAuthorReference(reference),
                componentBridge.getRoleType(reference), componentBridge.getRoleHint(reference),
                componentBridge.getScope(reference));
        rawComponent.setHandledMethods(componentBridge.getHandledMethods(reference));
        rawComponent.setImplementedInterfaces(componentBridge.getDeclaredInterfaces(reference));
        rawComponent.setDependencies(componentBridge.getDependencies(reference));
        rawComponent.setSyntax(componentBridge.getSyntax(reference));

        // Create the method invocation handler of the proxy
        InvocationHandler handler = new DefaultWikiComponentInvocationHandler(rawComponent, rootComponentManager);

        // Prepare a list containing the interfaces the component implements
        List<Class<?>> implementedInterfaces = new ArrayList<Class<?>>();
        // Add the main role
        Class<?> roleTypeClass = ReflectionUtils.getTypeClass(rawComponent.getRoleType());
        // Add the component role
        implementedInterfaces.add(ReflectionUtils.getTypeClass(roleTypeClass));
        // Add the additional interfaces declared through XObjects
        implementedInterfaces.addAll(rawComponent.getImplementedInterfaces());
        // Add the interfaces from the java class itself (interfaces implemented by DefaultWikiComponent)
        implementedInterfaces.addAll(Arrays.asList(rawComponent.getClass().getInterfaces()));

        // Create the proxy
        Class<?>[] implementedInterfacesArray = implementedInterfaces.toArray(new Class<?>[0]);
        WikiComponent component = (WikiComponent) Proxy.newProxyInstance(roleTypeClass.getClassLoader(),
            implementedInterfacesArray, handler);

        components.add(component);

        return components;
    }
}
