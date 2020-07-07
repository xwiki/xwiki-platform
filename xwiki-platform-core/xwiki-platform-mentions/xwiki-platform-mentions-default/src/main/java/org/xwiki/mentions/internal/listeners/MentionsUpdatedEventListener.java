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
package org.xwiki.mentions.internal.listeners;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.MentionsEventExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;

import static com.xpn.xwiki.doc.XWikiDocument.COMMENTSCLASS_REFERENCE;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.AWM_FIELD;
import static org.xwiki.mentions.MentionLocation.COMMENT;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;

/**
 * Listen to entities update. 
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
@Named("MentionsUpdatedEventListener")
public class MentionsUpdatedEventListener extends AbstractEventListener
{
    private static final List<DocumentUpdatedEvent> EVENTS = singletonList(new DocumentUpdatedEvent());

    @Inject
    private Logger logger;

    @Inject
    private MentionsEventExecutor executor;

    /**
     * Default constructor.
     */
    public MentionsUpdatedEventListener()
    {
        super("MentionsUpdatedEventListener", EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.logger.debug("Event [{}] received from [{}] with data [{}].",
            DocumentUpdatedEvent.class.getName(), source, data);

        XWikiDocument newDoc = (XWikiDocument) source;
        XWikiContext ctx = (XWikiContext) data;
        XWikiDocument oldDoc = newDoc.getOriginalDocument();
        DocumentReference authorReference = ctx.getUserReference();
        XDOM oldXdom = oldDoc.getXDOM();
        XDOM newXdom = newDoc.getXDOM();
        DocumentReference documentReference = newDoc.getDocumentReference();

        this.executor.executeUpdate(oldXdom, newXdom, authorReference, documentReference, DOCUMENT);

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
            if (Objects.equals(baseObject.getXClassReference().getLocalDocumentReference(), COMMENTSCLASS_REFERENCE)) {
                Optional.<Object>ofNullable(baseObject.getField("comment"))
                    .ifPresent(it -> {
                        LargeStringProperty lsp = (LargeStringProperty) it;
                        boolean isComment =
                            StringUtils.isEmpty(lsp.getObject().getField(SELECTION_FIELD).toFormString());
                        handleField(authorReference, documentReference, oldBaseObject, lsp,
                            isComment ? COMMENT : ANNOTATION);
                    });
            } else {
                for (Object o : baseObject.getProperties()) {
                    if (o instanceof LargeStringProperty) {
                        handleField(authorReference, documentReference, oldBaseObject, (LargeStringProperty) o,
                            AWM_FIELD);
                    }
                }
            }
        }
    }

    private void handleField(DocumentReference authorReference, DocumentReference documentReference,
        Optional<BaseObject> oldBaseObject, LargeStringProperty lsp,
        MentionLocation location)
    {
        Optional<String> oldDom = oldBaseObject.flatMap(it -> ofNullable(it.getField(lsp.getName())))
                                      .filter(it -> it instanceof LargeStringProperty)
                                      .map(it -> ((LargeStringProperty) it).getValue());

        this.executor.executeUpdate(oldDom.orElse(null), lsp.getValue(), authorReference, documentReference, location);
    }
}
