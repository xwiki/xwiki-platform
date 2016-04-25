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
package org.xwiki.gwt.wysiwyg.client.plugin.style.exec;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.Text;
import org.xwiki.gwt.dom.client.TextFragment;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.InlineStyleExecutable;

import com.google.gwt.dom.client.Node;

/**
 * Applies a given style name to each of the text nodes from the current text selection.
 * 
 * @version $Id$
 */
public class InlineStyleNameExecutable extends InlineStyleExecutable
{
    /**
     * Flag indicating if this executable has been executed on the current selection with the given parameter.
     */
    private boolean executed;

    /**
     * Creates a new instance.
     * 
     * @param rta the execution target
     */
    public InlineStyleNameExecutable(RichTextArea rta)
    {
        super(rta, null);
    }

    @Override
    public boolean execute(String parameter)
    {
        executed = getStyleNames(rta.getDocument().getSelection()).contains(parameter);
        return super.execute(parameter);
    }

    @Override
    protected TextFragment execute(Text text, int startIndex, int endIndex, String parameter)
    {
        if (executed) {
            return removeStyleName(text, startIndex, endIndex, parameter);
        } else if (!matchesStyleName(text, parameter)) {
            return super.execute(text, startIndex, endIndex, parameter);
        } else {
            return new TextFragment(text, startIndex, endIndex);
        }
    }

    @Override
    protected void addStyle(Element element, String parameter)
    {
        element.addClassName(parameter);
    }

    /**
     * @param text a text node
     * @param parameter a style name
     * @return {@code true} if any of the in-line ancestors of the given text node has the specified style name, {@code
     *         false} otherwise
     */
    protected boolean matchesStyleName(Text text, String parameter)
    {
        Node ancestor = text.getParentNode();
        while (ancestor != null && ancestor.getNodeType() == Node.ELEMENT_NODE && domUtils.isInline(ancestor)) {
            if (Element.as(ancestor).hasClassName(parameter)) {
                return true;
            }
            ancestor = ancestor.getParentNode();
        }
        return false;
    }

    /**
     * Removes the give style name from all the in-line element ancestors of the given text node, making sure no other
     * nodes are affected by this change (by isolating the nodes while iterating the ancestors).
     * 
     * @param text the target text node
     * @param startIndex the first character on which we remove the style name
     * @param endIndex the last character on which we remove the style name
     * @param styleName the style name to be removed
     * @return the text fragment where the given style name doesn't apply anymore
     */
    protected TextFragment removeStyleName(Text text, int startIndex, int endIndex, String styleName)
    {
        // Make sure we remove the style name only from the selected text.
        text.crop(startIndex, endIndex);

        // Remove the given style name from all the in-line ancestors.
        Node child = text;
        Node parent = child.getParentNode();
        while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE && domUtils.isInline(parent)) {
            domUtils.isolate(child);
            Element.as(parent).removeClassName(styleName);
            child = child.getParentNode();
            parent = child.getParentNode();
        }

        return new TextFragment(text, 0, text.getLength());
    }

    @Override
    public String getParameter()
    {
        return StringUtils.join(getStyleNames(rta.getDocument().getSelection()), " ");
    }

    /**
     * @param selection a text selection
     * @return the set of style names that affect all the text nodes touched by the given text selection
     */
    protected Set<String> getStyleNames(Selection selection)
    {
        Set<String> selectionStyleNames = null;
        for (int i = 0; i < selection.getRangeCount(); i++) {
            Set<String> rangeStyleNames = getStyleNames(selection.getRangeAt(i));
            if (selectionStyleNames == null) {
                selectionStyleNames = rangeStyleNames;
            } else {
                selectionStyleNames.retainAll(rangeStyleNames);
            }
            if (selectionStyleNames.isEmpty()) {
                break;
            }
        }
        if (selectionStyleNames == null) {
            selectionStyleNames = Collections.emptySet();
        }
        return selectionStyleNames;
    }

    /**
     * @param range a text range
     * @return the set of style names that affect all the text nodes touched by the given text range
     */
    protected Set<String> getStyleNames(Range range)
    {
        if (range.isCollapsed()) {
            return getStyleNames(range.getStartContainer());
        } else {
            List<Text> textNodes = getNonEmptyTextNodes(range);
            Set<String> rangeStyleNames = null;
            for (int i = 0; i < textNodes.size(); i++) {
                Set<String> textStyleNames = getStyleNames(textNodes.get(i));
                if (rangeStyleNames == null) {
                    rangeStyleNames = textStyleNames;
                } else {
                    rangeStyleNames.retainAll(textStyleNames);
                }
                if (rangeStyleNames.isEmpty()) {
                    break;
                }
            }
            if (rangeStyleNames == null) {
                rangeStyleNames = Collections.emptySet();
            }
            return rangeStyleNames;
        }
    }

    /**
     * @param node a DOM node
     * @return the set of style names that affect the given DOM node
     */
    protected Set<String> getStyleNames(Node node)
    {
        Set<String> styleNames = new HashSet<String>();
        Node ancestor = node.getParentElement();
        while (ancestor != null && ancestor.getNodeType() == Node.ELEMENT_NODE && domUtils.isInline(ancestor)) {
            String className = Element.as(ancestor).getClassName();
            if (!StringUtils.isEmpty(className)) {
                styleNames.addAll(Arrays.asList(className.split("\\s+")));
            }
            ancestor = ancestor.getParentNode();
        }
        return styleNames;
    }

    @Override
    public boolean isExecuted()
    {
        // Always return true because we cannot prove the contrary (and it's not important).
        return true;
    }
}
