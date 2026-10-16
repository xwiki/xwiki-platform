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
package org.xwiki.resource.test.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletResponse;

import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Handles an Entity Resource Action by describing, in plain text, the context in which it's executed, so that
 * functional tests can verify how the request has been routed and prepared.
 *
 * @version $Id$
 */
public abstract class AbstractTestEntityResourceActionHandler
    extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    private final String action;

    /**
     * @param action the name of the handled action
     */
    protected AbstractTestEntityResourceActionHandler(String action)
    {
        this.action = action;
    }

    protected abstract Container getContainer();

    protected abstract Provider<XWikiContext> getXWikiContextProvider();

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return List.of(new EntityResourceAction(this.action));
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        XWikiContext xcontext = getXWikiContextProvider().get();
        HttpServletResponse response = ((ServletResponse) getContainer().getResponse()).getResponse();
        response.setContentType("text/plain; charset=UTF-8");
        try {
            PrintWriter writer = response.getWriter();
            writer.println("handler=" + getClass().getSimpleName());
            writer.println("action=" + xcontext.getAction());
            writer.println("user=" + xcontext.getUserReference());
            writer.println("wiki=" + xcontext.getWikiId());
            writer.println("reference=" + ((EntityResourceReference) reference).getEntityReference());
            // Handlers rely on the current document being prepared like for the legacy XWikiActions.
            writer.println("doc=" + (xcontext.getDoc() != null ? xcontext.getDoc().getDocumentReference() : null));
            writer.flush();
        } catch (IOException e) {
            throw new ResourceReferenceHandlerException("Failed to write the response", e);
        }

        chain.handleNext(reference);
    }
}
