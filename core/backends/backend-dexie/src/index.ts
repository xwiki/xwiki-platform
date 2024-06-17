import DexieOfflineStorage from "./dexieOfflineStorage";
import { Container } from "inversify";
import { OfflineStorage } from "@xwiki/cristal-backend-api";
import type { WrappingStorage } from "@xwiki/cristal-api/dist";
import { WrappingOfflineStorage } from "./wrappingOfflineStorage";

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<WrappingStorage>("WrappingStorage")
      .to(WrappingOfflineStorage)
      .inSingletonScope();
    container
      .bind<OfflineStorage>("OfflineStorage")
      .to(DexieOfflineStorage)
      .inSingletonScope();
  }
}
