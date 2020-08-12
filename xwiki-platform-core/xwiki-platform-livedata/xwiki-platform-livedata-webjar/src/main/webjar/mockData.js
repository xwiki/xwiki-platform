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


// This is mock data used while the front-end is not communicating with the macros
// The data structure is not definitive, and may change to match the final specs

define({

  query: {
    properties: ["doc_title", "age", "country", "tags", "other"],

    source: {
      id: "...",
      url: "...",
    },

    hiddenFilters: {},

    filters: [
      {
        property: "country",
        matchAll: true,
        constrains: [
          {operator: "equals", value: "france"},
        ],
      },
    ],

    sort: [ {property: "age", descending: false} ],

    offset: 0,
    limit: 25,

  },



  meta: {

    defaultLayout: "table",

    layouts: [
      {
        id: "table",
        name: "Table",
        icon: {iconSetName: 'Font Awesome', cssClass: 'fa fa-table'},
      },
      {
        id: "cards",
        name: "Cards",
        icon: {iconSetName: 'Font Awesome', cssClass: 'fa fa-th'},
        titleProperty: "doc_title",
      },
    ],

    propertyDescriptors: [
      {
        id: "doc_title",
        name: "Name",
        type: "string",
        visible: true,
        displayer: {
          id: 'link',
          propertyHref: "doc_url",
        },
      },
      {
        id: "age",
        name: "Age",
        type: "number",
        visible: true,
      },
      {
        id: "tags",
        name: "Tags",
        type: "list",
        visible: true,
        filter: {
          options: ["Tag 1", "Tag 2", "Tag 3"],
        },
      },
      {
        id: "country",
        name: "Country",
        type: "string",
        visible: true,
      },
      {
        id: "other",
        name: "Autre truc",
        type: "string",
        visible: true,
        displayer: {
          id: "html",
        },
        sortable: false,
        filterable: false,
      },
    ],

    propertyTypes: [
      {
        id: 'string',
        name: 'String',
        displayer: {
          id: 'text',
        },
        sortable: true,
        filterable: true,
        filter: {
          id: 'text'
        },
      },
      {
        id: 'number',
        name: 'Number',
        displayer: {
          id: 'number',
        },
        sortable: true,
        filterable: true,
        filter: {
          id: 'number'
        },
      },
      {
        id: 'list',
        name: 'List',
        displayer: {
          id: 'list',
        },
        sortable: true,
        filterable: true,
        filter: {
          id: 'list'
        },
      },
    ],

    defaultFilter: 'text',

    filters: [
      {
        id: "text",
        defaultOperator: "contains",
        operators: [
          { id: "contains", name: "Contains", },
          { id: "equals", name: "Equals", },
          { id: "nequals", name: "Not Equals", },
        ],
      },
      {
        id: "list",
        defaultOperator: "is",
        operators: [
          { id: "is", name: "Is", },
          { id: "nis", name: "Is Not", },
        ],
      },
      {
        id: "number",
        defaultOperator: "equals",
        operators: [
          { id: "equals", name: "=", },
          { id: "nequals", name: "≠", },
          { id: "lower", name: "<", },
          { id: "greater", name: ">", },
        ],
      },
    ],

    defaultDisplayer: 'text',

    displayers: [
      {
        id: "text",
      },
      {
        id: "link",
      },
      {
        id: "html",
      },
    ],


    pagination: {
      maxShownPages: 10,
      pageSizes: [10, 25, 50, 100, 250],
      showNextPrevious: true,
      showFirstLast: false,
      showPageSizeDropdown: false,
    },


    entryDescriptor: {
      idProperty: "doc_url",
      propertySaveHref: "save_url",
    },

  },



  data: {

    count: 545,

    entries: [
      {
        "doc_url": "#link1",
        "doc_name": "Name1",
        "doc_date": "2020/03/27 13:23",
        "doc_title": "Title 1",
        "doc_author": "Author 1",
        "doc_creationDate": "2020/03/27 13:21",
        "doc_creator": "Creator 1",
        "age": 48,
        "tags": ["Tag 1"],
        "country": "France",
        "other": "<em>lorem ipsum<em>",
      },
      {
        "doc_url": "#link2",
        "doc_name": "Name2",
        "doc_date": "2020/04/22 14:07",
        "doc_title": "Title 2",
        "doc_author": "Author 2",
        "doc_creationDate": "2020/04/22 14:06",
        "doc_creator": "Creator 2",
        "age": 24,
        "tags": ["Tag 2"],
        "country": "France",
        "other": "<strong>dorol sit amet<strong>",
      },
      {
        "doc_url": "#link3",
        "doc_name": "Name3",
        "doc_date": "2020/03/27 14:34",
        "doc_title": "Title 3",
        "doc_author": "Author 3",
        "doc_creationDate": "2020/03/27 14:34",
        "doc_creator": "Creator 3",
        "age": 12,
        "tags": ["Tag 3"],
        "country": "Romania",
        "other": "<span style='color:red'>consequtir</span>",
      },
      {
        "doc_url": "#link4",
        "doc_name": "Name4",
        "doc_date": "2020/03/27 14:34",
        "doc_title": "Title 4",
        "doc_author": "Author 4",
        "doc_creationDate": "2020/03/27 14:34",
        "doc_creator": "Creator 4",
        "age": 52,
        "country": "Romania",
      },
    ],

  },

});