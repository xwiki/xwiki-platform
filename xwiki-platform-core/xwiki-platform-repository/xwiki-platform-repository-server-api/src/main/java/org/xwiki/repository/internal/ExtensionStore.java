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
package org.xwiki.repository.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.DefaultExtensionSupportPlan;
import org.xwiki.extension.DefaultExtensionSupportPlans;
import org.xwiki.extension.DefaultExtensionSupporter;
import org.xwiki.extension.ExtensionSupportPlan;
import org.xwiki.extension.ExtensionSupportPlans;
import org.xwiki.extension.ExtensionSupporter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.NumberProperty;

/**
 * Various helpers to manipulate the extension related pages.
 * 
 * @since 16.8.0RC1
 * @version $Id$
 */
@Component(roles = ExtensionStore.class)
@Singleton
public class ExtensionStore
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * @param <T> the expected type of the value to return
     * @param xobject the xobject
     * @param propertyName the property of the xobject
     * @return the value
     */
    public <T> T getValue(BaseObject xobject, String propertyName)
    {
        return getValue(xobject, propertyName, (T) null);
    }

    /**
     * @param <T> the expected type of the value to return
     * @param xobject the xobject
     * @param propertyName the property of the xobject
     * @param def the value to return if the property is not set
     * @return the value
     */
    public <T> T getValue(BaseObject xobject, String propertyName, T def)
    {
        BaseProperty<?> property = (BaseProperty<?>) xobject.safeget(propertyName);

        T value = def;
        if (property != null) {
            value = (T) property.getValue();
            if (value == null) {
                value = def;
            }
        }

        return value;
    }

    private URL getURLValue(BaseProperty<?> property, boolean fallbackOnDocumentURL, XWikiContext xcontext)
    {
        URL url = null;

        String urlString = (String) property.getValue();
        if (StringUtils.isNotEmpty(urlString)) {
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                this.logger.warn("The format of the URL property [{}] is wrong ({})", property.getReference(),
                    urlString);
            }
        }

        if (url == null && fallbackOnDocumentURL) {
            XWikiDocument document = property.getOwnerDocument();

            if (document != null) {
                try {
                    url = new URL(document.getExternalURL("view", xcontext));
                } catch (MalformedURLException e) {
                    this.logger.error("Failed to get the URL for document [{}]", document.getDocumentReference(), e);
                }
            }
        }

        return url;
    }

    /**
     * @param xobject the xobject
     * @param propertyName the property of the xobject
     * @param def the value to return if the property is not set
     * @return the value
     */
    public boolean getBooleanValue(BaseObject xobject, String propertyName, boolean def)
    {
        BaseProperty<?> property = (BaseProperty<?>) xobject.safeget(propertyName);

        if (property instanceof NumberProperty) {
            Number number = (Number) property.getValue();
            if (number != null) {
                return number.intValue() == 1;
            }
        }

        return def;
    }

    /**
     * @param xobject the xobject
     * @param propertyName the property of the xobject
     * @param fallbackOnDocumentURL true if the document's external URL should be used when the property URL is not
     *            provided or invalid
     * @param xcontext the XWiki context
     * @return the URL
     */
    public URL getURLValue(BaseObject xobject, String propertyName, boolean fallbackOnDocumentURL,
        XWikiContext xcontext)
    {
        URL url = null;

        BaseProperty<?> property = (BaseProperty<?>) xobject.safeget(propertyName);
        if (property != null) {
            return getURLValue(property, fallbackOnDocumentURL, xcontext);
        }

        return url;
    }

    /**
     * @param supportPlanIds the support plan identifiers
     * @return the support plans as a {@link ExtensionSupportPlans}
     */
    public ExtensionSupportPlans resolveExtensionSupportPlans(Collection<String> supportPlanIds)
    {
        if (supportPlanIds != null) {
            XWikiContext xcontext = this.xcontextProvider.get();

            List<ExtensionSupportPlan> plans = new ArrayList<>(supportPlanIds.size());
            for (String supportPlanId : supportPlanIds) {
                ExtensionSupportPlan plan = resolveExtensionSupportPlan(supportPlanId, xcontext);

                if (plan != null) {
                    plans.add(plan);
                }
            }

            return new DefaultExtensionSupportPlans(plans);
        }

        return new DefaultExtensionSupportPlans(Collections.emptyList());
    }

    /**
     * @param supportPlanId the support plan identifier
     * @param xcontext the XWiki context
     * @return the support plan as a {@link ExtensionSupportPlan}
     */
    public ExtensionSupportPlan resolveExtensionSupportPlan(String supportPlanId, XWikiContext xcontext)
    {
        if (supportPlanId != null) {
            try {
                XWikiDocument document = xcontext.getWiki().getDocument(supportPlanId, xcontext);

                BaseObject supportPlanObject = getExtensionSupportPlanObject(document);

                if (supportPlanObject != null) {
                    ExtensionSupporter supporter = resolveExtensionSupporter(
                        getValue(supportPlanObject, XWikiRepositoryModel.PROP_SUPPORTPLAN_SUPPORTER), xcontext);

                    if (supporter != null
                        && getBooleanValue(supportPlanObject, XWikiRepositoryModel.PROP_SUPPORTPLAN_ACTIVE, false)) {
                        ExtensionSupporter extensionSupporter = supporter;
                        String name = document.getTitle();
                        boolean paying =
                            getBooleanValue(supportPlanObject, XWikiRepositoryModel.PROP_SUPPORTPLAN_PAYING, false);
                        URL url =
                            getURLValue(supportPlanObject, XWikiRepositoryModel.PROP_SUPPORTPLAN_URL, true, xcontext);

                        return new DefaultExtensionSupportPlan(extensionSupporter, name, url, paying);
                    }
                }
            } catch (Exception e) {
                this.logger.error("Failed to resolve the support plan with id [{}]: {}", supportPlanId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return null;
    }

    /**
     * @param supporterId the supporter identifier
     * @param xcontext the XWiki context
     * @return the supporter as a {@link ExtensionSupporter}
     * @throws XWikiException when failing to load the supporter document
     */
    public ExtensionSupporter resolveExtensionSupporter(String supporterId, XWikiContext xcontext) throws XWikiException
    {
        if (supporterId != null) {
            XWikiDocument document = xcontext.getWiki().getDocument(supporterId, xcontext);

            BaseObject supporterObject = getExtensionSupporterObject(document);

            if (supporterObject != null
                && getBooleanValue(supporterObject, XWikiRepositoryModel.PROP_SUPPORTER_ACTIVE, false)) {
                String name = document.getTitle();
                URL url = getURLValue(supporterObject, XWikiRepositoryModel.PROP_SUPPORTER_URL, true, xcontext);

                return new DefaultExtensionSupporter(name, url);
            }
        }

        return null;
    }

    /**
     * @param extensionSupportPlanDocument the document containing the support plan
     * @return the xobject containing the support plan
     */
    public BaseObject getExtensionSupportPlanObject(XWikiDocument extensionSupportPlanDocument)
    {
        return extensionSupportPlanDocument.getXObject(XWikiRepositoryModel.EXTENSIONSUPPORTPLAN_CLASSREFERENCE);
    }

    /**
     * @param extensionSupporterDocument the document containing the supporter
     * @return the xobject containing the supporter
     */
    public BaseObject getExtensionSupporterObject(XWikiDocument extensionSupporterDocument)
    {
        return extensionSupporterDocument.getXObject(XWikiRepositoryModel.EXTENSIONSUPPORTER_CLASSREFERENCE);
    }

}
