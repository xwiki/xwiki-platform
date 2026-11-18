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
import { linkEditorName } from "@xwiki/platform-editors-blocknote-react";
import { LinkModal } from "@xwiki/platform-link-modal-ui";
import {
  linkTargetTypeManagerRole,
  unsupportedLinkTargetType,
} from "@xwiki/platform-link-type-api";
import { h, render } from "vue";
import type { HeadlessEditorOptions } from "./editorOptions";
import type {
  EditorInstance,
  LinkEditionData,
  LinkEditor,
} from "@xwiki/platform-editors-blocknote-react";
import type {
  LinkContext,
  LinkData,
  LinkTargetTypeManager,
} from "@xwiki/platform-link-type-api";
import type { Container } from "inversify";
import type { AppContext } from "vue";

/**
 * The name under which the context of the Vue application hosting the editor is bound in the service container.
 */
const vueAppContextName = "VueAppContext";

/**
 * The link editor of the Vue integration: shows the link modal, rendered with the context of the Vue application
 * hosting the editor, so that it gets the components (e.g. of the design system) and the plugins of that application.
 */
const defaultLinkEditor: LinkEditor = {
  insert: (instance, prefill) => showLinkModal(instance, prefill, "createNew"),
  edit: (instance, link) => showLinkModal(instance, link, "editExisting"),
};

/**
 * @param instance - the editor instance where the link is inserted or edited
 * @param current - the edited link, or the values to prefill the link modal with when creating a new link
 * @param mode - whether a new link is created, or an existing one is edited
 * @returns the link submitted by the user, or undefined if the user canceled
 */
function showLinkModal(
  instance: EditorInstance,
  current: Partial<LinkEditionData>,
  mode: "createNew" | "editExisting",
): Promise<LinkEditionData | undefined> {
  const { serviceContainer } = instance;
  const linkTargetTypeManager = serviceContainer.get<LinkTargetTypeManager>(
    linkTargetTypeManagerRole,
  );
  // The links are resolved and saved relative to the source document.
  const linkContext: LinkContext = {
    sourceDocument: instance.options.sourceDocumentReference,
  };
  const features = instance.options
    .features as HeadlessEditorOptions["features"];

  return new Promise((resolve) => {
    const container = document.createElement("div");
    document.body.appendChild(container);
    const close = (link?: LinkEditionData) => {
      render(null, container);
      container.remove();
      resolve(link);
    };

    const vnode = h(LinkModal, {
      current: {
        displayText: current.title ?? "",
        generatedDisplayText: current.generatedTitle,
        target: current.reference
          ? linkTargetTypeManager.fromReference(current.reference, linkContext)
          : linkTargetTypeManager.fromURL(current.url ?? ""),
      },
      depsContainer: serviceContainer,
      excludedLinkTargetTypes: features?.links?.excludedTargetTypes,
      linkContext,
      mode,
      onSubmit: ({ displayText, generatedDisplayText, target }: LinkData) =>
        close({
          title: displayText,
          generatedTitle: generatedDisplayText,
          // The URL of a resource that no link target type supports can't be computed client-side, so keep the URL
          // of the edited link. It's only a preview anyway: the link is saved from its reference.
          url:
            target.type === unsupportedLinkTargetType
              ? (current.url ?? "")
              : linkTargetTypeManager.toURL(target, linkContext),
          reference: linkTargetTypeManager.toReference(target, linkContext),
        }),
      onCancel: () => close(),
    });
    vnode.appContext = serviceContainer.get<AppContext>(vueAppContextName);
    render(vnode, container);
  });
}

/**
 * Binds the services of the Vue integration: the context of the Vue application hosting the editor, and the link
 * editor showing the link modal, unless the integration provides its own.
 *
 * @param container - the service container of the editor instance
 * @param appContext - the context of the Vue application hosting the editor
 */
function bindVueServices(container: Container, appContext: AppContext): void {
  container.bind<AppContext>(vueAppContextName).toConstantValue(appContext);
  if (!container.isBound(linkEditorName)) {
    container
      .bind<LinkEditor>(linkEditorName)
      .toConstantValue(defaultLinkEditor);
  }
}

export { bindVueServices };
