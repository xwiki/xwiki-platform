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
import type { LinkTargetTypeExtension } from "@xwiki/platform-link-type-api";
import type { Ref } from "vue";

/**
 * Resolve the labels of link target types in the given locale, and again each time it changes. The labels are resolved
 * asynchronously (a link target type may fetch its label from the server), so a link target type is labelled with its
 * type until its label is resolved, or when it fails to resolve.
 *
 * @param extensions - the link target types to resolve the labels of
 * @param locale - the locale to resolve the labels in
 * @returns the labels, indexed by link target type
 */
function useLinkTargetTypeLabels(
  extensions: LinkTargetTypeExtension[],
  locale: Ref<string>,
): Ref<Record<string, string>> {
  const labels = ref<Record<string, string>>(
    Object.fromEntries(
      extensions.map((extension) => [extension.type, extension.type]),
    ),
  );

  watch(
    locale,
    async (locale, _, onCleanup) => {
      let superseded = false;
      onCleanup(() => (superseded = true));

      const resolvedLabels = await Promise.all(
        extensions.map(async (extension) => [
          extension.type,
          await extension.getLabel(locale).catch(() => extension.type),
        ]),
      );

      // Ignore the labels resolved for a locale that changed in the meantime.
      if (!superseded) {
        labels.value = Object.fromEntries(resolvedLabels);
      }
    },
    { immediate: true },
  );

  return labels;
}

export { useLinkTargetTypeLabels };
