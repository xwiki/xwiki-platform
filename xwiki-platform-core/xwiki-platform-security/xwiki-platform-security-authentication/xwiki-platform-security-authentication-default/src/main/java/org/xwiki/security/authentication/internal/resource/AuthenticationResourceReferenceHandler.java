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
package org.xwiki.security.authentication.internal.resource;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.security.authentication.api.AuthenticationResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiContextInitializer;
import com.xpn.xwiki.web.Utils;

/**
 * Default component to handle {@link AuthenticationResourceReference}.
 *
 * @version $Id$
 * @since 13.0RC1
 */
@Component
@Named(AuthenticationResourceReference.RESOURCE_TYPE_ID)
@Singleton
public class AuthenticationResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    @Inject
    private XWikiContextInitializer xWikiContextInitializer;

    @Inject
    private Execution execution;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Collections.singletonList(AuthenticationResourceReference.TYPE);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        AuthenticationResourceReference authenticationResourceReference = (AuthenticationResourceReference) reference;

        switch (authenticationResourceReference.getAction()) {
            case FORGOT_USERNAME:
                this.handleAction("forgotusername");
                break;

            case RESET_PASSWORD:
                this.handleAction("resetpassword");
                break;

            default:
                // nothing to do here.
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private void handleAction(String templateName) throws ResourceReferenceHandlerException
    {
        ExecutionContext context = this.execution.getContext();
        if (context == null) {
            context = new ExecutionContext();
        }
        try {
            XWikiContext xWikiContext = this.xWikiContextInitializer.initialize(context);
            // We are directly relying on Utils#parseTemplate because we want the plugin manager to properly
            // handle the javascript placeholders and it avoids duplicating code.
            Utils.parseTemplate(templateName, true, xWikiContext);
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(
                String.format("Error while rendering template [%s]: [%s].",
                templateName, ExceptionUtils.getRootCauseMessage(e)), e);
        }
    }
}
