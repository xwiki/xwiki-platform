import { XWikiAuthenticationManager } from "./xWikiAuthenticationManager";
import { AuthenticationManager } from "@xwiki/cristal-authentication-api";
import type { Container } from "inversify";

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<AuthenticationManager>("AuthenticationManager")
      .to(XWikiAuthenticationManager)
      .inSingletonScope()
      .whenTargetNamed("XWiki");
  }
}
