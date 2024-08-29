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
package org.xwiki.mentions.internal.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.internal.MentionXDOMService;
import org.xwiki.mentions.internal.MentionedActorReference;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;

import static com.xpn.xwiki.doc.XWikiDocument.COMMENTSCLASS_REFERENCE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.TEXT_FIELD;
import static org.xwiki.mentions.MentionLocation.COMMENT;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;

/**
 * Analyzes the new mentions on updated documents.
 *
 * @version $Id$
 * @since 12.10
 */
@Component(roles = { UpdatedDocumentMentionsAnalyzer.class })
@Singleton
public class UpdatedDocumentMentionsAnalyzer extends AbstractDocumentMentionsAnalyzer
{
    @Inject
    private MentionXDOMService xdomService;

    /**
     * Identifies new mentions on an updated document by analyzing the content of the document body as well as the
     * content of the objects attached to the document and comparing them to the document and its objects before the
     * update.
     *
     * @param oldDoc the document before the update
     * @param newDoc the document after the update
     * @param documentReference the reference of the document
     * @param version the version of the document after the update
     * @param authorReference the reference of the author of the update
     * @return the list of the identified new mentions
     */
    public List<MentionNotificationParameters> analyze(XWikiDocument oldDoc, XWikiDocument newDoc,
        DocumentReference documentReference,
        String version, String authorReference)
    {
        List<MentionNotificationParameters> mentionNotificationParametersList = new ArrayList<>();
        handleUpdatedContent(oldDoc.getXDOM(), newDoc.getXDOM(), documentReference, version, authorReference, DOCUMENT)
            .ifPresent(mentionNotificationParametersList::add);
        mentionNotificationParametersList
            .addAll(traverseXObjectsOnUpdate(oldDoc.getXObjects(), newDoc.getXObjects(), version, authorReference,
                newDoc.getSyntax()));
        return mentionNotificationParametersList;
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
     * @return an empty {@link Optional} if no new mention is found, an {@link Optional} holding an {@link
     *     MentionNotificationParameters} if a new mention is found
     */
    private Optional<MentionNotificationParameters> handleUpdatedContent(XDOM oldXDOM, XDOM newXDOM,
        EntityReference entityReference,
        String version, String authorReference, MentionLocation location)
    {
        MentionNotificationParameters ret =
            new MentionNotificationParameters(authorReference, entityReference, location, version);

        List<MacroBlock> oldMentions = this.xdomService.listMentionMacros(oldXDOM);
        List<MacroBlock> newMentions = this.xdomService.listMentionMacros(newXDOM);

        Map<MentionedActorReference, List<String>> oldCounts =
            this.xdomService.groupAnchorsByUserReference(oldMentions);
        Map<MentionedActorReference, List<String>> newCounts =
            this.xdomService.groupAnchorsByUserReference(newMentions);

        addAllMentions(ret, newCounts, newMentions);

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
                DisplayStyle displayStyle = findDisplayStyle(newMentions, reference, "");
                addNewMention(ret, null,
                    new MentionNotificationParameter(reference, "", displayStyle));
            }

            // Notify all new mentions with new anchors.
            for (String anchor : anchorsToNotify) {
                DisplayStyle displayStyle = findDisplayStyle(newMentions, reference, anchor);
                addNewMention(ret, type,
                    new MentionNotificationParameter(reference, anchor, displayStyle));
            }
        }
        return wrapResult(ret);
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
     * @return the list of the identified new mentions
     */
    private List<MentionNotificationParameters> traverseXObjectsOnUpdate(
        Map<DocumentReference, List<BaseObject>> oldXObjects,
        Map<DocumentReference, List<BaseObject>> xObjects, String version, String authorReference, Syntax syntax)
    {
        List<MentionNotificationParameters> ret = new ArrayList<>();
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : xObjects.entrySet()) {
            List<BaseObject> oldEntry = oldXObjects.get(entry.getKey());
            for (BaseObject baseObject : entry.getValue()) {
                if (baseObject != null) {
                    ret
                        .addAll(handleBaseObjectOnUpdate(oldEntry, baseObject, version, authorReference, syntax));
                }
            }
        }
        return ret;
    }

    /**
     * Handles a base object during the update of a document to search for new mentions to notify.
     *
     * @param oldEntry the old base object (if it exists)
     * @param baseObject the new base object
     * @param version the version of the document holding the analyzed object when it was updated
     * @param authorReference the reference of the author of the change
     * @param syntax the syntax of the document
     * @return the list of the identified new mentions
     */
    private List<MentionNotificationParameters> handleBaseObjectOnUpdate(List<BaseObject> oldEntry,
        BaseObject baseObject, String version, String authorReference, Syntax syntax)
    {
        List<MentionNotificationParameters> mentionNotificationParametersList = new ArrayList<>();
        if (baseObject != null) {
            Optional<BaseObject> oldBaseObject = ofNullable(oldEntry).flatMap(
                optOldEntries -> optOldEntries
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(it -> it.getId() == baseObject.getId())
                    .findAny());

            // Special treatment on comment objects to analyse only the comment field.
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
                        handleProperty(oldBaseObject, lsp, version, location, authorReference, syntax)
                            .ifPresent(mentionNotificationParametersList::add);
                    });
            } else {
                for (Object o : baseObject.getProperties()) {
                    if (o instanceof LargeStringProperty) {
                        handleProperty(oldBaseObject, (LargeStringProperty) o, version, TEXT_FIELD, authorReference,
                            syntax).ifPresent(mentionNotificationParametersList::add);
                    }
                }
            }
        }
        return mentionNotificationParametersList;
    }

    /**
     * Handle a property of an object to search for new mentions to notify.
     *
     * @param oldBaseObject the old base object (if it exists).
     * @param largeStringProperty the large string property
     * @param version the version of the document holding the analysed property when it was updated
     * @param location the location of the property
     * @param authorReference the reference of the author change
     * @param syntax the syntax of the document
     * @return an empty {@link Optional} if no new mention is found, an {@link Optional} holding an {@link
     *     MentionNotificationParameters} if a new mention is found
     */
    private Optional<MentionNotificationParameters> handleProperty(Optional<BaseObject> oldBaseObject,
        LargeStringProperty largeStringProperty,
        String version, MentionLocation location, String authorReference, Syntax syntax)
    {
        Optional<XDOM> oldDom = oldBaseObject.flatMap(it -> ofNullable(it.getField(largeStringProperty.getName())))
            .filter(it -> it instanceof LargeStringProperty)
            .flatMap(it -> this.xdomService.parse(((LargeStringProperty) it).getValue(), syntax));
        return this.xdomService.parse(largeStringProperty.getValue(), syntax).flatMap(xdom -> {
            EntityReference entityReference = largeStringProperty.getReference();
            return oldDom
                .map(value -> handleUpdatedContent(value, xdom, entityReference, version, authorReference, location))
                .orElseGet(() -> handleCreatedContent(xdom, entityReference, version, authorReference, location));
        });
    }

    /**
     * Handles the analysis of the mentions of a created content to search for mentions to notify.
     *
     * @param newXdom the xdom of the created content
     * @param entityReference the reference of the analyzed entity
     * @param version the version of the document holding the analyzed entity when it was updated
     * @param authorReference the reference of the author of the change
     * @param location the location of the content
     * @return an empty {@link Optional} if no new mention is found, an {@link Optional} holding an {@link
     *     MentionNotificationParameters} if a new mention is found
     */
    private Optional<MentionNotificationParameters> handleCreatedContent(XDOM newXdom, EntityReference entityReference,
        String version, String authorReference, MentionLocation location)
    {
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(authorReference, entityReference, location, version);

        List<MacroBlock> newMentions = this.xdomService.listMentionMacros(newXdom);

        // The matching element has not be found in the previous version of the document.
        // Notification are send unconditionally to all mentioned users.
        Map<MentionedActorReference, List<String>> mentionedActorReferenceListMap =
            this.xdomService.groupAnchorsByUserReference(newMentions);
        addAllMentions(mentionNotificationParameters, mentionedActorReferenceListMap, newMentions);
        mentionedActorReferenceListMap
            .forEach((key, value) -> value.forEach(
                anchorId -> {
                    DisplayStyle displayStyle = findDisplayStyle(newMentions, key.getReference(), anchorId);
                    addNewMention(mentionNotificationParameters, key.getType(),
                        new MentionNotificationParameter(key.getReference(), anchorId, displayStyle));
                }));

        return wrapResult(mentionNotificationParameters);
    }
}
