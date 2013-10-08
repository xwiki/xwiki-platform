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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.wikimanager.WikiManager;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerException;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;

@Component
@Named("wiki")
@Singleton
public class WikiManagerScriptService implements ScriptService
{
    /**
     * Field name of the last API exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    @Inject
    private WikiManager wikiManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

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

    public Wiki createWiki(String wikiId, String wikiAlias)
    {
        Wiki descriptor = null;

        XWikiContext context = xcontextProvider.get();

        try {
            // Check right access
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(),
                    new WikiReference(context.getMainXWiki()));

            // Create the wiki
            descriptor = wikiManager.create(wikiId, wikiAlias);

        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
        } catch (AccessDeniedException e) {
            error("You don't have the right to create wiki", e);
        }

        return descriptor;
    }

    public boolean deleteWiki(String wikiId)
    {
        XWikiContext context = xcontextProvider.get();
        WikiReference wikiReference = new WikiReference(wikiId);

        try {
            // Check right access
            authorizationManager.checkAccess(Right.ADMIN, context.getUserReference(), wikiReference);
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(), wikiReference);

            // Delete the wiki
            wikiManager.delete(wikiId);

        } catch (WikiManagerException e) {
            return false;
        } catch (AccessDeniedException e) {
            error("You don't have the right to delete the wiki", e);
        }

        return true;
    }

    public Wiki getByAlias(String wikiAlias)
    {
        Wiki descriptor = null;

        try {
            descriptor = wikiManager.getByAlias(wikiAlias);
        } catch (WikiManagerException e) {
        }

        return descriptor;
    }

    public Wiki getById(String wikiId)
    {
        Wiki descriptor = null;

        try {
            descriptor = wikiManager.getById(wikiId);
        } catch (WikiManagerException e) {
        }

        return descriptor;
    }

    public Collection<Wiki> getAll()
    {
        Collection<Wiki> wikis;
        try {
            wikis = wikiManager.getAll();
        } catch (WikiManagerException e) {
            wikis = new ArrayList<Wiki>();
        }
        return wikis;
    }

    public boolean exists(String wikiId)
    {
        try {
            return wikiManager.exists(wikiId);
        } catch (WikiManagerException e) {
            return false;
        }
    }

    public boolean idAvailable(String wikiId)
    {
        try {
            return wikiManager.idAvailable(wikiId);
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
}
