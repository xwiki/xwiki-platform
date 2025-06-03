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
package org.xwiki.platform.security.requiredrights.rest.internal;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.rest.RequiredRightsRestResource;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRightsAnalysisResult;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * Default implementation of the {@link RequiredRightsRestResource}.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Named("org.xwiki.platform.security.requiredrights.rest.internal.DefaultRequiredRightsRestResource")
public class DefaultRequiredRightsRestResource extends XWikiResource implements RequiredRightsRestResource
{
    @Inject
    @Named("withTranslations")
    private RequiredRightAnalyzer<DocumentReference> requiredRightAnalyzer;

    @Inject
    private RequiredRightsObjectConverter objectConverter;

    @Inject
    private DocumentRequiredRightsManager documentRequiredRightsManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private DocumentRequiredRightsUpdater documentRequiredRightsUpdater;

    @Override
    public DocumentRightsAnalysisResult analyze(String spaceNames, String page, String wiki) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wiki, spaceNames, page, null, null, true, false);

            DocumentReference documentReference = documentInfo.getDocument().getDocumentReference();
            DocumentRequiredRights currentRights =
                this.documentRequiredRightsManager.getRequiredRights(documentReference)
                    .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));

            List<RequiredRightAnalysisResult> analysisResults = this.requiredRightAnalyzer.analyze(documentReference);

            return this.objectConverter.toDocumentRightsAnalysisResult(currentRights, analysisResults,
                documentReference);
        } catch (XWikiException | AuthorizationException e) {
            throw new XWikiRestException("Failed loading document", e);
        } catch (RequiredRightsException e) {
            throw new XWikiRestException("Failed analyzing required rights", e);
        }
    }

    @Override
    public org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights updateRequiredRights(
        String spaceNames, String page, String wiki,
        org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights documentRequiredRights)
        throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wiki, spaceNames, page, null, null, true, false);

            Document doc = documentInfo.getDocument();

            if (!this.authorization.hasAccess(Right.EDIT, doc.getDocumentReference())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            this.documentRequiredRightsUpdater.updateRequiredRights(documentRequiredRights, doc);

            return this.objectConverter.convertDocumentRequiredRights(
                this.documentRequiredRightsManager.getRequiredRights(doc.getDocumentReference())
                    .orElseThrow(() -> new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR)));
        } catch (XWikiException | AuthorizationException e) {
            throw new XWikiRestException(e);
        }
    }

}
