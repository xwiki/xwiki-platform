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

import { loadById } from "@xwiki/platform-xwiki-utils";

// Lists the properties whose value is submitted as HTML, which the server converts to their syntax.
const HTML_CONVERSION_FIELD = "RequiresHTMLConversion";

/**
 * Resolve the url of the document reference in the given mode.
 * @param documentReference - the document reference
 * @param mode - the mode
 * @returns the computed relative url
 */
function computeTargetURL(documentReference, mode) {
  return new XWiki.Document(
    XWiki.Model.resolve(documentReference, XWiki.EntityType.DOCUMENT),
  ).getURL(mode);
}

function getPropertyReference(propertyName, className) {
  let entityReference;
  if (propertyName.startsWith("doc.")) {
    entityReference = new XWiki.EntityReference(
      propertyName.substring("doc.".length),
      XWiki.EntityType.OBJECT_PROPERTY,
    );
  } else {
    let objectReference = new XWiki.EntityReference(
      className + "[0]",
      XWiki.EntityType.OBJECT,
    );
    entityReference = new XWiki.EntityReference(
      propertyName,
      XWiki.EntityType.OBJECT_PROPERTY,
      objectReference,
    );
  }
  return XWiki.Model.serialize(entityReference);
}

async function load(
  mode,
  documentReference,
  property,
  className,
  extraParams,
  pendingValues,
  template,
) {
  const [xcontext, $] = await loadById("xwiki-meta", "jquery");
  const parameters = {
    xpage: "display",
    mode: mode,
    // TODO: handle the object index when provided
    property: getPropertyReference(property, className),
    type: property.startsWith("doc.") ? "document" : "object",
    language: xcontext.locale,
    objectPolicy: "updateOrCreate",
    // Extra parameters such as force/force_token when the user confirms an edit confirmation.
    ...extraParams,
  };
  if (!pendingValues) {
    return $.get(computeTargetURL(documentReference, "get"), parameters);
  }
  // The values of a row whose entry does not exist yet are only held by the client, so the field is
  // rendered by the preview action, which applies them to the document the row would become without
  // saving anything. That action requires a POST carrying the CSRF token.
  const data = {
    ...parameters,
    ...toFormFields(pendingValues, className),
    form_token: xcontext.form_token,
  };
  // The preview action starts that document from the template, before applying the values.
  if (template) {
    data.template = template;
  }
  return $.ajax({
    type: "POST",
    url: computeTargetURL(documentReference, "preview"),
    data,
    // The wiki reads repeated parameters, not the indexed names jQuery produces by default, which
    // is how a property holding several values is sent.
    traditional: true,
  });
}

/**
 * Name the values of an entry the way the edit form of its document does, which is how the wiki
 * reads them back.
 *
 * @param pendingValues - the values held on the row, by property name
 * @param className - the class name of the XObject
 * @returns the values, by form field name
 */
function toFormFields(pendingValues, className) {
  return Object.fromEntries(
    Object.entries(pendingValues)
      // Document properties are not read from the object, and the field listing the properties to
      // convert from HTML already holds their form names.
      .filter(([name]) => !name.startsWith("doc."))
      .map(([name, value]) =>
        name === HTML_CONVERSION_FIELD
          ? [name, value]
          : [`${className}_0_${name}`, value],
      ),
  );
}

/**
 * Load an XObject property field in edit mode.
 * @param documentReference - the reference of the document containing the XObject
 * @param className - the class name of the XObject
 * @param property - the XObject property to display
 * @param extraParams - optional extra request parameters (e.g. force and force_token to force an edit confirmation)
 * @param pendingValues - the values of a row whose entry does not exist yet, which the field is
 * rendered from, or undefined to render the field from the stored document
 * @param template - the template the entry of that row is created from, if any
 * @returns the XObject property field html content in edit mode
 */
function edit(
  documentReference,
  className,
  property,
  extraParams,
  pendingValues,
  template,
) {
  return load(
    "edit",
    documentReference,
    property,
    className,
    extraParams,
    pendingValues,
    template,
  );
}

/**
 * Load an XObject property field in view mode.
 * @param documentReference - the reference of the document containing the XObject
 * @param className - the class name of the XObject
 * @param property - the XObject property to display
 * @param pendingValues - the values of a row whose entry does not exist yet, which the field is
 * rendered from, or undefined to render the field from the stored document
 * @param template - the template the entry of that row is created from, if any
 * @returns the XObject property field html content in view mode
 */
function view(documentReference, className, property, pendingValues, template) {
  return load(
    "view",
    documentReference,
    property,
    className,
    undefined,
    pendingValues,
    template,
  );
}

export { edit, view };
