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
import com.xpn.xwiki.wysiwyg.client.plugin.Plugin;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.util.Config;

/**
 * Abstract implementation of the {@link Plugin} interface. This could serve as a base class for all kind of plug-ins.
 */
public abstract class AbstractPlugin implements Plugin
{
    private Wysiwyg wysiwyg;

    private XRichTextArea textArea;

    private Config config;

    private boolean loaded = false;

    private final List<UIExtension> uiExtensions = new ArrayList<UIExtension>();

    protected Config getConfig()
    {
        return config;
    }

    protected XRichTextArea getTextArea()
    {
        return textArea;
    }

    protected Wysiwyg getWysiwyg()
    {
        return wysiwyg;
    }

    protected List<UIExtension> getUIExtensionList()
    {
        return uiExtensions;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Plugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
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
