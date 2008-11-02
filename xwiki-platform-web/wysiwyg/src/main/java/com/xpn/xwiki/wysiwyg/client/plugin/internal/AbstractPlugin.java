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
package com.xpn.xwiki.wysiwyg.client.plugin.internal;

import java.util.ArrayList;
import java.util.List;

import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.plugin.Plugin;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Abstract implementation of the {@link Plugin} interface. This could serve as a base class for all kind of plug-ins.
 * 
 * @version $Id$
 */
public abstract class AbstractPlugin implements Plugin
{
    /**
     * The application context.
     */
    private Wysiwyg wysiwyg;

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
     * {@link #init(Wysiwyg, RichTextArea, Config)} method must be called.
     */
    private boolean loaded;

    /**
     * The range affected by this plugin.<br/>
     * Most of the plugins alter the DOM document edited with the rich text area by executing commands on the current
     * selection (thus on the current range). In some cases, a plugin needs to get user input before executing such a
     * command. It can gather the needed information by opening a dialog, for instance. In some browsers this may lead
     * to loosing the selection on the rich text area. In this case the plugin can {@link #saveSelection()} before
     * opening the dialog and {@link #restoreSelection()} before executing the command.
     */
    private Range range;

    /**
     * The list of user interface extensions provided by this plugin.
     */
    private final List<UIExtension> uiExtensions = new ArrayList<UIExtension>();

    /**
     * @return {@link #config}
     */
    protected Config getConfig()
    {
        return config;
    }

    /**
     * @return {@link #textArea}
     */
    protected RichTextArea getTextArea()
    {
        return textArea;
    }

    /**
     * @return {@link #wysiwyg}
     */
    protected Wysiwyg getWysiwyg()
    {
        return wysiwyg;
    }

    /**
     * @return {@link #uiExtensions}
     */
    protected List<UIExtension> getUIExtensionList()
    {
        return uiExtensions;
    }

    /**
     * Saves the first range in the current selection for later changes.
     * 
     * @see #range
     * @see #restoreSelection()
     */
    protected void saveSelection()
    {
        range = getTextArea().getDocument().getSelection().getRangeAt(0);
    }

    /**
     * Restores the saved selection.
     * 
     * @see #range
     * @see #saveSelection()
     */
    protected void restoreSelection()
    {
        Selection selection = getTextArea().getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        range = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Plugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        if (loaded) {
            throw new IllegalStateException();
        }
        loaded = true;
        this.wysiwyg = wysiwyg;
        this.textArea = textArea;
        this.config = config;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Plugin#getUIExtensions()
     */
    public UIExtension[] getUIExtensions()
    {
        return uiExtensions.toArray(new UIExtension[uiExtensions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Plugin#destroy()
     */
    public void destroy()
    {
        wysiwyg = null;
        textArea = null;
        config = null;
        uiExtensions.clear();
    }
}
