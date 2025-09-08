/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import { inject, injectable } from "inversify";
import type { AlertsService } from "@xwiki/cristal-alerts-api";
import type { CristalApp, Logger, PageData } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";
import type {
  PageRevision,
  PageRevisionManager,
} from "@xwiki/cristal-history-api";

/**
 * Implementation of PageRevisionManager for the GitHub backend.
 *
 * @since 0.12
 * @beta
 **/
@injectable()
class GitHubPageRevisionManager implements PageRevisionManager {
  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject("Logger") private readonly logger: Logger,
    @inject("AlertsService")
    private readonly alertsService: AlertsService,
    @inject("AuthenticationManagerProvider")
    private readonly authenticationManagerProvider: AuthenticationManagerProvider,
  ) {
    this.logger.setModule("history-github.GitHubPageRevisionManager");
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  async getRevisions(pageData: PageData): Promise<Array<PageRevision>> {
    const revisions: Array<PageRevision> = [];
    if (pageData) {
      const currentId = pageData.id;

      const authorization = await this.authenticationManagerProvider
        .get()
        ?.getAuthorizationHeader();
      const headers: { Accept: string; Authorization?: string } = {
        Accept: "application/vnd.github+json",
      };
      if (authorization) {
        headers.Authorization = authorization;
      }

      const historyRequestUrl = new URL(
        `${this.cristalApp.getWikiConfig().baseRestURL}/commits`,
      );
      historyRequestUrl.search = new URLSearchParams([
        ["path", `${currentId}.md`],
      ]).toString();

      try {
        const response = await fetch(historyRequestUrl, {
          headers: headers,
        });
        const jsonResponse: Array<{
          sha: string;
          commit: { author: { name: string; date: string }; message: string };
          author: { avatar_url: string; html_url: string };
        }> = await response.json();
        for (const commit of jsonResponse) {
          revisions.push({
            version: commit.sha.slice(0, 7),
            date: new Date(commit.commit.author.date),
            user: {
              profile: commit.author.html_url,
              name: commit.commit.author.name,
            },
            comment: commit.commit.message,
            url: this.cristalApp.getRouter().resolve({
              name: "view",
              params: {
                page: currentId,
                revision: commit.sha.slice(0, 7),
              },
            }).href,
          });
        }
      } catch (error) {
        this.logger.error(error);
        this.alertsService.error(
          `Could not load page history for ${pageData.name}.`,
        );
      }
    }
    return revisions;
  }
}

export { GitHubPageRevisionManager };
