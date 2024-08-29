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
package org.xwiki.notifications.notifiers.internal.rss;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.rss.NotificationRSSRenderer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.feed.synd.SyndPersonImpl;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

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
     * The binding name of a composite event when the description of this composite event is rendered in a feed
     * entry.
     */
    public static final String COMPOSITE_EVENT_BUILDING_NAME = "event";

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("html/5.0")
    private BlockRenderer blockRenderer;

    @Override
    public SyndEntry renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        SyndEntry entry = new SyndEntryImpl();
        SyndContent entryDescription = new SyndContentImpl();

        // The users contained in the CompositeEvent are already stored in a Set, they are therefore necessarily unique
        List<SyndPerson> eventAuthors = new ArrayList<SyndPerson>();

        // Convert every author of the CompositeEvent to a SyndPerson and add it to the new entry
        for (DocumentReference author : eventNotification.getUsers()) {
            SyndPerson person = new SyndPersonImpl();
            XWikiContext context = this.contextProvider.get();
            XWiki wiki = context.getWiki();
            person.setName(wiki.getPlainUserName(author, context));
            eventAuthors.add(person);
        }
        entry.setAuthors(eventAuthors);

        // Define the GUID of the event
        entry.setUri(String.join("-", eventNotification.getEventIds()));

        // Set the entry title
        entry.setTitle(getTitle(eventNotification));

        // Render the description (the main part) of the feed entry
        try {
            this.scriptContextManager.getCurrentScriptContext().setAttribute(
                    COMPOSITE_EVENT_BUILDING_NAME, eventNotification, ScriptContext.ENGINE_SCOPE);

            // Try to get a template associated with the composite event
            Template template = this.templateManager.getTemplate(String.format("notification/rss/%s.vm",
                    eventNotification.getType().replaceAll("\\/", ".")));

            // If no template is found, fallback on the default one
            if (template == null) {
                template = this.templateManager.getTemplate("notification/rss/default.vm");
            }

            XDOM descriptionXDOM = this.templateManager.execute(template);

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

    private String getTitle(CompositeEvent eventNotification)
    {
        String entryTitle;
        String eventTitle = eventNotification.getEvents().get(0).getTitle();
        String documentTitle = eventNotification.getEvents().get(0).getDocumentTitle();
        boolean concernsDocument = !StringUtils.isEmpty(documentTitle);
        boolean translationExist = !StringUtils.isEmpty(eventTitle)
            && this.contextualLocalizationManager.getTranslation(eventTitle) != null;

        if  (translationExist && concernsDocument) {
            entryTitle = this.contextualLocalizationManager.getTranslationPlain(eventTitle, documentTitle);
        } else if (translationExist && !concernsDocument) {
            entryTitle = this.contextualLocalizationManager.getTranslationPlain(eventTitle);
        } else if (!translationExist && concernsDocument) {
            entryTitle = this.contextualLocalizationManager
                .getTranslationPlain("notifications.rss.defaultTitleWithPage", documentTitle);
        } else {
            entryTitle = this.contextualLocalizationManager
                .getTranslationPlain("notifications.rss.defaultTitle");
        }
        return entryTitle;
    }
}
