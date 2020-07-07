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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.MentionNotificationService;
import org.xwiki.mentions.MentionsEventExecutor;
import org.xwiki.mentions.internal.async.MentionsThreadPoolExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.XWikiContext;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Default implementation of {@link MentionsEventExecutor}.
 * All the operations are done asynchronously.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@Component
@Singleton
public class DefaultMentionsEventExecutor implements MentionsEventExecutor, Initializable
{
    private static final int POOL_SIZE = 4;

    private ThreadPoolExecutor executor;

    @Inject
    private MentionNotificationService notificationService;

    @Inject
    private MentionXDOMService xdomService;

    @Inject
    private ExecutionContextManager contextManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Override
    public void initialize()
    {
        this.executor = new MentionsThreadPoolExecutor(POOL_SIZE);
    }

    @Override
    public void executeCreate(XDOM xdom, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location)
    {
        this.executor.execute(new CreateRunnable(xdom, authorReference, documentReference, location));
    }

    @Override
    public void executeCreate(String content, DocumentReference authorReference, DocumentReference documentReference,
        MentionLocation location)
    {
        this.executor.execute(new CreateRunnable(content, authorReference, documentReference, location));
    }

    @Override
    public void executeUpdate(XDOM oldXdom, XDOM newXdom, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location)
    {
        this.executor.execute(new UpdateRunnable(oldXdom, newXdom, authorReference, documentReference, location));
    }

    @Override
    public void executeUpdate(String oldContent, String newContent, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location)
    {
        this.executor.execute(new UpdateRunnable(oldContent, newContent, authorReference, documentReference, location));
    }

    @Override
    public long getQueueSize()
    {
        return this.executor.getTaskCount();
    }

    @Override
    public void clearQueue()
    {
        this.executor.getQueue().clear();
    }

    private class UpdateRunnable implements Runnable
    {
        private final String oldContent;

        private final String newContent;

        private final XDOM oldXdom;

        private final XDOM newXdom;

        private final DocumentReference authorReference;

        private final DocumentReference documentReference;

        private final MentionLocation location;

        /**
         * Initialize the task with the xdom to compare, and other information about the element ot analyze.
         *
         * @param oldXdom the old xdom 
         * @param newXdom the new xdom
         * @param authorReference the author reference
         * @param documentReference the document reference
         * @param location the location type
         */
        UpdateRunnable(XDOM oldXdom, XDOM newXdom, DocumentReference authorReference,
            DocumentReference documentReference, MentionLocation location)
        {
            this.oldContent = null;
            this.newContent = null;
            this.oldXdom = oldXdom;
            this.newXdom = newXdom;
            this.authorReference = authorReference;
            this.documentReference = documentReference;
            this.location = location;
        }

        /**
         * Initialize the task with the string content to compare, and other information about the element to analyze.  
         * @param oldContent the old content
         * @param newContent the new content
         * @param authorReference the author reference
         * @param documentReference the document reference
         * @param location the location typew
         */
        UpdateRunnable(String oldContent, String newContent, DocumentReference authorReference,
            DocumentReference documentReference, MentionLocation location)
        {
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.oldXdom = null;
            this.newXdom = null;
            this.authorReference = authorReference;
            this.documentReference = documentReference;
            this.location = location;
        }

        @Override
        public void run()
        {
            try {
                DefaultMentionsEventExecutor.this.initContext(this.authorReference);
                getXdom(this.newXdom, this.newContent).ifPresent(newXdom -> {
                    if (this.oldXdom != null || this.oldContent != null) {
                        getXdom(this.oldXdom, this.oldContent).ifPresent(
                            oldXdom -> handle(oldXdom, newXdom, this.authorReference, this.documentReference,
                                this.location));
                    } else {
                        handleMissing(newXdom, this.authorReference, this.documentReference, this.location);
                    }
                });
            } catch (ExecutionContextException e) {
                DefaultMentionsEventExecutor.this.logger
                    .warn("Failed to initalize the context of the mention update runnable. Cause [{}]",
                        getRootCauseMessage(e));
            } finally {
                endContext();
            }
        }

        private void handle(XDOM oldXdom, XDOM newXdom, DocumentReference authorReference,
            DocumentReference documentReference, MentionLocation location)
        {
            List<MacroBlock> oldMentions = DefaultMentionsEventExecutor.this.xdomService.listMentionMacros(oldXdom);
            List<MacroBlock> newMentions = DefaultMentionsEventExecutor.this.xdomService.listMentionMacros(newXdom);

            Map<DocumentReference, List<String>> oldCounts =
                DefaultMentionsEventExecutor.this.xdomService.countByIdentifier(oldMentions);
            Map<DocumentReference, List<String>> newCounts =
                DefaultMentionsEventExecutor.this.xdomService.countByIdentifier(newMentions);

            for (Map.Entry<DocumentReference, List<String>> entry : newCounts.entrySet()) {
                DocumentReference key = entry.getKey();
                List<String> newAnchorIds = entry.getValue();
                List<String> oldAnchorsIds = oldCounts.getOrDefault(key, Collections.emptyList());

                // Compute if there's new mentions without an anchor
                long newEmptyAnchorsNumber = newAnchorIds.stream().filter(org.xwiki.text.StringUtils::isEmpty).count();
                long oldEmptyAnchorsNumber = oldAnchorsIds.stream().filter(org.xwiki.text.StringUtils::isEmpty).count();

                // Retrieve new mentions with a new anchor
                List<String> anchorsToNotify = newAnchorIds.stream()
                                                   .filter(value -> !org.xwiki.text.StringUtils.isEmpty(value)
                                                                        && !oldAnchorsIds.contains(value))
                                                   .collect(Collectors.toList());

                // Notify with an empty anchorId if there's new mentions without an anchor.
                if (newEmptyAnchorsNumber > oldEmptyAnchorsNumber) {
                    sendNotif(authorReference, documentReference, location, key, "");
                }

                // Notify all new mentions with new anchors.
                for (String anchorId : anchorsToNotify) {
                    sendNotif(authorReference, documentReference, location, key, anchorId);
                }
            }
        }

        private void handleMissing(XDOM newXdom, DocumentReference authorReference,
            DocumentReference documentReference, MentionLocation location)
        {
            List<MacroBlock> newMentions = DefaultMentionsEventExecutor.this.xdomService.listMentionMacros(newXdom);

            // the matching element has not be found in the previous version of the document
            // notification are send unconditionally to all mentioned users.
            DefaultMentionsEventExecutor.this.xdomService.countByIdentifier(newMentions)
                .forEach((key, value) -> value.forEach(
                    anchorId -> sendNotif(authorReference, documentReference, location, key, anchorId)));
        }
    }

    private void sendNotif(DocumentReference authorReference, DocumentReference documentReference,
        MentionLocation location, DocumentReference key, String anchorId)
    {
        this.notificationService.sendNotif(authorReference, documentReference, key, location, anchorId);
    }

    private class CreateRunnable implements Runnable
    {
        private final String content;

        private final XDOM xdom;

        private final DocumentReference authorReference;

        private final DocumentReference documentReference;

        private final MentionLocation location;

        /**
         * Initialize the task with the xdom, and other information about the element to analyze.  
         *
         * @param xdom the xdom
         * @param authorReference the author reference
         * @param documentReference the document reference
         * @param location the location typew
         */
        CreateRunnable(XDOM xdom, DocumentReference authorReference, DocumentReference documentReference,
            MentionLocation location)
        {
            this.content = null;
            this.xdom = xdom;
            this.authorReference = authorReference;
            this.documentReference = documentReference;
            this.location = location;
        }

        /**
         * Initialize the task with the string content, and other information about the element to analyze.  
         * @param content the content
         * @param authorReference the author reference
         * @param documentReference the document reference
         * @param location the location typew
         */
        CreateRunnable(String content, DocumentReference authorReference, DocumentReference documentReference,
            MentionLocation location)
        {
            this.content = content;
            this.xdom = null;
            this.authorReference = authorReference;
            this.documentReference = documentReference;
            this.location = location;
        }

        @Override
        public void run()
        {
            try {
                DefaultMentionsEventExecutor.this.initContext(this.authorReference);

                getXdom(this.xdom, this.content).ifPresent(xdom -> {
                    List<MacroBlock> blocks = DefaultMentionsEventExecutor.this.xdomService.listMentionMacros(xdom);

                    Map<DocumentReference, List<String>> counts =
                        DefaultMentionsEventExecutor.this.xdomService.countByIdentifier(blocks);

                    for (Map.Entry<DocumentReference, List<String>> entry : counts.entrySet()) {
                        boolean emptyAnchorProcessed = false;
                        for (String anchorId : entry.getValue()) {
                            if (!StringUtils.isEmpty(anchorId) || !emptyAnchorProcessed) {
                                sendNotif(this.authorReference, this.documentReference, this.location, entry.getKey(),
                                    anchorId);
                                emptyAnchorProcessed = emptyAnchorProcessed || StringUtils.isEmpty(anchorId);
                            }
                        }
                    }
                });
            } catch (ExecutionContextException e) {
                DefaultMentionsEventExecutor.this.logger
                    .warn("Failed to initalize the context of the mention create runnable. Cause [{}]",
                        getRootCauseMessage(e));
            } finally {
                endContext();
            }
        }
    }

    /**
     * Initialize a context for the duration of a {@link CreateRunnable} or {@link UpdateRunnable}.
     *
     * @param authorReference the author of the analyzed document
     * @throws ExecutionContextException in case one {@link ExecutionContextInitializer} fails to execute 
     * @see DefaultMentionsEventExecutor#endContext()
     */
    private void initContext(DocumentReference authorReference) throws ExecutionContextException
    {
        ExecutionContext context = new ExecutionContext();
        this.contextManager.initialize(context);

        XWikiContext xWikiContext = DefaultMentionsEventExecutor.this.xcontextProvider.get();
        xWikiContext.setUserReference(authorReference);
        xWikiContext.setWikiReference(authorReference.getWikiReference());
    }

    /**
     * Cleanup the context at the end of a {@link CreateRunnable} or {@link UpdateRunnable} execution.
     * @see DefaultMentionsEventExecutor#initContext(DocumentReference)
     */
    private void endContext()
    {
        this.execution.removeContext();
    }

    /**
     * Get the xdom, either directly if provided, or by parsing the provided content.
     *
     * @param xdom a xdom
     * @param content a page content in string format
     * @return the xdom
     */
    private Optional<XDOM> getXdom(XDOM xdom, String content)
    {
        if (xdom != null) {
            return Optional.of(xdom);
        } else {
            return DefaultMentionsEventExecutor.this.xdomService.parse(content);
        }
    }
    @Override
    public void setExecutor(ThreadPoolExecutor executor)
    {
        this.executor = executor;
    }
    
    
}

