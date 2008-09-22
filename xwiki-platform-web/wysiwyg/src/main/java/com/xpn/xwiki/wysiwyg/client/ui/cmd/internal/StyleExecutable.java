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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Element;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.Selection;
import com.xpn.xwiki.wysiwyg.client.selection.SelectionManager;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Executable;
import com.xpn.xwiki.wysiwyg.client.util.DOMUtils;

public class StyleExecutable extends DefaultExecutable
{
    private final String tagName;

    private final String className;

    private final String propertyName;

    private final String propertyValue;

    public StyleExecutable(String tagName, String className, String propertyName, String propertyValue, String command)
    {
        super(command);

        this.tagName = tagName;
        this.className = className;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public String getTagName()
    {
        return tagName;
    }

    public String getClassName()
    {
        return className;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public String getProeprtyValue()
    {
        return propertyValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#execute(Element, String)
     */
    public boolean execute(Element target, String parameter)
    {
        return super.execute(target, parameter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(Element)
     */
    public boolean isExecuted(Element target)
    {
        Selection sel = SelectionManager.INSTANCE.getSelection(IFrameElement.as(target));
        if (sel.getRangeCount() > 0) {
            Range range = sel.getRangeAt(0);
            if (range.isCollapsed() || range.getCommonAncestorContainer().getNodeType() == Node.TEXT_NODE
                || range.getStartContainer() == range.getEndContainer()) {
                return matchesStyle(range.getCommonAncestorContainer());
            } else {
                Node node = range.getStartContainer();
                if (!matchesStyle(node)) {
                    return false;
                }
                while (node != range.getEndContainer()) {
                    node = DOMUtils.getInstance().getNextLeaf(node);
                    if (!matchesStyle(node)) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return super.isExecuted(target);
        }
    }

    protected boolean matchesStyle(Node node)
    {
        if (node.getNodeType() == Node.TEXT_NODE) {
            node = node.getParentNode();
        }
        return DOMUtils.getInstance().getComputedStyleProperty((Element) node, propertyName).equalsIgnoreCase(
            propertyValue);
    }
}
