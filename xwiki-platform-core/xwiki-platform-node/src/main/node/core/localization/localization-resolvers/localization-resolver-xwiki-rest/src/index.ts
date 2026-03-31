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
  Translations,
  Translator,
} from "@xwiki/platform-localization-api";

/**
 * Initializes a translator that resolves translation keys by sending request to a XWiki REST endpoint.
 *
 * @param target - the url of the rest endpoint to use to resolve translation
 * @since 18.3.0RC1
 * @beta
 */
export function translatorFactory(target: string): Translator {
  const cache = {};
  // TODO: move inflight requests here, prevent calling twice the same query at the same time
  // and remove from the consumer.
  const inflightRequests = new Map<string, Promise<Translations>>();
  return {
    // eslint-disable-next-line max-statements
    async resolve(query): Promise<Translations> {
      const queryKey = JSON.stringify(query);
      if (inflightRequests.has(queryKey)) {
        return inflightRequests.get(queryKey)!;
      }

      let _resolve!: (translation: Translations) => void;
      const promise = new Promise<Translations>((resolve) => {
        _resolve = resolve;
      });
      inflightRequests.set(queryKey, promise);

      const urlSearchParams = new URLSearchParams();

      const cacheKeys = Object.keys(cache);
      const defaultLocale = () =>
        document.documentElement.getAttribute("lang") ?? "en";
      if (Array.isArray(query)) {
        const cleanedQuery = query.filter((key) => !cacheKeys.includes(key));

        for (const arg of cleanedQuery) {
          urlSearchParams.append("key", arg);
        }

        urlSearchParams.append("locale", defaultLocale());
      } else {
        const filteredKeys = query.keys.filter(
          (key) => !cacheKeys.includes((query.prefix ?? "") + key),
        );

        if (query.prefix) {
          urlSearchParams.append("prefix", query.prefix);
        }
        for (const arg of filteredKeys) {
          urlSearchParams.append("key", arg);
        }
        if (query.locale) {
          urlSearchParams.append("locale", query.locale);
        } else {
          urlSearchParams.append("locale", defaultLocale());
        }
      }

      // If there is no keys, it means that everything is already in the cache
      if (urlSearchParams.has("key")) {
        try {
          const response = await fetch(
            `${target}?${urlSearchParams.toString()}`,
            {
              method: "GET",
              headers: {
                Accept: "application/json",
              },
            },
          );

          const promise = await response.json();
          const translations: { key: string; rawSource: string }[] =
            promise.translations ?? {};

          // Save newly resolved keys to the cache
          Object.assign(
            cache,
            translations.reduce<{
              [key: string]: string;
            }>((acc, translation) => {
              acc[translation.key] = translation.rawSource;
              return acc;
            }, {}),
          );
        } catch (e) {
          console.log(
            `Failed to retrieve the translations for query [${query}]`,
            e,
          );
        }
      }

      _resolve(cache);
      inflightRequests.delete(queryKey);

      return promise;
    },
  };
}
