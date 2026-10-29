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
import {
  EntityType as XWikiEntityType,
  Model,
} from "@xwiki/platform-xwiki-model-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { EntityReference as XWikiEntityReference } from "@xwiki/platform-xwiki-model-api";

/**
 * Makes the reference of an attachment relative to the edited document, as the WYSIWYG editors do, so that the
 * attachment reference keeps working when the document is renamed or copied (e.g. `cat.png` rather than
 * `xwiki:Space.Page@cat.png` for a file attached to the edited document).
 *
 * @param reference - the resource reference of an attachment
 * @param documentReference - the reference of the edited document
 * @returns the given resource reference, with its attachment reference made relative to the edited document, or the
 *   given resource reference itself when it doesn't point to an attachment
 */
function toRelativeAttachmentResourceReference(
  reference: ResourceReference,
  documentReference: XWikiEntityReference,
): ResourceReference {
  if (reference.type !== "attach") {
    return reference;
  }
  const attachmentReference = Model.resolve(
    reference.reference,
    XWikiEntityType.ATTACHMENT,
    documentReference,
  );
  return attachmentReference
    ? {
        ...reference,
        reference: Model.serialize(attachmentReference, documentReference),
      }
    : reference;
}

export { toRelativeAttachmentResourceReference };
