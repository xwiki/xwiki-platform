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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
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

import static org.xwiki.mentions.MentionLocation.AWM_FIELD;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;

/**
 * Analyzes the new mentions on newly created documents.
 *
 * @version $Id$
 * @since 12.10RC1
 */
@Component(roles = { CreatedDocumentMentionsAnalyzer.class })
@Singleton
public class CreatedDocumentMentionsAnalyzer extends AbstractDocumentMentionsAnalyzer
{
    @Inject
    private MentionXDOMService xdomService;

    /**
     * Analyze a newly created document to identify new mentions.
     *
     * @param doc the analyzed document
     * @param documentReference the reference of the document
     * @param version the version of the created document
     * @param authorReference the reference of the author of the document
     * @return the list of {@link MentionNotificationParameters}, holding the new mentions identified for each entity of
     *     the document.
     */
    public List<MentionNotificationParameters> analyze(XWikiDocument doc,
        DocumentReference documentReference, String version, String authorReference)
    {
        Syntax syntax = doc.getSyntax();
        List<MentionNotificationParameters> ret = new ArrayList<>();
        handleContentOnCreate(doc.getXDOM(), documentReference, version, authorReference, DOCUMENT)
            .ifPresent(ret::add);
        ret.addAll(traverseXObjectsOnCreate(doc.getXObjects(), version, authorReference, syntax));
        return ret;
    }

    /**
     * Traverses the objects of a created document and search for mentions to notify.
     *
     * @param xObjects the objects of the document
     * @param version the version of the document holding the analyzed xobjects when it was created
     * @param authorReference the reference of the author of the document
     * @param syntax the syntax of the document
     * @return the list of mentions for the objects of the document
     */
    private List<MentionNotificationParameters> traverseXObjectsOnCreate(
        Map<DocumentReference, List<BaseObject>> xObjects, String version, String authorReference, Syntax syntax)
    {
        List<MentionNotificationParameters> ret = new ArrayList<>();
        for (List<BaseObject> baseObjects : xObjects.values()) {
            for (BaseObject baseObject : baseObjects) {
                if (baseObject != null) {
                    ret.addAll(handleBaseObjectOnCreate(baseObject, version, authorReference, syntax));
                }
            }
        }
        return ret;
    }

    /**
     * Handles the analysis of a created content to search for mentions to notify.
     *
     * @param xdom the xdom of the content
     * @param entityReference the reference to the analyzed entity
     * @param version the version of the document holding the analyzed entity when it was created
     * @param authorReference the reference of the author of the created document
     * @param location the location of the content
     * @return the list of {@link MentionNotificationParameters}
     */
    private Optional<MentionNotificationParameters> handleContentOnCreate(XDOM xdom, EntityReference entityReference,
        String version, String authorReference, MentionLocation location)
    {
        MentionNotificationParameters ret =
            new MentionNotificationParameters(authorReference, entityReference, location, version);

        List<MacroBlock> blocks = this.xdomService.listMentionMacros(xdom);

        Map<MentionedActorReference, List<String>> counts = this.xdomService.groupAnchorsByUserReference(blocks);

        addAllMentions(ret, counts);

        for (Map.Entry<MentionedActorReference, List<String>> entry : counts.entrySet()) {
            boolean emptyAnchorProcessed = false;
            String type = entry.getKey().getType();
            String reference = entry.getKey().getReference();
            for (String anchorId : entry.getValue()) {
                if (!StringUtils.isEmpty(anchorId) || !emptyAnchorProcessed) {
                    addNewMention(ret, type, new MentionNotificationParameter(reference, anchorId));
                    emptyAnchorProcessed = emptyAnchorProcessed || StringUtils.isEmpty(anchorId);
                }
            }
        }
        return wrapResult(ret);
    }

    /**
     * Handles an object of a created document and search for mentions to notify its content.
     *
     * @param baseObject the object
     * @param version the version of the document holding the base object when it was created
     * @param authorReference the reference of the author of the document
     * @param syntax the syntax of the document
     * @return the list of {@link MentionNotificationParameters} for the entities where new mentions have been
     *     identified on the base object.
     */
    private List<MentionNotificationParameters> handleBaseObjectOnCreate(BaseObject baseObject, String version,
        String authorReference, Syntax syntax)
    {
        List<MentionNotificationParameters> ret = new ArrayList<>();
        for (Object o : baseObject.getProperties()) {
            if (o instanceof LargeStringProperty) {
                LargeStringProperty largeStringProperty = (LargeStringProperty) o;
                String content = largeStringProperty.getValue();
                this.xdomService
                    .parse(content, syntax)
                    .flatMap(xdom -> handleContentOnCreate(xdom, largeStringProperty.getReference(), version,
                        authorReference, AWM_FIELD))
                    .ifPresent(ret::add);
            }
        }
        return ret;
    }
}
