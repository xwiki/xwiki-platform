/**
 * See the LICENSE file distributed with this work for additional
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

import { expect } from "@playwright/test";
import { Locator, Page } from "@playwright/test";
import { DesignSystem } from "../DesignSystem";

/**
 * The page object interface for a form element.
 * @since 0.19
 * @beta
 */
export interface FormElement {
  /**
   * Get the current value of the element.
   * @returns the text value of the element
   */
  getValue(): Promise<string>;

  /**
   * Update the value of the element.
   * @param value - the new value for the element
   */
  setValue(value: string): Promise<void>;
}

/**
 * Page object to interact with a form and its elements.
 * @since 0.19
 * @beta
 */
export class FormPageObject {
  constructor(
    private readonly page: Page,
    private readonly formLocator: Locator,
    private readonly designSystem: DesignSystem,
  ) {}

  public findInputFromLabel(label: string): FormElement {
    // TODO: we need to generalize that to make it easy to extend with more design systems
    switch (this.designSystem) {
      case DesignSystem.VUETIFY:
        return this.findInputFromLabelVuetify(label);
      case DesignSystem.SHOELACE:
        return this.findInputFromLabelShoelace(label);
    }
  }

  public findSelectFromLabel(label: string): FormElement {
    // TODO: we need to generalize that to make it easy to extend with more design systems
    switch (this.designSystem) {
      case DesignSystem.VUETIFY:
        return this.findSelectFromLabelVuetify(label);
      case DesignSystem.SHOELACE:
        return this.findSelectFromLabelShoelace(label);
    }
  }

  public async submit() {
    await this.formLocator.dispatchEvent("submit");
  }

  private findInputFromLabelShoelace(label: string): FormElement {
    const inputs = this.formLocator.locator("sl-input");
    return this.wrapInputShoelace(
      inputs.filter({
        has: this.page.locator("label").getByText(label, { exact: true }),
      }),
    );
  }

  private findInputFromLabelVuetify(label: string): FormElement {
    const inputs = this.formLocator.locator(".v-input");
    return this.wrapInputVuetify(
      inputs.filter({
        has: this.page.locator(".v-label").getByText(label, { exact: true }),
      }),
    );
  }

  private findSelectFromLabelShoelace(label: string): FormElement {
    const selects = this.formLocator.locator("sl-select");
    return this.wrapSelectShoelace(
      selects.filter({
        has: this.page.locator("label").getByText(label, { exact: true }),
      }),
    );
  }

  private findSelectFromLabelVuetify(label: string): FormElement {
    const selects = this.formLocator.locator(".v-select");
    return this.wrapSelectVuetify(
      selects.filter({
        has: this.page.locator(".v-label").getByText(label, { exact: true }),
      }),
    );
  }

  private wrapInputShoelace(input: Locator): FormElement {
    const inputValue = input.locator("input");
    return {
      async getValue() {
        return await inputValue.inputValue();
      },
      async setValue(value: string) {
        await inputValue.fill(value);
      },
    };
  }

  private wrapInputVuetify(input: Locator): FormElement {
    const inputValue = input.locator("input");
    return {
      async getValue() {
        return (await inputValue.getAttribute("value")) ?? "";
      },
      async setValue(value: string) {
        await inputValue.fill(value);
      },
    };
  }

  private wrapSelectShoelace(select: Locator): FormElement {
    const inputValue = select.locator("input").first();
    return {
      async getValue() {
        return await inputValue.inputValue();
      },
      async setValue(value: string) {
        await select.locator("sl-popup").click();
        const option = select
          .locator("sl-option")
          .getByText(value, { exact: true });
        await expect(option).toBeVisible();
        await option.click();
        await expect(option).toBeHidden();
      },
    };
  }

  private wrapSelectVuetify(select: Locator): FormElement {
    const inputValue = select.locator("input").first();
    const page = this.page;
    return {
      async getValue() {
        return await inputValue.inputValue();
      },
      async setValue(value: string) {
        await select.click();
        const option = page
          .locator(".v-overlay .v-list [role=option].v-list-item")
          .filter({
            visible: true,
          })
          .getByText(value, { exact: true });
        await option.click();
        await expect(option).toBeHidden();
      },
    };
  }
}
