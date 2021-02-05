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
[
  {
    "name": "Portable Document Format",
    "extensions": ["pdf"],
    "mediaType": "application/pdf",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {"FilterName": "draw_pdf_Export"},
      "SPREADSHEET": {"FilterName": "calc_pdf_Export"},
      "PRESENTATION": {"FilterName": "impress_pdf_Export"},
      "TEXT": {"FilterName": "writer_pdf_Export"}
    }
  },
  {
    "name": "Macromedia Flash",
    "extensions": ["swf"],
    "mediaType": "application/x-shockwave-flash",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {"FilterName": "draw_flash_Export"},
      "PRESENTATION": {"FilterName": "impress_flash_Export"}
    }
  },
  {
    "name": "HTML",
    "extensions": ["html"],
    "mediaType": "text/html",
    "inputFamily": "TEXT",
    "storeProperties": {
      "SPREADSHEET": {"FilterName": "HTML (StarCalc)"},
      "PRESENTATION": {
        "FilterName": "impress_html_Export",
        "FilterData": {"PublishMode": 0}
      },
      "TEXT": {"FilterName": "HTML (StarWriter)"}
    }
  },
  {
    "name": "OpenDocument Text",
    "extensions": ["odt"],
    "mediaType": "application/vnd.oasis.opendocument.text",
    "inputFamily": "TEXT",
    "storeProperties": {"TEXT": {"FilterName": "writer8"}}
  },
  {
    "name": "OpenOffice.org 1.0 Text Document",
    "extensions": ["sxw"],
    "mediaType": "application/vnd.sun.xml.writer",
    "inputFamily": "TEXT",
    "storeProperties": {"TEXT": {"FilterName": "StarOffice XML (Writer)"}}
  },
  {
    "name": "Microsoft Word",
    "extensions": ["doc"],
    "mediaType": "application/msword",
    "inputFamily": "TEXT",
    "storeProperties": {"TEXT": {"FilterName": "MS Word 97"}}
  },
  {
    "name": "Microsoft Word 2007 XML",
    "extensions": ["docx"],
    "mediaType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "inputFamily": "TEXT"
  },
  {
    "name": "Rich Text Format",
    "extensions": ["rtf"],
    "mediaType": "text/rtf",
    "inputFamily": "TEXT",
    "storeProperties": {"TEXT": {"FilterName": "Rich Text Format"}}
  },
  {
    "name": "WordPerfect",
    "extensions": ["wpd"],
    "mediaType": "application/wordperfect",
    "inputFamily": "TEXT"
  },
  {
    "name": "Plain Text",
    "extensions": ["txt"],
    "mediaType": "text/plain",
    "inputFamily": "TEXT",
    "loadProperties": {
      "FilterName": "Text (encoded)",
      "FilterOptions": "utf8"
    },
    "storeProperties": {"TEXT": {
        "FilterName": "Text (encoded)",
        "FilterOptions": "utf8"
      }}
  },
  {
    "name": "MediaWiki wikitext",
    "extensions": ["wiki"],
    "mediaType": "text/x-wiki",
    "inputFamily": "TEXT",
    "storeProperties": {"TEXT": {"FilterName": "MediaWiki"}}
  },
  {
    "name": "OpenDocument Spreadsheet",
    "extensions": ["ods"],
    "mediaType": "application/vnd.oasis.opendocument.spreadsheet",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {"SPREADSHEET": {"FilterName": "calc8"}}
  },
  {
    "name": "OpenOffice.org 1.0 Spreadsheet",
    "extensions": ["sxc"],
    "mediaType": "application/vnd.sun.xml.calc",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {"SPREADSHEET": {"FilterName": "StarOffice XML (Calc)"}}
  },
  {
    "name": "Microsoft Excel",
    "extensions": ["xls"],
    "mediaType": "application/vnd.ms-excel",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {"SPREADSHEET": {"FilterName": "MS Excel 97"}}
  },
  {
    "name": "Microsoft Excel 2007 XML",
    "extensions": ["xlsx"],
    "mediaType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "inputFamily": "SPREADSHEET"
  },
  {
    "name": "Comma Separated Values",
    "extensions": ["csv"],
    "mediaType": "text/csv",
    "inputFamily": "SPREADSHEET",
    "loadProperties": {
      "FilterName": "Text - txt - csv (StarCalc)",
      "FilterOptions": "44,34,0"
    },
    "storeProperties": {"SPREADSHEET": {
        "FilterName": "Text - txt - csv (StarCalc)",
        "FilterOptions": "44,34,0"
      }}
  },
  {
    "name": "Tab Separated Values",
    "extensions": ["tsv"],
    "mediaType": "text/tab-separated-values",
    "inputFamily": "SPREADSHEET",
    "loadProperties": {
      "FilterName": "Text - txt - csv (StarCalc)",
      "FilterOptions": "9,34,0"
    },
    "storeProperties": {"SPREADSHEET": {
        "FilterName": "Text - txt - csv (StarCalc)",
        "FilterOptions": "9,34,0"
      }}
  },
  {
    "name": "OpenDocument Presentation",
    "extensions": ["odp"],
    "mediaType": "application/vnd.oasis.opendocument.presentation",
    "inputFamily": "PRESENTATION",
    "storeProperties": {"PRESENTATION": {"FilterName": "impress8"}}
  },
  {
    "name": "OpenOffice.org 1.0 Presentation",
    "extensions": ["sxi"],
    "mediaType": "application/vnd.sun.xml.impress",
    "inputFamily": "PRESENTATION",
    "storeProperties": {"PRESENTATION": {"FilterName": "StarOffice XML (Impress)"}}
  },
  {
    "name": "Microsoft PowerPoint",
    "extensions": ["ppt"],
    "mediaType": "application/vnd.ms-powerpoint",
    "inputFamily": "PRESENTATION",
    "storeProperties": {"PRESENTATION": {"FilterName": "MS PowerPoint 97"}}
  },
  {
    "name": "Microsoft PowerPoint 2007 XML",
    "extensions": ["pptx"],
    "mediaType": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "inputFamily": "PRESENTATION"
  },
  {
    "name": "OpenDocument Drawing",
    "extensions": ["odg"],
    "mediaType": "application/vnd.oasis.opendocument.graphics",
    "inputFamily": "DRAWING",
    "storeProperties": {"DRAWING": {"FilterName": "draw8"}}
  },
  {
    "name": "Scalable Vector Graphics",
    "extensions": ["svg"],
    "mediaType": "image/svg+xml",
    "inputFamily": "DRAWING",
    "storeProperties": {"DRAWING": {"FilterName": "draw_svg_Export"}}
  }
]
