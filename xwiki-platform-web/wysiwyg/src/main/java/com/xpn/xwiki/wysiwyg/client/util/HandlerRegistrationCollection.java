package com.xpn.xwiki.wysiwyg.client.util;

import java.util.ArrayList;

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * A collection of {@link HandlerRegistration} instances.
 * 
 * @version $Id$
 */
public class HandlerRegistrationCollection extends ArrayList<HandlerRegistration>
{
    /**
     * Field required by all {@link java.io.Serializable} classes.
     */
    private static final long serialVersionUID = 9046100072911029017L;

    /**
     * Removes all handlers which have registrations in this collection.
     * 
     * @see HandlerRegistration#removeHandler()
     */
    public void removeHandlers()
    {
        for (HandlerRegistration registration : this) {
            registration.removeHandler();
        }
        clear();
    }
}
