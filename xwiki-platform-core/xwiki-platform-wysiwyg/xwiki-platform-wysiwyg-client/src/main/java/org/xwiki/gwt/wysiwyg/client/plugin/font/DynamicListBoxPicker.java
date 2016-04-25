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
package org.xwiki.gwt.wysiwyg.client.plugin.font;

import org.xwiki.gwt.user.client.StringUtils;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

/**
 * A list box picker that extends itself with the items that are not found in the list.
 * 
 * @version $Id$
 */
public class DynamicListBoxPicker extends AbstractListBoxPicker
{
    /**
     * The label of the additional option group.
     */
    private String additionalOptionGroupLabel;

    @Override
    protected void setSelectedValue(String value, Matcher<String> matcher)
    {
        super.setSelectedValue(value, matcher);
        if (getSelectedIndex() < 0 && !StringUtils.isEmpty(value)) {
            // None of the exiting items match the given value. Extend the list.
            Element additionalGroup = getAdditionalOptionGroup();
            addItem(value);
            additionalGroup.appendChild(getElement().getLastChild());
            setSelectedIndex(getItemCount() - 1);
        }
    }

    /**
     * @return the additional option group, which contains all the options that were added dynamically
     */
    private Element getAdditionalOptionGroup()
    {
        NodeList<Element> groups = getElement().getElementsByTagName("optgroup");
        if (groups.getLength() > 0) {
            // The last group should be the additional group.
            return groups.getItem(groups.getLength() - 1);
        } else {
            Element additionalGroup = getElement().getOwnerDocument().createOptGroupElement();
            additionalGroup.setAttribute("label", getAdditionalOptionGroupLabel());
            getElement().appendChild(additionalGroup);
            return additionalGroup;
        }
    }

    /**
     * @return the label of the additional option group
     */
    public String getAdditionalOptionGroupLabel()
    {
        return additionalOptionGroupLabel;
    }

    /**
     * Sets the label of the additional option group.
     * 
     * @param additionalOptionGroupLabel the new label
     */
    public void setAdditionalOptionGroupLabel(String additionalOptionGroupLabel)
    {
        this.additionalOptionGroupLabel = additionalOptionGroupLabel;
    }
}
