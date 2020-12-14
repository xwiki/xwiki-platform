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
package org.xwiki.mentions.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.mentions.events.NewMentionsEvent;
import org.xwiki.mentions.internal.analyzer.CreatedDocumentMentionsAnalyzer;
import org.xwiki.mentions.internal.analyzer.UpdatedDocumentMentionsAnalyzer;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.LargeStringProperty;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Default implementation of {@link MentionsDataConsumer}.
 * <p>
 * This class is responsible to analyze document updates in order to identify new user mentions. {@link
 * NewMentionsEvent} are then sent for each newly introduced user mentions. This analysis is done by identifying
 * mentions macro with new identifiers in the content of entities of the updated document. In other word, in the
 * document body as well as in the values of {@link LargeStringProperty} of the xObject objects attached to the
 * document.
 *
 * @version $Id$
 * @since 12.6
 */
@Component
@Singleton
public class DefaultMentionsDataConsumer implements MentionsDataConsumer
{
    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ExecutionContextManager contextManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Execution execution;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private CreatedDocumentMentionsAnalyzer createdDocumentMentionsAnalyzer;

    @Inject
    private UpdatedDocumentMentionsAnalyzer updatedDocumentMentionsAnalyzer;

    @Inject
    private Logger logger;

    /**
     * Initialize the context.
     *
     * @param authorReference the author of the analyzed document
     * @param wikiId the wiki id
     * @throws ExecutionContextException in case one {@link ExecutionContextInitializer} fails to execute
     */
    private void initContext(DocumentReference authorReference, String wikiId) throws ExecutionContextException
    {
        ExecutionContext context = new ExecutionContext();
        this.contextManager.initialize(context);

        XWikiContext xWikiContext = this.xcontextProvider.get();
        xWikiContext.setUserReference(authorReference);
        xWikiContext.setWikiReference(authorReference.getWikiReference());
        xWikiContext.setWikiId(wikiId);
    }

    @Override
    public void consume(MentionsData data) throws XWikiException
    {
        try {
            DocumentReference author = this.documentReferenceResolver.resolve(data.getAuthorReference());
            this.initContext(author, data.getWikiId());
            DocumentReference dr = this.documentReferenceResolver.resolve(data.getDocumentReference());
            String version = data.getVersion();
            XWikiDocument doc = this.documentRevisionProvider.getRevision(dr, version);
            if (doc != null) {
                // Stores the list of mentions found in the document and its attached objects.
                List<MentionNotificationParameters> mentionNotificationParameters;
                DocumentReference documentReference = doc.getDocumentReference();

                if (doc.getPreviousVersion() == null) {
                    // CREATE
                    mentionNotificationParameters = this.createdDocumentMentionsAnalyzer
                        .analyze(doc, documentReference, version, data.getAuthorReference());
                } else {
                    // UPDATE
                    XWikiDocument oldDoc = this.documentRevisionProvider.getRevision(dr, doc.getPreviousVersion());
                    mentionNotificationParameters = this.updatedDocumentMentionsAnalyzer
                        .analyze(oldDoc, doc, documentReference, version, data.getAuthorReference());
                }
                sendNotification(mentionNotificationParameters);
            }
        } catch (ExecutionContextException e) {
            this.logger.warn("Failed to initialize the context of the mention update runnable. Cause [{}]",
                getRootCauseMessage(e));
        } finally {
            this.execution.removeContext();
        }
    }

    private void sendNotification(List<MentionNotificationParameters> notificationParametersList)
    {
        // Notify the listeners of the exhaustive list of identified mentions.
        // The listeners are in charge of selecting the ones of interest for them.
        for (MentionNotificationParameters mentionNotificationParameters : notificationParametersList) {
            Map<String, Set<MentionNotificationParameter>> newMentions = mentionNotificationParameters.getNewMentions();
            if (!newMentions.isEmpty()) {
                this.observationManager
                    .notify(new NewMentionsEvent(), mentionNotificationParameters.getAuthorReference(),
                        mentionNotificationParameters);
            }
        }
    }
}
