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
package org.xwiki.wysiwyg.internal.macro;

import java.util.Comparator;

import org.xwiki.wysiwyg.macro.AbstractMacroUINode;
import org.xwiki.wysiwyg.macro.MacroUINodeParameter;

/**
 * Comparator for ordering {@link AbstractMacroUINode}.
 * The algorithm for ordering is to consider first order given by {@link AbstractMacroUINode#getOrder()} and if no
 * order is given, to compute order based on hidden, advanced or deprecated field, before falling back on identifier.
 *
 * @version $Id$
 * @since 17.5.0
 */
public class MacroUINodeComparator implements Comparator<AbstractMacroUINode>
{
    @Override
    public int compare(AbstractMacroUINode node1, AbstractMacroUINode node2)
    {
        // we don't want to use Integer.compare if the order are exactly the same as it will return 0 and the TreeSet
        // will consider them equal.
        if (node1.getOrder() != -1 && node2.getOrder() != -1 && node1.getOrder() != node2.getOrder()) {
            return Integer.compare(node1.getOrder(), node2.getOrder());
        } else if (node1.getOrder() == -1 && node2.getOrder() == -1) {
            return compareWhenNoOrder(node1, node2);
        } else if (node1.getOrder() == -1) {
            return 1;
        } else {
            return -1;
        }
    }

    private int compareWhenNoOrder(AbstractMacroUINode node1, AbstractMacroUINode node2)
    {
        if (node1 instanceof MacroUINodeParameter paramNode1
            && node2 instanceof MacroUINodeParameter paramNode2) {
            return compareMacroUINodeParametersWhenNoOrder(paramNode1, paramNode2);
        } else {
            if (node1.isHidden() == node2.isHidden()) {
                return node1.getId().compareTo(node2.getId());
            } else if (node1.isHidden()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    private int compareMacroUINodeParametersWhenNoOrder(MacroUINodeParameter paramNode1,
        MacroUINodeParameter paramNode2)
    {
        if (isFirstParamHidden(paramNode1, paramNode2)
            || isFirstParamDeprecated(paramNode1, paramNode2)
            || isFirstParamAdvanced(paramNode1, paramNode2)) {
            return 1;
        } else if (isFirstParamHidden(paramNode2, paramNode1)
            || isFirstParamDeprecated(paramNode2, paramNode1)
            || isFirstParamAdvanced(paramNode2, paramNode1)) {
            return -1;
        } else {
            return paramNode1.getId().compareTo(paramNode2.getId());
        }
    }

    private boolean isFirstParamHidden(MacroUINodeParameter node1, MacroUINodeParameter node2)
    {
        return node1.isHidden() && !node2.isHidden();
    }

    private boolean isFirstParamDeprecated(MacroUINodeParameter node1, MacroUINodeParameter node2)
    {
        return node1.isDeprecated() && !node2.isDeprecated();
    }

    private boolean isFirstParamAdvanced(MacroUINodeParameter node1, MacroUINodeParameter node2)
    {
        return node1.isAdvanced() && !node2.isAdvanced();
    }
}
