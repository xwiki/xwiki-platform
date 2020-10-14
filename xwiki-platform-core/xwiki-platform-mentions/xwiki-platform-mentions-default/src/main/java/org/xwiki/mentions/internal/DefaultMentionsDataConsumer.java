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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
import org.xwiki.mentions.events.NewMentionsEvent;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
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
import static org.xwiki.text.StringUtils.isEmpty;

/**
 * Default implementation of {@link MentionsDataConsumer}.
 * <p>
 * This class is responsible to analyze document updates in order to identify new user mentions.
 * {@link NewMentionsEvent} are then sent for each newly introduced user mentions.
 * This analysis is done by identifying mentions macro with new identifiers in the content of entities of the updated
 * document.
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
    private MentionXDOMService xdomService;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

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
            String version = data.getVersion();
            XWikiDocument doc = this.documentRevisionProvider.getRevision(dr, version);
            if (doc != null) {
                Syntax syntax = doc.getSyntax();
                // Stores the list of mentions found in the document and its attached objects.
                List<MentionNotificationParameters> mentionNotificationParameters = new ArrayList<>();
                DocumentReference documentReference = doc.getDocumentReference();
                String authorReference = this.entityReferenceSerializer.serialize(doc.getAuthorReference());

                if (doc.getPreviousVersion() == null) {
                    // CREATE
                    handleContentOnCreate(doc.getXDOM(), documentReference, version, authorReference, DOCUMENT,
                        mentionNotificationParameters);
                    traverseXObjectsOnCreate(doc.getXObjects(), version, authorReference, syntax,
                        mentionNotificationParameters);
                } else {
                    // UPDATE
                    XWikiDocument oldDoc = this.documentRevisionProvider.getRevision(dr, doc.getPreviousVersion());
                    handleUpdatedContent(oldDoc.getXDOM(), doc.getXDOM(), documentReference, version, authorReference,
                        DOCUMENT, mentionNotificationParameters);
                    traverseXObjectsOnUpdate(oldDoc.getXObjects(), doc.getXObjects(),
                        version, authorReference, syntax, mentionNotificationParameters);
                }
                sendNotification(mentionNotificationParameters);
            }
        } catch (ExecutionContextException e) {
            this.logger.warn("Failed to initalize the context of the mention update runnable. Cause [{}]",
                getRootCauseMessage(e));
        } finally {
            this.execution.removeContext();
        }
    }

    /**
     * Traverses the objects of a created document and search for mentions to notify.
     * @param xObjects the objects of the document
     * @param version the version of the document holding the analyzed xobjects when it was created
     * @param authorReference the reference of the author of the document
     * @param syntax the syntax of the document
     * @param mentionNotificationParametersList hold the list of identified mentions
     */
    private void traverseXObjectsOnCreate(Map<DocumentReference, List<BaseObject>> xObjects, String version,
        String authorReference, Syntax syntax, List<MentionNotificationParameters> mentionNotificationParametersList)
    {
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : xObjects.entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                if (baseObject != null) {
                    handleBaseObjectOnCreate(baseObject, version, authorReference, syntax,
                        mentionNotificationParametersList);
                }
            }
        }
    }

    /**
     * Handles an object of a created document and search for mentions to notify its content.
     * @param baseObject the object
     * @param version the version of the document holding the base object when it was created
     * @param authorReference the reference of the author of the document
     * @param syntax the syntax of the document
     * @param mentionNotificationParameters hold the list of identified mentions
     */
    private void handleBaseObjectOnCreate(BaseObject baseObject, String version, String authorReference, Syntax syntax,
        List<MentionNotificationParameters> mentionNotificationParameters)
    {
        for (Object o : baseObject.getProperties()) {
            if (o instanceof LargeStringProperty) {
                LargeStringProperty largeStringProperty = (LargeStringProperty) o;
                String content = largeStringProperty.getValue();
                this.xdomService
                    .parse(content, syntax)
                    .ifPresent(xdom -> handleContentOnCreate(xdom, largeStringProperty.getReference(), version,
                        authorReference,
                        AWM_FIELD, mentionNotificationParameters));
            }
        }
    }

    /**
     * Traverses the objects of an updated document and compares them to the objects of the document before the update
     * to search for new mentions to notify.
     *
     * @param oldXObjects the objects of the document before the update
     * @param xObjects the objects of the document after the update
     * @param version the version of the document holding the analyzed xobjects when it was updated
     * @param authorReference the reference of the author of the update
     * @param syntax the syntax of the document
     * @param mentionNotificationParameters hold the list of identified mentions
     */
    private void traverseXObjectsOnUpdate(Map<DocumentReference, List<BaseObject>> oldXObjects,
        Map<DocumentReference, List<BaseObject>> xObjects, String version, String authorReference, Syntax syntax,
        List<MentionNotificationParameters> mentionNotificationParameters)
    {
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : xObjects.entrySet()) {
            List<BaseObject> oldEntry = oldXObjects.get(entry.getKey());
            for (BaseObject baseObject : entry.getValue()) {
                if (baseObject != null) {
                    handleBaseObjectOnUpdate(oldEntry, baseObject, version, authorReference, syntax,
                        mentionNotificationParameters);
                }
            }
        }
    }

    /**
     * Handles the analysis of a created content to search for mentions to notify.
     * @param xdom the xdom of the content
     * @param entityReference the reference to the analyzed entity
     * @param version the version of the document holding the analyzed entity when it was created
     * @param authorReference the reference of the author of the created document
     * @param location the location of the content
     * @param mentionNotificationParametersList hold the list of identified mentions
     */
    private void handleContentOnCreate(XDOM xdom, EntityReference entityReference, String version,
        String authorReference, MentionLocation location,
        List<MentionNotificationParameters> mentionNotificationParametersList)
    {
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(authorReference, entityReference, location, version);
        mentionNotificationParametersList.add(mentionNotificationParameters);
        List<MacroBlock> blocks = this.xdomService.listMentionMacros(xdom);

        Map<MentionedActorReference, List<String>> counts =
            this.xdomService.groupAnchorsByUserReference(blocks);

        for (Map.Entry<MentionedActorReference, List<String>> entry : counts.entrySet()) {
            boolean emptyAnchorProcessed = false;
            String type = entry.getKey().getType();
            String reference = entry.getKey().getReference();
            for (String anchorId : entry.getValue()) {
                if (!StringUtils.isEmpty(anchorId) || !emptyAnchorProcessed) {
                    addNewMention(mentionNotificationParameters, type,
                        new MentionNotificationParameter(reference, anchorId));
                    emptyAnchorProcessed = emptyAnchorProcessed || StringUtils.isEmpty(anchorId);
                }
            }
        }
    }

    /**
     * Handles the analysis of an updated content to search for new mentions to notify.
     *
     * @param oldXDOM the old xdom of the content before the update
     * @param newXDOM the new xom of the content after the update
     * @param entityReference the reference to the analyzed entity
     * @param version the version of the document holding the analyzed entity  when it was updated
     * @param authorReference the reference of the author of the update
     * @param location the location of the content
     * @param mentionNotificationParametersList hold the list of identified mentions
     */
    private void handleUpdatedContent(XDOM oldXDOM, XDOM newXDOM, EntityReference entityReference,
        String version, String authorReference, MentionLocation location,
        List<MentionNotificationParameters> mentionNotificationParametersList)
    {
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(authorReference, entityReference, location, version);
        mentionNotificationParametersList.add(mentionNotificationParameters);

        List<MacroBlock> oldMentions = this.xdomService.listMentionMacros(oldXDOM);
        List<MacroBlock> newMentions = this.xdomService.listMentionMacros(newXDOM);

        Map<MentionedActorReference, List<String>> oldCounts =
            this.xdomService.groupAnchorsByUserReference(oldMentions);
        Map<MentionedActorReference, List<String>> newCounts =
            this.xdomService.groupAnchorsByUserReference(newMentions);

        for (Map.Entry<MentionedActorReference, List<String>> e : newCounts.entrySet()) {
            MentionedActorReference mentionReference = e.getKey();
            for (String anchorId : e.getValue()) {
                addMention(mentionNotificationParameters, mentionReference.getType(),
                    new MentionNotificationParameter(mentionReference.getReference(),
                        anchorId));
            }
        }

        for (Map.Entry<MentionedActorReference, List<String>> entry : newCounts.entrySet()) {
            MentionedActorReference key = entry.getKey();
            String type = key.getType();
            String reference = key.getReference();
            List<String> newAnchorIds = entry.getValue();
            List<String> oldAnchorsIds = oldCounts.getOrDefault(key, emptyList());

            // Retrieve new mentions with a new anchor
            List<String> anchorsToNotify = newAnchorIds.stream().filter(
                value -> !isEmpty(value) && !oldAnchorsIds.contains(value))
                .collect(toList());

            // Compute if there's new mentions without an anchor
            long newEmptyAnchorsNumber = newAnchorIds.stream().filter(StringUtils::isEmpty).count();
            long oldEmptyAnchorsNumber = oldAnchorsIds.stream().filter(StringUtils::isEmpty).count();

            // Notify with an empty anchorId if there's new mentions without an anchor.
            if (newEmptyAnchorsNumber > oldEmptyAnchorsNumber) {
                addNewMention(mentionNotificationParameters, null,
                    new MentionNotificationParameter(reference, ""));
            }

            // Notify all new mentions with new anchors.
            for (String anchor : anchorsToNotify) {
                addNewMention(mentionNotificationParameters, type,
                    new MentionNotificationParameter(reference, anchor));
            }
        }
    }

    /**
     * Handles the analysis of the mentions of a created content to search for mentions to notify.
     * @param newXdom the xdom of the created content
     * @param entityReference the reference of the analyzed entity
     * @param version the version of the document holding the analyzed entity when it was updated
     * @param authorReference the reference of the author of the change
     * @param location the location of the content
     * @param mentionNotificationParametersList hold the list of identified mentions
     */
    private void handleCreatedContent(XDOM newXdom, EntityReference entityReference, String version,
        String authorReference, MentionLocation location,
        List<MentionNotificationParameters> mentionNotificationParametersList)
    {
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(authorReference, entityReference, location, version);
        mentionNotificationParametersList.add(mentionNotificationParameters);

        List<MacroBlock> newMentions = this.xdomService.listMentionMacros(newXdom);

        // the matching element has not be found in the previous version of the document
        // notification are send unconditionally to all mentioned users.
        this.xdomService.groupAnchorsByUserReference(newMentions)
            .forEach((key, value) -> value.forEach(
                anchorId -> addNewMention(mentionNotificationParameters, key.getType(),
                    new MentionNotificationParameter(key.getReference(), anchorId))));
    }

    /**
     * Handles a base object during the update of a document to search for new mentions to notify.
     * @param oldEntry the old base object (if it exists)
     * @param baseObject the new base object
     * @param version the version of the document holding the analyzed object when it was updated
     * @param authorReference the reference of the author of the change
     * @param syntax the syntax of the document
     * @param mentionNotificationParameters hold the list of identified mentions
     */
    private void handleBaseObjectOnUpdate(List<BaseObject> oldEntry, BaseObject baseObject, String version,
        String authorReference, Syntax syntax, List<MentionNotificationParameters> mentionNotificationParameters)
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
                        handleProperty(oldBaseObject, lsp, version, location, authorReference, syntax,
                            mentionNotificationParameters);
                    });
            } else {
                for (Object o : baseObject.getProperties()) {
                    if (o instanceof LargeStringProperty) {
                        handleProperty(oldBaseObject, (LargeStringProperty) o, version, AWM_FIELD,
                            authorReference, syntax, mentionNotificationParameters);
                    }
                }
            }
        }
    }

    /**
     * Handle a property of an object to search for new mentions to notify.
     * @param oldBaseObject the old base object (if it exists).
     * @param largeStringProperty the large string property
     * @param version the version of the document holding the analysed property when it was updated
     * @param location the location of the property
     * @param authorReference the reference of the author change
     * @param syntax the syntax of the document
     * @param mentionNotificationParameters hold the list of identified mentions
     */
    private void handleProperty(Optional<BaseObject> oldBaseObject, LargeStringProperty largeStringProperty,
        String version, MentionLocation location, String authorReference, Syntax syntax,
        List<MentionNotificationParameters> mentionNotificationParameters)
    {
        Optional<XDOM> oldDom = oldBaseObject.flatMap(it -> ofNullable(it.getField(largeStringProperty.getName())))
            .filter(it -> it instanceof LargeStringProperty)
            .flatMap(it -> this.xdomService.parse(((LargeStringProperty) it).getValue(), syntax));
        this.xdomService.parse(largeStringProperty.getValue(), syntax).ifPresent(xdom -> {
            // can be replaced by ifPresentOrElse for in java 9+
            EntityReference entityReference = largeStringProperty.getReference();
            oldDom.ifPresent(
                od -> handleUpdatedContent(od, xdom, entityReference, version, authorReference, location,
                    mentionNotificationParameters));
            if (!oldDom.isPresent()) {
                handleCreatedContent(xdom, entityReference, version, authorReference, location,
                    mentionNotificationParameters);
            }
        });
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

    private void addNewMention(MentionNotificationParameters mentionNotificationParameters, String type,
        MentionNotificationParameter mentionedActorReference)
    {
        mentionNotificationParameters.addNewMention(type, mentionedActorReference);
    }

    private void addMention(MentionNotificationParameters mentionNotificationParameters, String type,
        MentionNotificationParameter mentionedActorReference)
    {
        mentionNotificationParameters.addMention(type, mentionedActorReference);
    }
}
