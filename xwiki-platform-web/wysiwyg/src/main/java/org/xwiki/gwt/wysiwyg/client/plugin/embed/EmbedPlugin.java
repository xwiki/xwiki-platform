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
package org.xwiki.gwt.wysiwyg.client.plugin.embed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.InnerHTMLListener;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;

/**
 * Handles objects embedded in the rich text area.
 * 
 * @version $Id$
 */
public class EmbedPlugin extends AbstractPlugin implements CommandListener, InnerHTMLListener
{
    /**
     * The command that notifies us when the content of the rich text area has been reset.
     */
    public static final Command RESET = new Command("reset");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        getTextArea().getCommandManager().addCommandListener(this);
        getTextArea().getDocument().addInnerHTMLListener(this);

        // Replace the initial embedded objects.
        replaceEmbeddedObjects(getTextArea().getDocument().getBody());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        getTextArea().getCommandManager().removeCommandListener(this);
        getTextArea().getDocument().removeInnerHTMLListener(this);

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onBeforeCommand(CommandManager, Command, String)
     */
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        // Do nothing.
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        if (RESET.equals(command)) {
            replaceEmbeddedObjects(getTextArea().getDocument().getBody());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InnerHTMLListener#onInnerHTMLChange(org.xwiki.gwt.dom.client.Element)
     */
    public void onInnerHTMLChange(org.xwiki.gwt.dom.client.Element element)
    {
        replaceEmbeddedObjects(element);
    }

    /**
     * Replaces with static image place-holders the objects embedded in the DOM subtree specified by the given root
     * node.
     * 
     * @param root the subtree where to look for embedded objects
     */
    private void replaceEmbeddedObjects(Element root)
    {
        for (Element embed : getEmbeddedObjects(root)) {
            replaceEmbeddedObject(embed);
        }
    }

    /**
     * Replaces the given embedded object with a static image place-holder of the same size.
     * 
     * @param embed the embedded object to be replaced
     */
    private void replaceEmbeddedObject(Element embed)
    {
        // Create the place-holder.
        ImageElement placeHolder = embed.getOwnerDocument().createImageElement();
        placeHolder.setSrc(GWT.getModuleBaseURL() + "clear.cache.gif");
        placeHolder.setAlt(Strings.INSTANCE.embeddedObject());
        placeHolder.setTitle(placeHolder.getAlt());
        placeHolder.setClassName("xEmbeddedObject");
        // We can't rely on the offsetWidth and offsetHeight because the embedded object is replaced before being
        // displayed (even before it is loaded). Also the embedded object is not redisplayed after it is detached and
        // re-attached (e.g. undo operation). We use the width and height specified on the embedded object.
        setSize(placeHolder, Style.WIDTH, embed.getPropertyString(Style.WIDTH));
        setSize(placeHolder, Style.HEIGHT, embed.getPropertyString(Style.HEIGHT));

        // Replace the embedded object.
        embed.getParentNode().replaceChild(placeHolder, embed);

        // Set the place-holder meta data to ensure the embedded object is restored when the DOM document is serialized.
        DocumentFragment metadata = getTextArea().getDocument().createDocumentFragment();
        metadata.appendChild(embed);
        org.xwiki.gwt.dom.client.Element.as(placeHolder).setMetaData(metadata);
    }

    /**
     * @param root the subtree where to look for embedded objects
     * @return the list of objects embedded in the specified subtree
     */
    private List<Element> getEmbeddedObjects(Element root)
    {
        return getMaximalElementsByTagName(root, "object", "embed");
    }

    /**
     * Sets the width or height of an element to the specified value.
     * 
     * @param element the element whose width or height is going to be set
     * @param property either {@code width} or {@code height}
     * @param value the new size
     */
    private void setSize(Element element, String property, String value)
    {
        if (!StringUtils.isEmpty(value)) {
            try {
                // If the unit is not specified we consider it to be pixel.
                element.getStyle().setPropertyPx(property, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                // Apply the value as it is (not necessarily in pixels).
                element.getStyle().setProperty(property, value);
            }
        }
    }

    /**
     * Computes the list of maximal element relative to the given list of tag names. An element is maximal relative to a
     * list of tag names if its tag name is included in the list and none of its ancestors has a tag name from the list.
     * 
     * @param root the root of the subtree where to look for maximal elements with the specified tag names
     * @param tagNames the list of tag names to look for
     * @return the list of maximal elements that match any of the given tag name
     */
    private List<Element> getMaximalElementsByTagName(Element root, String... tagNames)
    {
        List<String> tagNameList = Arrays.asList(tagNames);
        List<Element> maximalElements = new ArrayList<Element>();
        Node node = root;
        while (true) {
            // Check if the current node matches any of the tag names.
            if (node.getNodeType() == Node.ELEMENT_NODE && tagNameList.contains(node.getNodeName().toLowerCase())) {
                maximalElements.add(Element.as(node));
            } else if (node.hasChildNodes()) {
                // Go down only if the current node didn't match any of the tag names.
                node = node.getFirstChild();
                continue;
            }
            // Find the next node on the right, without passing through the root.
            while (node != root && node.getNextSibling() == null) {
                node = node.getParentNode();
            }
            if (node != root) {
                node = node.getNextSibling();
            } else {
                // We returned to the root.
                break;
            }
        }
        return maximalElements;
    }
}
