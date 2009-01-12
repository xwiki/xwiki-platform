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
package org.xwiki.rest;

import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.Handler;
import org.restlet.Restlet;
import org.restlet.Route;
import org.restlet.Router;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * A router that is able to handle the resource lifecycle using a component manager and that keeps track of the
 * associations between discovered resource classes and the URI template where they are attached using an
 * XWIkiResourceClass registry.
 * 
 * @version $Id$
 */
public class XWikiRouter extends Router
{
    private ComponentManager componentManager;

    /*
     * Since resources are dynamically discovered and added, and since Restlet doesn't provide a way for retrieving the
     * URI template associated to a resource class, we used this additional registry in order to keep track of it. This
     * registry is necessary in order to discover which URI template is associated to a resource when making links in
     * representations.
     */
    private XWikiResourceClassRegistry resourceClassRegistry;

    /**
     * A Finder that uses a component manager in order to instantiate target resources.
     */
    private static class XWikiFinder extends Finder
    {
        private ComponentManager componentManager;

        public XWikiFinder(ComponentManager componentManager, Context context, Class< ? extends Handler> targetClass)
        {
            super(context, targetClass);
            this.componentManager = componentManager;
        }

        @Override
        public Handler createTarget(Class< ? extends Handler> targetClass, Request request, Response response)
        {
            Handler handler = null;

            try {
                /* Use the component manager only for XWikiResource derived classes */
                if (XWikiResource.class.isAssignableFrom(targetClass)) {
                    handler = (Handler) componentManager.lookup(XWikiResource.class.getName(), targetClass.getName());
                    handler.init(getContext(), request, response);
                } else {
                    handler = super.createTarget(targetClass, request, response);
                }
            } catch (ComponentLookupException e) {
                e.printStackTrace();
            }

            return handler;
        }
    }

    /**
     * Constructor.
     * 
     * @param componentManager The component manager to be used for instantiating resource components.
     * @param context The Restlet context.
     */
    public XWikiRouter(ComponentManager componentManager, Context context)
    {
        super(context);
        this.componentManager = componentManager;
        try {
            resourceClassRegistry =
                (XWikiResourceClassRegistry) componentManager.lookup(XWikiResourceClassRegistry.class.getName());
        } catch (ComponentLookupException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.restlet.Router#createFinder(java.lang.Class)
     */
    @Override
    protected Finder createFinder(Class< ? extends Resource> targetClass)
    {
        return new XWikiFinder(componentManager, getContext(), targetClass);
    }

    /*
     * (non-Javadoc)
     * @see org.restlet.Router#attach(java.lang.String, java.lang.Class)
     */
    @Override
    public Route attach(String uriPattern, Class< ? extends Resource> targetClass)
    {
        resourceClassRegistry.registerResourceClass(targetClass, uriPattern);
        return super.attach(uriPattern, targetClass);
    }

    /*
     * (non-Javadoc)
     * @see org.restlet.Router#detach(org.restlet.Restlet)
     */
    @Override
    public void detach(Restlet target)
    {
        resourceClassRegistry.removeResourceClass(target.getClass());
        super.detach(target);
    }

}
