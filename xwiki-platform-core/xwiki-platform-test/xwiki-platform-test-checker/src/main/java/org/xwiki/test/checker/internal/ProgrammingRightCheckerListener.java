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
import java.util.Deque;
import java.util.LinkedList;
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
    /**
     * The context key used to store the status of the PR check.
     */
    public static final String PRCHECK_KEY = "PRCheckDroppedPermissions";

    private Pattern excludePattern;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

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
        XWikiDocument contextDocument = (XWikiDocument) context.get("sdoc");
        if (contextDocument == null) {
            contextDocument = context.getDoc();
        }

        if (contextDocument != null) {
            DocumentReference currentDocReference = contextDocument.getDocumentReference();
            if (this.excludePattern != null && this.excludePattern.matcher(currentDocReference.toString()).matches()) {
                if (event instanceof ScriptEvaluatingEvent) {
                    this.logger.info("PRChecker: Skipping check for [{}] since it's excluded", currentDocReference);
                }

                return;
            }

            if (event instanceof ScriptEvaluatingEvent) {
                push(currentDocReference);
            } else {
                pop();
            }
        }
    }

    /**
     * @param context the XWikicontext
     * @param create true if a stack should be create and added to the context if none exist
     * @return the stack
     */
    public static Deque<DocumentReference> getStack(XWikiContext context, boolean create)
    {
        Deque<DocumentReference> deque = null;
        if (context != null) {
            deque = (Deque<DocumentReference>) context.get(PRCHECK_KEY);

            if (deque == null && create) {
                deque = new LinkedList<>();
                context.put(PRCHECK_KEY, deque);
            }
        }

        return deque;
    }

    private void push(DocumentReference document)
    {
        XWikiContext context = this.xwikiContextProvider.get();

        if (context != null) {
            getStack(context, true).push(document);
        }
    }

    private void pop()
    {
        Deque<DocumentReference> deque = getStack(this.xwikiContextProvider.get(), false);

        if (deque != null) {
            deque.pop();
        }
    }
}
