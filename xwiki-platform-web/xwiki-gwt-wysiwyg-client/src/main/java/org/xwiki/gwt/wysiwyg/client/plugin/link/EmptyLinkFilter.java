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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;

/**
 * Client side empty link cleaner, to delete all links without content before the content is submitted. This is to
 * prevent the links which are not visible in the wysiwyg to get submitted as wysiwyg content.
 * 
 * @version $Id$
 */
public class EmptyLinkFilter implements CommandListener
{
    /**
     * The submit rich text area command.
     */
    private static final Command SUBMIT = new Command("submit");

    /**
     * The rich text area for which this command listener cleans the empty listener.
     */
    private RichTextArea rta;

    /**
     * Creates an empty link filter to handle links on the passed rich text area.
     * 
     * @param rta the {@link RichTextArea} to handle empty links for
     */
    public EmptyLinkFilter(RichTextArea rta)
    {
        this.rta = rta;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onBeforeCommand(CommandManager, Command, String)
     */
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        if (SUBMIT.equals(command)) {
            for (Element anchor : getEmptyAnchors()) {
                anchor.getParentNode().removeChild(anchor);
            }
        }
        return false;
    }

    /**
     * @return the list of empty anchors
     * @see #isEmpty(Element)
     */
    private List<Element> getEmptyAnchors()
    {
        List<Element> emptyAnchors = new ArrayList<Element>();
        NodeList<Element> anchorsList = rta.getDocument().getElementsByTagName("a");
        for (int i = 0; i < anchorsList.getLength(); i++) {
            Element anchor = anchorsList.getItem(i);
            if (isEmpty(AnchorElement.as(anchor))) {
                emptyAnchors.add(anchor);
            }
        }
        return emptyAnchors;
    }

    /**
     * @param anchor an anchor element
     * @return {@code true} if the given anchor has a reference and is not visible although its parent is, {@code false}
     *         otherwise
     */
    private boolean isEmpty(AnchorElement anchor)
    {
        // NOTE: Don't test the offsetHeight against 0 because it can be the same as the line height.
        return !StringUtils.isEmpty(anchor.getHref()) && anchor.getOffsetWidth() == 0 && isDisplayed(anchor);
    }

    /**
     * We have to iterate all the ancestor elements and check if each is displayed because the {@code display} CSS
     * property is not inherited.
     * 
     * @param element a DOM element
     * @return {@code true} if the given element is displayed, {@code false} otherwise
     */
    private boolean isDisplayed(Element element)
    {
        Element ancestor = element;
        while (ancestor != null) {
            if (Display.NONE.getCssName().equals(ancestor.getStyle().getDisplay())) {
                return false;
            }
            ancestor = ancestor.getParentElement();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        // nothing
    }
}
