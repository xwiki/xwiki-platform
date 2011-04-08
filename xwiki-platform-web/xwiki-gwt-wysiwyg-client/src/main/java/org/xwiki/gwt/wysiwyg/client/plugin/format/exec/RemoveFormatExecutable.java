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
package org.xwiki.gwt.wysiwyg.client.plugin.format.exec;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Text;
import org.xwiki.gwt.dom.client.TextFragment;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.ToggleInlineStyleExecutable;

import com.google.gwt.dom.client.Node;

/**
 * Removes the in-line style from the current selection.
 * 
 * @version $Id$
 */
public class RemoveFormatExecutable extends ToggleInlineStyleExecutable
{
    /**
     * The property of the style object which holds the value of the style attribute.
     */
    public static final String CSS_TEXT = "cssText";

    /**
     * The list of HTML tags that shouldn't be split while removing the in-line style. Don't include the {@code span}
     * tag in this list!
     */
    public static final List<String> DO_NOT_SPLIT = Arrays.asList("a");

    /**
     * Creates a new executable that can be used to remove the in-line style from the current text selection.
     * 
     * @param rta the execution target
     */
    public RemoveFormatExecutable(RichTextArea rta)
    {
        // We remove all the in-line CSS properties and all the formatting tags so there's no need for a specific
        // property or a specific tag name.
        super(rta, null, null, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ToggleInlineStyleExecutable#removeStyle(Text, int, int)
     */
    protected TextFragment removeStyle(Text text, int firstCharIndex, int lastCharIndex)
    {
        // Make sure we remove the style only from the selected text.
        text.crop(firstCharIndex, lastCharIndex);

        Stack<Node> stack = getInlineAncestorsStack(text);
        // top is the first ancestor that was split; all the ancestors that can't be split are moved before the top.
        Node top = null;
        while (stack.size() > 1) {
            Node parent = stack.pop();
            if (DO_NOT_SPLIT.contains(parent.getNodeName().toLowerCase())) {
                // If we can't split the parent then we move its in-line style down to its children.
                if (splitParenStyle(stack.peek())) {
                    stack.push(stack.peek().getParentNode());
                }
                if (top != null) {
                    // Move parent to the top.
                    reorder(top, stack.peek());
                }
            } else if (top == null) {
                domUtils.isolate(stack.peek());
                top = parent;
            } else {
                isolateUpTo(stack.peek(), top);
            }
        }

        // Remove the in-line style from the selected text.
        if (top != null) {
            top.getParentNode().replaceChild(text, top);
        }

        return new TextFragment(text, 0, text.getLength());
    }

    /**
     * Computes the stack of in-line ancestors, starting with the given node and ending with its top most in-line
     * ancestor.
     * 
     * @param node a DOM node
     * @return the stack of in-line ancestors of the given node
     */
    protected Stack<Node> getInlineAncestorsStack(Node node)
    {
        Stack<Node> stack = new Stack<Node>();
        Node ancestor = node;
        while (ancestor != null && domUtils.isInline(ancestor)) {
            stack.push(ancestor);
            ancestor = ancestor.getParentNode();
        }
        return stack;
    }

    /**
     * Removes the in-line style from the parent of the given node and applies it to the given node's siblings.
     * 
     * @param child a DOM node
     * @return {@code true} if the in-line style has been split, {@code false} otherwise
     */
    protected boolean splitParenStyle(Node child)
    {
        Element parent = (Element) child.getParentNode();
        if (parent == null || StringUtils.isEmpty(parent.getStyle().getProperty(CSS_TEXT))) {
            return false;
        }

        // Group the left siblings and apply the in-line style to the whole group.
        if (child.getPreviousSibling() != null) {
            Element left = child.getOwnerDocument().createSpanElement().cast();
            left.appendChild(child.getPreviousSibling());
            while (child.getPreviousSibling() != null) {
                left.insertBefore(child.getPreviousSibling(), left.getFirstChild());
            }
            left.getStyle().setProperty(CSS_TEXT, parent.getStyle().getProperty(CSS_TEXT));
            parent.insertBefore(left, child);
        }

        // Group the right siblings and apply the style to the whole group.
        if (child.getNextSibling() != null) {
            Element right = child.getOwnerDocument().createSpanElement().cast();
            do {
                right.appendChild(child.getNextSibling());
            } while (child.getNextSibling() != null);
            right.getStyle().setProperty(CSS_TEXT, parent.getStyle().getProperty(CSS_TEXT));
            parent.appendChild(right);
        }

        // Wrap the child node and apply the in-line style to the wrapper.
        Element wrapper = child.getOwnerDocument().createSpanElement().cast();
        wrapper.getStyle().setProperty(CSS_TEXT, parent.getStyle().getProperty(CSS_TEXT));
        parent.replaceChild(wrapper, child);
        wrapper.appendChild(child);

        // Remove the in-line style from the parent.
        parent.removeAttribute("style");

        return true;
    }

    /**
     * Moves the parent of the given child node before the specified top and replicates three times the ancestors up to
     * the top: once for the left siblings of the given child node, once the child node itself and once for the right
     * siblings of the given child node.
     * 
     * @param top the ancestor before which the parent is moved
     * @param child the child node whose parent is moved before the top
     */
    protected void reorder(Node top, Node child)
    {
        Node parent = child.getParentNode();
        if (parent == null || parent == top) {
            return;
        }

        Node grandParent = parent.getParentNode();
        if (grandParent == null) {
            return;
        }

        int index = domUtils.getNodeIndex(parent);
        grandParent.removeChild(parent);

        if (child.getPreviousSibling() != null) {
            Node left = top.cloneNode(true);
            Node leaf = domUtils.getLastLeaf(left);
            leaf.appendChild(child.getPreviousSibling());
            while (child.getPreviousSibling() != null) {
                leaf.insertBefore(child.getPreviousSibling(), leaf.getFirstChild());
            }
            parent.insertBefore(left, child);
        }

        if (child.getNextSibling() != null) {
            Node right = top.cloneNode(true);
            Node leaf = domUtils.getFirstLeaf(right);
            do {
                leaf.appendChild(child.getNextSibling());
            } while (child.getNextSibling() != null);
            parent.appendChild(right);
        }

        top.getParentNode().replaceChild(parent, top);
        parent.replaceChild(top, child);
        domUtils.insertAt(grandParent, child, index);
    }

    /**
     * Isolates the ancestors of the given child node up to the specified top ancestor.
     * 
     * @param child the child node whose ancestors will be isolated
     * @param top the top most ancestor that will be isolated
     */
    protected void isolateUpTo(Node child, Node top)
    {
        Node ancestor = child;
        do {
            domUtils.isolate(ancestor);
            ancestor = ancestor.getParentNode();
        } while (ancestor != null && ancestor != top);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ToggleInlineStyleExecutable#isExecuted()
     */
    public boolean isExecuted()
    {
        // NOTE: This is just a trick that forces removeStyle to be called each time execute is called. Returning false
        // all the time is not better so we keep it like this for now.
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ToggleInlineStyleExecutable#getParameter()
     */
    public String getParameter()
    {
        // No parameter.
        return null;
    }
}
