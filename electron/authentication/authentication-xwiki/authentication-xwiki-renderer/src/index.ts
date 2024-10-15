import type { Container } from "inversify";

import { AuthenticationManager } from "@xwiki/cristal-authentication-api";
import { XWikiAuthenticationManager } from "./xWikiAuthenticationManager";

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<AuthenticationManager>("AuthenticationManager")
      .to(XWikiAuthenticationManager)
      .inSingletonScope()
      .whenTargetNamed("XWiki");
  }
}
