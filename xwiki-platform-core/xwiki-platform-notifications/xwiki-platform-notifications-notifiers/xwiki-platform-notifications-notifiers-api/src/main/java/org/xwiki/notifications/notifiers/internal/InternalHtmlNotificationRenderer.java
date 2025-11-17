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
package org.xwiki.notifications.notifiers.internal;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.util.ErrorBlockGenerator;

/**
 * Render {@link org.xwiki.notifications.CompositeEvent} to HTML5 by using the right displayers first.
 *
 * @since 12.2
 * @version $Id$
 */
@Component(roles = InternalHtmlNotificationRenderer.class)
@Singleton
public class InternalHtmlNotificationRenderer
{
    private static final String TM_FAILEDRENDER = "notification.error.failedRender";

    private static final String CLASS_PARAMETER = "class";

    @Inject
    private NotificationRenderer notificationRenderer;

    @Inject
    private ErrorBlockGenerator errorBlockGenerator;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    @Named("html/5.0")
    private BlockRenderer htmlRenderer;

    /**
     * Render the notification counter used to display the number of notifications.
     *
     * @param count the number of notifications to display.
     * @return a string containing the HTML to display.
     */
    public String render(int count)
    {
        String displayCount = String.valueOf(count);
        FormatBlock formatBlock = new FormatBlock();
        WordBlock wordBlock = new WordBlock(displayCount);
        formatBlock.addChild(wordBlock);
        formatBlock.setParameter(CLASS_PARAMETER, "notifications-count badge");
        WikiPrinter printer = new DefaultWikiPrinter();
        this.htmlRenderer.render(formatBlock, printer);
        return printer.toString();
    }

    /**
     * Use the appropriate {@link NotificationRenderer} to render first the event to block, and then render it
     * to HTML5.
     * @param compositeEvent the event to render in HTML.
     * @param status the read status of the given event, this might be null in case of Guest user.
     * @return a string containing the HTML to display.
     * @throws Exception in case of error during some rendering operation.
     */
    public String render(CompositeEvent compositeEvent, CompositeEventStatus status) throws Exception
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        htmlRenderer.render(renderCompositeEvent(compositeEvent, status), printer);
        return printer.toString();
    }

    private Block renderCompositeEvent(CompositeEvent compositeEvent, CompositeEventStatus status)
        throws NotificationException
    {
        Block renderedEvent = notificationRenderer.render(compositeEvent);
        GroupBlock parentDiv = new GroupBlock();
        StringBuilder parentDivClass = new StringBuilder().append("notification-event");

        parentDiv.setParameter("data-eventtype", compositeEvent.getType());
        parentDiv.setParameter("data-ids", StringUtils.join(compositeEvent.getEventIds(), ","));
        parentDiv.setParameter("data-eventdate", String.valueOf(getLastCompositeEventDate(compositeEvent).getTime()));
        if (status != null && !status.getStatus()) {
            parentDivClass.append(" notification-event-unread");
        }

        parentDiv.setParameter(CLASS_PARAMETER, parentDivClass.toString());
        parentDiv.addChild(renderedEvent);
        return parentDiv;
    }

    private Date getLastCompositeEventDate(CompositeEvent compositeEvent)
    {
        return compositeEvent.getDates().get(compositeEvent.getDates().size() - 1);
    }

    private Block renderNoEvent()
    {
        Translation translation = localizationManager.getTranslation("notifications.menu.nothing");
        ParagraphBlock paragraphBlock = new ParagraphBlock(Collections.singletonList(translation.render()));
        paragraphBlock.setParameter(CLASS_PARAMETER, "text-center noitems");
        return paragraphBlock;
    }

    private Block renderLoadMore()
    {
        GroupBlock groupBlock = new GroupBlock();
        groupBlock.setParameter(CLASS_PARAMETER, "notifications-macro-load-more");
        return groupBlock;
    }

    /**
     * Helper to render a list of composite events by using {@link #render(CompositeEvent, CompositeEventStatus)}.
     * In case of exception during the rendering an {@link ErrorBlockGenerator} is used to render the error.
     * The argument lists must have the same size.
     *
     * @param compositeEvents the events to render
     * @param compositeEventStatuses the read status of the given list of events, in the same order as returned by
     *                              {@link org.xwiki.notifications.CompositeEventStatusManager}. This argument might be
     *                              empty or null if the current user is Guest. Note that if list of statuses is shorter
     *                              than the list of events, then it won't be used.
     * @param loadMore if {@code true} add a final div in the rendered HTML with a dedicated class to allow a load more
     *                 button.
     * @return a string contaning the HTML to display.
     */
    public String render(List<CompositeEvent> compositeEvents, List<CompositeEventStatus> compositeEventStatuses,
        boolean loadMore)
    {
        StringBuilder stringBuilder = new StringBuilder();

        boolean useStatus = (compositeEventStatuses != null && compositeEventStatuses.size() == compositeEvents.size());
        for (int i = 0; i < compositeEvents.size(); i++) {
            CompositeEvent compositeEvent = compositeEvents.get(i);
            CompositeEventStatus compositeEventStatus = null;
            if (useStatus) {
                compositeEventStatus = compositeEventStatuses.get(i);
            }
            try {
                stringBuilder.append(this.render(compositeEvent, compositeEventStatus));
            } catch (Exception e) {
                WikiPrinter printer = new DefaultWikiPrinter();
                htmlRenderer.render(errorBlockGenerator.generateErrorBlocks(false, TM_FAILEDRENDER,
                    "Error while rendering notification", null, e), printer);
                stringBuilder.append(printer.toString());
            }
        }
        if (compositeEvents.isEmpty()) {
            WikiPrinter printer = new DefaultWikiPrinter();
            htmlRenderer.render(renderNoEvent(), printer);
            stringBuilder.append(printer.toString());
        }
        if (loadMore) {
            WikiPrinter printer = new DefaultWikiPrinter();
            htmlRenderer.render(renderLoadMore(), printer);
            stringBuilder.append(printer.toString());
        }
        return stringBuilder.toString();
    }
}
