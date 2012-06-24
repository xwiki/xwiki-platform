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
package org.xwiki.gwt.wysiwyg.client.plugin.internal;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.HandlerRegistrationCollection;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.wysiwyg.client.plugin.Plugin;
import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Abstract implementation of the {@link Plugin} interface. This could serve as a base class for all kind of plug-ins.
 * 
 * @version $Id$
 */
public abstract class AbstractPlugin implements Plugin
{
    /**
     * The rich text area on which this plugin operates.
     */
    private RichTextArea textArea;

    /**
     * The configuration object used by this plugin. A plugin could behave differently depending on the parameter values
     * stored within the configuration object.
     */
    private Config config;

    /**
     * Flag that indicates if this plugin has been loaded. In order to load a plugin the
     * {@link #init(RichTextArea, Config)} method must be called.
     */
    private boolean loaded;

    /**
     * The list of user interface extensions provided by this plugin.
     */
    private final List<UIExtension> uiExtensions = new ArrayList<UIExtension>();

    /**
     * The collection of handler registrations used by this plug-in.
     */
    private final HandlerRegistrationCollection registrations = new HandlerRegistrationCollection();

    /**
     * @return {@link #config}
     */
    public Config getConfig()
    {
        return config;
    }

    /**
     * @return {@link #uiExtensions}
     */
    protected List<UIExtension> getUIExtensionList()
    {
        return uiExtensions;
    }

    @Override
    public void init(RichTextArea textArea, Config config)
    {
        if (loaded) {
            throw new IllegalStateException();
        }
        loaded = true;
        this.textArea = textArea;
        this.config = config;
    }

    /**
     * @return {@link #textArea}
     */
    public RichTextArea getTextArea()
    {
        return textArea;
    }

    @Override
    public UIExtension[] getUIExtensions()
    {
        return uiExtensions.toArray(new UIExtension[uiExtensions.size()]);
    }

    /**
     * Saves a handler registration in order for the handler to be automatically removed when the plug-in is destroyed.
     * 
     * @param registration the handler registration to be saved
     */
    protected void saveRegistration(HandlerRegistration registration)
    {
        registrations.add(registration);
    }

    /**
     * Saves a list of handler registrations in order for the handlers to be automatically removed when the plug-in is
     * destroyed.
     * 
     * @param registrations the list of handler registrations to be saved
     */
    protected void saveRegistrations(List<HandlerRegistration> registrations)
    {
        registrations.addAll(registrations);
    }

    @Override
    public void destroy()
    {
        textArea = null;
        config = null;
        uiExtensions.clear();
        registrations.removeHandlers();
    }
}
