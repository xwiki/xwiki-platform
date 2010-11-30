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
import java.util.Set;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.BlockStyleExecutable;

import com.google.gwt.dom.client.Node;

/**
 * Applies a given style name to each of the block nodes touched by the current text selection.
 * 
 * @version $Id$
 */
public class BlockStyleNameExecutable extends BlockStyleExecutable
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
    public BlockStyleNameExecutable(RichTextArea rta)
    {
        super(rta, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see BlockStyleExecutable#execute(String)
     */
    @Override
    public boolean execute(String parameter)
    {
        executed = getStyleNames(rta.getDocument().getSelection()).contains(parameter);
        return super.execute(parameter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see BlockStyleExecutable#execute(Node, int, int, String)
     */
    @Override
    protected void execute(Node node, int startOffset, int endOffset, String value)
    {
        if (executed) {
            removeStyleName(node, value);
        } else if (!matchesStyleName(node, value)) {
            super.execute(node, startOffset, endOffset, value);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see BlockStyleExecutable#addStyle(Element, String)
     */
    @Override
    protected void addStyle(Element element, String styleName)
    {
        element.addClassName(styleName);
    }

    /**
     * Removes the specified style name from the block-level ancestors of the given DOM node.
     * 
     * @param node a DOM node
     * @param styleName the style name to be removed
     */
    protected void removeStyleName(Node node, String styleName)
    {
        Node ancestor = domUtils.getNearestBlockContainer(node);
        while (ancestor != null && ancestor.getNodeType() == Node.ELEMENT_NODE) {
            Element.as(ancestor).removeClassName(styleName);
            ancestor = ancestor.getParentNode();
        }
    }

    /**
     * @param node a DOM node
     * @param styleName a style name
     * @return {@code true} if any of the block-level ancestors of the given node has the specified style name, {@code
     *         false} otherwise
     */
    protected boolean matchesStyleName(Node node, String styleName)
    {
        Node ancestor = domUtils.getNearestBlockContainer(node);
        while (ancestor != null && ancestor.getNodeType() == Node.ELEMENT_NODE) {
            if (Element.as(ancestor).hasClassName(styleName)) {
                return true;
            }
            ancestor = ancestor.getParentNode();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see BlockStyleExecutable#getParameter()
     */
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
     * @return the set of style names that affect all the leafs in the given DOM range
     */
    protected Set<String> getStyleNames(Range range)
    {
        Node leaf = domUtils.getFirstLeaf(range);
        if (leaf == null) {
            // The range is collapsed between nodes.
            return getStyleNames(range.getStartContainer());
        } else {
            Node lastLeaf = domUtils.getLastLeaf(range);
            Set<String> rangeStyleNames = getStyleNames(leaf);
            while (leaf != lastLeaf && !rangeStyleNames.isEmpty()) {
                leaf = domUtils.getNextLeaf(leaf);
                rangeStyleNames.retainAll(getStyleNames(leaf));
            }
            return rangeStyleNames;
        }
    }

    /**
     * @param node a DOM node
     * @return the set of style names that affect the block-level ancestors of the given DOM node
     */
    protected Set<String> getStyleNames(Node node)
    {
        Set<String> styleNames = new HashSet<String>();
        Node ancestor = domUtils.getNearestBlockContainer(node);
        while (ancestor != null && ancestor.getNodeType() == Node.ELEMENT_NODE) {
            String className = Element.as(ancestor).getClassName();
            if (!StringUtils.isEmpty(className)) {
                styleNames.addAll(Arrays.asList(className.split("\\s+")));
            }
            ancestor = ancestor.getParentNode();
        }
        return styleNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see BlockStyleExecutable#isExecuted()
     */
    @Override
    public boolean isExecuted()
    {
        // Always return true because we cannot prove the contrary (and it's not important).
        return true;
    }
}
