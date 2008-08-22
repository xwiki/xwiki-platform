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
package com.xpn.xwiki.wysiwyg.client.plugin;

import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;

/**
 * A plug-in for the WYSIWYG editor.
 */
public interface Plugin
{
    /**
     * Initialize the plug-in. Plug-ins need to know the text area in order to provide only those feature that are
     * supported.
     * 
     * @param wysiwyg The WYSIWYG application context.
     * @param textArea The text area of the editor.
     * @param config The configuration.
     */
    void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config);

    /**
     * @return All the user interface extensions that are provided by this plug-in.
     */
    UIExtension[] getUIExtensions();

    /**
     * Notifies the plug-in to release its resources before being unloaded from the WYSIWYG editor.
     */
    void destroy();
}
