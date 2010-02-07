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
package org.xwiki.gwt.wysiwyg.client.plugin.text.exec;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.ToggleInlineStyleExecutable;


/**
 * If there is no selection, the insertion point will set bold for subsequently typed characters. If there is a
 * selection and all of the characters are already bold, the bold will be removed. Otherwise, all selected characters
 * will become bold.
 * 
 * @version $Id$
 */
public class BoldExecutable extends ToggleInlineStyleExecutable
{
    /**
     * Creates a new executable of this type.
     * 
     * @param rta the execution target
     */
    public BoldExecutable(RichTextArea rta)
    {
        super(rta, Style.FONT_WEIGHT, Style.FontWeight.BOLD, "strong");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ToggleInlineStyleExecutable#matchesStyle(Element)
     */
    protected boolean matchesStyle(Element inputElement)
    {
        String fontWeight = inputElement.getComputedStyleProperty(getProperty().getJSName());
        if (Style.FontWeight.BOLD.equalsIgnoreCase(fontWeight)
            || Style.FontWeight.BOLDER.equalsIgnoreCase(fontWeight)) {
            return true;
        } else {
            try {
                int iFontWeight = Integer.parseInt(fontWeight);
                return iFontWeight > 400;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
