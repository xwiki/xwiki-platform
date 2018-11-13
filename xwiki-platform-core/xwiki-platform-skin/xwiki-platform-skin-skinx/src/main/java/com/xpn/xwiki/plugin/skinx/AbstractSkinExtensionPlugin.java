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
package com.xpn.xwiki.plugin.skinx;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.skinx.internal.async.SkinExtensionAsync;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.internal.cache.rendering.CachedItem;
import com.xpn.xwiki.internal.cache.rendering.CachedItem.UsedExtension;
import com.xpn.xwiki.internal.cache.rendering.RenderingCacheAware;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.Utils;

/**
 * <p>
 * Skin Extensions base plugin. It allows templates and document content to pull required clientside code in the
 * generated XHTML (or whatever XML) content.
 * </p>
 * <p>
 * The API provides a method {@link SkinExtensionPluginApi#use(String)}, which, when called, marks an extension as used
 * in the current result. Later on, all the used extensions are inserted in the content, by replacing the first
 * occurrence of the following string: <tt>&lt;!-- canonical.plugin.classname --&gt;</tt>, where the actual extension
 * type classname is used. For example, JS extensions are inserted in place of
 * <tt>&lt;!-- com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin --&gt;</tt>.
 * </p>
 *
 * @see SkinExtensionPluginApi
 * @see JsSkinExtensionPlugin
 * @see CssSkinExtensionPlugin
 * @see LinkExtensionPlugin
 * @version $Id$
 */
@SuppressWarnings("deprecation")
public abstract class AbstractSkinExtensionPlugin extends XWikiDefaultPlugin implements RenderingCacheAware
{
    /** Log object to log messages in this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSkinExtensionPlugin.class);

    /** The name of the context key for the list of pulled extensions. */
    protected final String contextKey = this.getClass().getCanonicalName();

    /** The name of the context key for the additional parameters for pulled extensions. */
    protected final String parametersContextKey = this.getClass().getCanonicalName() + "_parameters";

    /**
     * @see #getDefaultEntityReferenceSerializer()
     */
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * @see #getCurrentDocumentReferenceResolver()
     */
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    private SkinExtensionAsync async;

    /**
     * XWiki plugin constructor.
     *
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public AbstractSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * Abstract method for obtaining a link that points to the actual pulled resource. Each type of resource has its own
     * format for the link, for example Javascript uses <code>&lt;script src="/path/to/Document"&gt;</code>, while CSS
     * uses <code>&lt;link rel="stylesheet" href="/path/to/Document"&gt;</code> (the actual syntax is longer, this is
     * just a simplified example).
     *
     * @param resource the name of the wiki document holding the resource.
     * @param context the current request context, needed to access the URLFactory.
     * @return A <code>String</code> representation of the linking element that should be printed in the generated HTML.
     */
    public abstract String getLink(String resource, XWikiContext context);

    /**
     * Returns the list of always used extensions of this type. Which resources are always used depends on the type of
     * resource, for example document based StyleSheet extensions have a property in the object, <tt>use</tt>, which can
     * have the value <tt>always</tt> to declare that an extension should always be used.
     *
     * @param context The current request context.
     * @return A set of resource names that should be pulled in the current response. Note that this method is called
     *         for each request, as the list might change in time, and it can be different for each wiki in a farm.
     */
    public abstract Set<String> getAlwaysUsedExtensions(XWikiContext context);

    /**
     * Determines if the requested document contains on page skin extension objects of this type. True if at least one
     * of the extension objects has the <tt>currentPage</tt> value for the <tt>use</tt> property.
     *
     * @param context the current request context
     * @return a boolean specifying if the current document contains on page skin extensions
     */
    public abstract boolean hasPageExtensions(XWikiContext context);

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SkinExtensionPluginApi((AbstractSkinExtensionPlugin) plugin, context);
    }

    private void useResource(String resource, XWikiContext context)
    {
        LOGGER.debug("Using [{}] as [{}] extension", resource, this.getName());
        getPulledResources(context).add(resource);
    }

    /**
     * Mark a resource as used in the current result. A resource is registered only once per request, further calls will
     * not result in additional links, even if it is pulled with different parameters.
     *
     * @param resource The name of the resource to pull.
     * @param context The current request context.
     * @see #use(String, Map, XWikiContext)
     */
    public void use(String resource, XWikiContext context)
    {
        useResource(resource, context);

        // In case a previous call added some parameters, remove them, since the last call for a resource always
        // discards previous ones.
        getParametersMap(context).remove(resource);

        // Register the use of the resource in case the current execution is an asynchronous renderer
        getSkinExtensionAsync().use(getName(), resource, null);
    }

    /**
     * Mark a skin extension document as used in the current result, together with some parameters. How the parameters
     * are used, depends on the type of resource being pulled. For example, JS and CSS extensions use the parameters in
     * the resulting URL, while Link extensions use the parameters as attributes of the link tag. A resource is
     * registered only once per request, further calls will not result in additional links, even if it is pulled with
     * different parameters. If more than one calls per request are made, the parameters used are the ones from the last
     * call (or none, if the last call did not specify any parameters).
     *
     * @param resource The name of the resource to pull.
     * @param parameters The parameters for this resource.
     * @param context The current request context.
     * @see #use(String, XWikiContext)
     */
    public void use(String resource, Map<String, Object> parameters, XWikiContext context)
    {
        useResource(resource, context);

        // Associate parameters to the resource
        getParametersMap(context).put(resource, parameters);

        getSkinExtensionAsync().use(getName(), resource, parameters);
    }

    /**
     * Get the list of pulled resources (of the plugin's type) for the current request. The returned list is always
     * valid.
     *
     * @param context The current request context.
     * @return A set of names that holds the resources pulled in the current request.
     */
    @SuppressWarnings("unchecked")
    protected Set<String> getPulledResources(XWikiContext context)
    {
        initializeRequestListIfNeeded(context);
        return (Set<String>) context.get(this.contextKey);
    }

    /**
     * Get the map of additional parameters for each pulled resource (of the plugin's type) for the current request. The
     * returned map is always valid.
     *
     * @param context The current request context.
     * @return A map of resource parameters, where the key is the resource's name, and the value is a map holding the
     *         actual parameters for a given resource. If a resource was pulled without additional parameters, then no
     *         corresponding entry is added in this map.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Map<String, Object>> getParametersMap(XWikiContext context)
    {
        initializeRequestListIfNeeded(context);
        return (Map<String, Map<String, Object>>) context.get(this.parametersContextKey);
    }

    /**
     * Initializes the list of pulled extensions corresponding to this request, if it wasn't already initialized. This
     * method is not thread safe, since a context should not be shared among threads.
     *
     * @param context The current context where this list is stored.
     */
    protected void initializeRequestListIfNeeded(XWikiContext context)
    {
        if (!context.containsKey(this.contextKey)) {
            context.put(this.contextKey, new LinkedHashSet<String>());
        }
        if (!context.containsKey(this.parametersContextKey)) {
            context.put(this.parametersContextKey, new HashMap<String, Map<String, Object>>());
        }
    }

    /**
     * Composes and returns the links to the resources pulled in the current request. This method is called at the end
     * of each request, once for each type of resource (subclass), and the result is placed in the generated XHTML.
     *
     * @param context The current request context.
     * @return a XHMTL fragment with all extensions imports statements for this request. This includes both extensions
     *         that are defined as being "used always" and "on demand" extensions explicitly requested for this page.
     *         Always used extensions are always, before on demand extensions, so that on demand extensions can override
     *         more general elements in the always used ones.
     */
    public String getImportString(XWikiContext context)
    {
        StringBuilder result = new StringBuilder();
        // Using LinkedHashSet to preserve the extensions order.
        Set<String> extensions = new LinkedHashSet<String>();
        // First, we add to the import string the extensions that should always be used.
        // TODO Global extensions should be able to select a set of actions for which they are enabled.
        extensions.addAll(getAlwaysUsedExtensions(context));

        // Then, we add On-Demand extensions for this request.
        extensions.addAll(getPulledResources(context));

        // Add On-Page extensions
        if (hasPageExtensions(context)) {
            // Make sure to use a prefixed document full name for the current document as well, or else the "extensions"
            // set will not detect if it was added before and it will be added twice.
            EntityReferenceSerializer<String> serializer = getDefaultEntityReferenceSerializer();
            String serializedCurrentDocumentName = serializer.serialize(context.getDoc().getDocumentReference());

            // Add it to the list.
            extensions.add(serializedCurrentDocumentName);
        }

        for (String documentName : extensions) {
            result.append(getLink(documentName, context));
        }
        return result.toString();
    }

    /**
     * Get the parameters for a pulled resource. Note that a valid map is always returned, even if no parameters were
     * given when the resource was pulled.
     *
     * @param resource The resource for which to retrieve the parameters.
     * @param context The current request context.
     * @return The parameters for the resource, as a map where the keys are the parameter names, and the values are
     *         corresponding parameter value. If no parameters were given, an empty map is returned.
     */
    protected Map<String, Object> getParametersForResource(String resource, XWikiContext context)
    {
        Map<String, Object> result = getParametersMap(context).get(resource);
        if (result == null) {
            result = Collections.emptyMap();
        }
        return result;
    }

    /**
     * Get a parameter value for a pulled resource.
     *
     * @param parameterName the name of the parameter to retrieve
     * @param resource the resource for which to retrieve the parameter
     * @param context the current request context
     * @return The parameter value for the resource. If this parameter was not given, {@code null} is returned.
     */
    protected Object getParameter(String parameterName, String resource, XWikiContext context)
    {
        return getParametersForResource(resource, context).get(parameterName);
    }

    /**
     * This method converts the parameters for an extension to a query string that can be used with
     * {@link com.xpn.xwiki.doc.XWikiDocument#getURL(String, String, String, XWikiContext) getURL()} and printed in the
     * XHTML result. The parameters separator is the escaped &amp;amp;. The query string already starts with an
     * &amp;amp; if at least one parameter exists.
     *
     * @param resource The pulled resource whose parameters should be converted.
     * @param context The current request context.
     * @return The constructed query string, or an empty string if there are no parameters.
     */
    protected String parametersAsQueryString(String resource, XWikiContext context)
    {
        Map<String, Object> parameters = getParametersForResource(resource, context);
        StringBuilder query = new StringBuilder();
        for (Entry<String, Object> parameter : parameters.entrySet()) {
            // Skip the parameter that forces the file extensions to be sent through the /skin/ action
            if ("forceSkinAction".equals(parameter.getKey())) {
                continue;
            }
            query.append("&amp;");
            query.append(sanitize(parameter.getKey()));
            query.append("=");
            query.append(sanitize(parameter.getValue().toString()));
        }
        // If the main page is requested unminified, also send unminified extensions
        if ("false".equals(context.getRequest().getParameter("minify"))) {
            query.append("&amp;minify=false");
        }
        return query.toString();
    }

    /**
     * Prevent "HTML Injection" by making sure the rendered text does not escape the current element. This is achieved
     * by URL-encoding the following characters: '"&lt;&gt;
     *
     * @param value The string to sanitize.
     * @return The unchanged string, if it does not contain special characters, or the empty string.
     */
    protected String sanitize(String value)
    {
        String result = value;
        try {
            result = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Should never happen since the UTF-8 encoding is always available in the platform,
            // see http://java.sun.com/j2se/1.5.0/docs/api/java/nio/charset/Charset.html
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * At the end of the request, insert the links to the pulled resources in the response, in the place marked by an
     * XML comment of the format <tt>&lt;!-- canonical.plugin.classname --&gt;</tt>.
     * </p>
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#endParsing(String, XWikiContext)
     */
    @Override
    public String endParsing(String content, XWikiContext context)
    {
        // Using an XML comment is pretty safe, as extensions probably wouldn't work in other type
        // of documents, like RTF, CSV or JSON.
        String hook = "<!-- " + this.getClass().getCanonicalName() + " -->";
        String result = content.replaceFirst(hook, getImportString(context));
        return result;
    }

    @Override
    public UsedExtension getCacheResources(XWikiContext context) {
        return new CachedItem.UsedExtension(getPulledResources(context),
                       new HashMap<String, Map<String, Object>>(getParametersMap(context)));
    }

    @Override
    public void restoreCacheResources(XWikiContext context,
                   UsedExtension extension) {
        getPulledResources(context).addAll(extension.resources);
        getParametersMap(context).putAll(extension.parameters);
    }

    /**
     * Used to convert a proper Document Reference to string (standard form).
     */
    protected EntityReferenceSerializer<String> getDefaultEntityReferenceSerializer()
    {
        if (this.defaultEntityReferenceSerializer == null) {
            this.defaultEntityReferenceSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        }

        return this.defaultEntityReferenceSerializer;
    }

    /**
     * Used to resolve a document string reference to a Document Reference.
     */
    protected DocumentReferenceResolver<String> getCurrentDocumentReferenceResolver()
    {
        if (this.currentDocumentReferenceResolver == null) {
            this.currentDocumentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        }

        return this.currentDocumentReferenceResolver;
    }

    protected SkinExtensionAsync getSkinExtensionAsync()
    {
        if (this.async == null) {
            this.async =
                Utils.getComponent(SkinExtensionAsync.class);
        }

        return this.async;
    }
}
