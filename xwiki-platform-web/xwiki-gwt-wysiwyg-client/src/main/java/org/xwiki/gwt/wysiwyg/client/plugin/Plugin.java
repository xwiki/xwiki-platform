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
package org.xwiki.gwt.wysiwyg.client.plugin;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;


/**
 * A plug-in for the WYSIWYG editor.
 * 
 * @version $Id$
 */
public interface Plugin
{
    /**
     * Initialize the plug-in. Plug-ins need to know the text area in order to provide only those feature that are
     * supported.
     * 
     * @param textArea the text area of the editor
     * @param config the configuration object
     */
    void init(RichTextArea textArea, Config config);

    /**
     * @return all the user interface extensions that are provided by this plug-in
     */
    UIExtension[] getUIExtensions();

    /**
     * Notifies the plug-in to release its resources before being unloaded from the WYSIWYG editor.
     */
    void destroy();
}
