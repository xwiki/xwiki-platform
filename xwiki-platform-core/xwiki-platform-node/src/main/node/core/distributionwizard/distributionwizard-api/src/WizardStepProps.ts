/**
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

type UIComponent = {
  component?: string;
  module?: string;
  html?: string;
};
enum StepState {
  DISPLAYED,
  VALIDATED,
  PROCESSING,
  PROCESSED,
  PROCESS_ERROR,
}
type WizardStepSummary = {
  id: string;
  title: string;
  index: number;
  dependsOnPreviousStep: boolean;
  startsOnDisplay: boolean;
};
type WizardStepProps = WizardStepSummary & {
  uiComponent: UIComponent;
  state?: StepState;
  needsInput: boolean;
  skippable: boolean;
};
enum DialogState {
  INITIALIZING,
  STEPS_LOADED,
  LOADING_STEP,
  STEP_DISPLAYED,
}

type WizardDialogProps = {
  wizardTitle: string;
  steps: WizardStepSummary[];
  state: DialogState;
  stepIndex: number;
};

export {
  DialogState,
  StepState,
  type UIComponent,
  type WizardDialogProps,
  type WizardStepProps,
  type WizardStepSummary,
};
