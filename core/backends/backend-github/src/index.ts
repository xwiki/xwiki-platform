import type { WikiConfig } from "@xwiki/cristal-api";
import { Storage } from "@xwiki/cristal-api";
import { Container } from "inversify";
import { GitHubWikiConfig } from "./GitHubWikiConfig";
import { GitHubStorage } from "./githubStorage";

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<WikiConfig>("WikiConfig")
      .to(GitHubWikiConfig)
      .whenTargetNamed("GitHub");
    container
      .bind<Storage>("Storage")
      .to(GitHubStorage)
      .whenTargetNamed("GitHub");
  }
}
