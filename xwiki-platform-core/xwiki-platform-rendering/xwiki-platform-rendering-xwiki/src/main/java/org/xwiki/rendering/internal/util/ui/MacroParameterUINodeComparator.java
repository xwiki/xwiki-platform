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
package org.xwiki.rendering.internal.util.ui;

import java.util.Comparator;

public class MacroParameterUINodeComparator implements Comparator<MacroParameterUINode>
{
    @Override
    public int compare(MacroParameterUINode node1, MacroParameterUINode node2)
    {
        if (node1.getOrder() != -1 && node2.getOrder() != -1) {
            return Integer.compare(node1.getOrder(), node2.getOrder());
        } else if (node1.getOrder() == -1 && node2.getOrder() == -1) {
            if (node1.isAdvanced() == node2.isAdvanced() && node1.isHidden() == node2.isHidden()) {
                return node1.getId().compareTo(node2.getId());
            } else if (node1.isAdvanced() || node1.isHidden()) {
                return 1;
            } else {
                return -1;
            }
        } else if (node1.getOrder() == -1) {
            return 1;
        } else {
            return -1;
        }
    }
}
