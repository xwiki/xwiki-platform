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
package com.xpn.xwiki.internal.security.authorization;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorExecutor;

import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link AuthorExecutor}.
 * 
 * @version $Id$
 * @since 8.3RC1
 */
@Component
@Singleton
// FIXME: It would probably make more sense to put it in xwiki-platform-security-bridge but it's a lot easier for tests
// to have it in oldcore right now
public class DefaultAuthorExecutor implements AuthorExecutor
{
    /**
     * Contain the informations to restore.
     *
     * @version $Id$
     */
    private final class DefaultAuthorExecutorContext implements AutoCloseable
    {
        private XWikiDocument currentSecureDocument;

        private Object xwikiContextDropPermissionHack;

        private Object documentDropPermissionHack;

        private DefaultAuthorExecutorContext()
        {
            // Only SUExecutor can create SUExecutorContext
        }

        @Override
        public void close() throws Exception
        {
            after(this);
        }
    }

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Execution execution;

    @Override
    public <V> V call(Callable<V> callable, DocumentReference authorReference) throws Exception
    {
        try (AutoCloseable context = before(authorReference)) {
            return callable.call();
        }
    }

    @Override
    public AutoCloseable before(DocumentReference authorReference)
    {
        DefaultAuthorExecutorContext suContext;

        XWikiContext xwikiContext = this.xcontextProvider.get();

        if (xwikiContext != null) {
            suContext = new DefaultAuthorExecutorContext();

            // Make sure to have the right secure document
            suContext.currentSecureDocument = (XWikiDocument) xwikiContext.get(XWikiDocument.CKEY_SDOC);
            XWikiDocument secureDocument = new XWikiDocument(new DocumentReference(
                authorReference != null ? authorReference.getWikiReference().getName() : "xwiki", "SUSpace", "SUPage"));
            secureDocument.setContentAuthorReference(authorReference);
            secureDocument.setAuthorReference(authorReference);
            secureDocument.setCreatorReference(authorReference);
            xwikiContext.put(XWikiDocument.CKEY_SDOC, secureDocument);

            // Make sure to disable XWikiContext#dropPermission hack
            suContext.xwikiContextDropPermissionHack = xwikiContext.remove(XWikiConstant.DROPPED_PERMISSIONS);

            // Make sure to disable Document#dropPermission hack
            ExecutionContext econtext = this.execution.getContext();
            if (econtext != null) {
                suContext.documentDropPermissionHack = econtext.getProperty(XWikiConstant.DROPPED_PERMISSIONS);
                econtext.removeProperty(XWikiConstant.DROPPED_PERMISSIONS);
            }
        } else {
            suContext = null;
        }

        return suContext;
    }

    @Override
    public void after(AutoCloseable context)
    {
        XWikiContext xwikiContext = this.xcontextProvider.get();

        if (xwikiContext != null) {
            DefaultAuthorExecutorContext internalContext = (DefaultAuthorExecutorContext) context;

            // Restore context document's content author
            xwikiContext.put(XWikiDocument.CKEY_SDOC, internalContext.currentSecureDocument);

            // Restore XWikiContext#dropPermission hack
            if (internalContext.xwikiContextDropPermissionHack != null) {
                xwikiContext.put(XWikiConstant.DROPPED_PERMISSIONS, internalContext.xwikiContextDropPermissionHack);
            }

            // Restore Document#dropPermission hack
            if (internalContext.documentDropPermissionHack != null) {
                ExecutionContext econtext = this.execution.getContext();
                econtext.setProperty(XWikiConstant.DROPPED_PERMISSIONS, internalContext.documentDropPermissionHack);
            }
        }
    }
}
