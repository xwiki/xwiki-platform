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
    "name": "OpenDocument Text",
    "extensions": [
      "odt"
    ],
    "mediaType": "application/vnd.oasis.opendocument.text",
    "inputFamily": "TEXT",
    "storeProperties": {
      "TEXT": {
        "FilterName": "writer8"
      },
      "WEB": {
        "FilterName": "writerweb8_writer"
      }
    }
  },
  {
    "name": "OpenDocument Text Template",
    "extensions": [
      "ott"
    ],
    "mediaType": "application/vnd.oasis.opendocument.text-template",
    "inputFamily": "TEXT",
    "storeProperties": {
      "TEXT": {
        "FilterName": "writer8_template"
      },
      "WEB": {
        "FilterName": "writerweb8_writer_template"
      }
    }
  },
  {
    "name": "OpenDocument Text Flat XML",
    "extensions": [
      "fodt"
    ],
    "mediaType": "application/vnd.oasis.opendocument.text-flat-xml",
    "inputFamily": "TEXT",
    "storeProperties": {
      "TEXT": {
        "FilterName": "OpenDocument Text Flat XML"
      }
    }
  },
  {
    "name": "Microsoft Word 2007-2013 XML",
    "extensions": [
      "docx"
    ],
    "mediaType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "inputFamily": "TEXT",
    "storeProperties": {
      "TEXT": {
        "FilterName": "MS Word 2007 XML"
      }
    }
  },
  {
    "name": "Word Open XML Document Template",
    "extensions": [
      "dotx"
    ],
    "mediaType": "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
    "inputFamily": "TEXT",
    "storeProperties": {
      "TEXT": {
        "FilterName": "MS Word 2007 XML"
      }
    }
  },
  {
    "name": "Microsoft Word 97-2003",
    "extensions": [
      "doc"
    ],
    "mediaType": "application/msword",
    "inputFamily": "TEXT",
    "storeProperties": {
      "TEXT": {
        "FilterName": "MS Word 97"
      }
    }
  },
  {
    "name": "HTML",
    "extensions": [
      "html"
    ],
    "mediaType": "text/html",
    "inputFamily": "WEB",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "HTML (StarCalc)"
      },
      "PRESENTATION": {
        "FilterName": "impress_html_Export",
        "FilterData": {
          "PublishMode": 0
        }
      },
      "TEXT": {
        "FilterName": "HTML (StarWriter)"
      }
    }
  },
  {
    "name": "XHTML",
    "extensions": [
      "xhtml"
    ],
    "mediaType": "application/xhtml+xml",
    "inputFamily": "TEXT",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "XHTML Calc File"
      },
      "PRESENTATION": {
        "FilterName": "XHTML Impress File"
      },
      "TEXT": {
        "FilterName": "XHTML Writer File"
      }
    }
  },
  {
    "name": "Rich Text Format",
    "extensions": [
      "rtf"
    ],
    "mediaType": "text/rtf",
    "inputFamily": "TEXT",
    "storeProperties": {
      "TEXT": {
        "FilterName": "Rich Text Format"
      }
    }
  },
  {
    "name": "Plain Text",
    "extensions": [
      "txt"
    ],
    "mediaType": "text/plain",
    "inputFamily": "TEXT",
    "loadProperties": {
      "FilterName": "Text (encoded)",
      "FilterOptions": "utf8"
    },
    "storeProperties": {
      "TEXT": {
        "FilterName": "Text (encoded)",
        "FilterOptions": "utf8"
      }
    }
  },
  {
    "name": "OpenOffice.org 1.0 Text Document",
    "extensions": [
      "sxw"
    ],
    "mediaType": "application/vnd.sun.xml.writer",
    "inputFamily": "TEXT",
    "storeProperties": {
      "TEXT": {
        "FilterName": "StarOffice XML (Writer)"
      }
    }
  },
  {
    "name": "WordPerfect",
    "extensions": [
      "wpd"
    ],
    "mediaType": "application/wordperfect",
    "inputFamily": "TEXT"
  },
  {
    "name": "OpenDocument Spreadsheet",
    "extensions": [
      "ods"
    ],
    "mediaType": "application/vnd.oasis.opendocument.spreadsheet",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "calc8"
      }
    }
  },
  {
    "name": "OpenDocument Spreadsheet Template",
    "extensions": [
      "ots"
    ],
    "mediaType": "application/vnd.oasis.opendocument.spreadsheet-template",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "calc8_template"
      }
    }
  },
  {
    "name": "OpenDocument Spreadsheet Flat XML",
    "extensions": [
      "fods"
    ],
    "mediaType": "application/vnd.oasis.opendocument.spreadsheet-flat-xml",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "OpenDocument Spreadsheet Flat XML"
      }
    }
  },
  {
    "name": "Microsoft Excel 2007-2013 XML",
    "extensions": [
      "xlsx"
    ],
    "mediaType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "Calc MS Excel 2007 XML"
      }
    }
  },
  {
    "name": "Microsoft Excel 2007-2013 XML Template",
    "extensions": [
      "xltx"
    ],
    "mediaType": "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "Calc MS Excel 2007 XML"
      }
    }
  },
  {
    "name": "Microsoft Excel 97-2003",
    "extensions": [
      "xls"
    ],
    "mediaType": "application/vnd.ms-excel",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "MS Excel 97"
      }
    }
  },
  {
    "name": "Comma Separated Values",
    "extensions": [
      "csv"
    ],
    "mediaType": "text/csv",
    "inputFamily": "SPREADSHEET",
    "loadProperties": {
      "FilterName": "Text - txt - csv (StarCalc)",
      "FilterOptions": "44,34,0"
    },
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "Text - txt - csv (StarCalc)",
        "FilterOptions": "44,34,0"
      }
    }
  },
  {
    "name": "Tab Separated Values",
    "extensions": [
      "tsv"
    ],
    "mediaType": "text/tab-separated-values",
    "inputFamily": "SPREADSHEET",
    "loadProperties": {
      "FilterName": "Text - txt - csv (StarCalc)",
      "FilterOptions": "9,34,0"
    },
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "Text - txt - csv (StarCalc)",
        "FilterOptions": "9,34,0"
      }
    }
  },
  {
    "name": "OpenOffice.org 1.0 Spreadsheet",
    "extensions": [
      "sxc"
    ],
    "mediaType": "application/vnd.sun.xml.calc",
    "inputFamily": "SPREADSHEET",
    "storeProperties": {
      "SPREADSHEET": {
        "FilterName": "StarOffice XML (Calc)"
      }
    }
  },
  {
    "name": "OpenDocument Presentation",
    "extensions": [
      "odp"
    ],
    "mediaType": "application/vnd.oasis.opendocument.presentation",
    "inputFamily": "PRESENTATION",
    "storeProperties": {
      "PRESENTATION": {
        "FilterName": "impress8"
      }
    }
  },
  {
    "name": "OpenDocument Presentation Template",
    "extensions": [
      "otp"
    ],
    "mediaType": "application/vnd.oasis.opendocument.presentation-template",
    "inputFamily": "PRESENTATION",
    "storeProperties": {
      "PRESENTATION": {
        "FilterName": "impress8_template"
      }
    }
  },
  {
    "name": "OpenDocument Presentation Flat XML",
    "extensions": [
      "fodp"
    ],
    "mediaType": "application/vnd.oasis.opendocument.presentation-flat-xml",
    "inputFamily": "PRESENTATION",
    "storeProperties": {
      "PRESENTATION": {
        "FilterName": "OpenDocument Presentation Flat XML"
      }
    }
  },
  {
    "name": "Microsoft PowerPoint 2007-2013 XML",
    "extensions": [
      "pptx"
    ],
    "mediaType": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "inputFamily": "PRESENTATION",
    "storeProperties": {
      "PRESENTATION": {
        "FilterName": "Impress MS PowerPoint 2007 XML"
      }
    }
  },
  {
    "name": "Microsoft PowerPoint 2007-2013 XML Template",
    "extensions": [
      "potx"
    ],
    "mediaType": "application/vnd.openxmlformats-officedocument.presentationml.template",
    "inputFamily": "PRESENTATION",
    "storeProperties": {
      "PRESENTATION": {
        "FilterName": "Impress MS PowerPoint 2007 XML"
      }
    }
  },
  {
    "name": "Microsoft PowerPoint 97-2003",
    "extensions": [
      "ppt"
    ],
    "mediaType": "application/vnd.ms-powerpoint",
    "inputFamily": "PRESENTATION",
    "storeProperties": {
      "PRESENTATION": {
        "FilterName": "MS PowerPoint 97"
      }
    }
  },
  {
    "name": "OpenOffice.org 1.0 Presentation",
    "extensions": [
      "sxi"
    ],
    "mediaType": "application/vnd.sun.xml.impress",
    "inputFamily": "PRESENTATION",
    "storeProperties": {
      "PRESENTATION": {
        "FilterName": "StarOffice XML (Impress)"
      }
    }
  },
  {
    "name": "OpenDocument Drawing",
    "extensions": [
      "odg"
    ],
    "mediaType": "application/vnd.oasis.opendocument.graphics",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw8"
      }
    }
  },
  {
    "name": "OpenDocument Drawing Template",
    "extensions": [
      "otg"
    ],
    "mediaType": "application/vnd.oasis.opendocument.graphics-template",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw8_template"
      }
    }
  },
  {
    "name": "OpenDocument Drawing Flat XML",
    "extensions": [
      "fodg"
    ],
    "mediaType": "application/vnd.oasis.opendocument.graphics-flat-xml",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "OpenDocument Drawing Flat XML"
      }
    }
  },
  {
    "name": "Portable Document Format",
    "extensions": [
      "pdf"
    ],
    "mediaType": "application/pdf",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_pdf_Export"
      },
      "SPREADSHEET": {
        "FilterName": "calc_pdf_Export"
      },
      "PRESENTATION": {
        "FilterName": "impress_pdf_Export"
      },
      "TEXT": {
        "FilterName": "writer_pdf_Export"
      },
      "WEB": {
        "FilterName": "writer_web_pdf_Export"
      }
    }
  },
  {
    "name": "Macromedia Flash",
    "extensions": [
      "swf"
    ],
    "mediaType": "application/x-shockwave-flash",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_flash_Export"
      },
      "PRESENTATION": {
        "FilterName": "impress_flash_Export"
      }
    }
  },
  {
    "name": "Visio XML",
    "extensions": [
      "vsdx"
    ],
    "mediaType": "application/vnd-ms-visio.drawing",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_pdf_Export"
      }
    }
  },
  {
    "name": "Visio",
    "extensions": [
      "vsd"
    ],
    "mediaType": "application/vnd-visio",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_pdf_Export"
      }
    }
  },
  {
    "name": "Windows Bitmap",
    "extensions": [
      "bmp"
    ],
    "mediaType": "image/bmp",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_bmp_Export"
      },
      "PRESENTATION": {
        "FilterName": "impress_bmp_Export"
      }
    }
  },
  {
    "name": "Graphics Interchange Format",
    "extensions": [
      "gif"
    ],
    "mediaType": "image/gif",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_gif_Export"
      },
      "PRESENTATION": {
        "FilterName": "impress_gif_Export"
      }
    }
  },
  {
    "name": "Joint Photographic Experts Group",
    "extensions": [
      "jpg",
      "jpeg"
    ],
    "mediaType": "image/jpeg",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_jpg_Export"
      },
      "SPREADSHEET": {
        "FilterName": "calc_jpg_Export"
      },
      "PRESENTATION": {
        "FilterName": "impress_jpg_Export"
      },
      "TEXT": {
        "FilterName": "writer_jpg_Export"
      },
      "WEB": {
        "FilterName": "writer_web_jpg_Export"
      }
    }
  },
  {
    "name": "Portable Network Graphics",
    "extensions": [
      "png"
    ],
    "mediaType": "image/png",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_png_Export"
      },
      "SPREADSHEET": {
        "FilterName": "calc_png_Export"
      },
      "PRESENTATION": {
        "FilterName": "impress_png_Export"
      },
      "TEXT": {
        "FilterName": "writer_png_Export"
      },
      "WEB": {
        "FilterName": "writer_web_png_Export"
      }
    }
  },
  {
    "name": "Scalable Vector Graphics",
    "extensions": [
      "svg"
    ],
    "mediaType": "image/svg+xml",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_svg_Export"
      },
      "SPREADSHEET": {
        "FilterName": "calc_svg_Export"
      },
      "PRESENTATION": {
        "FilterName": "impress_svg_Export"
      },
      "TEXT": {
        "FilterName": "writer_svg_Export"
      },
      "WEB": {
        "FilterName": "writer_svg_Export"
      }
    }
  },
  {
    "name": "Tagged Image File Format",
    "extensions": [
      "tif",
      "tiff"
    ],
    "mediaType": "image/tiff",
    "inputFamily": "DRAWING",
    "storeProperties": {
      "DRAWING": {
        "FilterName": "draw_tif_Export"
      },
      "PRESENTATION": {
        "FilterName": "impress_tif_Export"
      }
    }
  }
]
