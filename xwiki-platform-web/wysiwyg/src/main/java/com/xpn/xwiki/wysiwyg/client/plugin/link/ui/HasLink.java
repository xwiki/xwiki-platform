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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import com.google.gwt.user.client.ui.SourcesClickEvents;

/**
 * Interface for widgets that return a link html block from user input and are clickable.
 * 
 * @version $Id$
 */
public interface HasLink extends SourcesClickEvents
{
    /**
     * Returns the link html block to use with the <i>create_link</i> command.
     * 
     * @return the link html block
     */
    String getLink();

    /**
     * Sets the label for which this {@link HasLink} will generate the link HTML.
     * 
     * @param labelHTML the label's HTML value
     * @param labelText the label's text value
     * @param readOnly specifies if the user will be allowed to edit the link's label or not.
     */
    void setLabel(String labelHTML, String labelText, boolean readOnly);

    /**
     * Some setup need for the moment when the widget will be display.
     */
    void initialize();

    /**
     * Validates the user input for this widget, to be used for validation before the link is created / returned. 
     * 
     * @return true if the user input for this widget is valid, false otherwise.
     */
    boolean validateUserInput();
}
