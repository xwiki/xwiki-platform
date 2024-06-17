import type { WikiConfig } from "@xwiki/cristal-api/dist";
import { Storage } from "@xwiki/cristal-api/dist";
import { Container } from "inversify";
import { XWikiWikiConfig } from "./XWikiWikiConfig";
import { XWikiStorage } from "./xwikiStorage";

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<WikiConfig>("WikiConfig")
      .to(XWikiWikiConfig)
      .whenTargetNamed("XWiki");
    container
      .bind<Storage>("Storage")
      .to(XWikiStorage)
      .whenTargetNamed("XWiki");
  }
}
