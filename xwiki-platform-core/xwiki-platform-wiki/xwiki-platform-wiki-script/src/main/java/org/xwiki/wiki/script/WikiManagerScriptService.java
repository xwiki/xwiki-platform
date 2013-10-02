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
package org.xwiki.wiki.script;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;

@Component
@Named("wiki")
@Singleton
public class WikiManagerScriptService implements ScriptService
{
    /**
     * Field name of the last API exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception2";

    @Inject
    private WikiManager wikiManager;

    /**
     * Execution context.
     */
    @Inject
    private Execution execution;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Logging tool.
     */
    @Inject
    private Logger logger;

    public WikiDescriptor createWiki(String wikiId, String wikiAlias)
    {
        WikiDescriptor descriptor = null;

        XWikiContext context = getXWikiContext();

        try {
            // Check right access
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(),
                    new WikiReference(context.getMainXWiki()));

            // Create the wiki
            descriptor = wikiManager.createWiki(wikiId, wikiAlias);

        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
        } catch (AccessDeniedException e) {
            error("You don't have the right to create wiki", e);
        }

        return descriptor;
    }

    public boolean deleteWiki(WikiDescriptor descriptor)
    {
        XWikiContext context = getXWikiContext();

        WikiReference wikiReference = new WikiReference(descriptor.getWikiId());

        try {
            // Check right access
            authorizationManager.checkAccess(Right.ADMIN, context.getUserReference(), wikiReference);
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(), wikiReference);

            // Delete the wiki
            wikiManager.deleteWiki(descriptor);

        } catch (WikiManagerException e) {
            return false;
        } catch (AccessDeniedException e) {
            error("You don't have the right to delete the wiki", e);
        }

        return true;
    }

    public WikiDescriptor getByWikiAlias(String wikiAlias)
    {
        WikiDescriptor descriptor = null;

        try {
            descriptor = wikiManager.getByWikiAlias(wikiAlias);
        } catch (WikiManagerException e) {
        }

        return descriptor;
    }

    public WikiDescriptor getByWikiId(String wikiId)
    {
        WikiDescriptor descriptor = null;

        try {
            descriptor = wikiManager.getByWikiId(wikiId);
        } catch (WikiManagerException e) {
        }

        return descriptor;
    }

    public Collection<WikiDescriptor> getAll()
    {
        Collection<WikiDescriptor> descriptors;
        try {
            descriptors = wikiManager.getAll();
        } catch (WikiManagerException e) {
            descriptors = new ArrayList<WikiDescriptor>();
        }
        return descriptors;
    }

    public boolean wikiIdExists(String wikiId)
    {
        try {
            return wikiManager.wikiIdExists(wikiId);
        } catch (WikiManagerException e) {
            return false;
        }
    }

    public boolean isWikiIdAvailable(String wikiId)
    {
        try {
            return wikiManager.isWikiIdAvailable(wikiId);
        } catch (WikiManagerException e) {
            return false;
        }
    }

    /**
     * Log exception and store it in the context.
     *
     * @param errorMessage error message
     * @param e the caught exception
     * @see #CONTEXT_LASTEXCEPTION
     */
    private void error(String errorMessage, Exception e)
    {
        String errorMessageToLog = errorMessage;
        if (errorMessageToLog == null) {
            errorMessageToLog = e.getMessage();
        }

        /* Log exception. */
        logger.error(errorMessageToLog, e);

        /* Store exception in context. */
        this.execution.getContext().setProperty(CONTEXT_LASTEXCEPTION, e);
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
