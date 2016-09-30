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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Additionally to using {@link XWikiContext#clone()}, this implementation also tries to make sure that the cloned
 * context is usable in a different thread. To do this, the request and the response are stubbed, the URL factory is
 * reinitialized and the store session/transaction are cleared. This latter point is sensible since there is currently
 * no way to clear the session/transaction only in the copied context. So the session/transaction is cleared in the
 * original context before cloning, causing a side effect on the original context. You should never copy a context
 * while a create/update transaction is in progress, since some changes would get rollbacked.
 * <p>
 * Note: The clone is still rather shallow, since many fields will still be shared with the original
 * {@link XWikiContext}.
 *
 * @version $Id$
 * @since 7.1M2
 */
@Component
@Singleton
public class XWikiContextCopier implements Copier<XWikiContext>
{
    @Inject
    private Copier<XWikiRequest> xwikiRequestCloner;

    /**
     * {@inheritDoc}
     *
     * Any in progress session/transaction on the store retained by the original context will be closed/rollbacked
     * prior cloning (see {@link com.xpn.xwiki.store.XWikiStoreInterface#cleanUp(XWikiContext)}). Therefore,
     * the copy operation has a side effect on the original context. You should never copy a context
     * while a create/update transaction is in progress, since some changes would get rollbacked.
     */
    @Override
    public XWikiContext copy(XWikiContext originalXWikiContext)
    {
        // Clean up the store session/transaction before cloning. For the hibernate store, in progress
        // session/transaction is stored in the context, and would be swallow copied when the context is cloned.
        // Cleaning after clone would not help, since it would close/rollback the session/transaction still referenced
        // in the original context as well, causing this context to be corrupted.
        // The correct way would be to not shallow clone the session/transaction when cloning the original context,
        // since session/transaction are lazy initialize on request when missing.
        originalXWikiContext.getWiki().getStore().cleanUp(originalXWikiContext);

        // This is still a shallow clone, but at least for stuff like wikiID and userReference it gets the job done.
        XWikiContext clonedXWikiContext = originalXWikiContext.clone();

        // lets now build the stub context

        // Copy the request from the context.
        clonedXWikiContext.setRequest(this.xwikiRequestCloner.copy(originalXWikiContext.getRequest()));

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
