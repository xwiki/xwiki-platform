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
package org.xwiki.rest.internal;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiRestComponent;

import com.xpn.xwiki.XWikiContext;

/**
 * The filter is called before serving any request and it is responsible to:
 * <ul>
 * <li>Set in user in the response header. The user is expected to be authenticate in a preceding filter along with the
 * XWikiContext initialization</li>
 * <li>Make sure a CRSF is provided for form based requests</li>
 * </ul>
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
@Component
@Named("org.xwiki.rest.internal.ResponseFilter")
@Singleton
@Provider
public class ResponseFilter implements ContainerResponseFilter, XWikiRestComponent
{
    private static final String FORM_TOKEN_HEADER = "XWiki-Form-Token";

    @Inject
    private javax.inject.Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private CSRFToken csrfToken;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        responseContext.getHeaders().addFirst("XWiki-User", this.serializer.serialize(xcontext.getUserReference()));
        responseContext.getHeaders().addFirst("XWiki-Version", xcontext.getWiki().getVersion());

        if (this.csrfToken != null) {
            responseContext.getHeaders().addFirst(FORM_TOKEN_HEADER, this.csrfToken.getToken());
        }
    }
}
