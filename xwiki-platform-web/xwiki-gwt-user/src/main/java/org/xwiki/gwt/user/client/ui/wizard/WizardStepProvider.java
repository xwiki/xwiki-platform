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

/**
 * Component which handles the instantiation and resolution of wizard steps based on the step names. Will be used by the
 * {@link Wizard} to determine the next {@link WizardStep} based on the name returned by the current dialog's
 * {@link WizardStep#getNextStep()}.
 * 
 * @version $Id$
 */
public interface WizardStepProvider
{
    /**
     * Returns the {@link WizardStep} corresponding to the passed name.
     * 
     * @param name the name of the {@link WizardStep} to find
     * @return the instance of the {@link WizardStep} corresponding to the passed name or null if no such step exists.
     */
    WizardStep getStep(String name);
}
