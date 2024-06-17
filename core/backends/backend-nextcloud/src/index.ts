import type { Storage, WikiConfig } from "@xwiki/cristal-api/dist";
import { NextcloudWikiConfig } from "./NextcloudWikiConfig";
import { NextcloudStorage } from "./nextcloudStorage";
import { Container } from "inversify";

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<WikiConfig>("WikiConfig")
      .to(NextcloudWikiConfig)
      .whenTargetNamed("Nextcloud");
    container
      .bind<Storage>("Storage")
      .to(NextcloudStorage)
      .whenTargetNamed("Nextcloud");
  }
}
