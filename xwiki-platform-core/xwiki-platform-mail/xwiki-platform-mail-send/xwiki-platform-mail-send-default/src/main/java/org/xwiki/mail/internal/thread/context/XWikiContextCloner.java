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
package org.xwiki.mail.internal.thread.context;

import java.net.URL;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Additionally to using {@link XWikiContext#clone()}, this implementation also tries to make sure that the cloned
 * context is usable in a different thread. To do this, the request and the response are stubbed, the URL factory is
 * reinitialized and the store cache is cleared.
 * <p/>
 * Note: The clone is still rather shallow, since many fields will still be shared with the original
 * {@link XWikiContext}.
 *
 * @version $Id$
 * @since 7.1M2
 */
@Component
@Singleton
public class XWikiContextCloner implements Cloner<XWikiContext>
{
    @Override
    public XWikiContext clone(XWikiContext originalXWikiContext)
    {
        // This is still a shallow clone, but at least for stuff like wikiID and userReference it gets the job done.
        XWikiContext clonedXWikiContext = originalXWikiContext.clone();

        // lets now build the stub context
        clonedXWikiContext.getWiki().getStore().cleanUp(originalXWikiContext);

        // We are sure the context request is a real servlet request
        // So we force the dummy request with the current host
        XWikiServletRequestStub dummy = new XWikiServletRequestStub();
        XWikiRequest originalRequest = originalXWikiContext.getRequest();
        dummy.setHost(((HttpServletRequest) originalRequest).getHeader("x-forwarded-host"));
        dummy.setScheme(((HttpServletRequest) originalRequest).getScheme());
        dummy.setContextPath(((HttpServletRequest) originalRequest).getContextPath());
        // TODO: include the original parameters map?

        XWikiServletRequest request = new XWikiServletRequest(dummy);
        clonedXWikiContext.setRequest(request);

        // Force forged context response to a stub response, since the current context response
        // will not mean anything anymore when running in the scheduler's thread, and can cause
        // errors.
        XWikiResponse stub = new XWikiServletResponseStub();
        clonedXWikiContext.setResponse(stub);

        // feed the dummy context
        if (clonedXWikiContext.getURL() == null) {
            try {
                clonedXWikiContext.setURL(new URL("http://www.mystuburl.com/"));
            } catch (Exception e) {
                // the URL is clearly well formed
            }
        }

        XWikiURLFactory xurf =
            originalXWikiContext.getWiki().getURLFactoryService()
                .createURLFactory(originalXWikiContext.getMode(), originalXWikiContext);
        clonedXWikiContext.setURLFactory(xurf);

        return clonedXWikiContext;
    }
}
