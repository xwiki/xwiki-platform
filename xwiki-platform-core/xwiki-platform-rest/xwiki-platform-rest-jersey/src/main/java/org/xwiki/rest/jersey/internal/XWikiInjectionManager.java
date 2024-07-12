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
package org.xwiki.rest.jersey.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.internal.process.RequestProcessingContextReference;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.rest.XWikiRestComponent;

/**
 * Wrap the regular Jersey {@link InjectionManager} to get some of the components as XWiki components.
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
@Component(roles = XWikiInjectionManager.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XWikiInjectionManager implements InjectionManager, Initializable
{
    private static final Set<Class<?>> CONTEXT_REQUEST =
        Set.of(HttpHeaders.class, Request.class, ContainerRequestContext.class);

    private static final Set<Class<?>> CONTEXT_ROUTING = Set.of(UriInfo.class, ResourceInfo.class);

    private static final Set<Class<?>> CONTEXT_ASYNC = Set.of(AsyncResponse.class);

    private static final Set<Class<?>> CONTEXT_SECURITY = Set.of(SecurityContext.class);

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private InjectionManager injectionManager;

    @Override
    public void initialize()
    {
        Hk2InjectionManagerFactory factory = new Hk2InjectionManagerFactory();

        this.injectionManager = factory.create();
    }

    // XWiki

    @Override
    public <T> T getInstance(Class<T> contractOrImpl)
    {
        // If the asked component is an XWikiResource, create and initialize it as a XWiki component
        if (XWikiRestComponent.class.isAssignableFrom(contractOrImpl)
            && this.componentManager.hasComponent(XWikiRestComponent.class, contractOrImpl.getName())) {
            try {
                T component = this.componentManager.getInstance(XWikiRestComponent.class, contractOrImpl.getName());

                // In a more standard setup of Jersey, the JAX-RS injection is done at the same time than the JSR-330
                // injection.
                // Problem is that for XWiki components we want to use XWiki component manager and not JSR-330 component
                // manager so we have to take care of the few field injection in the JAX-RS specification.
                injectJAXRS(component);

                return component;
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup the component [{}]", contractOrImpl.getName(), e);
            }
        }

        return this.injectionManager.getInstance(contractOrImpl);
    }

    private RequestProcessingContext getRequestProcessingContext()
    {
        return this.injectionManager.getInstance(RequestProcessingContextReference.class).get();
    }

    private ContainerRequestContext getContainerRequestContext()
    {
        return this.injectionManager.getInstance(ContainerRequestContext.class);
    }

    private void injectJAXRS(Object component)
    {
        // Inject the various JAX-RS context fields
        // TODO: find a way to fully reuse Jersey binders instead of reinventing the wheel
        for (Field field : FieldUtils.getFieldsListWithAnnotation(component.getClass(), Context.class)) {
            try {
                Object value;
                if (CONTEXT_REQUEST.contains(field.getType())) {
                    value = getRequestProcessingContext().request();
                } else if (CONTEXT_ROUTING.contains(field.getType())) {
                    value = getRequestProcessingContext().routingContext();
                } else if (CONTEXT_ASYNC.contains(field.getType())) {
                    value = getRequestProcessingContext().asyncContext();
                } else if (CONTEXT_SECURITY.contains(field.getType())) {
                    value = getContainerRequestContext().getSecurityContext();
                } else {
                    // Fallback on regular CDI components
                    value = this.injectionManager.getInstance(field.getType());
                }

                FieldUtils.writeField(field, component, value, true);
            } catch (IllegalAccessException e) {
                this.logger.error("Failed to inject JAX-RS fields in component [{}]", component.getClass(), e);
            }
        }
    }

    // Standard InjectionManager

    @Override
    public <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers)
    {
        return this.injectionManager.getInstance(contractOrImpl, qualifiers);
    }

    @Override
    public <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer)
    {
        return injectionManager.getInstance(contractOrImpl, classAnalyzer);
    }

    @Override
    public <T> T getInstance(Type contractOrImpl)
    {
        return injectionManager.getInstance(contractOrImpl);
    }

    @Override
    public Object getInstance(ForeignDescriptor foreignDescriptor)
    {
        return injectionManager.getInstance(foreignDescriptor);
    }

    @Override
    public void completeRegistration()
    {
        this.injectionManager.completeRegistration();
    }

    @Override
    public void shutdown()
    {
        this.injectionManager.shutdown();
    }

    @Override
    public boolean isShutdown()
    {
        return this.injectionManager.isShutdown();
    }

    @Override
    public void register(Binding binding)
    {
        this.injectionManager.register(binding);
    }

    @Override
    public void register(Iterable<Binding> descriptors)
    {
        this.injectionManager.register(descriptors);
    }

    @Override
    public void register(Binder binder)
    {
        this.injectionManager.register(binder);
    }

    @Override
    public void register(Object provider) throws IllegalArgumentException
    {
        this.injectionManager.register(provider);
    }

    @Override
    public boolean isRegistrable(Class<?> clazz)
    {
        return this.injectionManager.isRegistrable(clazz);
    }

    @Override
    public <T> T create(Class<T> createMe)
    {
        return this.injectionManager.create(createMe);
    }

    @Override
    public <T> T createAndInitialize(Class<T> createMe)
    {
        return this.injectionManager.createAndInitialize(createMe);
    }

    @Override
    public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contractOrImpl, Annotation... qualifiers)
    {
        return this.injectionManager.getAllServiceHolders(contractOrImpl, qualifiers);
    }

    @Override
    public ForeignDescriptor createForeignDescriptor(Binding binding)
    {
        return this.injectionManager.createForeignDescriptor(binding);
    }

    @Override
    public <T> List<T> getAllInstances(Type contractOrImpl)
    {
        return this.injectionManager.getAllInstances(contractOrImpl);
    }

    @Override
    public void inject(Object injectMe)
    {
        this.injectionManager.inject(injectMe);
    }

    @Override
    public void inject(Object injectMe, String classAnalyzer)
    {
        this.injectionManager.inject(injectMe, classAnalyzer);
    }

    @Override
    public void preDestroy(Object preDestroyMe)
    {
        this.injectionManager.preDestroy(preDestroyMe);
    }
}
