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
package org.xwiki.livedata.internal.livetable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.WithParameters;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.LevelsClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * {@link LiveDataPropertyDescriptorStore} implementation that exposes the known live table columns as live data
 * properties.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Named("liveTable")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class LiveTableLiveDataPropertyStore extends WithParameters implements LiveDataPropertyDescriptorStore
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @Named("liveTable")
    private Provider<LiveDataConfiguration> defaultConfigProvider;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Override
    public Collection<LiveDataPropertyDescriptor> get() throws LiveDataException
    {
        List<LiveDataPropertyDescriptor> properties = new ArrayList<>();
        properties.addAll(getDocumentProperties());
        try {
            properties.addAll(getClassProperties());
        } catch (Exception e) {
            throw new LiveDataException("Failed to retrieve class properties.", e);
        }
        return properties;
    }

    private Collection<LiveDataPropertyDescriptor> getDocumentProperties()
    {
        // The default configuration includes only the descriptors for the document properties (which are read-only).
        return this.defaultConfigProvider.get().getMeta().getPropertyDescriptors();
    }

    private List<LiveDataPropertyDescriptor> getClassProperties() throws Exception
    {
        Object className = getParameters().get("className");
        if (className instanceof String) {
            DocumentReference classReference = this.currentDocumentReferenceResolver.resolve((String) className);
            return getClassProperties(classReference);
        } else {
            return Collections.emptyList();
        }
    }

    private List<LiveDataPropertyDescriptor> getClassProperties(DocumentReference classReference) throws Exception
    {
        if (this.authorization.hasAccess(Right.VIEW, classReference)) {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument classDoc = xcontext.getWiki().getDocument(classReference, xcontext);
            return classDoc.getXClass().getEnabledProperties().stream().map(this::getLiveDataPropertyDescriptor)
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    // TODO: we should have a helper in the localization component for this kind of fallback
    private String getRightTranslationWithFallback(String right)
    {
        String result = this.localizationManager.getTranslationPlain("rightsmanager." + right);
        if (StringUtils.isEmpty(result)) {
            result = right;
        }
        return result;
    }

    private LiveDataPropertyDescriptor getLiveDataPropertyDescriptor(PropertyClass xproperty)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setId(xproperty.getName());
        descriptor.setName(xproperty.getTranslatedPrettyName(xcontext));
        descriptor.setDescription(xproperty.getHint());
        descriptor.setType(xproperty.getClassType());
        // List properties are sortable by default, but only if they have single selection.
        if (xproperty instanceof ListClass && ((ListClass) xproperty).isMultiSelect()) {
            descriptor.setSortable(false);
        }
        // The returned property value is the displayer output.
        descriptor.setDisplayer(new DisplayerDescriptor("xObjectProperty"));
        if (xproperty instanceof ListClass) {
            descriptor.setFilter(new FilterDescriptor("list"));
            descriptor.getFilter().addOperator("empty", null);
            if (xproperty instanceof LevelsClass) {
                // We need to provide a list of maps of value / labels so that selectize can interpret them.
                descriptor.getFilter().setParameter("options", ((LevelsClass) xproperty).getList(xcontext)
                    .stream()
                    .map(item -> Map.of(
                        "value", item,
                        "label", getRightTranslationWithFallback(item)
                    ))
                    .collect(Collectors.toList()));
            } else {
                descriptor.getFilter().setParameter("searchURL", getSearchURL(xproperty));
            }
            if (xproperty.newProperty() instanceof StringListProperty) {
                // The default live table results page currently supports only exact matching for list properties with
                // multiple selection and no relational storage (selected values are stored concatenated on a single
                // database column).
                descriptor.getFilter().addOperator("equals", null);
            }
        } else if (xproperty instanceof DateClass) {
            String dateFormat = ((DateClass) xproperty).getDateFormat();
            if (!StringUtils.isEmpty(dateFormat)) {
                descriptor.setFilter(new FilterDescriptor("date"));
                descriptor.getFilter().setParameter("dateFormat", dateFormat);
            }
        }
        return descriptor;
    }

    private String getSearchURL(PropertyClass xproperty)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentReference xclassReference = xproperty.getObject().getDocumentReference();
        List<String> pathElements = Arrays.asList("rest", "wikis", xclassReference.getWikiReference().getName(),
            "classes", this.localEntityReferenceSerializer.serialize(xclassReference), "properties",
            xproperty.getName(), "values");
        String path = pathElements.stream().map(element -> {
            try {
                return URLEncoder.encode(element, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // This shouldn't happen.
                return element;
            }
        }).collect(Collectors.joining("/"));
        return xcontext.getRequest().getContextPath() + '/' + path + "?fp={encodedQuery}";
    }
}
