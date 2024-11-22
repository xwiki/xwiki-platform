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

import { decorate, injectable } from "inversify";
import { Suspense, defineComponent, h } from "vue";
import { useI18n } from "vue-i18n";

/**
 * Wraps a component with an async setup in a suspense component and pass it
 * the provided props
 * @param component - the component with an async setup to wrap in a suspense
 * @param props - the props of the wrapped component
 */
function wrapInSuspense(
  component: ReturnType<typeof defineComponent>,
  { props }: { props: object },
) {
  return defineComponent({
    render() {
      return h(Suspense, null, {
        default() {
          return h(component, props);
        },
      });
    },
  });
}

/**
 * @param clazz - the class to decorate with an injectable
 * @since 0.11
 */
function makeInjectable(clazz: object): object {
  decorate(injectable(), clazz);
  return clazz;
}

function mockI18n() {
  useI18n.mockReturnValue({
    t: (tKey) => tKey,
  });
}

export { makeInjectable, mockI18n, wrapInSuspense };
