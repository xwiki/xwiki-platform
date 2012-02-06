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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.auth.BasicScheme;
import org.xwiki.component.annotation.Component;
import org.xwiki.wysiwyg.plugin.alfresco.server.AlfrescoConfiguration;
import org.xwiki.wysiwyg.plugin.alfresco.server.Authenticator;

/**
 * HTTP basic access authentication.
 * 
 * @version $Id$
 */
@Component
@Named("basic")
public class BasicAuthenticator implements Authenticator
{
    /**
     * The component that specifies the user name and password.
     */
    @Inject
    private AlfrescoConfiguration configuration;

    @Override
    public void authenticate(HttpRequestBase request)
    {
        UsernamePasswordCredentials credentials =
            new UsernamePasswordCredentials(configuration.getUserName(), configuration.getPassword());
        request.addHeader(BasicScheme.authenticate(credentials, "US-ASCII", false));
    }
}
