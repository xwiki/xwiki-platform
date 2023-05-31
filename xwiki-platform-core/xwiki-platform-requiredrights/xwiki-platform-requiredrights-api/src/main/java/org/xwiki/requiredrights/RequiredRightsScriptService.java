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
package org.xwiki.requiredrights;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

import static org.xwiki.security.authorization.internal.RequiredRightsSkipContext.SKIP_REQUIRED_RIGHT;

/**
 * Provides script operations related to required rights. For instance, updating a document required rights according to
 * the current request parameters.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named("requiredrights")
@Unstable
public class RequiredRightsScriptService implements ScriptService
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private Execution execution;

    /**
     * Initializes the required rights of the given document based on the request parameters.
     *
     * @param document the document to update
     */
    public void loadFromRequest(Document document)
    {
        XWikiRequest request = this.contextProvider.get().getRequest();

        XWikiDocument doc = document.getDocument();
        String activateRequiredRights = request.getParameter("activateRequiredRights");
        if (activateRequiredRights != null) {
            doc.setRequiredRightsActivated(Objects.equals(activateRequiredRights, "1"));
        }

        if (request.getParameterMap() != null && request.getParameterMap().containsKey("updateRequiredRights")
            && doc.getRequiredRights().activated())
        {
            ExecutionContext context = new ExecutionContext();
            this.execution.pushContext(context, true);
            context.setProperty(SKIP_REQUIRED_RIGHT, String.valueOf(Boolean.TRUE));
            try {
                doc.getRequiredRights().setRights(Arrays.stream(request.getParameterValues("requiredRights"))
                    .filter(StringUtils::isNotBlank)
                    .map(Right::toRight)
                    .filter(this.contextualAuthorizationManager::hasAccess)
                    .collect(Collectors.toSet()));
            } finally {
                this.execution.popContext();
            }
        }
    }
}
