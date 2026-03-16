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
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { Query, Resolver } from "@xwiki/platform-localization-api";
import type { Ref } from "vue";
import type { Composer } from "vue-i18n";

const inflightRequests = new Map<string, Promise<void>>();

/**
 * @param resolver - the resolve to use to load the translations.
 * @param query - the query to execute
 * @since 18.2.0RC1
 * @beta
 */
export function useRemoteI18n(
  resolver: Resolver,
  query: Query,
): {
  t: Composer["t"];
  isLoading: Ref<boolean>;
} {
  const { locale, mergeLocaleMessage, t } = useI18n({
    useScope: "local",
    inheritLocale: true,
  });

  const isLoading = ref(false);

  // eslint-disable-next-line max-statements
  async function load(lang: string) {
    const queryKey = JSON.stringify(query);
    isLoading.value = true;
    if (inflightRequests.has(queryKey)) {
      // In case of identical query already performed, blocks until the query is done.
      // Then continue, the resolve will use the cache to resolve the keys again.
      await inflightRequests.get(queryKey);
    }
    let _resolve!: () => void;
    const promise = new Promise<void>((resolve) => {
      _resolve = resolve;
    });
    inflightRequests.set(queryKey, promise);

    try {
      const res = await resolver.resolve(query);
      mergeLocaleMessage(lang, res.translations);
    } catch (e) {
      console.error(e);
    } finally {
      isLoading.value = false;
      _resolve();
    }
  }

  watch(locale, (lang) => load(lang), { immediate: true });

  return { t, isLoading };
}
