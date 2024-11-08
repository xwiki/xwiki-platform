import { UserDetails } from "@xwiki/cristal-authentication-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type { AuthenticationManager } from "@xwiki/cristal-authentication-api";

// TODO: find out how to move the type declaration to a separate location.
// eslint-disable-next-line @typescript-eslint/prefer-namespace-keyword, @typescript-eslint/no-namespace
declare module window {
  interface authenticationXWiki {
    login: (oidcUrl: string) => void;

    isLoggedIn(): Promise<boolean>;

    getUserDetails(baseURL: string): Promise<UserDetails>;

    getAuthorizationValue(): Promise<{
      tokenType: string;
      accessToken: string;
    }>;

    logout(): Promise<void>;
  }

  // eslint-disable-next-line import/group-exports
  export const authenticationXWiki: authenticationXWiki;
}

@injectable()
class XWikiAuthenticationManager implements AuthenticationManager {
  constructor(
    @inject<CristalApp>("CristalApp") private cristalApp: CristalApp,
  ) {}

  start(): void {
    window.authenticationXWiki.login(this.cristalApp.getWikiConfig().baseURL);
  }

  callback(): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async getAuthorizationHeader(): Promise<string | undefined> {
    const authenticated = await this.isAuthenticated();
    if (authenticated) {
      const { tokenType, accessToken } =
        await window.authenticationXWiki.getAuthorizationValue();
      return `${tokenType} ${accessToken}`;
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return window.authenticationXWiki.isLoggedIn();
  }

  async getUserDetails(): Promise<UserDetails> {
    return window.authenticationXWiki.getUserDetails(
      this.cristalApp.getWikiConfig().baseURL,
    );
  }

  async logout(): Promise<void> {
    await window.authenticationXWiki.logout();
  }
}

// eslint-disable-next-line import/group-exports
export { XWikiAuthenticationManager };
