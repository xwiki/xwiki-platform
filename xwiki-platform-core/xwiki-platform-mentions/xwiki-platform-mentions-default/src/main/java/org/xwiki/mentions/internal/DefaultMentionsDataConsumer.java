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
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.MentionNotificationService;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;

import static com.xpn.xwiki.doc.XWikiDocument.COMMENTSCLASS_REFERENCE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.AWM_FIELD;
import static org.xwiki.mentions.MentionLocation.COMMENT;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;

/**
 * Default implementation of {@link MentionsDataConsumer}.
 *
 * This class is responsible to analyze documents update in order to identify new user mentions.
 * Notifications are then produced for each newly introduced user mentions.
 * This analysis is done by identifying mentions macro with new identifiers in document content.
 * In other word, in the document body as well as in the values of {@link LargeStringProperty} of the xObject objects
 * attached to the document.
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
    private Logger logger;

    @Inject
    private MentionNotificationService notificationService;

    @Inject
    private MentionXDOMService xdomService;

    /**
     * Initialize the context.
     *
     * @param authorReference the author of the analyzed document
     * @param wikiId the wiki id
     * @throws ExecutionContextException in case one {@link ExecutionContextInitializer} fails to execute 
     *
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
            XWikiDocument doc = this.documentRevisionProvider.getRevision(dr, data.getVersion());
            if (doc != null) {
                Syntax syntax = doc.getSyntax();
                DocumentReference documentReference = doc.getDocumentReference();
                DocumentReference authorReference = doc.getAuthorReference();

                if (doc.getPreviousVersion() == null) {
                    // CREATE
                    handleOnCreate(doc.getXDOM(), documentReference, authorReference, DOCUMENT);
                    traverseXObjectsOnCreate(doc.getXObjects(), documentReference, authorReference, syntax);
                } else {
                    // UPDATE
                    XWikiDocument oldDoc = this.documentRevisionProvider.getRevision(dr, doc.getPreviousVersion());
                    handleOnUpdate(oldDoc.getXDOM(), doc.getXDOM(), documentReference, authorReference, DOCUMENT);
                    traverseXObjectsOnUpdate(oldDoc.getXObjects(), doc.getXObjects(), documentReference,
                        authorReference, syntax);
                }
            }
        } catch (ExecutionContextException e) {
            this.logger.warn("Failed to initalize the context of the mention update runnable. Cause [{}]",
                getRootCauseMessage(e));
        } finally {
            this.execution.removeContext();
        }
    }

    private void traverseXObjectsOnCreate(Map<DocumentReference, List<BaseObject>> xObjects,
        DocumentReference documentReference, DocumentReference authorReference, Syntax syntax)
    {
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : xObjects.entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                if (baseObject != null) {
                    for (Object o : baseObject.getProperties()) {
                        if (o instanceof LargeStringProperty) {
                            String content = ((LargeStringProperty) o).getValue();
                            this.xdomService
                                .parse(content, syntax)
                                .ifPresent(xdom -> handleOnCreate(xdom, documentReference, authorReference, AWM_FIELD));
                        }
                    }
                }
            }
        }
    }

    private void traverseXObjectsOnUpdate(Map<DocumentReference, List<BaseObject>> oldXObjects,
        Map<DocumentReference, List<BaseObject>> xObjects, DocumentReference documentReference,
        DocumentReference authorReference, Syntax syntax)
    {
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : xObjects.entrySet()) {
            List<BaseObject> oldEntry = oldXObjects.get(entry.getKey());
            for (BaseObject baseObject : entry.getValue()) {
                if (baseObject != null) {
                    handleBaseObject(oldEntry, baseObject, documentReference, authorReference, syntax);
                }
            }
        }
    }

    private void handleOnCreate(XDOM xdom, DocumentReference documentReference, DocumentReference authorReference,
        MentionLocation location)
    {
        List<MacroBlock> blocks = this.xdomService.listMentionMacros(xdom);

        Map<DocumentReference, List<String>> counts =
            this.xdomService.countByIdentifier(blocks);

        for (Map.Entry<DocumentReference, List<String>> entry : counts.entrySet()) {
            boolean emptyAnchorProcessed = false;
            for (String anchorId : entry.getValue()) {
                if (!StringUtils.isEmpty(anchorId) || !emptyAnchorProcessed) {
                    sendNotif(
                        new MentionNotificationParameters(authorReference, documentReference, entry.getKey(), location,
                            anchorId, xdom));
                    emptyAnchorProcessed = emptyAnchorProcessed || StringUtils.isEmpty(anchorId);
                }
            }
        }
    }

    private void handleOnUpdate(XDOM oldXDOM, XDOM newXDOM, DocumentReference documentReference,
        DocumentReference authorReference, MentionLocation location)
    {
        List<MacroBlock> oldMentions = this.xdomService.listMentionMacros(oldXDOM);
        List<MacroBlock> newMentions = this.xdomService.listMentionMacros(newXDOM);

        Map<DocumentReference, List<String>> oldCounts = this.xdomService.countByIdentifier(oldMentions);
        Map<DocumentReference, List<String>> newCounts = this.xdomService.countByIdentifier(newMentions);

        for (Map.Entry<DocumentReference, List<String>> entry : newCounts.entrySet()) {
            DocumentReference key = entry.getKey();
            List<String> newAnchorIds = entry.getValue();
            List<String> oldAnchorsIds = oldCounts.getOrDefault(key, emptyList());

            // Compute if there's new mentions without an anchor
            long newEmptyAnchorsNumber = newAnchorIds.stream().filter(org.xwiki.text.StringUtils::isEmpty).count();
            long oldEmptyAnchorsNumber = oldAnchorsIds.stream().filter(org.xwiki.text.StringUtils::isEmpty).count();

            // Retrieve new mentions with a new anchor
            List<String> anchorsToNotify = newAnchorIds.stream().filter(
                value -> !org.xwiki.text.StringUtils.isEmpty(value) && !oldAnchorsIds.contains(value))
                                               .collect(toList());

            // Notify with an empty anchorId if there's new mentions without an anchor.
            if (newEmptyAnchorsNumber > oldEmptyAnchorsNumber) {
                sendNotif(new MentionNotificationParameters(authorReference, documentReference, key, location, "",
                    newXDOM));
            }

            // Notify all new mentions with new anchors.
            for (String anchorId : anchorsToNotify) {
                sendNotif(
                    new MentionNotificationParameters(authorReference, documentReference, key, location, anchorId,
                        newXDOM));
            }
        }
    }

    private void handleMissing(XDOM newXdom, DocumentReference documentReference, DocumentReference authorReference,
        MentionLocation location)
    {
        List<MacroBlock> newMentions = this.xdomService.listMentionMacros(newXdom);

        // the matching element has not be found in the previous version of the document
        // notification are send unconditionally to all mentioned users.
        this.xdomService
            .countByIdentifier(newMentions)
            .forEach((key, value) -> value.forEach(
                anchorId -> sendNotif(
                    new MentionNotificationParameters(authorReference, documentReference, key, location, anchorId,
                        newXdom))));
    }

    private void handleBaseObject(List<BaseObject> oldEntry, BaseObject baseObject, DocumentReference documentReference,
        DocumentReference authorReference, Syntax syntax)
    {
        Optional<BaseObject> oldBaseObject = ofNullable(oldEntry).flatMap(
            optOldEntries -> optOldEntries
                                 .stream()
                                 .filter(Objects::nonNull)
                                 .filter(it -> it.getId() == baseObject.getId())
                                 .findAny());
        if (baseObject != null) {
            // special treatment on comment object to analyse only the comment field.
            if (Objects.equals(baseObject
                                   .getXClassReference()
                                   .getLocalDocumentReference(), COMMENTSCLASS_REFERENCE))
            {
                Optional.<Object>ofNullable(baseObject.getField("comment"))
                    .ifPresent(it -> {
                        LargeStringProperty lsp = (LargeStringProperty) it;
                        PropertyInterface field = lsp.getObject().getField(SELECTION_FIELD);
                        boolean isComment = field == null || StringUtils.isEmpty(field.toFormString());
                        MentionLocation location = isComment ? COMMENT : ANNOTATION;
                        handleField(oldBaseObject, lsp, location, documentReference, authorReference, syntax);
                    });
            } else {
                for (Object o : baseObject.getProperties()) {
                    if (o instanceof LargeStringProperty) {
                        handleField(oldBaseObject, (LargeStringProperty) o, AWM_FIELD, documentReference,
                            authorReference, syntax);
                    }
                }
            }
        }
    }

    private void handleField(Optional<BaseObject> oldBaseObject, LargeStringProperty lsp, MentionLocation location,
        DocumentReference documentReference, DocumentReference authorReference, Syntax syntax)
    {
        Optional<XDOM> oldDom = oldBaseObject.flatMap(it -> ofNullable(it.getField(lsp.getName())))
                                    .filter(it -> it instanceof LargeStringProperty)
                                    .flatMap(
                                        it -> this.xdomService.parse(((LargeStringProperty) it).getValue(), syntax));
        this.xdomService.parse(lsp.getValue(), syntax).ifPresent(xdom -> {
            // can be replaced by ifPresentOrElse for in java 9+ 
            oldDom.ifPresent(od -> handleOnUpdate(od, xdom, documentReference, authorReference, location));
            if (!oldDom.isPresent()) {
                handleMissing(xdom, documentReference, authorReference, location);
            }
        });
    }

    private void sendNotif(MentionNotificationParameters notificationParameters)
    {
        this.notificationService.sendNotification(notificationParameters);
    }
}
