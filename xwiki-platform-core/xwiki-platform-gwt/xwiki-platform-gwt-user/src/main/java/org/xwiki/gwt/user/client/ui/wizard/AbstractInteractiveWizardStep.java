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
package org.xwiki.gwt.user.client.ui.wizard;

import com.google.gwt.user.client.ui.FlowPanel;

/**
 * An abstract wizard step that requires user input in order to move forward.
 * 
 * @version $Id$
 */
public abstract class AbstractInteractiveWizardStep extends AbstractNavigationAwareWizardStep
{
    /**
     * The step title.
     */
    private String title;

    /**
     * The panel holding this step's widgets.
     */
    private final FlowPanel panel;

    /**
     * Creates a new composite wizard step.
     */
    public AbstractInteractiveWizardStep()
    {
        this(new FlowPanel());
    }

    /**
     * Creates a new interactive wizard step that uses the given panel to hold its widgets.
     * 
     * @param panel the panel where this wizard step will add its widgets
     */
    public AbstractInteractiveWizardStep(FlowPanel panel)
    {
        this.panel = panel;
    }

    /**
     * {@inheritDoc}
     * <p>
     * You can safely use this method to access the panel and add widget to it.
     * 
     * @see AbstractNavigationAwareWizardStep#display()
     */
    @Override
    public FlowPanel display()
    {
        return panel;
    }

    @Override
    public final boolean isAutoSubmit()
    {
        // This step has a panel for a reason.
        return false;
    }

    @Override
    public String getStepTitle()
    {
        return title;
    }

    /**
     * Sets the step title.
     * 
     * @param title the new step title
     */
    public void setStepTitle(String title)
    {
        this.title = title;
    }
}
