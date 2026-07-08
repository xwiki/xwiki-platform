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
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private static final String APPLICATION_POSTSCRIPT = "application/postscript";

    private static final String APPLICATION_X_DIRECTOR = "application/x-director";

    private static final String APPLICATION_X_KOAN = "application/x-koan";

    private static final String APPLICATION_X_TROFF = "application/x-troff";

    private static final String AUDIO_MPEG = "audio/mpeg";

    private static final String AUDIO_X_AIFF = "audio/x-aiff";

    private static final String IMAGE_JPEG = "image/jpeg";

    private static final String VIDEO_MPEG = "video/mpeg";

    protected static final String[][] MIME_TYPES =
        { {"application/mac-binhex40", "hqx"}, {"application/mac-compactpro", "cpt"},
        {"application/msword", "doc"}, {APPLICATION_OCTET_STREAM, "bin"},
        {APPLICATION_OCTET_STREAM, "dms"}, {APPLICATION_OCTET_STREAM, "lha"},
        {APPLICATION_OCTET_STREAM, "lzh"}, {APPLICATION_OCTET_STREAM, "exe"},
        {APPLICATION_OCTET_STREAM, "class"}, {"application/oda", "oda"},
        {"application/pdf", "pdf"}, {APPLICATION_POSTSCRIPT, "ai"},
        {APPLICATION_POSTSCRIPT, "eps"}, {APPLICATION_POSTSCRIPT, "ps"},
        {"application/powerpoint", "ppt"}, {"application/rtf", "rtf"},
        {"application/x-bcpio", "bcpio"}, {"application/x-cdlink", "vcd"},
        {"application/x-compress", "Z"}, {"application/x-cpio", "cpio"},
        {"application/x-csh", "csh"}, {APPLICATION_X_DIRECTOR, "dcr"},
        {APPLICATION_X_DIRECTOR, "dir"}, {APPLICATION_X_DIRECTOR, "dxr"},
        {"application/x-dvi", "dvi"}, {"application/x-gtar", "gtar"},
        {"application/x-gzip", "gz"}, {"application/x-hdf", "hdf"},
        {"application/x-httpd-cgi", "cgi"}, {APPLICATION_X_KOAN, "skp"},
        {APPLICATION_X_KOAN, "skd"}, {APPLICATION_X_KOAN, "skt"},
        {APPLICATION_X_KOAN, "skm"}, {"application/x-latex", "latex"},
        {"application/x-mif", "mif"}, {"application/x-netcdf", "nc"},
        {"application/x-netcdf", "cdf"}, {"application/x-sh", "sh"},
        {"application/x-shar", "shar"}, {"application/x-stuffit", "sit"},
        {"application/x-sv4cpio", "sv4cpio"}, {"application/x-sv4crc", "sv4crc"},
        {"application/x-tar", "tar"}, {"application/x-tcl", "tcl"}, {"application/x-tex", "tex"},
        {"application/x-texinfo", "texinfo"}, {"application/x-texinfo", "texi"},
        {APPLICATION_X_TROFF, "t"}, {APPLICATION_X_TROFF, "tr"},
        {APPLICATION_X_TROFF, "roff"}, {"application/x-troff-man", "man"},
        {"application/x-troff-me", "me"}, {"application/x-troff-ms", "ms"},
        {"application/x-ustar", "ustar"}, {"application/x-wais-source", "src"},
        {"application/zip", "zip"}, {"audio/basic", "au"}, {"audio/basic", "snd"},
        {AUDIO_MPEG, "mpga"}, {AUDIO_MPEG, "mp2"}, {AUDIO_MPEG, "mp3"},
        {AUDIO_X_AIFF, "aif"}, {AUDIO_X_AIFF, "aiff"}, {AUDIO_X_AIFF, "aifc"},
        {"audio/x-pn-realaudio", "ram"}, {"audio/x-pn-realaudio-plugin", "rpm"},
        {"audio/x-realaudio", "ra"}, {"audio/x-wav", "wav"}, {"chemical/x-pdb", "pdb"},
        {"chemical/x-pdb", "xyz"}, {"image/gif", "gif"}, {"image/ief", "ief"},
        {IMAGE_JPEG, "jpeg"}, {IMAGE_JPEG, "jpg"}, {IMAGE_JPEG, "jpe"},
        {"image/png", "png"}, {"image/tiff", "tiff"}, {"image/tiff", "tif"},
        {"image/x-cmu-raster", "ras"}, {"image/x-portable-anymap", "pnm"},
        {"image/x-portable-bitmap", "pbm"}, {"image/x-portable-graymap", "pgm"},
        {"image/x-portable-pixmap", "ppm"}, {"image/x-rgb", "rgb"}, {"image/x-xbitmap", "xbm"},
        {"image/x-xpixmap", "xpm"}, {"image/x-xwindowdump", "xwd"}, {"text/html", "html"},
        {"text/html", "htm"}, {"text/plain", "txt"}, {"text/richtext", "rtx"},
        {"text/tab-separated-values", "tsv"}, {"text/x-setext", "etx"}, {"text/x-sgml", "sgml"},
        {"text/x-sgml", "sgm"}, {VIDEO_MPEG, "mpeg"}, {VIDEO_MPEG, "mpg"},
        {VIDEO_MPEG, "mpe"}, {"video/quicktime", "qt"}, {"video/quicktime", "mov"},
        {"video/x-msvideo", "avi"}, {"video/x-sgi-movie", "movie"},
        {"x-conference/x-cooltalk", "ice"}, {"x-world/x-vrml", "wrl"}, {"x-world/x-vrml", "vrml"}};


    public static String getMimeTypeFromFilename(String filename)
    {
        int index = filename.lastIndexOf(".");
        String extension = filename;
        if (index != -1) {
            if (index == filename.length())
                return (APPLICATION_OCTET_STREAM);
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
            return (APPLICATION_OCTET_STREAM);
        else
            return (type);
    }
    
    public static void main(String[] args) {
        String file = "fsd.png";
        String type = MimeTypesUtil.getMimeTypeFromFilename(file);
        System.out.println(type);
    }

}
