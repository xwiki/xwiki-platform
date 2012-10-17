package org.xwiki.localization.internal.xwikipreferences;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.localization.Translation;
import org.xwiki.localization.internal.AbstractBundle;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.RegexEntityReference;
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

    private List<Event> events;

    private String wiki;

    public XWikiPreferencesWikiBundle(String wiki, ObservationManager observation)
    {
        super(XWikiPreferencesBundle.ID + '.' + wiki);

        this.observation = observation;
        this.wiki = wiki;

        DocumentReference preferences = new DocumentReference(this.wiki, "XWiki", "XWikiPreferences");

        RegexEntityReference documentBundlesProperty =
            new RegexEntityReference(Pattern.compile(DOCUMENT_BUNDLE_PROPERTY), EntityType.OBJECT_PROPERTY,
                new RegexEntityReference(Pattern.compile(this.wiki + ":XWiki.XWikiPreferences\\[\\d*\\]"),
                    EntityType.OBJECT, preferences));

        this.events = Arrays.<Event> asList(new XObjectPropertyUpdatedEvent(documentBundlesProperty));

        this.observation.addListener(this);
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
        // TODO Auto-generated method stub

    }

    // Bundle

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dispose() throws Exception
    {
        this.observation.removeListener(getName());
    }
}
