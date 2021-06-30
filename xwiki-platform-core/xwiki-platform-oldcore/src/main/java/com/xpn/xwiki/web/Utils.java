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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.struts2.dispatcher.multipart.MultiPartRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.util.Util;

public class Utils
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    /** A key that is used for placing a map of replaced (for protection) strings in the context. */
    private static final String PLACEHOLDERS_CONTEXT_KEY = Utils.class.getCanonicalName() + "_placeholders";

    /** Whether placeholders are enabled or not. */
    private static final String PLACEHOLDERS_ENABLED_CONTEXT_KEY = Utils.class.getCanonicalName()
        + "_placeholders_enabled";

    /**
     * The component manager used by {@link #getComponent(Class)} and {@link #getComponent(Class, String)}. It is useful
     * for any non component code that need to initialize/access components.
     */
    private static ComponentManager rootComponentManager;

    /**
     * Generate the response by parsing a velocity template and printing the result to the {@link XWikiResponse
     * Response}. This is the main entry point to the View part of the XWiki MVC architecture.
     *
     * @param template The name of the template to parse, without the {@code .vm} prefix. The template will be searched
     *            in the usual places: current XWikiSkins object, attachment of the current skin document, current skin
     *            folder, baseskin folder, /templates/ folder.
     * @param context the current context
     * @throws XWikiException when the response cannot be written to the client (for example when the client canceled
     *             the request, thus closing the socket)
     * @see XWiki#parseTemplate(String, XWikiContext)
     */
    public static void parseTemplate(String template, XWikiContext context) throws XWikiException
    {
        parseTemplate(template, true, context);
    }

    /**
     * Generate the response by parsing a velocity template and (optionally) printing the result to the
     * {@link XWikiResponse Response}.
     *
     * @param template The name of the template to parse, without the {@code .vm} prefix. The template will be searched
     *            in the usual places: current XWikiSkins object, attachment of the current skin document, current skin
     *            folder, baseskin folder, /templates/ folder.
     * @param write Whether the generated response should be written to the client or not. If {@code false}, only the
     *            needed headers are generated, suitable for implementing a HEAD response.
     * @param context the current context
     * @throws XWikiException when the response cannot be written to the client (for example when the client canceled
     *             the request, thus closing the socket)
     * @see XWiki#parseTemplate(String, XWikiContext)
     */
    public static void parseTemplate(String template, boolean write, XWikiContext context) throws XWikiException
    {
        XWikiResponse response = context.getResponse();

        // If a Redirect has already been sent then don't process the template since it means and we shouldn't write
        // anymore to the servlet output stream!
        // See: http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletResponse.html#sendRedirect(String)
        // "After using this method, the response should be considered to be committed and should not be written
        // to."
        if ((response instanceof XWikiServletResponse)
            && ((XWikiServletResponse) response).getStatus() == HttpServletResponse.SC_FOUND) {
            return;
        }

        // Set content-type and encoding (this can be changed later by pages themselves)
        response.setContentType("text/html; charset=" + context.getWiki().getEncoding());

        String action = context.getAction();
        long cacheSetting = context.getWiki().getXWikiPreferenceAsLong("headers_nocache", -1, context);
        if (cacheSetting == -1) {
            cacheSetting = context.getWiki().ParamAsLong("xwiki.httpheaders.cache", -1);
        }
        if (cacheSetting == -1) {
            cacheSetting = 1;
        }
        if ((!"download".equals(action)) && (!"skin".equals(action))) {
            if (context.getResponse() instanceof XWikiServletResponse) {
                // Add a last modified to tell when the page was last updated
                if (context.getWiki().getXWikiPreferenceAsLong("headers_lastmodified", 0, context) != 0) {
                    if (context.getDoc() != null) {
                        response.setDateHeader("Last-Modified", context.getDoc().getDate().getTime());
                    }
                }
                // Set a nocache to make sure the page is reloaded after an edit
                if (cacheSetting == 1) {
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Cache-Control", "no-cache");
                } else if (cacheSetting == 2) {
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Cache-Control", "max-age=0, no-cache, no-store");
                } else if (cacheSetting == 3) {
                    response.setHeader("Cache-Control", "private");
                } else if (cacheSetting == 4) {
                    response.setHeader("Cache-Control", "public");
                }

                // Set an expires in one month
                long expires = context.getWiki().getXWikiPreferenceAsLong("headers_expires", -1, context);
                if (expires == -1) {
                    response.setDateHeader("Expires", -1);
                } else if (expires != 0) {
                    response.setDateHeader("Expires", (new Date()).getTime() + 30 * 24 * 3600 * 1000L);
                }
            }
        }

        if (("download".equals(action)) || ("skin".equals(action))) {
            // Set a nocache to make sure these files are not cached by proxies
            if (cacheSetting == 1 || cacheSetting == 2) {
                response.setHeader("Cache-Control", "no-cache");
            }
        }

        context.getWiki().getPluginManager().beginParsing(context);
        // This class allows various components in the rendering chain to use placeholders for some fragile data. For
        // example, the URL generated for the image macro should not be further rendered, as it might get broken by wiki
        // filters. For this to work, keep a map of [used placeholders -> values] in the context, and replace them when
        // the content is fully rendered. The rendering code can use Utils.createPlaceholder.
        // Initialize the placeholder map
        enablePlaceholders(context);
        String content = "";
        try {
            // Note: This line below can change the state of the response. For example a vm file can have a call to
            // sendRedirect. In this case we need to be careful to not write to the output stream since it's already
            // been committed. This is why we do a check below before calling response.getOutputStream().write().
            content = context.getWiki().evaluateTemplate(template + ".vm", context);
            // Replace all placeholders with the protected values
            content = replacePlaceholders(content, context);
            disablePlaceholders(context);
            content = context.getWiki().getPluginManager().endParsing(content.trim(), context);
        } catch (IOException e) {
            LOGGER.debug("IOException while evaluating template [{}] from /templates/", template, e);

            // get Error template "This template does not exist
            try {
                content = context.getWiki().evaluateTemplate("templatedoesnotexist.vm", context);
                content = content.trim();
            } catch (IOException ex) {
                // Cannot write output, can't do anything else
            }
        }

        if (!context.isFinished()) {
            if (context.getResponse() instanceof XWikiServletResponse) {
                // Set the content length to the number of bytes, not the
                // string length, so as to handle multi-byte encodings
                try {
                    response.setContentLength(content.getBytes(context.getWiki().getEncoding()).length);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            // We only write if the caller has asked.
            // We also make sure to verify that there hasn't been a call to sendRedirect before since it would mean the
            // response has already been written to and we shouldn't try to write in it.
            if (write
                && ((response instanceof XWikiServletResponse) && ((XWikiServletResponse) response).getStatus() != HttpServletResponse.SC_FOUND)) {
                try {
                    try {
                        response.getOutputStream().write(content.getBytes(context.getWiki().getEncoding()));
                    } catch (IllegalStateException ex) {
                        response.getWriter().write(content);
                    }
                } catch (IOException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                        XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while sending response", e);
                }
            }
        }

        try {
            response.getOutputStream().flush();
        } catch (Throwable ex) {
            try {
                response.getWriter().flush();
            } catch (Throwable ex2) {
            }
        }
    }

    /**
     * Retrieve the URL to which the client should be redirected after the successful completion of the requested
     * action. This is taken from the {@code xredirect} parameter in the query string. If this parameter is not set, or
     * is set to an empty value, return the default redirect specified as the second argument.
     *
     * @param request the current request
     * @param defaultRedirect the default value to use if no {@code xredirect} parameter is present
     * @return the destination URL, as specified in the {@code xredirect} parameter, or the specified default URL
     */
    public static String getRedirect(XWikiRequest request, String defaultRedirect)
    {
        String redirect = request.getParameter("xredirect");
        if (StringUtils.isBlank(redirect)) {
            redirect = defaultRedirect;
        }

        return redirect;
    }

    /**
     * Retrieve the URL to which the client should be redirected after the successful completion of the requested
     * action. This is taken from the {@code xredirect} parameter in the query string. If this parameter is not set, or
     * is set to an empty value, compose an URL back to the current document, using the specified action and query
     * string, and return it.
     *
     * @param action the XWiki action to use for composing the default redirect URL ({@code view}, {@code edit}, etc)
     * @param queryString the query parameters to append to the fallback URL
     * @param context the current context
     * @return the destination URL, as specified in the {@code xredirect} parameter, or computed using the current
     *         document and the specified action and query string
     */
    public static String getRedirect(String action, String queryString, XWikiContext context)
    {
        return getRedirect(action, queryString, "xredirect");
    }

    /**
     * Retrieve the URL to which the client should be redirected after the successful completion of the requested
     * action. If any of the specified {@code redirectParameters} (in order) is present in the query string, it is
     * returned as the redirect destination. If none of the parameters is set, compose an URL back to the current
     * document using the specified action and query string, and return it.
     *
     * @param action the XWiki action to use for composing the default redirect URL ({@code view}, {@code edit}, etc)
     * @param queryString the query parameters to append to the fallback URL
     * @param redirectParameters list of request parameters to look for as the redirect destination; each of the
     *            parameters is tried in the order they are passed, and the first one set to a non-empty value is
     *            returned, if any
     * @return the destination URL, as specified in one of the {@code redirectParameters}, or computed using the current
     *         document and the specified action and query string
     */
    public static String getRedirect(String action, String queryString, String... redirectParameters)
    {
        XWikiContext context = getContext();
        XWikiRequest request = context.getRequest();
        String redirect = null;
        for (String p : redirectParameters) {
            redirect = request.getParameter(p);
            if (StringUtils.isNotEmpty(redirect)) {
                break;
            }
        }

        if (StringUtils.isEmpty(redirect)) {
            redirect = context.getDoc().getURL(action, queryString, true, context);
        }

        return redirect;
    }

    /**
     * Retrieve the URL to which the client should be redirected after the successful completion of the requested
     * action. This is taken from the {@code xredirect} parameter in the query string. If this parameter is not set, or
     * is set to an empty value, compose an URL back to the current document, using the specified action, and return it.
     *
     * @param action the XWiki action to use for composing the default redirect URL ({@code view}, {@code edit}, etc)
     * @param context the current context
     * @return the destination URL, as specified in the {@code xredirect} parameter, or computed using the current
     *         document and the specified action
     */
    public static String getRedirect(String action, XWikiContext context)
    {
        return getRedirect(action, null, context);
    }

    /**
     * Retrieve the name of the velocity template which should be used to generate the response. This is taken from the
     * {@code xpage} parameter in the query string. If this parameter is not set, or is set to an empty value, return
     * the provided default name.
     *
     * @param request the current request
     * @param defaultpage the default value to use if no {@code xpage} parameter is set
     * @return the name of the requested template, as specified in the {@code xpage} parameter, or the specified default
     *         template
     */
    public static String getPage(XWikiRequest request, String defaultpage)
    {
        String page = request.getParameter("xpage");
        if (StringUtils.isEmpty(page)) {
            page = defaultpage;
        }

        return page;
    }

    /**
     * Get the name of an uploaded file, corresponding to the specified form field.
     *
     * @param filelist the list of uploaded files, computed by the FileUpload plugin
     * @param name the name of the form field
     * @return the original name of the file, if the specified field name does correspond to an uploaded file, or
     *         {@code null} otherwise
     */
    public static String getFileName(List<FileItem> filelist, String name)
    {
        for (FileItem item : filelist) {
            if (name.equals(item.getFieldName())) {
                return item.getName();
            }
        }

        return null;
    }

    /**
     * Get the content of an uploaded file, corresponding to the specified form field.
     *
     * @param filelist the list of uploaded files, computed by the FileUpload plugin
     * @param name the name of the form field
     * @return the content of the file, if the specified field name does correspond to an uploaded file, or {@code null}
     *         otherwise
     * @throws XWikiException if the file cannot be read due to an underlying I/O exception
     */
    public static byte[] getContent(List<FileItem> filelist, String name) throws XWikiException
    {
        for (FileItem item : filelist) {
            if (name.equals(item.getFieldName())) {
                byte[] data = new byte[(int) item.getSize()];
                InputStream fileis = null;
                try {
                    fileis = item.getInputStream();
                    fileis.read(data);
                } catch (IOException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                        XWikiException.ERROR_XWIKI_APP_UPLOAD_FILE_EXCEPTION,
                        "Exception while reading uploaded parsed file", e);
                } finally {
                    IOUtils.closeQuietly(fileis);
                }

                return data;
            }
        }

        return null;
    }

    public static XWikiContext prepareContext(String action, XWikiRequest request, XWikiResponse response,
        XWikiEngineContext engine_context) throws XWikiException
    {
        XWikiContext context = new XWikiContext();
        String dbname = "xwiki";
        URL url = XWiki.getRequestURL(request);
        context.setURL(url);

        context.setEngineContext(engine_context);
        context.setRequest(request);
        context.setResponse(response);
        context.setAction(action);
        context.setWikiId(dbname);

        int mode = 0;
        if (request instanceof XWikiServletRequest) {
            mode = XWikiContext.MODE_SERVLET;
        }
        context.setMode(mode);

        return context;
    }

    /**
     * Parse the request parameters from the specified String using the specified encoding. <strong>IMPLEMENTATION
     * NOTE</strong>: URL decoding is performed individually on the parsed name and value elements, rather than on the
     * entire query string ahead of time, to properly deal with the case where the name or value includes an encoded "="
     * or "&" character that would otherwise be interpreted as a delimiter.
     * <p>
     * Code borrowed from Apache Tomcat 5.0
     * </p>
     *
     * @param data input string containing request parameters
     * @param encoding the encoding to use for transforming bytes into characters
     * @throws IllegalArgumentException if the data is malformed
     */
    public static Map<String, String[]> parseParameters(String data, String encoding)
        throws UnsupportedEncodingException
    {
        if (!StringUtils.isEmpty(data)) {
            // use the specified encoding to extract bytes out of the
            // given string so that the encoding is not lost. If an
            // encoding is not specified, let it use platform default
            byte[] bytes = null;
            try {
                if (encoding == null) {
                    bytes = data.getBytes();
                } else {
                    bytes = data.getBytes(encoding);
                }
            } catch (UnsupportedEncodingException uee) {
            }

            return parseParameters(bytes, encoding);
        }

        return Collections.emptyMap();
    }

    /**
     * Parse the request parameters from the specified byte array using the specified encoding. <strong>IMPLEMENTATION
     * NOTE</strong>: URL decoding is performed individually on the parsed name and value elements, rather than on the
     * entire query string ahead of time, to properly deal with the case where the name or value includes an encoded "="
     * or "&" character that would otherwise be interpreted as a delimiter.
     * <p>
     * NOTE: byte array data is modified by this method. Caller beware.
     * </p>
     * <p>
     * Code borrowed from Apache Tomcat 5.0
     * </p>
     *
     * @param data input byte array containing request parameters
     * @param encoding Encoding to use for converting hex
     * @throws UnsupportedEncodingException if the data is malformed
     */
    public static Map<String, String[]> parseParameters(byte[] data, String encoding)
        throws UnsupportedEncodingException
    {
        Map<String, String[]> map = new HashMap<String, String[]>();

        if (data != null && data.length > 0) {
            int ix = 0;
            int ox = 0;
            String key = null;
            String value = null;
            while (ix < data.length) {
                byte c = data[ix++];
                switch ((char) c) {
                    case '&':
                        value = new String(data, 0, ox, encoding);
                        if (key != null) {
                            putMapEntry(map, key, value);
                            key = null;
                        }
                        ox = 0;
                        break;
                    case '=':
                        if (key == null) {
                            key = new String(data, 0, ox, encoding);
                            ox = 0;
                        } else {
                            data[ox++] = c;
                        }
                        break;
                    case '+':
                        data[ox++] = (byte) ' ';
                        break;
                    case '%':
                        data[ox++] = (byte) ((convertHexDigit(data[ix++]) << 4) + convertHexDigit(data[ix++]));
                        break;
                    default:
                        data[ox++] = c;
                }
            }
            // The last value does not end in '&'. So save it now.
            if (key != null) {
                value = new String(data, 0, ox, encoding);
                putMapEntry(map, key, value);
            }
        }

        return map;
    }

    /**
     * Convert a byte character value to the corresponding hexidecimal digit value.
     * <p>
     * Code borrowed from Apache Tomcat 5.0
     * </p>
     *
     * @param b the character value byte
     */
    private static byte convertHexDigit(byte b)
    {
        if ((b >= '0') && (b <= '9')) {
            return (byte) (b - '0');
        }
        if ((b >= 'a') && (b <= 'f')) {
            return (byte) (b - 'a' + 10);
        }
        if ((b >= 'A') && (b <= 'F')) {
            return (byte) (b - 'A' + 10);
        }

        return 0;
    }

    /**
     * Put name-value pair in map. When an entry for {@code name} already exist, add the new value to the array of
     * values.
     * <p>
     * Code borrowed from Apache Tomcat 5.0
     * </p>
     *
     * @param map the map that is being constructed
     * @param name the name of the parameter
     * @param value the value of the parameter
     */
    private static void putMapEntry(Map<String, String[]> map, String name, String value)
    {
        String[] newValues = null;
        String[] oldValues = map.get(name);
        if (oldValues == null) {
            newValues = new String[] {value};
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }

    /**
     * Escapes the XML special characters in a <code>String</code> using numerical XML entities.
     *
     * @param value the text to escape, may be null
     * @return a new escaped <code>String</code>, <code>null</code> if null input
     * @deprecated starting with 2.7 use {@link XMLUtils#escape(Object) $services.xml.escape(content)}
     */
    @Deprecated
    public static String formEncode(String value)
    {
        return XMLUtils.escape(value);
    }

    public static String SQLFilter(String text)
    {
        try {
            return text.replaceAll("'", "''");
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * @deprecated replaced by {@link com.xpn.xwiki.util.Util#encodeURI(String, XWikiContext)} since 1.3M2
     */
    @Deprecated
    public static String encode(String text, XWikiContext context)
    {
        return Util.encodeURI(text, context);
    }

    /**
     * @deprecated replaced by {@link com.xpn.xwiki.util.Util#decodeURI(String, XWikiContext)} since 1.3M2
     */
    @Deprecated
    public static String decode(String text, XWikiContext context)
    {
        return Util.decodeURI(text, context);
    }

    /**
     * Process a multi-part request, extracting all the uploaded files.
     *
     * @param request the current request to process
     * @param context the current context
     * @return the instance of the {@link FileUploadPlugin} used to parse the uploaded files
     */
    public static FileUploadPlugin handleMultipart(HttpServletRequest request, XWikiContext context)
    {
        FileUploadPlugin fileupload = null;
        try {
            if (request instanceof MultiPartRequestWrapper) {
                fileupload = new FileUploadPlugin("fileupload", "fileupload", context);
                context.put("fileuploadplugin", fileupload);
                fileupload.loadFileList(context);
                MultiPartRequestWrapper mpreq = (MultiPartRequestWrapper) request;
                List<FileItem> fileItems = fileupload.getFileItems(context);
                for (FileItem item : fileItems) {
                    if (item.isFormField()) {
                        String sName = item.getFieldName();
                        String sValue = item.getString(context.getWiki().getEncoding());
                        mpreq.setAttribute(sName, sValue);
                    }
                }
            }
        } catch (Exception e) {
            if ((e instanceof XWikiException)
                && (((XWikiException) e).getCode() == XWikiException.ERROR_XWIKI_APP_FILE_EXCEPTION_MAXSIZE)) {
                context.put("exception", e);
            } else {
                LOGGER.error("Failed to process MultiPart request", e);
            }
        }
        return fileupload;
    }

    /**
     * @param componentManager the root component manager used by {@link #getComponent(Class)} and
     *            {@link #getComponent(Class, String)}
     */
    public static void setComponentManager(ComponentManager componentManager)
    {
        Utils.rootComponentManager = componentManager;
    }

    /**
     * @return the root component manager
     * @deprecated last resort way of accessing the {@link ComponentManager}, make sure you cannot do it any other way
     *             possible since it add a strong dependency to a static to your code
     */
    @Deprecated
    public static ComponentManager getRootComponentManager()
    {
        return rootComponentManager;
    }

    /**
     * @return the contextual component manager used by {@link #getComponent(Class)} and
     *         {@link #getComponent(Class, String)}
     * @deprecated since 6.1M1, use {@link #getContextComponentManager()} instead
     */
    @Deprecated
    public static ComponentManager getComponentManager()
    {
        ComponentManager contextComponentManager;

        try {
            contextComponentManager = rootComponentManager.getInstance(ComponentManager.class, "context/root");
        } catch (ComponentLookupException e) {
            // This means the Context Root CM doesn't exist, use the Root CM.
            contextComponentManager = rootComponentManager;
            LOGGER.debug("Failed to find the [context/root] Component Manager. Cause: [{}]. Using the Root Component "
                + "Manager", ExceptionUtils.getRootCauseMessage(e));
        }

        return contextComponentManager;
    }

    /**
     * @return the contextual component manager used by {@link #getComponent(Class)} and
     *         {@link #getComponent(Class, String)}
     * @since 6.0RC1
     * @deprecated last resort way of accessing the {@link ComponentManager}, make sure you cannot do it any other way
     *             possible since it add a strong dependency to a static to your code
     */
    @Deprecated
    public static ComponentManager getContextComponentManager()
    {
        ComponentManager contextComponentManager;

        try {
            contextComponentManager = rootComponentManager.getInstance(ComponentManager.class, "context");
        } catch (ComponentLookupException e) {
            // This means the Context CM doesn't exist, use the Root CM.
            contextComponentManager = rootComponentManager;
            LOGGER.debug("Failed to find the [context] Component Manager. Cause: [{}]. Using the Root Component "
                + "Manager", ExceptionUtils.getRootCauseMessage(e));
        }

        return contextComponentManager;
    }

    /**
     * Lookup a XWiki component by role and hint.
     *
     * @param role the class (aka role) that the component implements
     * @param hint a value to differentiate different component implementations for the same role
     * @return the component's instance
     * @throws RuntimeException if the component cannot be found/initialized, or if the component manager is not
     *             initialized
     * @deprecated since 4.0M1 use {@link #getComponent(Type, String)} instead
     */
    @Deprecated
    public static <T> T getComponent(Class<T> role, String hint)
    {
        return getComponent((Type) role, hint);
    }

    /**
     * Lookup a XWiki component by role (uses the default hint).
     *
     * @param role the class (aka role) that the component implements
     * @return the component's instance
     * @throws RuntimeException if the component cannot be found/initialized, or if the component manager is not
     *             initialized
     * @deprecated since 4.0M1 use {@link #getComponent(Type)} instead
     */
    @Deprecated
    public static <T> T getComponent(Class<T> role)
    {
        return getComponent((Type) role);
    }

    /**
     * Lookup a XWiki component by role and hint.
     *
     * @param roleType the class (aka role) that the component implements
     * @param roleHint a value to differentiate different component implementations for the same role
     * @return the component's instance
     * @throws RuntimeException if the component cannot be found/initialized, or if the component manager is not
     *             initialized
     * @deprecated starting with 4.1M2 use the Component Script Service instead
     */
    @Deprecated
    public static <T> T getComponent(Type roleType, String roleHint)
    {
        T component;

        ComponentManager componentManager = getContextComponentManager();

        if (componentManager != null) {
            try {
                component = componentManager.getInstance(roleType, roleHint);
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Failed to load component for type [" + roleType + "] for hint [" + roleHint
                    + "]", e);
            }
        } else {
            throw new RuntimeException("Component manager has not been initialized before lookup for [" + roleType
                + "] for hint [" + roleHint + "]");
        }

        return component;
    }

    /**
     * Lookup a XWiki component by role (uses the default hint).
     *
     * @param roleType the class (aka role) that the component implements
     * @return the component's instance
     * @throws RuntimeException if the component cannot be found/initialized, or if the component manager is not
     *             initialized
     * @deprecated starting with 4.1M2 use the Component Script Service instead
     */
    @Deprecated
    public static <T> T getComponent(Type roleType)
    {
        return getComponent(roleType, "default");
    }

    /**
     * @param <T> the component type
     * @param role the role for which to return implementing components
     * @return all components implementing the passed role
     * @throws RuntimeException if some of the components cannot be found/initialized, or if the component manager is
     *             not initialized
     * @since 2.0M3
     * @deprecated since 4.0M1 use {@link #getComponentManager()} instead
     */
    @Deprecated
    public static <T> List<T> getComponentList(Class<T> role)
    {
        List<T> components;

        ComponentManager componentManager = getContextComponentManager();

        if (componentManager != null) {
            try {
                components = componentManager.getInstanceList(role);
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Failed to load components with role [" + role.getName() + "]", e);
            }
        } else {
            throw new RuntimeException("Component manager has not been initialized before lookup for role ["
                + role.getName() + "]");
        }

        return components;
    }

    /**
     * Helper method for obtaining a valid xcontext from the execution context.
     * <p>
     * NOTE: Don't use this method to access the XWiki context in a component because
     * {@link #setComponentManager(ComponentManager)} is not called when running component unit tests. You have to take
     * the XWiki context yourself from the injected Execution when inside a component. This method should be used only
     * by non-component code.
     *
     * @return the current context or {@code null} if the execution context is not yet initialized
     * @since 3.2M3
     */
    public static XWikiContext getContext()
    {
        Provider<XWikiContext> xcontextProvider = getComponent(XWikiContext.TYPE_PROVIDER);

        return xcontextProvider != null ? xcontextProvider.get() : null;
    }

    /**
     * Check if placeholders are enabled in the current context.
     *
     * @param context The current context.
     * @return <code>true</code> if placeholders can be used, <code>false</code> otherwise.
     */
    public static boolean arePlaceholdersEnabled(XWikiContext context)
    {
        Boolean enabled = (Boolean) context.get(PLACEHOLDERS_ENABLED_CONTEXT_KEY);

        return enabled != null && enabled;
    }

    /**
     * Enable placeholder support in the current request context.
     *
     * @param context The current context.
     */
    public static void enablePlaceholders(XWikiContext context)
    {
        context.put(PLACEHOLDERS_CONTEXT_KEY, new HashMap<String, String>());
        context.put(PLACEHOLDERS_ENABLED_CONTEXT_KEY, new Boolean(true));
    }

    /**
     * Disable placeholder support in the current request context.
     *
     * @param context The current context.
     */
    public static void disablePlaceholders(XWikiContext context)
    {
        context.remove(PLACEHOLDERS_CONTEXT_KEY);
        context.remove(PLACEHOLDERS_ENABLED_CONTEXT_KEY);
    }

    /**
     * Create a placeholder key for a string that should be protected from further processing. The value is stored in
     * the context, and the returned key can be used by the calling code as many times in the rendering result. At the
     * end of the rendering process all placeholder keys are replaced with the values they replace.
     *
     * @param value The string to hide.
     * @param context The current context.
     * @return The key to be used instead of the value.
     */
    public static String createPlaceholder(String value, XWikiContext context)
    {
        if (!arePlaceholdersEnabled(context)) {
            return value;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> renderingKeys = (Map<String, String>) context.get(PLACEHOLDERS_CONTEXT_KEY);
        String key;
        do {
            key = "KEY" + RandomStringUtils.randomAlphanumeric(10) + "KEY";
        } while (renderingKeys.containsKey(key));
        renderingKeys.put(key, value);

        return key;
    }

    /**
     * Insert back the replaced strings.
     *
     * @param content The rendered content, with placeholders.
     * @param context The current context.
     * @return The content with all placeholders replaced with the real values.
     */
    public static String replacePlaceholders(String content, XWikiContext context)
    {
        if (!arePlaceholdersEnabled(context)) {
            return content;
        }

        String result = content;
        @SuppressWarnings("unchecked")
        Map<String, String> renderingKeys = (Map<String, String>) context.get(PLACEHOLDERS_CONTEXT_KEY);
        for (Entry<String, String> e : renderingKeys.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }

        return result;
    }

    /**
     * Verify if the current request is an AJAX request.
     *
     * @param context the current request context
     * @return True if this is an AJAX request, false otherwise.
     * @since 2.4M2
     */
    public static Boolean isAjaxRequest(XWikiContext context)
    {
        return BooleanUtils.isTrue((Boolean) context.get("ajax"));
    }
}
