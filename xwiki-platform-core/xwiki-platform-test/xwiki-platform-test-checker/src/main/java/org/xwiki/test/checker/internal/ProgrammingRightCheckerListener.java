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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.script.event.ScriptEvaluatedEvent;
import org.xwiki.script.event.ScriptEvaluatingEvent;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Drop Programming Rights (PR) for all scripts in wiki pages so that functional tests can discover pages that require
 * PR and shouldn't. Also note that it's possible to exclude some pages from being tested by setting an XWiki property
 * named {@code test.prchecker.excludePattern} (e.g. {@code .*:XWiki\.DeletedDocuments}) in xwiki.properties.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("PRChecker")
@Singleton
public class ProgrammingRightCheckerListener implements EventListener, Initializable
{
    private static final String PRCHECK_KEY = "PRCheckDroppedPermissions";

    private Pattern excludePattern;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Override
    public void initialize() throws InitializationException
    {
        String regex = this.configurationSource.getProperty("test.prchecker.excludePattern");
        if (regex != null) {
            this.excludePattern = Pattern.compile(regex);
        }
    }

    @Override
    public String getName()
    {
        return "PRChecker";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.asList(new ScriptEvaluatingEvent(), new ScriptEvaluatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = this.xwikiContextProvider.get();

        // Should the current document be excluded from the PR check?
        XWikiDocument contextDocument = context.getDoc();
        DocumentReference currentDocReference = contextDocument.getDocumentReference();
        if (this.excludePattern != null && this.excludePattern.matcher(currentDocReference.toString()).matches()) {
            this.logger.info("PRChecker: Skipping check for [{}] since it's excluded", currentDocReference);
            return;
        }

        if (event instanceof ScriptEvaluatingEvent) {

            // Make it simpler to debug why a test will fail with the PR checker active by logging that we dropped
            // permissions.
            boolean logPrinted = false;
            boolean hasPR = this.contextualAuthorizationManager.hasAccess(Right.PROGRAM, currentDocReference);
            if (hasPR) {
                // Ideally we would print this in info mode but since right now all pages have PR by default, this is
                // just swamping the logs...
                this.logger.debug("PRChecker: Dropping permissions for page [{}], which had PR",
                    currentDocReference);
                logPrinted = true;
            }

            // Save the original value
            Boolean originalValue = (Boolean) context.get(XWikiConstant.DROPPED_PERMISSIONS);
            if (originalValue != null) {
                context.put(PRCHECK_KEY, originalValue);
            }

            if (!logPrinted) {
                this.logger.debug("PRChecker: Dropping permissions for page [{}]", currentDocReference);
            }
            context.dropPermissions();
        } else {
            // Restore the original value
            Boolean originalValue = (Boolean) context.get(PRCHECK_KEY);
            this.logger.debug("PRChecker: Restoring permissions for page [{}]", currentDocReference);
            if (originalValue != null) {
                context.put(XWikiConstant.DROPPED_PERMISSIONS, originalValue);
            } else {
                context.remove(XWikiConstant.DROPPED_PERMISSIONS);
            }
            context.remove(PRCHECK_KEY);
        }
    }
}
