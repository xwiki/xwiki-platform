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
package org.xwiki.notifications.internal.rss;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.internal.ModelBridge;
import org.xwiki.notifications.rss.NotificationRSSRenderer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.feed.synd.SyndPersonImpl;

/**
 * This is the default implementation of {@link NotificationRSSRenderer}.
 *
 * @since 9.6RC1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultNotificationRSSRenderer implements NotificationRSSRenderer
{
    /**
     * The binding name of a composite event when the description of this composite event is rendered.
     */
    public static final String COMPOSITE_EVENT_BUILDING_NAME = "event";

    @Inject
    private Logger logger;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    @Named("html/5.0")
    private BlockRenderer blockRenderer;

    @Override
    public SyndEntry renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        // We need at least one event in the given CompositeEvent
        if (eventNotification.getEvents().size() == 0) {
            throw new NotificationException("Unable to create a RSS entry from an empty composite event.");
        }

        SyndEntry entry = new SyndEntryImpl();
        SyndContent entryDescription = new SyndContentImpl();

        // The users contained in the CompositeEvent are already stored in a Set, they are therefore necessarily unique
        List<SyndPerson> eventAuthors = new ArrayList<SyndPerson>();

        // Convert every author of the CompositeEvent to a SyndPerson and add it to the new entry
        for (DocumentReference author : eventNotification.getUsers()) {
            SyndPerson person = new SyndPersonImpl();
            person.setName(author.getName());
            eventAuthors.add(person);
        }
        entry.setAuthors(eventAuthors);

        // Define the GUID of the event
        entry.setUri(String.join("-", eventNotification.getEventIds()));

        // Set the entry title
        entry.setTitle(this.contextualLocalizationManager.getTranslationPlain(
                eventNotification.getEvents().get(0).getTitle(),
                eventNotification.getEvents().get(0).getDocumentTitle()));

        // Render the description (the main part) of the feed entry
        try {
            this.scriptContextManager.getCurrentScriptContext().setAttribute(
                    COMPOSITE_EVENT_BUILDING_NAME, eventNotification, ScriptContext.ENGINE_SCOPE);

            XDOM descriptionXDOM = this.templateManager.execute("notification/rss/default.vm");

            WikiPrinter printer = new DefaultWikiPrinter();
            blockRenderer.render(descriptionXDOM, printer);

            // Add the description to the entry
            entryDescription.setType("text/html");
            entryDescription.setValue(printer.toString());
            entry.setDescription(entryDescription);
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to render the description of the event [%s].", eventNotification), e);
        } finally {
            this.scriptContextManager.getCurrentScriptContext().removeAttribute(
                    COMPOSITE_EVENT_BUILDING_NAME, ScriptContext.ENGINE_SCOPE);
        }

        // Dates are sorted in descending order in a CompositeEvent, the first date is then the most recent one
        entry.setUpdatedDate(eventNotification.getDates().get(0));

        return entry;
    }

    @Override
    public SyndFeed renderFeed(List<CompositeEvent> events)
    {
        SyndFeed feed = new SyndFeedImpl();

        // Define the general properties of the rss
        feed.setFeedType("rss_2.0");
        feed.setTitle(this.contextualLocalizationManager.getTranslationPlain("notifications.rss.feedTitle"));

        // Set the RSS feed link to the service generating the feed
        feed.setLink(this.modelBridge.getDocumentURL(
                this.documentReferenceResolver.resolve("XWiki.Notifications.Code.NotificationRSSService"),
                "get", "outputSyntax=plain"));

        // Set the feed description
        feed.setDescription(this.contextualLocalizationManager.getTranslationPlain(
                "notifications.rss.feedDescription"));

        // Add every given CompositeEvent entry to the rss
        List<SyndEntry> entries = new ArrayList<>();
        for (CompositeEvent event : events) {
            try {
                entries.add(this.renderNotification(event));
            } catch (NotificationException e) {
                this.logger.warn(
                        String.format("Unable to render RSS entry for CompositeEvent : %s", e.getMessage()));
            }
        }
        feed.setEntries(entries);

        return feed;
    }
}
