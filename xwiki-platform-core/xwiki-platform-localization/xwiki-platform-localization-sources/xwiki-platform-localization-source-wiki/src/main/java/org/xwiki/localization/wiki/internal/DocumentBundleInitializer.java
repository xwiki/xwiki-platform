package org.xwiki.localization.wiki.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.BundleFactory;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

@Component
@Named(DocumentBundleInitializer.NAME)
@Singleton
public class DocumentBundleInitializer implements EventListener
{
    protected static final String NAME = "localization.bundle.DocumentBundleInitializer";

    private static List<Event> EVENTS = Arrays.<Event> asList(new ApplicationReadyEvent());

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        // Start DocumentBundleFactory initialization
        // TODO: do something cleaner
        try {
            this.componentManager.getInstance(BundleFactory.class, "document");
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to initialization [{}] component", DocumentBundleFactory.class);
        }
    }
}
