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
package org.xwiki.component.wiki;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.component.wiki.internal.DefaultMethodOutputHandler;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Method invocation handler for wiki component proxy instances. Has a reference on a map of name/body wiki code of
 * supported methods.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public class WikiComponentInvocationHandler implements InvocationHandler
{


    /**
     * The key under which the component reference (a virtual "this") is kept in the method invocation context. 
     */
    private static final String METHOD_CONTEXT_COMPONENT_KEY = "component";

    /**
     * The key under which inputs are kept in the method invocation context.
     */
    private static final String METHOD_CONTEXT_INPUT_KEY = "input";
    
    /**
     * The key under which the output is kept in the method invocation context.
     */
    private static final String METHOD_CONTEXT_OUTPUT_KEY = "output";

    /**
     * The key under which the context document is kept in the XWiki context.
     */
    private static final String XWIKI_CONTEXT_DOC_KEY = "doc";

    /**
     * Pre-loaded hasCode Method.
     * 
     * @see {@link Object#hashCode()}
     */
    private static Method hashCodeMethod;

    /**
     * Pre-loaded equals method.
     * 
     * @see {@link Object#equals(Object)}
     */
    private static Method equalsMethod;

    /**
     * Pre-loaded toString method.
     * 
     * @see {@link Object#toString()}
     */
    private static Method toStringMethod;

    /**
     * The logger to log.
     */
    private final Logger logger = LoggerFactory.getLogger(WikiComponentInvocationHandler.class);

    static {
        try {
            hashCodeMethod = Object.class.getMethod("hashCode", null);
            equalsMethod = Object.class.getMethod("equals", new Class[] {Object.class});
            toStringMethod = Object.class.getMethod("toString", null);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    /**
     * Our component manager.
     */
    private ComponentManager componentManager;

    /**
     * The proxied wiki component.
     */
    private WikiComponent wikiComponent;
    
    /**
     * Constructor of this invocation handler.
     * 
     * @param wikiComponent the proxied wiki component
     * @param componentManager the component manager
     */
    public WikiComponentInvocationHandler(WikiComponent wikiComponent, ComponentManager componentManager)
    {
        this.wikiComponent = wikiComponent;
        this.componentManager = componentManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        Method componentMethods[] = this.wikiComponent.getClass().getDeclaredMethods();

        // We look at the methods in the Class, in case it implements the method.
        if (method.getDeclaringClass().isAssignableFrom(this.wikiComponent.getClass()))
        {
            for (Method componentMethod : componentMethods) {
                if (method.getName().equals(componentMethod.getName())) {
                        return method.invoke(this.wikiComponent, args);
                }
            }
        }

        // We look for the method in the XObjects.
        if (!this.wikiComponent.getHandledMethods().containsKey(method.getName())) {
            if (method.getDeclaringClass() == Object.class) {
                return this.proxyObjectMethod(proxy, method, args);
            } else {
                // Note: We throw a runtime exception so that our exception doesn't get wrapped by a generic
                // UndeclaredThrowableException which would not make much sense for the user.
                // See http://docs.oracle.com/javase/6/docs/api/java/lang/reflect/UndeclaredThrowableException.html
                throw new WikiComponentRuntimeException(
                    String.format("You need to add an Object of type [%s] in document [%s] to implement method [%s.%s]",
                        WikiComponentConstants.METHOD_CLASS,
                        getSerializedWikiComponentDocumentReference(),
                        method.getDeclaringClass().getName(),
                        method.getName()));
            }
        } else {
            return this.executeWikiContent(proxy, method, args);
        }
    }

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

    /**
     * "Executes" the wiki content associated to the passed method.
     * 
     * @param method the method to execute
     * @return the result of the execution
     * @throws Exception when an error occurs during execution
     */
    @SuppressWarnings("unchecked")
    private Object executeWikiContent(Object proxy, Method method, Object[] args) throws Exception
    {
        XDOM xdom = this.wikiComponent.getHandledMethods().get(method.getName());

        Execution execution = componentManager.getInstance(Execution.class);
        Transformation macroTransformation = componentManager.getInstance(Transformation.class, "macro");
        DocumentAccessBridge docBridge = componentManager.getInstance(DocumentAccessBridge.class);

        Map<String, Object> methodContext = new HashMap<String, Object>();
        methodContext.put(METHOD_CONTEXT_OUTPUT_KEY, new DefaultMethodOutputHandler());
        methodContext.put(METHOD_CONTEXT_COMPONENT_KEY, proxy);

        Map<Integer, Object> inputs = new HashMap<Integer, Object>();
        if (args != null && args.length > 0) {
            // Start with "0" as first input key.
            for (int i = 0; i < args.length; i++) {
                inputs.put(i, args[i]);
            }
        }
        methodContext.put(METHOD_CONTEXT_INPUT_KEY, inputs);

        this.injectComponentDependencies(methodContext);

        // TODO: Fix this since the XWiki Context is the old way of doing things. We need a solution based on the
        // Execution Context instead.
        // Place macro context inside xwiki context ($context.macro).
        Map<Object, Object> xwikiContext = (Map<Object, Object>) execution.getContext().getProperty("xwikicontext");
        xwikiContext.put("method", methodContext);
        // Save current context document, to put it back after the execution.
        Object contextDoc = xwikiContext.get(XWIKI_CONTEXT_DOC_KEY);
        // Make sure has prog rights
        xwikiContext.put(XWIKI_CONTEXT_DOC_KEY, docBridge.getDocument(this.wikiComponent.getDocumentReference()));

        try {
            // Perform internal macro transformations.
            XDOM transformedXDOM;
            try {
                Syntax syntax = docBridge.getDocument(this.wikiComponent.getDocumentReference()).getSyntax();
                TransformationContext transformationContext = new TransformationContext(xdom, syntax);
                transformationContext.setId(method.getClass().getName() + "#" + method.getName());
                // We need to clone the xdom to avoid transforming the original and make it useless after the first
                // transformation
                transformedXDOM = xdom.clone();
                macroTransformation.transform(transformedXDOM, transformationContext);
            } catch (TransformationException e) {
                throw new WikiComponentRuntimeException(String.format(
                    "Error while executing wiki component macro transformation for method [%s]", method.getName()), e);
            }

            if (!method.getReturnType().getName().equals("void")) {
                if (methodContext.get(METHOD_CONTEXT_OUTPUT_KEY) != null
                    && ((MethodOutputHandler) methodContext.get(METHOD_CONTEXT_OUTPUT_KEY)).getValue() != null) {
                    return method.getReturnType().cast(((MethodOutputHandler)
                        methodContext.get(METHOD_CONTEXT_OUTPUT_KEY)).getValue());
                } else {
                    // Since no return value has been explicitly provided, we try to convert the result of the rendering
                    // into the expected return type using a Converter.
                    WikiPrinter printer = new DefaultWikiPrinter();
                    BlockRenderer renderer =
                        componentManager.getInstance(BlockRenderer.class, Syntax.PLAIN_1_0.toIdString());
                    renderer.render(transformedXDOM, printer);
                    String contentResult = printer.toString();

                    // Do the conversion!
                    ConverterManager converterManager = componentManager.getInstance(ConverterManager.class);
                    try {
                        return converterManager.convert(method.getGenericReturnType(), contentResult);
                    } catch (ConversionException e) {
                        // Surrender!
                        throw new WikiComponentRuntimeException(String.format("Failed to convert result [%s] to type [%s] "
                            + "for method [%s.%s] found in Object [%s] of document [%s]",
                            contentResult,
                            method.getGenericReturnType(),
                            method.getDeclaringClass().getName(),
                            method.getName(),
                            WikiComponentConstants.METHOD_CLASS,
                            getSerializedWikiComponentDocumentReference()), e);
                    }
                }
            } else {
                return null;
            }
        } finally {
            if (contextDoc != null) {
                xwikiContext.put(XWIKI_CONTEXT_DOC_KEY, contextDoc);
            }
        }
    }

    /**
     * Helper code to get a serialized document reference for the document containing the wiki component.
     *
     * @return the String representation of the document reference
     * @throws ComponentLookupException if the reference serializer component couldn't be found
     */
    private String getSerializedWikiComponentDocumentReference() throws ComponentLookupException
    {
        EntityReferenceSerializer<String> serializer =
            this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
        return serializer.serialize(this.wikiComponent.getDocumentReference());
    }

    /**
     * Proxies a method of the {@link Object} class.
     * 
     * @param proxy the proxy instance
     * @param method the method to proxy
     * @param args possible arguments to the method invocation
     * @return the result of the proxied call
     */
    private Object proxyObjectMethod(Object proxy, Method method, Object[] args)
    {
        if (method.equals(hashCodeMethod)) {
            return proxyHashCode(proxy);
        } else if (method.equals(equalsMethod)) {
            return proxyEquals(proxy, args[0]);
        } else if (method.equals(toStringMethod)) {
            return proxyToString(proxy);
        } else {
            throw new InternalError("unexpected Object method dispatched: " + method);
        }
    }

    /**
     * Default behavior for {@link Object#hashCode()} when not overridden in the wiki component definition.
     * 
     * @param proxy the proxy object
     * @return a hash code for the proxy object, as if using standard {Object{@link #hashCode()}.
     */
    protected Integer proxyHashCode(Object proxy)
    {
        return new Integer(System.identityHashCode(proxy));
    }

    /**
     * Default behavior for {@link Object#equals(Object)} when not overridden in the wiki component definition.
     * 
     * @param proxy the proxy object
     * @param other the other object of the comparison
     * @return the result of the equality comparison between the passed proxy and other object
     */
    protected Boolean proxyEquals(Object proxy, Object other)
    {
        return (proxy == other ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Default behavior for {@link Object#toString()} when not overridden in the wiki component definition.
     * 
     * @param proxy the proxy object
     * @return the String representation of the passed proxy object
     */
    protected String proxyToString(Object proxy)
    {
        return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
    }

}
