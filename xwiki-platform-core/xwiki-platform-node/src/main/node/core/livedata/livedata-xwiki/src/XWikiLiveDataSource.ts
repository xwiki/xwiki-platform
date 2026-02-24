/**
 * See the NOTICE file distributed with this work for additional
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
import type {
  LiveDataSource,
  Query,
  Source,
  Values,
} from "@xwiki/platform-livedata-api";

/**
 * @since 18.2.0RC1
 * @beta
 */
export class XWikiLiveDataSource implements LiveDataSource {
  private readonly baseURL = `${XWiki.contextPath}/rest/liveData/sources/`;
  private entriesRequest?: JQuery.jqXHR | null;

  constructor(private readonly $: JQueryStatic) {}

  // eslint-disable-next-line max-statements
  async getEntries(
    liveDataQuery: Query,
  ): Promise<{ count: number; entries: Values[] }> {
    const entriesURL = this.getEntriesURL(liveDataQuery.source);

    const parameters: {
      properties: string[];
      offset: number;
      limit: number;
      matchAll: string[];
    } & { [key: string]: unknown } = {
      properties: liveDataQuery.properties,
      offset: liveDataQuery.offset,
      limit: liveDataQuery.limit,
      matchAll: [],
    };
    liveDataQuery.filters.forEach((filter) => {
      if (filter.matchAll) {
        parameters.matchAll.push(filter.property);
      }
      parameters["filters." + filter.property] = filter.constraints
        .filter((constraint) => constraint.value !== undefined)
        .map((constraint) => {
          if (constraint.operator === undefined) {
            constraint.operator = "";
          }
          return constraint;
        })
        .map((constraint) => constraint.operator + ":" + constraint.value);
    });
    // Add sort.
    parameters.sort = liveDataQuery.sort.map((sort) => sort.property);
    parameters.descending = liveDataQuery.sort.map((sort) => sort.descending);

    // We abort previous requests to avoid a race condition. It can happen that getEntries is
    // called twice in a short time (when the user is typing in a filter field for instance,
    // quickly changing sorting, or just if the network  is slow) and that the first request
    // succeeds after the second request, and its results would replace the "fresher" state.
    this.entriesRequest?.abort();
    this.entriesRequest = this.$.getJSON(
      entriesURL,
      this.$.param(parameters, true),
    );

    try {
      const resolved = await this.entriesRequest;
      return this.toLiveData(resolved);
    } finally {
      this.cleanupRequest(this.entriesRequest);
    }
  }

  updateEntry(source: Source, entryId: string, values: unknown): Promise<void> {
    return Promise.resolve(
      this.$.ajax({
        type: "PUT",
        url: this.getEntryURL(source, entryId),
        contentType: "application/json",
        data: JSON.stringify({ values }),
      }),
    );
  }

  updateEntryProperty(
    source: Source,
    entryId: string,
    propertyId: string,
    propertyValue: unknown,
  ): Promise<void> {
    return Promise.resolve(
      this.$.ajax({
        type: "PUT",
        url: this.getEntryPropertyURL(source, entryId, propertyId),
        contentType: "text/plain",
        data: `${propertyValue}`,
      }),
    );
  }

  private getEntriesURL(source: Source) {
    const entriesURL =
      this.baseURL + encodeURIComponent(source.id) + "/entries";
    return this.addSourcePathParameters(source, entriesURL);
  }

  private getEntryURL(source: Source, entryId: string) {
    const encodedSourceId = encodeURIComponent(source.id);
    const encodedEntryId = encodeURIComponent(entryId);
    const url = `${this.baseURL}${encodedSourceId}/entries/${encodedEntryId}`;
    return this.addSourcePathParameters(source, url);
  }

  private addSourcePathParameters(source: Source, url: string) {
    const parameters = {
      // Make sure the response is not retrieved from cache (IE11 doesn't obey the caching HTTP
      // headers).
      timestamp: new Date().getTime(),
      namespace: `wiki:${encodeURIComponent(XWiki.currentWiki)}`,
    };
    this.addSourceParameters(parameters, source);
    return `${url}?${this.$.param(parameters, true)}`;
  }

  private addSourceParameters(
    parameters: { [key: string]: unknown },
    source: Source,
  ) {
    this.$.each(source, (key, value) => {
      if (key !== "id") {
        parameters[`sourceParams.${String(key)}`] = value;
      }
    });
  }

  private toLiveData(data: { count: number; entries: { values: Values }[] }): {
    count: number;
    entries: Values[];
  } {
    return {
      count: data.count,
      entries: data.entries.map((entry) => entry.values),
    };
  }

  private cleanupRequest(requestToClean: JQuery.jqXHR) {
    // We reset the request object to null for two reasons:
    // - avoid keeping an object we don't need anymore in memory, preventing it from being GC'd
    // - make sure we don't attempt to abort a request that already terminated.
    //
    // We only nullify the request if it is the request we just handled.
    // Otherwise, this means that a fresher request is in flight. In which case
    // we need to be able to abort this fresher one if yet another request is
    // fired before it succeeds.
    if (requestToClean === this.entriesRequest) {
      this.entriesRequest = null;
    }
  }

  private getEntryPropertyURL(
    source: Source,
    entryId: string,
    propertyId: string,
  ) {
    const encodedSourceId = encodeURIComponent(source.id);
    const encodedEntryId = encodeURIComponent(entryId);
    const encodedPropertyId = encodeURIComponent(propertyId);
    const url = `${this.baseURL}${encodedSourceId}/entries/${encodedEntryId}/properties/${encodedPropertyId}`;
    return this.addSourcePathParameters(source, url);
  }
}
