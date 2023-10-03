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
package org.xwiki.platform.security.requiredrights;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.internal.XWikiDocumentRequiredRightAnalyzer;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Programming;

/**
 * Gives access to the analysis of the required rights for a document.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
@Named(SecurityScriptService.ROLEHINT + "." + RequiredRightsScriptService.ID)
@Unstable
public class RequiredRightsScriptService implements ScriptService
{
    /**
     * The id of this component.
     */
    public static final String ID = "requiredRights";

    @Inject
    @Named(XWikiDocumentRequiredRightAnalyzer.ID)
    private RequiredRightAnalyzer<XWikiDocument> analyzer;

    /**
     * Analyze if the given document's authors have all rights that are required by the document's content.
     *
     * @param document the document to analyze
     * @return the result of the analysis
     * @throws RequiredRightsException in case of error during the analysis
     */
    @Programming
    public List<RequiredRightAnalysisResult> analyze(Document document) throws RequiredRightsException
    {
        return this.analyzer.analyze(document.getDocument());
    }
}
