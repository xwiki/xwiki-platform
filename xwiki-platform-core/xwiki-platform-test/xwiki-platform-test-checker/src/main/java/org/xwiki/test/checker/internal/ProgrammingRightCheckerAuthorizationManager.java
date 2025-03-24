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
package org.xwiki.test.checker.internal;

import java.util.Collection;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.internal.BridgeAuthorizationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.template.InternalTemplateManager;

/**
 * Override {@link BridgeAuthorizationManager} to forbid programming right in wiki content.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class ProgrammingRightCheckerAuthorizationManager extends BridgeAuthorizationManager implements Initializable
{
    private static final LocalDocumentReference SUREFERENCE = new LocalDocumentReference("SUSpace", "SUPage");

    @Inject
    private Provider<XWikiContext> prxwikiContextProvider;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Inject
    private Logger prlogger;

    private Pattern excludePattern;

    @Override
    public void initialize() throws InitializationException
    {
        // There can be several properties named "test.prchecker.excludePattern" in the source (in the case of an
        // AllIT test that merges some IT tests for example). In this case, we get an ArrayList result and not a
        // String. We need to handle both cases.
        String regex;
        Object result = this.configurationSource.getProperty("test.prchecker.excludePattern");
        if (result instanceof Collection) {
            // Convert to a single regex string
            regex = StringUtils.join((Collection<String>) result, "|");
        } else {
            regex = (String) result;
        }
        if (regex != null) {
            this.excludePattern = Pattern.compile(regex);
        }
    }

    @Override
    public void checkAccess(Right right, DocumentReference userReference, EntityReference entityReference)
        throws AccessDeniedException
    {
        super.checkAccess(right, userReference, entityReference);

        if (!check(right, userReference)) {
            throw new AccessDeniedException(right, userReference, entityReference);
        }
    }

    @Override
    public boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference)
    {
        boolean hasAccess = super.hasAccess(right, userReference, entityReference);

        if (hasAccess && !check(right, userReference)) {
            return false;
        }

        return hasAccess;
    }

    private boolean check(Right right, DocumentReference userReference)
    {
        if (right == Right.PROGRAM) {
            XWikiContext xcontext = this.prxwikiContextProvider.get();

            if (xcontext != null) {
                // Get the current secure document
                XWikiDocument sdoc = (XWikiDocument) xcontext.get("sdoc");

                return check(userReference, sdoc);
            }
        }

        return true;
    }

    private boolean check(DocumentReference userReference, XWikiDocument sdoc)
    {
        // Try to make sure we are in a script associated to a wiki page: a secure document has been set and
        // it's not the author/secure document used for things authorized to have PR by definition (like
        // filesystem templates)
        if (sdoc != null && (!InternalTemplateManager.SUPERADMIN_REFERENCE.equals(userReference)
            || !SUREFERENCE.equals(sdoc.getDocumentReference().getLocalDocumentReference()))) {
            DocumentReference sref = sdoc.getDocumentReference();

            if (this.excludePattern != null && this.excludePattern.matcher(sref.toString()).matches()) {
                this.prlogger.info("PRChecker: Skipping check for [{}] since it's excluded", sref);

                return true;
            } else {
                this.prlogger.info("PRChecker: Block programming right for page [{}]", sref);

                return false;
            }
        }

        return true;
    }
}
