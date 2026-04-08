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
import type {
  WizardStepProps,
  WizardStepSummary,
} from "@xwiki/platform-distributionwizard-api";

type JSONSteps = {
  step: WizardStepSummary[];
};

async function fetchSteps(restURL: string): Promise<JSONSteps> {
  const response = await fetch(restURL, {
    headers: {
      Accept: "application/json",
    },
  });
  // FIXME: handle fetch error
  return response.json();
}

/**
 * Resolve the steps using the given REST API URL.
 * @param restURL - the URL of the REST API to call for getting step info.
 * @beta
 */
async function XWikiStepsResolver(
  restURL: string,
): Promise<WizardStepSummary[]> {
  const steps = await fetchSteps(restURL);
  return steps.step;
}

async function XWikiStepResolver(restURL: string): Promise<WizardStepProps> {
  const response = await fetch(restURL, {
    headers: {
      Accept: "application/json",
    },
  });
  // FIXME: handle fetch error
  return response.json();
}

export { XWikiStepResolver, XWikiStepsResolver };
