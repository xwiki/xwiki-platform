import { WikiConfig } from "@cristal/api";

export interface BrowserApi {
  switchLocation(wikiConfig: WikiConfig): void;
}

export const name = "BrowserApi";
