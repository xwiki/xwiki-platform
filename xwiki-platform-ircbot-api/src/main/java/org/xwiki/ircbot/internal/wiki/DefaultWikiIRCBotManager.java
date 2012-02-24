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
package org.xwiki.ircbot.internal.wiki;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.context.Execution;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCBotListenerFactory;
import org.xwiki.ircbot.wiki.WikiIRCBotManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link WikiIRCBotManager}.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class DefaultWikiIRCBotManager implements WikiIRCBotManager, WikiIRCBotConstants
{
    /**
     * Creates {@link WikiIRCBotListener} objects.
     */
    @Inject
    private WikiIRCBotListenerFactory listenerFactory;

    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to perform search for IRC Bot listener classes in the current wiki.
     */
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("current/reference")
    private DocumentReferenceResolver<EntityReference> currentDocumentReferenceResolver;

    /**
     * The {@link org.xwiki.context.Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    @Inject
    private IRCBot bot;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer compactWikiSerializer;

    // TODO: local is deprecated, find out what to replace it with
    @Inject
    @Named("local")
    private EntityReferenceSerializer localSerializer;

    @Override
    public void startBot() throws IRCBotException
    {
        // Get configuration data for the Bot
        XWikiDocument configurationDocument = getIRCBotConfigurationDocument();
        BaseObject configurationObject = configurationDocument.getXObject(WIKI_BOT_CONFIGURATION_CLASS);
        if (configurationObject == null) {
            // There's no Bot Configuration object
            throw new IRCBotException(String.format("Cannot find IRC Bot Configuration object in [%s] document",
                this.compactWikiSerializer.serialize(configurationDocument.getDocumentReference())));
        }

        boolean isActive = configurationObject.getIntValue(INACTIVE_PROPERTY) != 1;

        if (isActive) {
            String channel = configurationObject.getStringValue(CHANNEL_PROPERTY);

            // Connect to server if not already connected
            if (!this.bot.isConnected()) {
                String botName = configurationObject.getStringValue(BOTNAME_PROPERTY);
                String server = configurationObject.getStringValue(SERVER_PROPERTY);
                String password = configurationObject.getStringValue(PASSWORD_PROPERTY);

                this.bot.connect(botName, server);

                // Identify if a password is set
                if (!StringUtils.isEmpty(password)) {
                    this.bot.identify(password);
                }
            }

            // Join channel
            this.bot.joinChannel(channel);

            registerBotListeners();
        }
    }

    @Override
    public void stopBot() throws IRCBotException
    {
        unregisterBotListeners();

        if (this.bot.isConnected()) {
            this.bot.disconnect();
        }
    }

    @Override
    public void registerBotListener(DocumentReference reference) throws IRCBotException
    {
        // Step 1: Verify if the bot listener is already registered
        String hint = this.entityReferenceSerializer.serialize(reference);
        if (!this.componentManager.hasComponent(IRCBotListener.class, hint)) {
            // Step 2: Create the Wiki Bot Listener component if the document has the correct objects
            if (this.listenerFactory.containsWikiListener(reference)) {
                WikiIRCBotListener wikiListener = this.listenerFactory.createWikiListener(reference);
                // Step 3: Register it!
                try {
                    DefaultComponentDescriptor<IRCBotListener> componentDescriptor =
                        new DefaultComponentDescriptor<IRCBotListener>();
                    componentDescriptor.setRole(IRCBotListener.class);
                    componentDescriptor.setRoleHint(this.entityReferenceSerializer.serialize(reference));
                    this.componentManager.registerComponent(componentDescriptor, wikiListener);
                } catch (ComponentRepositoryException e) {
                    throw new IRCBotException(String.format("Unable to register Wiki IRC Bot Listener in document [%s]",
                        this.compactWikiSerializer.serialize(reference)), e);
                }
            }
        }
    }

    @Override
    public void unregisterBotListener(DocumentReference reference)
    {
        String hint = this.entityReferenceSerializer.serialize(reference);
        this.componentManager.unregisterComponent(IRCBotListener.class, hint);
    }

    @Override
    public void registerBotListeners() throws IRCBotException
    {
        for (DocumentReference reference: getBotListenerDocumentReferences()) {
            registerBotListener(reference);
        }
    }

    @Override
    public void unregisterBotListeners() throws IRCBotException
    {
        for (DocumentReference reference: getBotListenerDocumentReferences()) {
            unregisterBotListener(reference);
        }
    }

    @Override
    public boolean isBotStarted()
    {
        return this.bot.isConnected();
    }

    /**
     * @return the list of references to documents containing {@link #WIKI_BOT_LISTENER_CLASS} objects in the current
     *         wiki
     * @throws IRCBotException if we fail in searching the wiki
     */
    private List<DocumentReference> getBotListenerDocumentReferences() throws IRCBotException
    {
        List<Object[]> results;
        try {
            Query query = this.queryManager.createQuery(
                String.format("select distinct doc.space, doc.name from Document doc, "
                    + "doc.object(%s) as listener", this.localSerializer.serialize(WIKI_BOT_LISTENER_CLASS)),
                        Query.XWQL);
            results = query.execute();
        } catch (QueryException e) {
            throw new IRCBotException("Failed to locate IRC Bot listener objects in the wiki", e);
        }

        List<DocumentReference> references = new ArrayList<DocumentReference>();
        for (Object[] documentData : results) {
            EntityReference relativeReference = new EntityReference((String) documentData[1], EntityType.DOCUMENT,
                new EntityReference((String) documentData[0], EntityType.SPACE));
            DocumentReference reference = this.currentDocumentReferenceResolver.resolve(relativeReference);
            references.add(reference);
        }

        return references;
    }

    /**
     * Utility method for accessing XWikiContext.
     *
     * @return the XWikiContext.
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    private XWikiDocument getIRCBotConfigurationDocument() throws IRCBotException
    {
        XWikiDocument doc;
        DocumentReference docReference = new DocumentReference(getContext().getDatabase(), SPACE, CONFIGURATION_PAGE);
        try {
            doc = getContext().getWiki().getDocument(docReference, getContext());
        } catch (XWikiException ex) {
            throw new IRCBotException(String.format("Could not build Bot Listener from: [%s], unable to load document",
                this.compactWikiSerializer.serialize(docReference)), ex);
        }
        return doc;
    }
}
