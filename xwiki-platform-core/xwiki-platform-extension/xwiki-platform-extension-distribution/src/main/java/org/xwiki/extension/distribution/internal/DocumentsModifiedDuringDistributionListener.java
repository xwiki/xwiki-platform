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
package org.xwiki.extension.distribution.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionContext;
import org.xwiki.extension.ExtensionSession;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.distribution.internal.DocumentsModifiedDuringDistributionListener.DocumentStatus.Action;
import org.xwiki.extension.xar.internal.handler.XarExtensionPlan;
import org.xwiki.job.JobContext;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Gather the pages modified during the Distribution Wizard execution.
 * 
 * @version $Id$
 * @since 5.4RC1
 */
@Component
@Named(DocumentsModifiedDuringDistributionListener.NAME)
@Singleton
// TODO: replace this by the concatenation and analysis of the distribution log
public class DocumentsModifiedDuringDistributionListener extends AbstractEventListener
{
    public static final String NAME = "distribution.DocumentsModifiedDuringDistributionListener";

    @Inject
    private ExtensionContext extensionContext;

    @Inject
    private JobContext jobContext;

    /**
     * Map<wiki, Map<document, extension>>.
     */
    private Map<String, Map<DocumentReference, DocumentStatus>> documents =
        new HashMap<String, Map<DocumentReference, DocumentStatus>>();

    public static class DocumentStatus
    {
        public enum Action
        {
            CREATED,
            DELETED,
            MODIFIED
        }

        private final DocumentReference reference;

        private final String previousVersion;

        private final Action action;

        private final Extension previousExtension;

        private final Extension nextExtension;

        public DocumentStatus(DocumentReference reference, String version, Action action)
        {
            this(reference, version, action, null, null);
        }

        public DocumentStatus(DocumentReference reference, String previousVersion, Action action,
            Extension previousExtension, Extension nextExtension)
        {
            this.reference = reference;
            this.previousVersion = previousVersion;
            this.action = action;
            this.previousExtension = previousExtension;
            this.nextExtension = nextExtension;
        }

        public DocumentReference getReference()
        {
            return this.reference;
        }

        public String getPreviousVersion()
        {
            return this.previousVersion;
        }

        public Action getAction()
        {
            return this.action;
        }

        public Extension getNextExtension()
        {
            return this.nextExtension;
        }

        public Extension getPreviousExtension()
        {
            return this.previousExtension;
        }
    }

    /**
     * Setup event listener.
     */
    public DocumentsModifiedDuringDistributionListener()
    {
        super("DocumentsModifiedDuringDistributionListener", new DocumentCreatedEvent(), new DocumentDeletedEvent(),
            new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        checkXARHandler(event, (XWikiDocument) source);
        checkDistributionAction(event, (XWikiDocument) source, (XWikiContext) data);
    }

    private static Action toAction(Event event)
    {
        DocumentStatus.Action action;
        if (event instanceof DocumentCreatedEvent) {
            action = Action.CREATED;
        } else if (event instanceof DocumentDeletedEvent) {
            action = Action.DELETED;
        } else {
            action = Action.MODIFIED;
        }

        return action;
    }

    private void checkXARHandler(Event event, XWikiDocument document)
    {
        Optional<ExtensionSession> extensionSession = this.extensionContext.getExtensionSession();

        if (extensionSession.isPresent()) {
            XarExtensionPlan xarExtensionPlan = extensionSession.get().get(XarExtensionPlan.SESSIONTKEY_XARINSTALLPLAN);

            if (xarExtensionPlan != null) {
                Request request = this.jobContext.getCurrentJob().getRequest();

                // It's a job started by the Distribution Wizard
                if (StringUtils.equals(request.<String>getProperty("context.action"), "distribution")) {
                    String distributionWiki = request.getProperty("context.wiki");

                    if (distributionWiki != null) {
                        DocumentReference reference = document.getDocumentReferenceWithLocale();

                        DocumentStatus.Action action = toAction(event);
                        LocalExtension previousExtension = xarExtensionPlan.getPreviousXarExtension(reference);
                        LocalExtension nextExtension = xarExtensionPlan.getNextXarExtension(reference);

                        addDocument(distributionWiki, document, action, previousExtension, nextExtension);
                    }
                }
            }
        }
    }

    private void checkDistributionAction(Event event, XWikiDocument document, XWikiContext xcontext)
    {
        if (DistributionAction.DISTRIBUTION_ACTION.equals(xcontext.getAction())) {
            String distributionWiki = xcontext.getOriginalWikiId();

            DocumentStatus.Action action = toAction(event);

            addDocument(distributionWiki, document, action, null, null);
        }
    }

    private void addDocument(String distributionWiki, XWikiDocument document, Action action,
        LocalExtension previousExtension, LocalExtension nextExtension)
    {
        Map<DocumentReference, DocumentStatus> wikiDocuments = this.documents.get(distributionWiki);

        if (wikiDocuments == null) {
            wikiDocuments =
                new HashMap<DocumentReference, DocumentsModifiedDuringDistributionListener.DocumentStatus>();
            this.documents.put(distributionWiki, wikiDocuments);
        }

        DocumentReference reference = document.getDocumentReferenceWithLocale();

        DocumentStatus currentStatus = wikiDocuments.get(reference);

        String previousVersion;
        if (currentStatus != null) {
            previousVersion = currentStatus.getPreviousVersion();

            if (action == Action.CREATED) {
                if (previousVersion != null) {
                    action = Action.MODIFIED;
                }
            } else if (action == Action.DELETED) {
                if (previousVersion == null) {
                    // Back to square one
                    wikiDocuments.remove(reference);

                    return;
                }
            } else if (action == Action.MODIFIED) {
                action = currentStatus.getAction();
            }
        } else {
            if (action != Action.CREATED) {
                previousVersion = document.getOriginalDocument().getVersion();
            } else {
                previousVersion = null;
            }
        }

        wikiDocuments.put(reference,
            new DocumentStatus(reference, previousVersion, action, previousExtension, nextExtension));
    }

    public Map<String, Map<DocumentReference, DocumentStatus>> getDocuments()
    {
        return this.documents;
    }
}
