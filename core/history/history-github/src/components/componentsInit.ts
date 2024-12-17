/*
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
import type {
  PageRevision,
  PageRevisionManager,
} from "@xwiki/cristal-history-api";

/**
 * Implementation of PageRevisionManager for the GitHub backend.
 *
 * @since 0.12
 **/
@injectable()
class GitHubPageRevisionManager implements PageRevisionManager {
  constructor(
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
    @inject<Logger>("Logger") private readonly logger: Logger,
    @inject<AlertsService>("AlertsService")
    private readonly alertsService: AlertsService,
  ) {
    this.logger.setModule("history-github.GitHubPageRevisionManager");
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  async getRevisions(pageData: PageData): Promise<Array<PageRevision>> {
    const revisions: Array<PageRevision> = [];
    if (pageData) {
      const currentId = pageData.id;

      const historyRequestUrl = new URL(
        this.cristalApp
          .getWikiConfig()
          .baseURL.replace("/tree/", "/commits/deferred_commit_data/"),
      );
      historyRequestUrl.search = new URLSearchParams([
        ["path", currentId],
      ]).toString();

      try {
        const response = await fetch(historyRequestUrl, {
          headers: {
            Accept: "application/json",
          },
        });
        const jsonResponse = await response.json();
        revisions.push(
          ...(await Promise.all(
            jsonResponse.deferredCommits.map(
              async (commit: { oid: string }) => {
                const commitRequestUrl = this.cristalApp
                  .getWikiConfig()
                  .baseURL.replace(/\/tree\/.*$/, `/commits/${commit.oid}`);
                const commitResponse = await fetch(commitRequestUrl, {
                  headers: {
                    Accept: "application/json",
                  },
                });
                const jsonCommitResponse = await commitResponse.json();
                const version: string = commit.oid.substring(0, 7);
                const commitData: {
                  committedDate: string;
                  shortMessage: string;
                  authors: Array<{ displayName: string; path: string }>;
                } = jsonCommitResponse.payload.commitGroups[0].commits[0];
                const authorProfile: URL = new URL(
                  this.cristalApp.getWikiConfig().baseURL,
                );
                authorProfile.pathname = commitData.authors[0].path;
                return {
                  version: version,
                  date: new Date(commitData.committedDate),
                  user: {
                    profile: authorProfile.toString(),
                    name: commitData.authors[0].displayName,
                  },
                  comment: commitData.shortMessage,
                  url: this.cristalApp.getRouter().resolve({
                    name: "view",
                    params: {
                      page: currentId,
                      revision: version,
                    },
                  }).href,
                };
              },
            ),
          )),
        );
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
