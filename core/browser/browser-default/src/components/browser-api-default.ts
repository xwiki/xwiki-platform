import { BrowserApi } from "@cristal/browser-api";
import { injectable } from "inversify";
import { WikiConfig } from "@cristal/api";

@injectable()
export class BrowserApiDefault implements BrowserApi {
  switchLocation(wikiConfig: WikiConfig) {
    window.location.href = `/${wikiConfig.name}/#/${wikiConfig.homePage}/`;
  }
}
