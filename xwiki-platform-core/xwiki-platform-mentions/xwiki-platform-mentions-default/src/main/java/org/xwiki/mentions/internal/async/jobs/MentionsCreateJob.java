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
package org.xwiki.mentions.internal.async.jobs;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.MentionNotificationService;
import org.xwiki.mentions.internal.MentionXDOMService;
import org.xwiki.mentions.internal.async.MentionsCreatedRequest;
import org.xwiki.mentions.internal.async.MentionsCreatedStatus;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;

import static org.xwiki.mentions.internal.async.jobs.MentionsCreateJob.ASYNC_REQUEST_TYPE;

/**
 * Handles asynchronously the identification of new mentions in document's body, comments, annotations and AWM fields.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Named(ASYNC_REQUEST_TYPE)
public class MentionsCreateJob extends AbstractJob<MentionsCreatedRequest, MentionsCreatedStatus>
{
    /**
     * The name of the job.
     */
    public static final String ASYNC_REQUEST_TYPE = "mentions-create-job";

    @Inject
    private MentionNotificationService notificationService;

    @Inject
    private MentionXDOMService xdomService;

    @Override
    protected void runInternal()
    {
        MentionsCreatedRequest request = this.getRequest();
        XWikiDocument doc = request.getDoc();
        DocumentReference authorReference = doc.getAuthorReference();
        DocumentReference documentReference = doc.getDocumentReference();

        handleMentions(doc.getXDOM(), authorReference, documentReference, MentionLocation.DOCUMENT);

        traverseXObjects(doc.getXObjects(), authorReference, documentReference);
    }

    private void handleMentions(XDOM xdom, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location)
    {
        List<MacroBlock> blocks = this.xdomService.listMentionMacros(xdom);

        Map<DocumentReference, List<String>> counts = this.xdomService.countByIdentifier(blocks);

        for (Entry<DocumentReference, List<String>> entry : counts.entrySet()) {
            boolean emptyAnchorProcessed = false;
            for (String anchorId : entry.getValue()) {
                if (!StringUtils.isEmpty(anchorId) || !emptyAnchorProcessed) {
                    MentionNotificationParameters parameters =
                        new MentionNotificationParameters(authorReference, documentReference, entry.getKey(), location,
                            anchorId, xdom);
                    this.notificationService.sendNotification(parameters);
                    emptyAnchorProcessed = emptyAnchorProcessed || StringUtils.isEmpty(anchorId);
                }
            }
        }
    }

    private void traverseXObjects(Map<DocumentReference, List<BaseObject>> xObjects, DocumentReference authorReference,
        DocumentReference documentReference)
    {
        for (Entry<DocumentReference, List<BaseObject>> entry : xObjects.entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                if (baseObject != null) {
                    for (Object o : baseObject.getProperties()) {
                        if (o instanceof LargeStringProperty) {
                            this.xdomService.parse(((LargeStringProperty) o).getValue())
                                .ifPresent(xdom ->
                                    handleMentions(xdom, authorReference, documentReference,
                                        MentionLocation.AWM_FIELD));
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getType()
    {
        return ASYNC_REQUEST_TYPE;
    }
}
