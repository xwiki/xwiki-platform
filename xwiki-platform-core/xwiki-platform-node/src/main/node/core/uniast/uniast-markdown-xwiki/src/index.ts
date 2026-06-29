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
import type { InternalLinksSerializer } from "@xwiki/platform-uniast-markdown";
import type { Container, Factory, Newable, ResolutionContext } from "inversify";

/**
 * @beta
 * @since 18.2.0RC1
 */
export class ComponentInit {
  constructor(container: Container) {
    initXWikiFactory(container);
  }
}

function initXWikiFactory(container: Container) {
  const name = "XWiki";
  container
    .bind<Factory<Promise<InternalLinksSerializer>>>(
      "Factory<InternalLinksSerializer>",
    )
    .toFactory((context) => {
      return async () => {
        const component = (await import("./xwiki-internal-link-serializer"))
          .XWikiInternalLinkSerializer;
        return bindAndLoad(container, name, component, context);
      };
    })
    .whenNamed(name);
}

/**
 * Register the component in the container on demand.
 *
 * @param container - the container
 * @param name - the name of the component interface
 * @param component - the actual component to register
 * @param context - the context
 */
function bindAndLoad<T extends InternalLinksSerializer>(
  container: Container,
  name: string,
  component: Newable<T>,
  context: ResolutionContext,
) {
  if (!container.isBound("InternalLinksSerializer", { name: name })) {
    container
      .bind<InternalLinksSerializer>("InternalLinksSerializer")
      .to(component)
      .whenNamed(name);
  }
  return context.get<InternalLinksSerializer>("InternalLinksSerializer", {
    name: name,
  });
}
