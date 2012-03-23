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

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.internal.BotData;
import org.xwiki.ircbot.internal.BotListenerData;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
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
 * Default implementation of {@link org.xwiki.ircbot.wiki.WikiIRCModel}.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultWikiIRCModel implements WikiIRCModel, WikiIRCBotConstants
{
    /**
     * Name of the security document property in the XWiki Context.
     */
    private static final String SECURITY_DOC_PROPERTY = "sdoc";

    /**
     * The {@link org.xwiki.context.Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * Used to format references in exception messages.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultSerializer;

    /**
     * Used to compute a Wiki Bot Listener id from a relative Document reference without the wiki part.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    /**
     * Used to perform search for IRC Bot listener classes in the current wiki.
     */
    @Inject
    private QueryManager queryManager;

    @Override
    public XWikiDocument getDocument(DocumentReference reference) throws IRCBotException
    {
        XWikiDocument doc;
        XWikiContext xwikiContext = getXWikiContext();
        try {
            doc = xwikiContext.getWiki().getDocument(reference, xwikiContext);
        } catch (XWikiException e) {
            throw new IRCBotException(String.format("Unable to load document [%s]",
                this.defaultSerializer.serialize(reference)), e);
        }
        return doc;
    }

    @Override
    public XWikiDocument getConfigurationDocument() throws IRCBotException
    {
        return getDocument(new DocumentReference(getXWikiContext().getDatabase(), SPACE, CONFIGURATION_PAGE));
    }

    @Override
    public XWikiContext getXWikiContext() throws IRCBotException
    {
        XWikiContext xwikiContext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        if (xwikiContext == null) {
            throw new IRCBotException("The XWiki Context is not available in the Execution Contexte");
        }
        return xwikiContext;
    }

    @Override
    public BotData loadBotData() throws IRCBotException
    {
        BaseObject configurationObject = getIRCConfigurationObject();
        BotData botData = new BotData(
            configurationObject.getStringValue(BOTNAME_PROPERTY),
            configurationObject.getStringValue(SERVER_PROPERTY),
            configurationObject.getStringValue(PASSWORD_PROPERTY),
            configurationObject.getStringValue(CHANNEL_PROPERTY),
            configurationObject.getIntValue(INACTIVE_PROPERTY) != 1);
        return botData;
    }

    @Override
    public void setActive(boolean isActive) throws IRCBotException
    {
        getIRCConfigurationObject().set(INACTIVE_PROPERTY, isActive ? 0 : 1, getXWikiContext());
    }

    @Override
    public List<BotListenerData> getWikiBotListenerData() throws IRCBotException
    {
        List<Object[]> results;
        try {
            Query query = this.queryManager.createQuery(
                String.format("select distinct doc.space, doc.name, listener.name, listener.description "
                    + "from Document doc, doc.object(%s) as listener",
                        this.defaultSerializer.serialize(WIKI_BOT_LISTENER_CLASS)), Query.XWQL);
            results = query.execute();
        } catch (QueryException e) {
            throw new IRCBotException("Failed to locate IRC Bot listener objects in the wiki", e);
        }

        List<BotListenerData> data = new ArrayList<BotListenerData>();
        for (Object[] documentData : results) {
            EntityReference relativeReference = new EntityReference((String) documentData[1], EntityType.DOCUMENT,
                new EntityReference((String) documentData[0], EntityType.SPACE));
            data.add(new BotListenerData(this.compactWikiSerializer.serialize(relativeReference),
                (String) documentData[2], (String) documentData[3], true));
        }

        return data;
    }

    @Override
    public void executeAsUser(DocumentReference executingUserReference, DocumentReference securityDocumentReference,
        Executor executor) throws Exception
    {
        XWikiContext xwikiContext = getXWikiContext();
        DocumentReference currentUserReference = xwikiContext.getUserReference();
        XWikiDocument currentSecurityDocument = (XWikiDocument) xwikiContext.get(SECURITY_DOC_PROPERTY);
        try {
            // Set executing user in the XWiki Context
            xwikiContext.setUserReference(executingUserReference);
            // Set the security document which is used to test permissions
            xwikiContext.put(SECURITY_DOC_PROPERTY, getDocument(securityDocumentReference));
            executor.execute();
        } finally {
            xwikiContext.setUserReference(currentUserReference);
            if (currentSecurityDocument != null) {
                xwikiContext.put(SECURITY_DOC_PROPERTY, currentSecurityDocument);
            } else {
                xwikiContext.remove(SECURITY_DOC_PROPERTY);
            }
        }
    }

    /**
     * @return the XObject in the IRC Configuration document that represents the IRC Configuration.
     * @throws IRCBotException if the document failed to be retrieved
     */
    private BaseObject getIRCConfigurationObject() throws IRCBotException
    {
        XWikiDocument configurationDocument = getConfigurationDocument();
        BaseObject configurationObject = configurationDocument.getXObject(WIKI_BOT_CONFIGURATION_CLASS);
        if (configurationObject == null) {
            // There's no Bot Configuration object
            throw new IRCBotException(String.format("Cannot find the IRC Configuration object in the [%s] document",
                this.defaultSerializer.serialize(configurationDocument.getDocumentReference())));
        }
        return configurationObject;
    }
}
