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
package org.xwiki.security.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.UserSecurityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Temporary implementation of the (@link XWikiBridge} interface to access xwiki information.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultXWikiBridge implements XWikiBridge
{
    /** Document reference resolver for user and group. */
    @Inject
    @Named("user")
    private DocumentReferenceResolver<String> resolver;

    /** Execution object. */
    @Inject
    private Execution execution;

    /** Cached main wiki reference. */
    private WikiReference mainWikiReference;

    /**
     * @return the current {@code XWikiContext}
     */
    private XWikiContext getXWikiContext() {
        return ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
    }

    @Override
    public WikiReference getMainWikiReference()
    {
        if (mainWikiReference == null) {
            mainWikiReference = new WikiReference(getXWikiContext().getMainXWiki());
        }
        return mainWikiReference;
    }

    @Override
    public boolean isWikiReadOnly()
    {
        return getXWikiContext().getWiki().isReadOnly();
    }

    @Override
    public boolean isWikiOwner(UserSecurityReference user, WikiReference wikiReference)
    {
        if (user == null || wikiReference == null) {
            return false;
        }

        XWikiContext context = getXWikiContext();
        String wikiOwner;
        try {
            wikiOwner = context.getWiki().getWikiOwner(wikiReference.getName(), context);
        } catch (XWikiException e) {
            return false;
        }

        if (wikiOwner == null) {
            return false;
        }

        DocumentReference ownerRef = resolver.resolve(wikiOwner, wikiReference);
        return user.getOriginalReference().equals(ownerRef);
    }
}
