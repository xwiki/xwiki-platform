package com.xpn.xwiki.internal.observation.remote.converter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xwiki.bridge.DocumentName;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Convert all document event to remote events and back to local events.
 * <p>
 * It also make sure the context contains the proper information like the user or the wiki.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component("document")
public class DocumentEventConverter extends AbstractEventConverter
{
    /**
     * Used to set some proper context informations.
     */
    @Requirement
    private Execution execution;

    /**
     * The events supported by this converter.
     */
    private Set<Class< ? extends Event>> events = new HashSet<Class< ? extends Event>>()
    {
        {
            add(DocumentDeleteEvent.class);
            add(DocumentSaveEvent.class);
            add(DocumentUpdateEvent.class);
        }
    };

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.LocalEventConverter#toRemote(org.xwiki.observation.remote.LocalEventData,
     *      org.xwiki.observation.remote.RemoteEventData)
     */
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (this.events.contains(localEvent.getEvent())) {
            XWikiDocument document = (XWikiDocument) localEvent.getSource();
            XWikiContext context = (XWikiContext) localEvent.getData();

            HashMap<String, Serializable> remoteData = new HashMap<String, Serializable>();

            remoteData.put("contextwiki", context.getDatabase());
            remoteData.put("contextuser", context.getUser());

            remoteData.put("docname", new DocumentName(document.getWikiName(), document.getSpaceName(),
                document.getPageName()));
            remoteData.put("docversion", document.getVersion());
            remoteData.put("doclanguage", document.getLanguage());

            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setData(remoteData);

            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.RemoteEventConverter#fromRemote(org.xwiki.observation.remote.RemoteEventData,
     *      org.xwiki.observation.remote.LocalEventData)
     */
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (this.events.contains(remoteEvent.getEvent())) {
            Map<String, Serializable> remoteData = (Map<String, Serializable>) remoteEvent.getData();

            // set some context information
            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            context.setDatabase((String) remoteData.get("contextwiki"));
            context.setUser((String) remoteData.get("contextuser"));

            // get the document on which the event append
            DocumentName docName = (DocumentName) remoteData.get("docname");
            XWikiDocument doc = new XWikiDocument(docName.getWiki(), docName.getSpace(), docName.getPage());
            doc.setLanguage((String) remoteData.get("doclanguage"));
            try {
                doc = context.getWiki().getDocument(doc, context);
            } catch (Exception e) {
                getLogger().error("Failed to get document [" + docName + "]", e);

                doc = new XWikiDocument(docName.getWiki(), docName.getSpace(), docName.getPage());
            }

            // fill the local event
            localEvent.setEvent((Event) remoteEvent.getEvent());
            localEvent.setSource(doc);
            localEvent.setData(context);

            return true;
        }

        return false;
    }
}
