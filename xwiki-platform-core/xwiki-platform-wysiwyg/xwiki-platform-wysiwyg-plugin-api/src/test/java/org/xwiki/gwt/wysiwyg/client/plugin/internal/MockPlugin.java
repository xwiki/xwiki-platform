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

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.wysiwyg.client.plugin.Plugin;
import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;


/**
 * Mock plug-in to be used in unit tests.
 * 
 * @version $Id$
 */
public class MockPlugin implements Plugin
{
    /**
     * The only UI extension provided by this mock plugin.
     */
    private final UIExtension uie;

    /**
     * Creates a new mock plugin that provides the given UI extension.
     * 
     * @param uie A user interface extension.
     */
    public MockPlugin(UIExtension uie)
    {
        this.uie = uie;
    }

    @Override
    public void destroy()
    {
        // ignore
    }

    @Override
    public UIExtension[] getUIExtensions()
    {
        return new UIExtension[] {uie};
    }

    @Override
    public void init(RichTextArea textArea, Config config)
    {
        // ignore
    }
}
