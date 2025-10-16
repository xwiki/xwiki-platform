/*
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

import { loadById } from "@/services/require.js";

/**
 * Resolve the url of the document reference in the given mode.
 * @param documentReference the document reference
 * @param mode the mode
 * @returns {*} the computed relative url
 */
function computeTargetURL(documentReference, mode) {
  return new XWiki.Document(
    XWiki.Model.resolve(documentReference, XWiki.EntityType.DOCUMENT)).getURL(mode);
}

function getPropertyReference(propertyName, className) {
  let entityReference;
  if (propertyName.startsWith("doc.")) {
    entityReference =
      new XWiki.EntityReference(propertyName.substring("doc.".length), XWiki.EntityType.OBJECT_PROPERTY);
  } else {
    let objectReference = new XWiki.EntityReference(className + "[0]", XWiki.EntityType.OBJECT);
    entityReference = new XWiki.EntityReference(propertyName, XWiki.EntityType.OBJECT_PROPERTY, objectReference);
  }
  return XWiki.Model.serialize(entityReference);
}

async function load(mode, documentReference, property, className) {
  const targetUrl = computeTargetURL(documentReference, "get");
  const [xcontext, $] = await loadById("xwiki-meta", "jquery");
  return $.get(targetUrl, {
    xpage: "display",
    mode: mode,
    // TODO: handle the object index when provided
    property: getPropertyReference(property, className),
    type: property.startsWith("doc.") ? "document" : "object",
    language: xcontext.locale,
  }).catch((error) => {
    new XWiki.widgets.Notification(
      this.$t("livedata.displayer.xObjectProperty.failedToRetrieveField.errorMessage", [mode]),
      "error");
    throw error;
  });
}

/**
 * Load an XObject property field in edit mode.
 * @param documentReference the reference of the document containing the XObject
 * @param className the class name of the XObject
 * @param property the XObject property to display
 * @returns {*} the XObject property field html content in edit mode
 */
function edit(documentReference, className, property) {
  return load("edit", documentReference, property, className);
}

/**
 * Load an XObject property field in view mode.
 * @param documentReference the reference of the document containing the XObject
 * @param className the class name of the XObject
 * @param property the XObject property to display
 * @returns {*} the XObject property field html content in view mode
 */
function view(documentReference, className, property) {
  return load("view", documentReference, property, className);
}

export { edit, view };
