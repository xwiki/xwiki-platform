package org.xwiki.localization.internal.xwikipreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import org.apache.ecs.storage.Array;
import org.infinispan.util.concurrent.ConcurrentHashSet;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.Translation;
import org.xwiki.localization.internal.AbstractBundle;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;

public class XWikiPreferencesWikiBundle extends AbstractBundle implements EventListener, DisposableCacheValue
{
    /**
     * The name of the property containing the list of global document bundles.
     */
    private static final String DOCUMENT_BUNDLE_PROPERTY = "documentBundles";

    /**
     * String to use when joining the list of document names.
     */
    private static final String JOIN_SEPARATOR = ",";

    private final ObservationManager observation;
    
    private DocumentAccessBridge documentAccessBridge;

    private List<Event> events;

    private String wiki;

    private Map<DocumentReference, Bundle> bundles;

    public XWikiPreferencesWikiBundle(String wiki, ObservationManager observation)
    {
        super("localization." + XWikiPreferencesBundle.ID + '.' + wiki);

        this.observation = observation;
        this.wiki = wiki;

        intializeBundles();

        // Observation

        DocumentReference preferences = new DocumentReference(this.wiki, "XWiki", "XWikiPreferences");

        RegexEntityReference documentBundlesProperty =
            new RegexEntityReference(Pattern.compile(DOCUMENT_BUNDLE_PROPERTY), EntityType.OBJECT_PROPERTY,
                new RegexEntityReference(Pattern.compile(this.wiki + ":XWiki.XWikiPreferences\\[\\d*\\]"),
                    EntityType.OBJECT, preferences));

        this.events = Arrays.<Event> asList(new XObjectPropertyUpdatedEvent(documentBundlesProperty));

        this.observation.addListener(this);
    }

    private void intializeBundles()
    {
        List<String> documentList = this.documentAccessBridge.getProperty(arg0);
        
        WikiReference wikiReference = new WikiReference(this.wiki);
        
        Map<DocumentReference, Bundle> bundles = new LinkedHashMap<DocumentReference, Bundle>(documentList.size());
        for (String document : documentList) {
            DocumentReference reference = this.resolver.resolve(document, wikiReference);

            bundles.put(reference, new DocumentBundle(reference));
        }
    }

    // EventListener

    @Override
    public String getName()
    {
        return "localization.bundle." + getId();
    }

    @Override
    public List<Event> getEvents()
    {
        return this.events;
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {

    }

    // Bundle

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        for (Bundle bundle : bundles) {
            Translation translation = bundle.getTranslation(key, locale);
            if (translation != null) {
                return translation;
            }
        }

        return null;
    }

    @Override
    public void dispose() throws Exception
    {
        this.observation.removeListener(getName());
    }
}
