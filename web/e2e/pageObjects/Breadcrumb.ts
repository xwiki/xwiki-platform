import { Locator, Page } from "@playwright/test";
import { DesignSystem } from "../DesignSystem";

/**
 * The page object interface for a segment of a breadcrumb.
 */
export interface BreadcrumbSegmentElement {
  /**
   * @returns the text element of the segment
   */
  getText(): Locator;

  /**
   * @returns the href value of the segment link
   */
  getLink(): Promise<string>;
}

/**
 * Page object to interact with a breadcrumb element.
 */
export class BreadcrumbPageObject {
  constructor(
    private readonly page: Page,
    private readonly designSystem: DesignSystem,
  ) {}

  async findItems(): Promise<BreadcrumbSegmentElement[]> {
    // TODO: we need to generalize that to make it easy to extend with more design systems
    switch (this.designSystem) {
      case DesignSystem.VUETIFY:
        return await this.findItemsVuetify();
      case DesignSystem.SHOELACE:
        return await this.findItemsShoelace();
    }
  }

  private async findItemsShoelace(): Promise<BreadcrumbSegmentElement[]> {
    return await this.findItemsInternal(
      "#breadcrumbRoot sl-breadcrumb sl-breadcrumb-item",
      (element) => {
        return {
          getText() {
            return element;
          },
          async getLink() {
            const link = element.locator(".breadcrumb-item__label--link");
            return (await link.getAttribute("href"))!;
          },
        };
      },
    );
  }

  private async findItemsVuetify(): Promise<BreadcrumbSegmentElement[]> {
    return await this.findItemsInternal("#breadcrumbRoot li a", (element) => {
      return {
        getText() {
          return element;
        },
        async getLink() {
          return (await element.getAttribute("href"))!;
        },
      };
    });
  }

  private async findItemsInternal(
    selector: string,
    wrap: (element: Locator) => BreadcrumbSegmentElement,
  ) {
    const locator = this.page.locator(selector);
    // Waiting for the first breadcrumb segment to be visible before calling .all() (which otherwise returns an empty
    // list if no elements are visible at the time of calling). Assuming the rest of the breadcrumb segment becomes
    // visible at the same time as the first one.
    await locator.nth(0).waitFor({
      state: "visible",
    });
    return (await locator.all()).map(wrap);
  }
}
