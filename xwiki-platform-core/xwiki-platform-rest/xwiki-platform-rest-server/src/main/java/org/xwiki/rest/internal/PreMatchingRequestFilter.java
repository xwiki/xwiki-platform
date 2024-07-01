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
package org.xwiki.rest.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.rest.XWikiRestComponent;

/**
 * The filter is called before serving any request and it is responsible to:
 * <ul>
 * <li>Set in user in the response header. The user is expected to be authenticate in a preceding filter along with the
 * XWikiContext initialization</li>
 * <li>Restlet retro compatibility:
 * <ul>
 * <li>Allow changing the HTTP request method through the URL parameter "method"</li>
 * <li>Allow changing the HTTP request Accept header through the URL parameter "media"</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
@Component
@Named("org.xwiki.rest.internal.PreMatchingRequestFilter")
@Singleton
@Provider
@PreMatching
public class PreMatchingRequestFilter implements ContainerRequestFilter, XWikiRestComponent
{
    /**
     * Content-types that are allowed in a
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS#simple_requests">simple request</a> that don't
     * trigger a CORS preflight request in browsers.
     */
    private static final List<String> SIMPLE_CONTENT_TYPES =
        List.of("application/x-www-form-urlencoded", "multipart/form-data", "text/plain");

    private static final String FORM_TOKEN_HEADER = "XWiki-Form-Token";

    private static final String ACCEPT_HEADER = "Accept";

    // Copied from Restlet org.restlet.service.MetadataService#mappings
    // Only kept media types supported by JAX-RS
    private static final Map<String, MediaType> ACCEPT_MAPPING = new HashMap<>();

    static {
        // "ai", MediaType.APPLICATION_POSTSCRIPT,
        ACCEPT_MAPPING.put("atom", MediaType.APPLICATION_ATOM_XML_TYPE);
        // "atomcat", MediaType.APPLICATION_ATOMPUB_CATEGORY,
        // "atomsvc", MediaType.APPLICATION_ATOMPUB_SERVICE,
        // "au", MediaType.AUDIO_BASIC,
        ACCEPT_MAPPING.put("bin", MediaType.APPLICATION_OCTET_STREAM_TYPE);
        // "bmp", MediaType.IMAGE_BMP,
        // "class", MediaType.APPLICATION_JAVA,
        // "css", MediaType.TEXT_CSS,
        // "csv", MediaType.TEXT_CSV,
        // "dat", MediaType.TEXT_DAT,
        // "dib", MediaType.IMAGE_BMP,
        // "doc", MediaType.APPLICATION_WORD,
        // "docm", MediaType.APPLICATION_MSOFFICE_DOCM,
        // "docx", MediaType.APPLICATION_MSOFFICE_DOCX,
        // "dotm", MediaType.APPLICATION_MSOFFICE_DOTM,
        // "dotx", MediaType.APPLICATION_MSOFFICE_DOTX,
        // "dtd", MediaType.APPLICATION_XML_DTD,
        // "ecore", MediaType.APPLICATION_ECORE,
        // "eps", MediaType.APPLICATION_POSTSCRIPT,
        ACCEPT_MAPPING.put("exe", MediaType.APPLICATION_OCTET_STREAM_TYPE);
        ACCEPT_MAPPING.put("form", MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        // "gif", MediaType.IMAGE_GIF,
        // "gwt", MediaType.APPLICATION_JAVA_OBJECT_GWT,
        // "hqx", MediaType.APPLICATION_MAC_BINHEX40,
        // "ico", MediaType.IMAGE_ICON,
        // "jad", MediaType.TEXT_J2ME_APP_DESCRIPTOR,
        // "jar", MediaType.APPLICATION_JAVA_ARCHIVE,
        ACCEPT_MAPPING.put("java", MediaType.TEXT_PLAIN_TYPE);
        // "jnlp", MediaType.APPLICATION_JNLP,
        // "jpe", MediaType.IMAGE_JPEG,
        // "jpeg", MediaType.IMAGE_JPEG,
        // "jpg", MediaType.IMAGE_JPEG,
        // "js", MediaType.APPLICATION_JAVASCRIPT,
        ACCEPT_MAPPING.put("jsf", MediaType.TEXT_PLAIN_TYPE);
        // "kar", MediaType.AUDIO_MIDI,
        // "latex", MediaType.APPLICATION_LATEX,
        // "man", MediaType.APPLICATION_TROFF_MAN,
        // "mathml", MediaType.APPLICATION_MATHML,
        // "mid", MediaType.AUDIO_MIDI,
        // "midi", MediaType.AUDIO_MIDI,
        // "mov", MediaType.VIDEO_QUICKTIME,
        // "mp2", MediaType.AUDIO_MPEG,
        // "mp3", MediaType.AUDIO_MPEG,
        // "mp4", MediaType.VIDEO_MP4,
        // "mpe", MediaType.VIDEO_MPEG,
        // "mpeg", MediaType.VIDEO_MPEG,
        // "mpg", MediaType.VIDEO_MPEG,
        // "n3", MediaType.TEXT_RDF_N3,
        ACCEPT_MAPPING.put("nt", MediaType.TEXT_PLAIN_TYPE);
        // "odb", MediaType.APPLICATION_OPENOFFICE_ODB,
        // "odc", MediaType.APPLICATION_OPENOFFICE_ODC,
        // "odf", MediaType.APPLICATION_OPENOFFICE_ODF,
        // "odi", MediaType.APPLICATION_OPENOFFICE_ODI,
        // "odm", MediaType.APPLICATION_OPENOFFICE_ODM,
        // "odg", MediaType.APPLICATION_OPENOFFICE_ODG,
        // "odp", MediaType.APPLICATION_OPENOFFICE_ODP,
        // "ods", MediaType.APPLICATION_OPENOFFICE_ODS,
        // "odt", MediaType.APPLICATION_OPENOFFICE_ODT,
        // "onetoc", MediaType.APPLICATION_MSOFFICE_ONETOC,
        // "onetoc2", MediaType.APPLICATION_MSOFFICE_ONETOC2,
        // "otg", MediaType.APPLICATION_OPENOFFICE_OTG,
        // "oth", MediaType.APPLICATION_OPENOFFICE_OTH,
        // "otp", MediaType.APPLICATION_OPENOFFICE_OTP,
        // "ots", MediaType.APPLICATION_OPENOFFICE_OTS,
        // "ott", MediaType.APPLICATION_OPENOFFICE_OTT,
        // "oxt", MediaType.APPLICATION_OPENOFFICE_OXT,
        // "pdf", MediaType.APPLICATION_PDF,
        // "png", MediaType.IMAGE_PNG,
        // "potx", MediaType.APPLICATION_MSOFFICE_POTX,
        // "potm", MediaType.APPLICATION_MSOFFICE_POTM,
        // "ppam", MediaType.APPLICATION_MSOFFICE_PPAM,
        // "pps", MediaType.APPLICATION_POWERPOINT,
        // "ppsm", MediaType.APPLICATION_MSOFFICE_PPSM,
        // "ppsx", MediaType.APPLICATION_MSOFFICE_PPSX,
        // "ppt", MediaType.APPLICATION_POWERPOINT,
        // "pptm", MediaType.APPLICATION_MSOFFICE_PPTM,
        // "pptx", MediaType.APPLICATION_MSOFFICE_PPTX,
        // "ps", MediaType.APPLICATION_POSTSCRIPT,
        // "qt", MediaType.VIDEO_QUICKTIME,
        // "rdf", MediaType.APPLICATION_RDF_XML,
        // "rnc", MediaType.APPLICATION_RELAXNG_COMPACT,
        // "rng", MediaType.APPLICATION_RELAXNG_XML,
        // "rss", MediaType.APPLICATION_RSS,
        // "rtf", MediaType.APPLICATION_RTF,
        // "sav", MediaType.APPLICATION_SPSS_SAV,
        // "sit", MediaType.APPLICATION_STUFFIT,
        // "sldm", MediaType.APPLICATION_MSOFFICE_SLDM,
        // "sldx", MediaType.APPLICATION_MSOFFICE_SLDX,
        // "snd", MediaType.AUDIO_BASIC,
        // "sps", MediaType.APPLICATION_SPSS_SPS,
        // "sta", MediaType.APPLICATION_STATA_STA,
        // "svg", MediaType.IMAGE_SVG,
        // "swf", MediaType.APPLICATION_FLASH,
        // "tar", MediaType.APPLICATION_TAR,
        // "tex", MediaType.APPLICATION_TEX,
        // "tif", MediaType.IMAGE_TIFF,
        // "tiff", MediaType.IMAGE_TIFF,
        // "tsv", MediaType.TEXT_TSV,
        // "ulw", MediaType.AUDIO_BASIC,
        // "vrml", MediaType.MODEL_VRML,
        // "vxml", MediaType.APPLICATION_VOICEXML,
        // "wadl", MediaType.APPLICATION_WADL,
        // "wav", MediaType.AUDIO_WAV,
        // "wrl", MediaType.MODEL_VRML,
        // "xht", MediaType.APPLICATION_XHTML,
        // "xls", MediaType.APPLICATION_EXCEL,
        // "xlsx", MediaType.APPLICATION_MSOFFICE_XLSX,
        // "xlsm", MediaType.APPLICATION_MSOFFICE_XLSM,
        // "xltx", MediaType.APPLICATION_MSOFFICE_XLTX,
        // "xltm", MediaType.APPLICATION_MSOFFICE_XLTM,
        // "xlsb", MediaType.APPLICATION_MSOFFICE_XLSB,
        // "xlam", MediaType.APPLICATION_MSOFFICE_XLAM,
        // "xmi", MediaType.APPLICATION_XMI,
        // "xsd", MediaType.APPLICATION_W3C_SCHEMA,
        // "xsl", MediaType.APPLICATION_W3C_XSLT,
        // "xslt", MediaType.APPLICATION_W3C_XSLT,
        // "xul", MediaType.APPLICATION_XUL,
        // "yaml", MediaType.APPLICATION_YAML,
        // "yaml", MediaType.TEXT_YAML,
        // "z", MediaType.APPLICATION_COMPRESS,
        // "zip", MediaType.APPLICATION_ZIP,
        ACCEPT_MAPPING.put("htm", MediaType.TEXT_HTML_TYPE);
        ACCEPT_MAPPING.put("html", MediaType.TEXT_HTML_TYPE);
        ACCEPT_MAPPING.put("json", MediaType.APPLICATION_JSON_TYPE);
        // "jsonsmile", MediaType.APPLICATION_JSON_SMILE,
        ACCEPT_MAPPING.put("txt", MediaType.TEXT_PLAIN_TYPE);
        ACCEPT_MAPPING.put("xhtml", MediaType.APPLICATION_XHTML_XML_TYPE);
        // "xml", MediaType.TEXT_XML_TYPE,
        ACCEPT_MAPPING.put("xml", MediaType.APPLICATION_XML_TYPE);
    }

    @Inject
    private CSRFToken csrfToken;

    @Inject
    private Logger logger;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException
    {
        // Require a CSRF token for requests that browsers allow through HTML forms and across origins.
        // See https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS for more information.
        // Compare to the method from the servlet request to avoid the automatic conversion from POST to PUT request.
        // Check for a prefix match to make sure it matches regardless of the supplied parameters (like charset).
        if (HttpMethod.POST.equals(requestContext.getMethod())) {
            if (SIMPLE_CONTENT_TYPES.stream()
                .anyMatch(expectedType -> requestContext.getMediaType() != null && StringUtils
                    .startsWith(StringUtils.lowerCase(requestContext.getMediaType().toString()), expectedType))) {
                String formToken = requestContext.getHeaders().getFirst(FORM_TOKEN_HEADER);

                // Skip the main request handler but allow cleanup if either the CSRF validator failed or the token is
                // invalid.
                if (!this.csrfToken.isTokenValid(formToken)) {
                    requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                        .entity("Invalid or missing form token.").type(MediaType.TEXT_PLAIN).build());
                }
            }

            // Override a few request properties based on some URL parameters URL parameters
            // Allow setting the HTTP method through a "method" URL parameter
            String methodOverride = requestContext.getUriInfo().getQueryParameters().getFirst("method");
            if (StringUtils.isNotEmpty(methodOverride)) {
                requestContext.setMethod(methodOverride);
            }
        }

        // Allow setting the HTTP accept media type through a "media" URL parameter
        String mediaOverride = requestContext.getUriInfo().getQueryParameters().getFirst("media");
        if (StringUtils.isNotEmpty(mediaOverride)) {
            MediaType mediaType = ACCEPT_MAPPING.get(mediaOverride);
            if (mediaType == null) {
                try {
                    mediaType = MediaType.valueOf(mediaOverride);
                } catch (IllegalArgumentException e) {
                    // Ignore badly formed media type
                    this.logger.warn("Bad media type value [{}] provided: {}", mediaOverride,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
            if (mediaType != null) {
                // Replace any potentially existing Accept header by the resolved one
                requestContext.getHeaders().remove(ACCEPT_HEADER);
                requestContext.getHeaders().add(ACCEPT_HEADER, mediaType.toString());
            }
        }
    }
}
