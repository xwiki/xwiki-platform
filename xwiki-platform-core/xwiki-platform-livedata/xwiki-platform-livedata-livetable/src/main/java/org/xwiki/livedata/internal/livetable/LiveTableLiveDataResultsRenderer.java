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
package org.xwiki.livedata.internal.livetable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.livetable.LiveDataLivetableException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Provide operations to render the results, either from a page or from a template.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@Component(roles = LiveTableLiveDataResultsRenderer.class)
@Singleton
public class LiveTableLiveDataResultsRenderer
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private LiveTableRequestHandler liveTableRequestHandler;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * Render a page with a context initialized from the query.
     *
     * @param page the page to render (e.g., XWiki.LiveTableResults)
     * @param query the live data query
     * @return the rendered page, expected to contain a json object with the results
     * @throws AccessDeniedException if the current user can't view the requested page
     * @throws RuntimeException in case of error when rendering the page, or if the requested page does not exist
     */
    public String getLiveTableResultsFromPage(String page, LiveDataQuery query) throws AccessDeniedException
    {
        DocumentReference documentReference = this.currentDocumentReferenceResolver.resolve(page);
        this.authorization.checkAccess(Right.VIEW, documentReference);

        return this.liveTableRequestHandler.getLiveTableResults(query, () -> {
            XWikiContext xcontext = this.xcontextProvider.get();
            WikiReference currentWiki = xcontext.getWikiReference();
            xcontext.setWikiReference(documentReference.getWikiReference());
            try {
                // The live table results page may use "global" variables.
                this.templateManager.render("xwikivars.vm");
            } catch (Exception e) {
                this.logger.warn(
                    "Failed to evaluate [xwikivars.vm] when getting the Livetable results from page [{}]. Cause: [{}].",
                    page, getRootCauseMessage(e));
            }

            try {
                XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
                if (document.isNew()) {
                    throw new LiveDataLivetableException(String.format("Page [%s] does not exist.", documentReference));
                }
                return document.getRenderedContent(Syntax.PLAIN_1_0, xcontext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                xcontext.setWikiReference(currentWiki);
            }
        });
    }

    /**
     * Render a template with a context initialized from the query.
     *
     * @param template the template to render (e.g., mytemplate.vm)
     * @param query the live data query
     * @return the rendered template, expected to contain a json object with the results
     * @throws RuntimeException in case of error when rendering the template, or if the requested template does not
     *     exist
     */
    public String getLiveTableResultsFromTemplate(String template, LiveDataQuery query)
    {
        return this.liveTableRequestHandler.getLiveTableResults(query, () -> {
            try {
                if (this.templateManager.getTemplate(template) == null) {
                    throw new LiveDataLivetableException(String.format("Template [%s] does not exist.", template));
                }
                return this.templateManager.render(template);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
