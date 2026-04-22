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
package org.xwiki.distributionwizard;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Step of the distribution wizard.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Unstable
@Role
public interface DistributionWizardStep
{
    /**
     * @return the hint of the step.
     */
    String getHint();

    /**
     * Define if the step depends on another step or not. If it does this should return the hint of the step it
     * depends on.
     * @return an empty optional if it doesn't depend on any step, or the hint of the step it depends on.
     */
    Optional<String> dependsOnPreviousStep();

    /**
     * @return {@code true} if the step needs to be started right away without any user interaction (e.g. starting
     * flavor install after selecting it in another step).
     */
    boolean startsOnDisplay();

    /**
     * @return {@code true} if the step needs user inputs.
     */
    boolean needsInput();

    /**
     * Process the step with the provided user inputs if any. Some steps might not need any input, in which case the
     * provided map should be empty.
     * @param input an empty map if not input is needed or a map of user inputs crafted for the specific step needs.
     * @throws DistributionWizardException in case of problem when processing the step.
     */
    void processStep(Map<String, Serializable> input) throws DistributionWizardException;

    /**
     * @return {@code true} if the step can be skipped immediately (e.g. welcome step)
     */
    boolean isSkippable();

    /**
     * @return {@code true} if the step is done already.
     * @throws DistributionWizardException in case of problem to compute the step status.
     */
    boolean isStepDone() throws DistributionWizardException;

    /**
     * @return {@code true} if the step can be processed again.
     */
    boolean isRedoable();

    /**
     * Compute the map of information to be displayed once the step is done. This method might be called only when the
     * step is not redoable.
     *
     * @return a map containing the information to be displayed when the step is done.
     */
    Map<String, Serializable> getStepDoneInformation() throws DistributionWizardException;

    /**
     * @return the UI information for displaying the step.
     */
    DistributionWizardUIDefinition getUIDefinition();
}
