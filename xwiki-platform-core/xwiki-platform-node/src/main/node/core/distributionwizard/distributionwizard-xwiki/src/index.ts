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
import { DialogState } from "@xwiki/platform-distributionwizard-api";
import type {
  WizardDialogProps,
  WizardStepProps,
} from "@xwiki/platform-distributionwizard-api";

async function XWikiStepsResolver(restURL: string): Promise<WizardDialogProps> {
  const response = await fetch(restURL, {
    headers: {
      Accept: "application/json",
    },
  });
  if (response.ok) {
    return {
      ...(await response.json()),
      stepIndex: -1,
      state: DialogState.STEPS_LOADED,
    };
  } else {
    throw new Error(response.status + ":" + response.statusText);
  }
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

async function XWikiStartStepResolver(restURL: string): Promise<boolean> {
  const response = await fetch(restURL, {
    method: "PUT",
  });
  // FIXME: handle fetch error
  return response.status >= 200 && response.status < 300;
}

export { XWikiStartStepResolver, XWikiStepResolver, XWikiStepsResolver };
