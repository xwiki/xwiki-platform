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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.xwiki.gwt.user.client.Strings;
import org.xwiki.gwt.user.client.ui.ComplexDialogBox;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;

import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wizard dialog class, used to display the current wizard step.
 * 
 * @version $Id$
 */
public class WizardDialog extends ComplexDialogBox implements SourcesNavigationEvents
{
    /**
     * The set of navigation listeners to be handled by this dialog.
     */
    private final NavigationListenerCollection navigationListeners = new NavigationListenerCollection();

    /**
     * The list with all buttons currently displayed on this dialog.
     */
    private final List<Button> buttons = new ArrayList<Button>();

    /**
     * Builds a wizard dialog with the passed title and icon.
     * 
     * @param title the title of this wizard dialog
     * @param icon the icon of this wizard dialog
     */
    public WizardDialog(String title, Image icon)
    {
        super(false, true);
        getDialog().setCaption(title);
        getDialog().setIcon(icon);
        // set style
        addStyleName("xWizardDialog");
    }

    /**
     * Displays the passed wizard step in this dialog.
     * 
     * @param step the step to display in this dialog
     * @param hasPrevious true if the currently displayed step has a previous step in the wizard, false otherwise.
     */
    public void displayStep(WizardStep step, boolean hasPrevious)
    {
        setLoading(false);

        getBody().clear();
        Widget stepDisplay = step.display();
        if (stepDisplay != null) {
            getBody().add(stepDisplay);
        }

        getHeader().clear();
        String stepTitle = step.getStepTitle();
        if (stepTitle != null) {
            Label titleLabel = new Label(stepTitle);
            titleLabel.addStyleName("title");
            getHeader().add(titleLabel);
        }

        getFooter().clear();
        fillButtonsContainers(step, hasPrevious);
    }

    /**
     * Toggles the loading state on the current dialog (busy icon and disabled buttons).
     * 
     * @param loading true if the dialog should enter the loading state, false if it should exit.
     */
    public void setLoading(boolean loading)
    {
        super.setLoading(loading);
        // show or hide the dialog's main widget (the wizard step widget)
        if (getBody().getWidgetCount() > 0) {
            // use the visibility attribute instead of the setVisible method to have the layout of the dialog on errors
            // display correctly recomputed, having elements sizes even is the container is not visible
            getBody().getWidget(0).getElement().getStyle()
                .setVisibility(!loading ? Visibility.VISIBLE : Visibility.HIDDEN);
        }
        // toggle buttons state
        for (Button b : buttons) {
            b.setEnabled(!loading);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Expose the protected {@link ComplexDialogBox#showError(Throwable)} method.
     * 
     * @see ComplexDialogBox#showError(Throwable)
     */
    @Override
    public void showError(Throwable caught)
    {
        // Show only the error message.
        getBody().clear();
        super.showError(caught);
    }

    /**
     * Fill the buttons containers according to the passed wizard step valid directions. The second parameter specifies
     * whether there exists a previous dialog on the current navigation stack. The previous button will be shown only if
     * such exists and {@link NavigationDirection#PREVIOUS} is valid for the current step.
     * 
     * @param step the current wizard step
     * @param hasPrevious true if the previous button can be displayed (i.e. there is a previous step in the displayed
     *            wizard), false otherwise.
     */
    protected void fillButtonsContainers(WizardStep step, boolean hasPrevious)
    {
        buttons.clear();
        EnumSet<NavigationDirection> validDirections = step.getValidDirections();
        // add the buttons
        // add here the cancel button if you want one on the form

        // special handling of the previous button since it needs to be added only if wizard allows it
        if (hasPrevious && validDirections.contains(NavigationDirection.PREVIOUS)) {
            addButton(step, NavigationDirection.PREVIOUS, getFooter(), Strings.INSTANCE.wizardPrevious());
        }

        if (validDirections.contains(NavigationDirection.NEXT)) {
            addButton(step, NavigationDirection.NEXT, getFooter(), Strings.INSTANCE.wizardNext());
        }

        if (validDirections.contains(NavigationDirection.FINISH)) {
            addButton(step, NavigationDirection.FINISH, getFooter(), Strings.INSTANCE.wizardFinish());
        }
    }

    /**
     * Adds the button for the specified direction for the step in the passed container.
     * 
     * @param step the step to process (add buttons for)
     * @param direction the direction of the button to add
     * @param container the UI container in which the button needs to be added
     * @param defaultLabel the default label of the button, if the step does not specify any.
     */
    protected void addButton(WizardStep step, final NavigationDirection direction, Panel container, String defaultLabel)
    {
        String buttonLabel = step.getDirectionName(direction);
        if (buttonLabel == null) {
            buttonLabel = defaultLabel;
        }
        Button button = new Button(buttonLabel);
        button.addClickHandler(new ClickHandler()
        {
            public void onClick(ClickEvent event)
            {
                navigationListeners.fireNavigationEvent(direction);
            }
        });
        container.add(button);
        buttons.add(button);
    }

    @Override
    public void addNavigationListener(NavigationListener listener)
    {
        navigationListeners.add(listener);
    }

    @Override
    public void removeNavigationListener(NavigationListener listener)
    {
        navigationListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Expose the protected {@link ComplexDialogBox#setCanceled(boolean)} method.
     * 
     * @see ComplexDialogBox#setCanceled(boolean)
     */
    @Override
    public void setCanceled(boolean canceled)
    {
        super.setCanceled(canceled);
    }
}
