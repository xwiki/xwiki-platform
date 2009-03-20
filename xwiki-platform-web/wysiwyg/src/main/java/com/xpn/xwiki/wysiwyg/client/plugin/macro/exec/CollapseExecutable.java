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
package com.xpn.xwiki.wysiwyg.client.plugin.macro.exec;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.Element;

import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroSelector;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.AbstractExecutable;

/**
 * Collapses or expands the selected macros or all the macros present in the edited document if no macros are selected.
 * 
 * @version $Id$
 */
public class CollapseExecutable extends AbstractExecutable
{
    /**
     * The selector used to query for the currently selected macros. It also holds the displayer used to collapse and
     * expand macros.
     */
    private final MacroSelector selector;

    /**
     * Flag that specifies if the macros are collapsed or expanded.
     */
    private final boolean collapse;

    /**
     * Creates a new executable.
     * 
     * @param selector {@link #selector}
     * @param collapse {@code true} to collapse all macros, {@code false} to expand all macros
     */
    public CollapseExecutable(MacroSelector selector, boolean collapse)
    {
        this.selector = selector;
        this.collapse = collapse;
    }

    /**
     * @return the list of selected macros if there are any, otherwise the list of all the macros present in the
     *         underlying rich text area
     */
    private List<Element> getMacros()
    {
        if (selector.getMacroCount() > 0) {
            List<Element> macros = new ArrayList<Element>();
            for (int i = 0; i < selector.getMacroCount(); i++) {
                macros.add(selector.getMacro(i));
            }
            return macros;
        } else {
            Element body = (Element) selector.getDisplayer().getTextArea().getDocument().getBody().cast();
            return selector.getDisplayer().getMacroContainers(body);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        for (Element macro : getMacros()) {
            selector.getDisplayer().setCollapsed(macro, collapse);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        for (Element macro : getMacros()) {
            if (selector.getDisplayer().isCollapsed(macro) != collapse) {
                return false;
            }
        }
        return true;
    }
}
