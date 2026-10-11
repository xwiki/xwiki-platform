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
 * Resolved translations are cached per locale, so that the same key requested in two locales leads to two requests
 * and two cache entries.
 *
 * @param target - the url of the rest endpoint to use to resolve translation
 * @since 18.3.0RC1
 * @beta
 */
export function translatorFactory(target: string): Translator {
  const caches = new Map<string, Translations>();
  const inflightRequests = new Map<string, Promise<Translations>>();
  return {
    // eslint-disable-next-line max-statements
    async resolve(query): Promise<Translations> {
      const isArrayQuery = Array.isArray(query);
      const prefix = isArrayQuery ? "" : (query.prefix ?? "");
      const keys = isArrayQuery ? query : query.keys;
      const locale =
        (isArrayQuery ? undefined : query.locale) ??
        document.documentElement.getAttribute("lang") ??
        "en";

      const cache = caches.get(locale) ?? {};
      caches.set(locale, cache);
      const fullKey = (key: string) => prefix + key;

      // The locale is part of the identity of a request: the same keys asked in two locales are two distinct
      // requests.
      const queryKey = JSON.stringify({ locale, prefix, keys });
      if (inflightRequests.has(queryKey)) {
        return inflightRequests.get(queryKey)!;
      }

      let _resolve!: (translation: Translations) => void;
      let _reject!: (reason: unknown) => void;
      const promise = new Promise<Translations>((resolve, reject) => {
        _resolve = resolve;
        _reject = reject;
      });
      inflightRequests.set(queryKey, promise);

      const urlSearchParams = new URLSearchParams();
      if (prefix) {
        urlSearchParams.append("prefix", prefix);
      }
      for (const key of keys.filter((key) => !(fullKey(key) in cache))) {
        urlSearchParams.append("key", key);
      }
      urlSearchParams.append("locale", locale);

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

          if (!response.ok) {
            throw new Error(
              `Unexpected response status [${response.status}] from [${target}]`,
            );
          }

          const promise = await response.json();
          const translations: { key: string; rawSource: string }[] =
            promise.translations ?? {};

          // Save newly resolved keys to the cache of the requested locale
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
          console.error(
            `Failed to retrieve the translations for query [${JSON.stringify(query)}]`,
            e,
          );
          // Let the caller know the translations could not be retrieved, instead of silently returning the keys
          // resolved so far. Dropping the inflight entry allows a later identical query to be retried.
          inflightRequests.delete(queryKey);
          _reject(e);
          return promise;
        }
      }

      // Only the translations of the query, so that the caller does not receive the shared mutable cache, holding
      // keys it never asked for.
      const resolved: Translations = {};
      for (const key of keys.filter((key) => fullKey(key) in cache)) {
        resolved[fullKey(key)] = cache[fullKey(key)];
      }

      _resolve(resolved);
      inflightRequests.delete(queryKey);

      return promise;
    },
  };
}
