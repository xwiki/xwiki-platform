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
package org.xwiki.gwt.user.client.ui;

import org.xwiki.gwt.user.client.StringUtils;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.TextBox;

/**
 * A labeled text box. The label is displayed inside the text box whenever the text box is empty and not focused.
 * 
 * @version $Id$
 */
public class LabeledTextBox extends TextBox implements FocusHandler, BlurHandler
{
    /**
     * The style name applied when the {@link #textBox} is labeled.
     */
    private static final String DEPENDENT_STYLE_NAME_LABELED = "labeled";

    /**
     * The label of the text box.
     */
    private final String label;

    /**
     * Indicates if this text box is labeled or not.
     */
    private boolean labeled;

    /**
     * Creates a new labeled text box.
     * 
     * @param label the label of the newly created text box
     */
    public LabeledTextBox(String label)
    {
        this.label = label;
        setLabeled(true);
        setTitle(label);

        addFocusHandler(this);
        addBlurHandler(this);
    }

    @Override
    public void onFocus(FocusEvent event)
    {
        if (event.getSource() == this) {
            if (labeled) {
                setLabeled(false);
            }
            selectAll();
        }
    }

    @Override
    public void onBlur(BlurEvent event)
    {
        if (event.getSource() == this && StringUtils.isEmpty(super.getText())) {
            setLabeled(true);
        }
    }

    /**
     * Adds or removes the label based on the given flag.
     * 
     * @param labeled {@code true} to add the label, {@code false} to remove it
     */
    private void setLabeled(boolean labeled)
    {
        this.labeled = labeled;
        if (labeled) {
            setValue(label);
            addStyleDependentName(DEPENDENT_STYLE_NAME_LABELED);
        } else {
            setValue("");
            removeStyleDependentName(DEPENDENT_STYLE_NAME_LABELED);
        }
    }

    @Override
    public String getText()
    {
        return labeled ? "" : super.getText();
    }
}
