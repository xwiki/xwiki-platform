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
const config = {
  prefix: "livedata.",
  keys: [
    "dropdownMenu.title",
    "dropdownMenu.actions",
    "dropdownMenu.layouts",
    "dropdownMenu.panels",
    "dropdownMenu.panels.properties",
    "dropdownMenu.panels.sort",
    "dropdownMenu.panels.filter",
    "selection.selectInAllPages",
    "selection.infoBar.selectedCount",
    "selection.infoBar.allSelected",
    "selection.infoBar.allSelectedBut",
    "pagination.label",
    "pagination.label.empty",
    "pagination.currentEntries",
    "pagination.pageSize",
    "pagination.selectPageSize",
    "pagination.resultsPerPage",
    "pagination.loadPageByNumber",
    "pagination.page",
    "pagination.first",
    "pagination.previous",
    "pagination.next",
    "pagination.last",
    "action.refresh",
    "action.addEntry",
    "action.columnName.sortable.hint",
    "action.columnName.default.hint",
    "action.resizeColumn.hint",
    "panel.filter.title",
    "panel.filter.noneFilterable",
    "panel.filter.addConstraint",
    "panel.filter.addProperty",
    "panel.filter.delete",
    "panel.filter.deleteAll",
    "panel.properties.title",
    "panel.sort.title",
    "panel.sort.noneSortable",
    "panel.sort.direction.ascending",
    "panel.sort.direction.descending",
    "panel.sort.add",
    "panel.sort.delete",
    "displayer.emptyValue",
    "displayer.link.noValue",
    "displayer.boolean.true",
    "displayer.boolean.false",
    "displayer.xObjectProperty.missingDocumentName.errorMessage",
    "displayer.xObjectProperty.failedToRetrieveField.errorMessage",
    "displayer.actions.edit",
    "displayer.actions.followLink",
    "filter.boolean.label",
    "filter.date.label",
    "filter.list.label",
    "filter.list.emptyLabel",
    "filter.number.label",
    "filter.text.label",
    "footnotes.computedTitle",
    "footnotes.propertyNotViewable",
    "bottombar.noEntries",
    "error.updateEntriesFailed",
  ],
};

function buildRequest(translationsURL, locale, prefix, keys) {
  const usp = new URLSearchParams({
    locale: locale,
    prefix: prefix,
  });
  for (let key of keys) {
    usp.append("key", key);
  }
  return `${translationsURL}?${usp.toString()}`;
}

async function getTranslations(locale, prefix, keys) {
  const translationsURL = `${XWiki.contextPath}/rest/wikis/${encodeURIComponent(
    XWiki.currentWiki)}/localization/translations`;
  const input = buildRequest(translationsURL, locale, prefix, keys);

  const res = await fetch(input, {
    headers: {
      Accept: "application/json",
    },
  });

  const translations = (await res.json()).translations;
  const resMap = {};
  for (let value of translations) {
    resMap[value.key] = value.rawSource;
  }
  return resMap;
}

/**
 * @param i18n the i18n instance to initialize
 * @param locale the locale to load
 * @return {Promise<void>} continues once the translation values are fetched remotely
 */
export async function i18nResolver(i18n, locale) {
  const messages = await getTranslations(locale, config.prefix, config.keys);
  i18n.global.setLocaleMessage(locale, messages);
}
