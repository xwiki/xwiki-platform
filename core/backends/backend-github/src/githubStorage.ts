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

import { DefaultPageData } from "@xwiki/cristal-api";
import { AbstractStorage } from "@xwiki/cristal-backend-api";
import {
  DefaultPageReader,
  DefaultPageWriter,
} from "@xwiki/cristal-page-default";
import { inject, injectable } from "inversify";
import mime from "mime";
import type { AlertsServiceProvider } from "@xwiki/cristal-alerts-api";
import type {
  AttachmentsData,
  Logger,
  PageAttachment,
  PageData,
} from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";

@injectable()
export class GitHubStorage extends AbstractStorage {
  private readonly ATTACHMENTS = "attachments";

  constructor(
    @inject("Logger") logger: Logger,
    @inject("AuthenticationManagerProvider")
    private readonly authenticationManagerProvider: AuthenticationManagerProvider,
    @inject("AlertsServiceProvider")
    private readonly alertsServiceProvider: AlertsServiceProvider,
  ) {
    super(logger, "storage.components.githubStorage");
  }

  public async isStorageReady(): Promise<boolean> {
    return true;
  }

  getPageRestURL(page: string, _syntax: string, revision?: string): string {
    this.logger?.debug("GitHub Loading page", page);
    let baseRestURL = `${this.wikiConfig.baseRestURL}/contents/${page}`;
    if (revision) {
      baseRestURL = `${baseRestURL}?ref=${revision}`;
    }
    return baseRestURL;
  }

  private getPageRestMetaURL(page: string, _syntax: string, revision?: string) {
    const split = page.split("/");
    split[split.length - 1] = `.${split[split.length - 1]}`;
    let baseRestURL = `${this.wikiConfig.baseRestURL}/contents/${split.join("/")}`;
    if (revision) {
      baseRestURL = `${baseRestURL}?ref=${revision}`;
    }
    return baseRestURL;
  }

  getPageFromViewURL(url: string): string | null {
    let page = null;
    if (url.startsWith(this.wikiConfig.baseURL)) {
      const uri = url.replace(this.wikiConfig.baseURL, "");
      page = uri;
    }
    return page;
  }

  getImageURL(page: string, image: string): string {
    const directory = page.replace(/[^/]*$/, "");
    return `${this.wikiConfig.baseURL}/${directory}${image}`;
  }

  hashCode = function (str: string): string {
    let hash = 0,
      i,
      chr;
    if (str.length === 0) {
      return "" + hash;
    }
    for (i = 0; i < str.length; i++) {
      chr = str.charCodeAt(i);
      hash = (hash << 5) - hash + chr;
      hash |= 0; // Convert to 32bit integer
    }
    return "" + hash;
  };

  async getPageContent(
    page: string,
    syntax: string,
    revision?: string,
  ): Promise<PageData | undefined> {
    this.logger?.debug("GitHub Loading page", page);
    const url = this.getPageRestURL(`${page}.md`, syntax, revision);
    const response = await fetch(url, {
      cache: "no-store",
      headers: {
        ...(await this.getCredentials()),
        Accept: "application/vnd.github.raw+json",
      },
    });

    if (response.status >= 200 && response.status < 300) {
      const { content, ...json } = new DefaultPageReader().readPage(
        await response.text(),
      );
      const { date, name, username } = await this.getLastEditDetails(
        page,
        revision,
      );

      return Object.assign(new DefaultPageData(), {
        source: content,
        syntax: "markdown/1.2",
        ...json,
        id: page,
        headline: json.name,
        headlineRaw: json.name,
        canEdit:
          (await this.authenticationManagerProvider.get()?.isAuthenticated()) ??
          false,
        lastModificationDate: date,
        lastAuthor: { name, username },
      });
    } else {
      return undefined;
    }
  }

  /**
   * @since 0.9
   * @beta
   */
  async getAttachments(page: string): Promise<AttachmentsData | undefined> {
    const url = `${this.getPageRestMetaURL(page, "")}/${this.ATTACHMENTS}`;
    const response = await fetch(url, {
      cache: "no-store",
      headers: {
        ...(await this.getCredentials()),
        Accept: "application/vnd.github.raw+json",
      },
    });
    if (response.status >= 200 && response.status < 300) {
      const jsonResponse: Array<{ name: string }> = await response.json();
      return {
        attachments: await Promise.all(
          jsonResponse.map(
            async (a) => (await this.getAttachment(page, a.name))!,
          ),
        ),
        count: jsonResponse.length,
      };
    } else {
      return { attachments: [], count: 0 };
    }
  }

  async getAttachment(
    page: string,
    name: string,
  ): Promise<PageAttachment | undefined> {
    const url = `${this.getPageRestMetaURL(page, "")}/${this.ATTACHMENTS}/${name}`;
    const response = await fetch(url, {
      cache: "no-store",
      headers: {
        ...(await this.getCredentials()),
        Accept: "application/vnd.github.object+json",
      },
    });
    if (response.status >= 200 && response.status < 300) {
      const jsonResponse: {
        download_url: string;
        name: string;
        path: string;
        size: number;
      } = await response.json();

      const commitsUrl = new URL(`${this.wikiConfig.baseRestURL}/commits`);
      commitsUrl.search = new URLSearchParams([
        ["path", jsonResponse.path],
        ["per_page", "1"],
      ]).toString();
      const commitsResponse = await fetch(commitsUrl, {
        cache: "no-store",
        headers: {
          ...(await this.getCredentials()),
          Accept: "application/vnd.github+json",
        },
      });
      const jsonCommitsResponse: Array<{
        commit: { author: { name: string; date: string } };
      }> = await commitsResponse.json();

      return {
        reference: jsonResponse.name,
        id: jsonResponse.path,
        size: jsonResponse.size,
        href: jsonResponse.download_url,
        mimetype: mime.getType(jsonResponse.name) ?? "",
        date: new Date(jsonCommitsResponse[0].commit.author.date),
        author: jsonCommitsResponse[0].commit.author.name,
      };
    } else {
      return undefined;
    }
  }

  async getPanelContent(): Promise<PageData> {
    return new DefaultPageData();
  }

  async getEditField(): Promise<string> {
    return "";
  }

  async save(page: string, title: string, content: string): Promise<unknown> {
    const pageRestUrl = this.getPageRestURL(`${page}.md`, "");

    const headResponse = await fetch(pageRestUrl, {
      method: "HEAD",
      cache: "no-store",
      headers: {
        ...(await this.getCredentials()),
        Accept: "application/vnd.github.object+json",
      },
    });
    const sha =
      headResponse.status >= 200 && headResponse.status < 300
        ? headResponse.headers.get("ETag")!.slice(3, -1)
        : undefined;

    const pageContent = new DefaultPageWriter().writePage({
      content,
      name: title,
      syntax: "markdown/1.2",
    });
    const putResponse = await fetch(pageRestUrl, {
      method: "PUT",
      headers: {
        "Content-Type": "application/vnd.github+json",
        ...(await this.getCredentials()),
      },
      body: JSON.stringify({
        content: btoa(pageContent),
        message: `Update ${page}`,
        sha: sha,
      }),
    });
    if (!putResponse.ok) {
      const errorMessage = await putResponse.text();
      // TODO: Fix CRISTAL-383 (Error messages in Storages are not translated)
      this.alertsServiceProvider
        .get()
        .error(`Could not save page ${page}. Reason: ${errorMessage}`);
      // We need to throw an error to notify the editor that the save failed.
      throw new Error(errorMessage);
    }
    return;
  }

  async saveAttachments(
    page: string,
    files: File[],
  ): Promise<undefined | (string | undefined)[]> {
    const urls: Promise<string | undefined>[] = [];

    for (const file of files) {
      const fileUrl = `${this.getPageRestMetaURL(page, "")}/${this.ATTACHMENTS}/${file.name}`;
      const sha = await this.computeAttachmentSHA(fileUrl);

      urls.push(this.uploadAttachment(fileUrl, file, sha, page));
    }
    return Promise.all(urls);
  }

  private async computeAttachmentSHA(fileUrl: string) {
    const headResponse = await fetch(fileUrl, {
      method: "HEAD",
      cache: "no-store",
      headers: {
        ...(await this.getCredentials()),
        Accept: "application/vnd.github.object+json",
      },
    });
    return headResponse.status >= 200 && headResponse.status < 300
      ? headResponse.headers.get("ETag")!.slice(3, -1)
      : undefined;
  }

  private uploadAttachment(
    fileUrl: string,
    file: File,
    sha: string | undefined,
    page: string,
  ): Promise<string | undefined> {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = async () => {
        const putResponse = await fetch(fileUrl, {
          method: "PUT",
          headers: {
            "Content-Type": "application/vnd.github+json",
            ...(await this.getCredentials()),
          },
          body: JSON.stringify({
            content: (reader.result! as string).split(",")[1],
            message: `Upload ${file.name}`,
            sha: sha,
          }),
        });
        if (!putResponse.ok) {
          const errorMessage = await putResponse.text();
          // TODO: Fix CRISTAL-383 (Error messages in Storages are not translated)
          this.alertsServiceProvider
            .get()
            .error(
              `Could not upload attachment ${file.name} for page ${page}. Reason: ${errorMessage}`,
            );
          resolve(undefined);
        } else {
          resolve((await putResponse.json()).content.download_url as string);
        }
      };
      reader.readAsDataURL(file);
    });
  }

  async delete(page: string): Promise<{ success: boolean; error?: string }> {
    // We only support the default branch in GitHubStorage right now.
    // We can only get its name from the baseURL property, or by querying it.
    // TODO: Support branches (https://jira.xwiki.org/browse/CRISTAL-563)
    const branch = this.wikiConfig.baseURL.split("/").pop();

    // Get the current HEAD before doing anything.
    const headCommit = (
      await (
        await fetch(`${this.wikiConfig.baseRestURL}/git/ref/heads/${branch}`, {
          method: "GET",
          headers: {
            "Content-Type": "application/vnd.github+json",
            ...(await this.getCredentials()),
          },
        })
      ).json()
    ).object.sha;

    // Create a new tree without the page we want removed.
    // Using the tree creation API with a base tree and a node with a null sha
    // will clone the base tree with the node removed.
    const treeResponse = await fetch(
      `${this.wikiConfig.baseRestURL}/git/trees`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/vnd.github+json",
          ...(await this.getCredentials()),
        },
        body: JSON.stringify({
          base_tree: branch,
          tree: [
            {
              path: `${page}.md`,
              mode: "040000",
              type: "tree",
              sha: null,
            },
          ],
        }),
      },
    );

    if (!treeResponse.ok) {
      return { success: false, error: await treeResponse.text() };
    }

    // Create a child commit to the HEAD commit, referencing the new tree.
    const commitResponse = await fetch(
      `${this.wikiConfig.baseRestURL}/git/commits`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/vnd.github+json",
          ...(await this.getCredentials()),
        },
        body: JSON.stringify({
          tree: (await treeResponse.json()).sha,
          message: `Delete ${page}`,
          parents: [headCommit],
        }),
      },
    );

    if (!commitResponse.ok) {
      return { success: false, error: await commitResponse.text() };
    }

    // Finally, we update the branch ref to target the new commit.
    // We don't use "force" to ensure fast-forward only.
    return await fetch(
      `${this.wikiConfig.baseRestURL}/git/refs/heads/${branch}`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/vnd.github+json",
          ...(await this.getCredentials()),
        },
        body: JSON.stringify({
          sha: (await commitResponse.json()).sha,
        }),
      },
    ).then(async (response) => {
      if (response.ok) {
        return { success: true };
      } else {
        return { success: false, error: await response.text() };
      }
    });
  }

  async move(): Promise<{ success: boolean; error?: string }> {
    // TODO: to be implemented in CRISTAL-436.
    throw new Error("Move not supported");
  }

  private async getCredentials(): Promise<{ Authorization?: string }> {
    const authorizationHeader = await this.authenticationManagerProvider
      .get()
      ?.getAuthorizationHeader();
    const headers: { Authorization?: string } = {};
    if (authorizationHeader) {
      headers["Authorization"] = authorizationHeader;
    }
    return headers;
  }

  private async getLastEditDetails(
    page: string,
    revision?: string,
  ): Promise<{ date: Date; username: string; name: string }> {
    if (revision) {
      const commitsUrl = new URL(
        `${this.wikiConfig.baseRestURL}/commits/${revision}`,
      );
      const commitsResponse = await fetch(commitsUrl, {
        cache: "no-store",
        headers: {
          ...(await this.getCredentials()),
          Accept: "application/vnd.github+json",
        },
      });
      const jsonCommitsResponse: {
        commit: { author: { login: string; name: string; date: string } };
      } = await commitsResponse.json();

      return {
        date: new Date(jsonCommitsResponse.commit.author.date),
        username: jsonCommitsResponse.commit.author.login,
        name: jsonCommitsResponse.commit.author.name,
      };
    } else {
      const commitsUrl = new URL(`${this.wikiConfig.baseRestURL}/commits`);
      commitsUrl.search = new URLSearchParams([
        ["path", `${page}.md`],
        ["per_page", "1"],
      ]).toString();
      const commitsResponse = await fetch(commitsUrl, {
        cache: "no-store",
        headers: {
          ...(await this.getCredentials()),
          Accept: "application/vnd.github+json",
        },
      });
      const jsonCommitsResponse: Array<{
        commit: { author: { login: string; name: string; date: string } };
      }> = await commitsResponse.json();

      return {
        date: new Date(jsonCommitsResponse[0].commit.author.date),
        username: jsonCommitsResponse[0].commit.author.login,
        name: jsonCommitsResponse[0].commit.author.name,
      };
    }
  }
}
