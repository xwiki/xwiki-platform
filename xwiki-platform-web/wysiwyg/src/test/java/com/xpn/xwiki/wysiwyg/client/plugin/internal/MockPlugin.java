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

import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.Config;
import com.xpn.xwiki.wysiwyg.client.plugin.Plugin;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;

/**
 * Mock plug-in to be used in unit tests.
 */
public class MockPlugin implements Plugin
{
    private final UIExtension uie;

    public MockPlugin(UIExtension uie)
    {
        this.uie = uie;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Plugin#destroy()
     */
    public void destroy()
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see Plugin#getUIExtensions()
     */
    public UIExtension[] getUIExtensions()
    {
        return new UIExtension[] {uie};
    }

    /**
     * {@inheritDoc}
     * 
     * @see Plugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        // ignore
    }
}
