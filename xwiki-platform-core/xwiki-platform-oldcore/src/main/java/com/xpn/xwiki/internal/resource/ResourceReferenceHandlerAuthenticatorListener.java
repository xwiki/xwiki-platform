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
package com.xpn.xwiki.internal.resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.annotations.Authenticate;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.events.ResourceReferenceHandlingEvent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiContextInitializer;
import com.xpn.xwiki.XWikiException;

/**
 * Automatically initialize and authenticate a {@link XWikiContext} around {@link ResourceReferenceHandler} execution
 * requiring it.
 * 
 * @version $Id$
 * @since 9.11RC1
 */
@Component
@Named("ResourceReferenceHandlerAuthenticatorListener")
@Singleton
public class ResourceReferenceHandlerAuthenticatorListener extends AbstractEventListener
{
    /**
     * Marks the Execution Context in which the XWiki Context has already been initialized and authenticated, so that
     * nested Handlers (e.g. the Entity Resource Action Handlers called by the Entity Resource Reference Handler) don't
     * authenticate the request again: authenticating twice would, among others, record a failed login twice.
     */
    static final String AUTHENTICATED_PROPERTY =
        ResourceReferenceHandlerAuthenticatorListener.class.getName() + ".authenticated";

    @Inject
    private Provider<XWikiContextInitializer> xcontextInitializeProvider;

    @Inject
    private Execution execution;

    /**
     * The default constructor.
     */
    public ResourceReferenceHandlerAuthenticatorListener()
    {
        super("ResourceReferenceHandlerAuthenticatorListener", new ResourceReferenceHandlingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ExecutionContext econtext = this.execution.getContext();
        if (source.getClass().getAnnotation(Authenticate.class) != null
            && (econtext == null || !econtext.hasProperty(AUTHENTICATED_PROPERTY)))
        {
            // Initialize a new XWikiContext no matter one since the one in place is generally a stub context
            // (automatically initialized with the ExecutionContext)
            // TODO: find a way to avoid hacks like this one
            XWikiContext xcontext;
            try {
                // Initialize and authenticate the XWikiContext
                xcontext = this.xcontextInitializeProvider.get().authenticate().initialize(econtext);
            } catch (XWikiException e) {
                throw new RuntimeException("Failed to initialize and authenticate the XWiki context", e);
            }

            // The Handlers of Entity Resource Actions replace the legacy XWikiActions, which run with the action of
            // the URL set in the XWiki Context: code relying on XWikiContext#getAction() must keep working for them.
            if (((ResourceReferenceHandlingEvent) event)
                .getResourceReference() instanceof EntityResourceReference entityReference)
            {
                xcontext.setAction(entityReference.getAction().getActionName());
            }

            if (econtext != null) {
                econtext.setProperty(AUTHENTICATED_PROPERTY, true);
            }
        }
    }
}
