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
package com.xpn.xwiki.plugin.mailsender;

public class MimeTypesUtil
{
    protected static final String[][] MIME_TYPES =
        { {"application/mac-binhex40", "hqx"}, {"application/mac-compactpro", "cpt"},
        {"application/msword", "doc"}, {"application/octet-stream", "bin"},
        {"application/octet-stream", "dms"}, {"application/octet-stream", "lha"},
        {"application/octet-stream", "lzh"}, {"application/octet-stream", "exe"},
        {"application/octet-stream", "class"}, {"application/oda", "oda"},
        {"application/pdf", "pdf"}, {"application/postscript", "ai"},
        {"application/postscript", "eps"}, {"application/postscript", "ps"},
        {"application/powerpoint", "ppt"}, {"application/rtf", "rtf"},
        {"application/x-bcpio", "bcpio"}, {"application/x-cdlink", "vcd"},
        {"application/x-compress", "Z"}, {"application/x-cpio", "cpio"},
        {"application/x-csh", "csh"}, {"application/x-director", "dcr"},
        {"application/x-director", "dir"}, {"application/x-director", "dxr"},
        {"application/x-dvi", "dvi"}, {"application/x-gtar", "gtar"},
        {"application/x-gzip", "gz"}, {"application/x-hdf", "hdf"},
        {"application/x-httpd-cgi", "cgi"}, {"application/x-koan", "skp"},
        {"application/x-koan", "skd"}, {"application/x-koan", "skt"},
        {"application/x-koan", "skm"}, {"application/x-latex", "latex"},
        {"application/x-mif", "mif"}, {"application/x-netcdf", "nc"},
        {"application/x-netcdf", "cdf"}, {"application/x-sh", "sh"},
        {"application/x-shar", "shar"}, {"application/x-stuffit", "sit"},
        {"application/x-sv4cpio", "sv4cpio"}, {"application/x-sv4crc", "sv4crc"},
        {"application/x-tar", "tar"}, {"application/x-tcl", "tcl"}, {"application/x-tex", "tex"},
        {"application/x-texinfo", "texinfo"}, {"application/x-texinfo", "texi"},
        {"application/x-troff", "t"}, {"application/x-troff", "tr"},
        {"application/x-troff", "roff"}, {"application/x-troff-man", "man"},
        {"application/x-troff-me", "me"}, {"application/x-troff-ms", "ms"},
        {"application/x-ustar", "ustar"}, {"application/x-wais-source", "src"},
        {"application/zip", "zip"}, {"audio/basic", "au"}, {"audio/basic", "snd"},
        {"audio/mpeg", "mpga"}, {"audio/mpeg", "mp2"}, {"audio/mpeg", "mp3"},
        {"audio/x-aiff", "aif"}, {"audio/x-aiff", "aiff"}, {"audio/x-aiff", "aifc"},
        {"audio/x-pn-realaudio", "ram"}, {"audio/x-pn-realaudio-plugin", "rpm"},
        {"audio/x-realaudio", "ra"}, {"audio/x-wav", "wav"}, {"chemical/x-pdb", "pdb"},
        {"chemical/x-pdb", "xyz"}, {"image/gif", "gif"}, {"image/ief", "ief"},
        {"image/jpeg", "jpeg"}, {"image/jpeg", "jpg"}, {"image/jpeg", "jpe"},
        {"image/png", "png"}, {"image/tiff", "tiff"}, {"image/tiff", "tif"},
        {"image/x-cmu-raster", "ras"}, {"image/x-portable-anymap", "pnm"},
        {"image/x-portable-bitmap", "pbm"}, {"image/x-portable-graymap", "pgm"},
        {"image/x-portable-pixmap", "ppm"}, {"image/x-rgb", "rgb"}, {"image/x-xbitmap", "xbm"},
        {"image/x-xpixmap", "xpm"}, {"image/x-xwindowdump", "xwd"}, {"text/html", "html"},
        {"text/html", "htm"}, {"text/plain", "txt"}, {"text/richtext", "rtx"},
        {"text/tab-separated-values", "tsv"}, {"text/x-setext", "etx"}, {"text/x-sgml", "sgml"},
        {"text/x-sgml", "sgm"}, {"video/mpeg", "mpeg"}, {"video/mpeg", "mpg"},
        {"video/mpeg", "mpe"}, {"video/quicktime", "qt"}, {"video/quicktime", "mov"},
        {"video/x-msvideo", "avi"}, {"video/x-sgi-movie", "movie"},
        {"x-conference/x-cooltalk", "ice"}, {"x-world/x-vrml", "wrl"}, {"x-world/x-vrml", "vrml"}};


    public static String getMimeTypeFromFilename(String filename)
    {
        int index = filename.lastIndexOf(".");
        String extension = filename;
        if (index != -1) {
            if (index == filename.length())
                return ("application/octet-stream");
            else
                extension = filename.substring(index + 1);
        }
        String type = null;
        for (int i = 0; i < MIME_TYPES.length; i++) {
            if (MIME_TYPES[i][1].equals(extension)) {
               type = MIME_TYPES[i][0];
               break;
            }
        }
        if (type == null)
            return ("application/octet-stream");
        else
            return (type);
    }
    
    public static void main(String[] args) {
        String file = "fsd.png";
        String type = MimeTypesUtil.getMimeTypeFromFilename(file);
        System.out.println(type);
    }

}
