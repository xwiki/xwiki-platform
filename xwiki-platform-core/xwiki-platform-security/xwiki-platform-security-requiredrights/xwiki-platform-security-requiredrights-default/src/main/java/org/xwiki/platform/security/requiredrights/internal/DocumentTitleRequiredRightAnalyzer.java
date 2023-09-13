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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.event.Level;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Analyzer that checks if a document's title potentially contains a velocity script that is currently not executed.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named(DocumentTitleRequiredRightAnalyzer.ID)
public class DocumentTitleRequiredRightAnalyzer implements RequiredRightAnalyzer<String>
{
    /**
     * The id of this analyzer.
     */
    public static final String ID = "document/title";

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    @Named(StringVelocityRequiredRightAnalyzer.ID)
    private RequiredRightAnalyzer<String> stringVelocityRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(String title) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> result;

        if (this.contextualAuthorizationManager.hasAccess(Right.PROGRAM)) {
            result = List.of();
        } else {
            result = this.stringVelocityRequiredRightAnalyzer.analyze(title);
        }

        return result.stream()
            .map(r -> {
                    r.setAnalyzerHint(DocumentTitleRequiredRightAnalyzer.ID)
                        .setMessage("security.requiredrights.title");
                    if (this.contextualAuthorizationManager.hasAccess(Right.SCRIPT)) {
                        r.setLevel(Level.WARN);
                        r.setRequiredRight(Right.PROGRAM);
                    }
                    return r;
                }
            ).collect(Collectors.toList());
    }
}
