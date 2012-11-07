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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentRuntimeException;
import org.xwiki.rendering.block.XDOM;

/**
 * Method invocation handler for wiki component proxy instances. Has a reference on a map of name/body wiki code of
 * supported methods.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public class DefaultWikiComponentInvocationHandler implements InvocationHandler
{
    /**
     * The key under which the component reference (a virtual "this") is kept in the method invocation context. 
     */
    private static final String METHOD_CONTEXT_COMPONENT_KEY = "component";

    /**
     * The logger to log.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultWikiComponentInvocationHandler.class);

    /**
     * Our component manager.
     */
    private ComponentManager componentManager;

    /**
     * The proxied wiki component.
     */
    private DefaultWikiComponent wikiComponent;
    
    /**
     * Constructor of this invocation handler.
     * 
     * @param wikiComponent the proxied wiki component
     * @param componentManager the component manager
     */
    public DefaultWikiComponentInvocationHandler(DefaultWikiComponent wikiComponent, ComponentManager componentManager)
    {
        this.wikiComponent = wikiComponent;
        this.componentManager = componentManager;
    }

    /**
     * Retrieves the wiki component dependencies from the component manager and puts them in the method context under
     * the configured key.
     *
     * @param methodContext The context where the dependencies must be injected
     */
    private void injectComponentDependencies(Map<String, Object> methodContext)
    {
        for (Map.Entry<String, ComponentDescriptor> dependency : this.wikiComponent.getDependencies().entrySet()) {
            ComponentDescriptor cd = dependency.getValue();
            Class<?> roleTypeClass = ReflectionUtils.getTypeClass(cd.getRoleType());
            Object componentDependency = null;
            try {
                if (roleTypeClass.isAssignableFrom(List.class)) {
                    // If the ParameterizedType is a List, the raw Type is the List Class and the first Type argument
                    // is the actual component (which can be a ParameterizedType itself).
                    // Example: java.util.List<org.xwiki.model.reference.EntityReferenceSerializer<java.lang.String>>
                    // raw Type: java.util.List
                    // Type arguments [0]: org.xwiki.model.reference.EntityReferenceSerializer<java.lang.String>
                    componentDependency =
                        componentManager.getInstanceList(
                            ((ParameterizedType) cd.getRoleType()).getActualTypeArguments()[0]);
                } else if (roleTypeClass.isAssignableFrom(Map.class)) {
                    // If the ParameterizedType is a Map, the raw Type is the Map, the first argument can only be a
                    // String in our implementation and the second argument is the actual component.
                    // Example: java.util.Map<java.lang.String,
                    // org.xwiki.model.reference.EntityReferenceSerializer<java.lang.String>>
                    // raw Type: java.util.Map
                    // Type arguments [0]: java.lang.String
                    // [1]: org.xwiki.model.reference.EntityReferenceSerializer<java.lang.String>
                    componentDependency = componentManager.getInstanceMap(
                        ((ParameterizedType) cd.getRoleType()).getActualTypeArguments()[1]);
                } else {
                    // Not a List or a Map, note that the role Type can be a ParameterizedType itself
                    // Example: org.xwiki.model.reference.EntityReferenceSerializer<java.lang.String>
                    componentDependency = componentManager.getInstance(cd.getRoleType(), cd.getRoleHint());
                }
            } catch (ComponentLookupException e) {
                this.logger.warn(String.format(
                    "No component found for role [%s] with hint [%s], declared as dependency for wiki component [%s]",
                    cd.getRoleType().toString(), cd.getRoleHint(), this.wikiComponent.getDocumentReference()));
            }
            methodContext.put(dependency.getKey(), componentDependency);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception
    {
        // We look for the method in the XObjects.
        if (!this.wikiComponent.getHandledMethods().containsKey(method.getName())) {
            if (method.getDeclaringClass() == Object.class || method.getDeclaringClass() == WikiComponent.class) {
                // return ObjectMethodsProxy.invoke(proxy, method, args);
                return method.invoke(wikiComponent, args);
            } else {
                // Note: We throw a runtime exception so that our exception doesn't get wrapped by a generic
                // UndeclaredThrowableException which would not make much sense for the user.
                // See http://docs.oracle.com/javase/6/docs/api/java/lang/reflect/UndeclaredThrowableException.html
                throw new WikiComponentRuntimeException(
                    String.format("You need to add an Object of type [%s] in document [%s] to implement method [%s.%s]",
                        WikiComponentConstants.METHOD_CLASS,
                        this.wikiComponent.getDocumentReference(),
                        method.getDeclaringClass().getName(),
                        method.getName()));
            }
        } else {
            WikiComponentMethodExecutor methodExecutor =
                componentManager.getInstance(WikiComponentMethodExecutor.class);
            Map<String, Object> methodContext = new HashMap<String, Object>();
            XDOM xdom = this.wikiComponent.getHandledMethods().get(method.getName());
            methodContext.put(METHOD_CONTEXT_COMPONENT_KEY, proxy);
            this.injectComponentDependencies(methodContext);
            return methodExecutor.execute(method, args, wikiComponent.getDocumentReference(), xdom,
                wikiComponent.getSyntax(), methodContext);
        }
    }
}
