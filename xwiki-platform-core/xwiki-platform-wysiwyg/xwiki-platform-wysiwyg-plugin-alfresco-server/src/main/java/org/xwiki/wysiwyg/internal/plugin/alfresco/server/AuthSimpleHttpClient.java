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
package org.xwiki.wysiwyg.internal.plugin.alfresco.server;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.http.client.methods.HttpRequestBase;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.wysiwyg.plugin.alfresco.server.AlfrescoConfiguration;
import org.xwiki.wysiwyg.plugin.alfresco.server.Authenticator;

/**
 * Extends {@link NoAuthSimpleHttpClient} with authentication capabilities.
 * 
 * @version $Id$
 */
@Component
public class AuthSimpleHttpClient extends NoAuthSimpleHttpClient
{
    /**
     * The component used to get the authenticator hint.
     */
    @Inject
    private AlfrescoConfiguration configuration;

    /**
     * The component used to lookup the authenticator.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    protected <T> T sendRequest(HttpRequestBase request, ResponseHandler<T> handler) throws IOException
    {
        try {
            componentManager.lookup(Authenticator.class, configuration.getAuthenticatorHint()).authenticate(request);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to authenticate the request.", e);
        }
        return super.sendRequest(request, handler);
    }
}
