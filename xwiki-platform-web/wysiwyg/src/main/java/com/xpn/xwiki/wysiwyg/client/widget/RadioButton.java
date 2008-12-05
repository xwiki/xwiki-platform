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
package com.xpn.xwiki.wysiwyg.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

/**
 * Radio button widget to override the default GWT radio button to work around bug #458:
 * http://code.google.com/p/google-web-toolkit/issues/detail?id=458 .
 * 
 * @version $Id$
 */
public class RadioButton extends com.google.gwt.user.client.ui.RadioButton
{
    /**
     * The name of the input element, to set the value attribute on.
     */
    private static final String INPUT_TAG_NAME = "input";

    /**
     * The name of the value attribute to set.
     */
    private static final String VALUE_ATTR_NAME = "value";

    /**
     * Builds a radio button in the specified group with the specified label.
     * 
     * @param name the name of the group of this radio button.
     * @param label the label of the radio button.
     */
    public RadioButton(String name, String label)
    {
        super(name, label);
    }

    /**
     * Sets the value attribute of this radio button.
     * 
     * @param value the value to set for this radio button.
     */
    public void setValue(String value)
    {
        NodeList<Element> innerInputList = this.getElement().getElementsByTagName(INPUT_TAG_NAME);
        if (innerInputList.getLength() > 0) {
            Element wrappedInput = innerInputList.getItem(0);
            wrappedInput.setAttribute(VALUE_ATTR_NAME, value);
        }
    }

    /**
     * @return the value attribute of this radio button.
     */
    public String getValue()
    {
        NodeList<Element> innerInputList = this.getElement().getElementsByTagName(INPUT_TAG_NAME);
        if (innerInputList.getLength() > 0) {
            Element wrappedInput = innerInputList.getItem(0);
            return wrappedInput.getAttribute(VALUE_ATTR_NAME);
        }
        return "";
    }
}
