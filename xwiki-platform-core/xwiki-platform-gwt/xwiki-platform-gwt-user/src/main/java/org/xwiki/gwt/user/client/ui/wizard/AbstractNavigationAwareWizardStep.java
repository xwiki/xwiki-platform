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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;

/**
 * An abstract wizard step that is aware of the wizard navigation.
 * 
 * @version $Id$
 */
public abstract class AbstractNavigationAwareWizardStep implements WizardStep
{
    /**
     * The map used to associate a name to a navigation direction.
     */
    private Map<NavigationDirection, String> directionName = new HashMap<NavigationDirection, String>();

    /**
     * The name of the next wizard step.
     */
    private String nextStep;

    /**
     * The set of valid directions the wizard can go to from this wizard step.
     */
    private EnumSet<NavigationDirection> validDirections = EnumSet.allOf(NavigationDirection.class);

    @Override
    public String getDirectionName(NavigationDirection direction)
    {
        return directionName.get(direction);
    }

    /**
     * Sets the name for a navigation direction.
     * 
     * @param direction a wizard navigation direction
     * @param name a string that will be used on the UI to indicate the specified direction
     * @return the previous name associated with the given direction
     */
    public String setDirectionName(NavigationDirection direction, String name)
    {
        return directionName.put(direction, name);
    }

    @Override
    public String getNextStep()
    {
        return nextStep;
    }

    /**
     * Sets the name of the next wizard step.
     * 
     * @param nextStep the name of the next wizard step
     */
    public void setNextStep(String nextStep)
    {
        this.nextStep = nextStep;
    }

    @Override
    public EnumSet<NavigationDirection> getValidDirections()
    {
        return validDirections;
    }

    /**
     * Sets the valid directions the wizard can go to from this wizard step.
     * 
     * @param validDirections the valid directions the wizard can go to from this wizard step
     */
    public void setValidDirections(EnumSet<NavigationDirection> validDirections)
    {
        this.validDirections = validDirections;
    }
}
