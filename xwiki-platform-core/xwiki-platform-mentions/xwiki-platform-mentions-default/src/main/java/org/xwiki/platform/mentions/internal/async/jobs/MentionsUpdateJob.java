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
package org.xwiki.platform.mentions.internal.async.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.mentions.MentionNotificationService;
import org.xwiki.platform.mentions.internal.MentionXDOMService;
import org.xwiki.platform.mentions.MentionLocation;
import org.xwiki.platform.mentions.internal.async.MentionsUpdatedRequest;
import org.xwiki.platform.mentions.internal.async.MentionsUpdatedStatus;
import org.xwiki.job.AbstractJob;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;

import static java.util.Optional.ofNullable;
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.platform.mentions.internal.async.jobs.MentionsUpdateJob.ASYNC_REQUEST_TYPE;

/**
 * Handles asynchronously the identification of new mentions in document's body, comments, annotations and AWM fields.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Named(ASYNC_REQUEST_TYPE)
public class MentionsUpdateJob extends AbstractJob<MentionsUpdatedRequest, MentionsUpdatedStatus>
{
    /**
     * The name of the job.
     */
    public static final String ASYNC_REQUEST_TYPE = "mentions-update-job";

    @Inject
    private MentionNotificationService notificationService;

    @Inject
    private MentionXDOMService xdomService;

    @Override
    protected void runInternal()
    {
        MentionsUpdatedRequest request = this.getRequest();
        XWikiDocument oldDoc = request.getOldDoc();
        XWikiDocument newDoc = request.getNewDoc();
        XDOM oldXdom = oldDoc.getXDOM();
        XDOM newXdom = newDoc.getXDOM();
        DocumentReference authorReference = request.getAuthorReference();
        DocumentReference documentReference = newDoc.getDocumentReference();

        handle(oldXdom, newXdom, authorReference, documentReference, MentionLocation.DOCUMENT);

        Map<DocumentReference, List<BaseObject>> xObjects = newDoc.getXObjects();
        Map<DocumentReference, List<BaseObject>> oldXObjects = oldDoc.getXObjects();

        for (Map.Entry<DocumentReference, List<BaseObject>> entry : xObjects.entrySet()) {
            List<BaseObject> oldEntry = oldXObjects.get(entry.getKey());
            for (BaseObject baseObject : entry.getValue()) {
                handBaseObject(authorReference, documentReference, oldEntry, baseObject);
            }
        }
    }

    private void handBaseObject(DocumentReference authorReference, DocumentReference documentReference,
        List<BaseObject> oldEntry, BaseObject baseObject)
    {
        Optional<BaseObject> oldBaseObject = Optional.ofNullable(oldEntry).flatMap(
            optOldEntries -> optOldEntries.stream().filter(it -> it.getId() == baseObject.getId()).findAny());
        if (baseObject != null) {
            // special treatment on comment object to analyse only the comment field.
            if (Objects.equals(baseObject.getXClassReference().getLocalDocumentReference(),
                XWikiDocument.COMMENTSCLASS_REFERENCE))
            {
                Optional.<Object>ofNullable(baseObject.getField("comment"))
                    .ifPresent(it -> {
                        LargeStringProperty lsp = (LargeStringProperty) it;
                        boolean isComment =
                            StringUtils.isEmpty(lsp.getObject().getField(SELECTION_FIELD).toFormString());
                        handleField(authorReference, documentReference, oldBaseObject, lsp,
                            isComment ? MentionLocation.COMMENT : MentionLocation.ANNOTATION);
                    });
            } else {
                for (Object o : baseObject.getProperties()) {
                    if (o instanceof LargeStringProperty) {
                        handleField(authorReference, documentReference, oldBaseObject, (LargeStringProperty) o,
                            MentionLocation.AWM_FIELD);
                    }
                }
            }
        }
    }

    private void handleField(DocumentReference authorReference, DocumentReference documentReference,
        Optional<BaseObject> oldBaseObject, LargeStringProperty lsp,
        MentionLocation location)
    {
        Optional<XDOM> oldDom = oldBaseObject.flatMap(it -> ofNullable(it.getField(lsp.getName())))
                                    .filter(it -> it instanceof LargeStringProperty)
                                    .flatMap(it -> this.xdomService.parse(((LargeStringProperty) it).getValue()));
        this.xdomService.parse(lsp.getValue()).ifPresent(xdom -> {
            // can be replaced by ifPresentOrElse for in java 9+ 
            oldDom.ifPresent(od -> handle(od, xdom, authorReference, documentReference, location));
            if (!oldDom.isPresent()) {
                handleMissing(xdom, authorReference, documentReference, location);
            }
        });
    }

    private void handle(XDOM oldXdom, XDOM newXdom, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location)
    {

        List<MacroBlock> oldMentions = this.xdomService.listMentionMacros(oldXdom);
        List<MacroBlock> newMentions = this.xdomService.listMentionMacros(newXdom);

        Map<DocumentReference, List<String>> oldCounts = this.xdomService.countByIdentifier(oldMentions);
        Map<DocumentReference, List<String>> newCounts = this.xdomService.countByIdentifier(newMentions);

        // for each user, we check its number of mentions and compare it to the same number on the 
        // old document (or 0 if the user wan't mentionned before).
        // If the number increased, a notification is send.
        newCounts.forEach((key, value) -> {
            List<String> oldCount = oldCounts.getOrDefault(key, Collections.emptyList());
            List<String> mutatedValues = new ArrayList(value);
            mutatedValues.removeAll(oldCount);
            mutatedValues.forEach(anchorId ->
                this.notificationService.sendNotif(authorReference, documentReference, key, location, anchorId));
        });
    }

    private void handleMissing(XDOM newXdom, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location)
    {
        List<MacroBlock> newMentions = this.xdomService.listMentionMacros(newXdom);

        // the matching element has not be found in the previous version of the document
        // notification are send unconditionally to all mentioned users.
        this.xdomService.countByIdentifier(newMentions).forEach((key, value) -> value.forEach(anchorId ->
            this.notificationService.sendNotif(authorReference, documentReference, key, location, anchorId)));
    }

    @Override
    public String getType()
    {
        return ASYNC_REQUEST_TYPE;
    }
}
