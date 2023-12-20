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
package org.xwiki.extension.security.internal;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.template.TemplateManager;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.stream.Collectors.joining;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static org.apache.commons.lang.StringEscapeUtils.escapeXml;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_REVIEWED_SAFE;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_SAFE_EXPLANATIONS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_ADVICE;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_CVSS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_ID;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_LINK;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_FIX_VERSION;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_MAX_CVSS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID;
import static org.xwiki.extension.security.internal.ExtensionSecurityAdvice.TRANSITIVE_DEPENDENCY_ADVICE;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.ADVICE;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.CVE_ID;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.FIX_VERSION;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.MAX_CVSS;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.NAME;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.WIKIS;

/**
 * Converts a {@link SolrDocument} to a {@link Map} of Live Data entries.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component(roles = SolrToLiveDataEntryMapper.class)
@Singleton
public class SolrToLiveDataEntryMapper
{
    private static final String EXTENSION_ID = "extensionId";

    private static final String EXTENSION_VERSION = "extensionVersion";

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ExtensionIndexStore extensionIndexStore;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ExecutionContextManager contextManager;

    @Inject
    private BackwardDependenciesResolver backwardDependenciesResolver;

    @Inject
    private Logger logger;

    /**
     * @param doc the document to convert to Live Data entries.
     * @return Converts a {@link SolrDocument} to a {@link Map} of Live Data entries.
     */
    public Map<String, Object> mapDocToEntries(SolrDocument doc)
    {
        return ofEntries(
            entry(NAME, buildExtensionName(doc)),
            // Even if not displayed, the extension id must be returned as it is used as the id.
            entry(EXTENSION_ID, buildExtensionId(doc)),
            entry(MAX_CVSS, buildMaxCVSS(doc)),
            entry(CVE_ID, buildCVEList(doc)),
            entry(FIX_VERSION, buildFixVersion(doc)),
            entry(ADVICE, buildAdvice(doc)),
            entry(WIKIS, buildWikis(doc))
        );
    }

    private String buildCVEList(SolrDocument doc)
    {
        // The CVEs of the current extension vulnerabilities.
        return withNewExecutionContext(() -> {
            ScriptContext currentScriptContext = this.scriptContextManager.getCurrentScriptContext();
            currentScriptContext.setAttribute("cveIds", mapToStrings(doc, SECURITY_CVE_ID), ENGINE_SCOPE);
            // The CVE links of the current extension vulnerabilities.
            currentScriptContext.setAttribute("cveLinks", mapToStrings(doc, SECURITY_CVE_LINK), ENGINE_SCOPE);

            // The CVSS of the current extension vulnerabilities.
            currentScriptContext.setAttribute("cveCVSS", mapToStrings(doc, SECURITY_CVE_CVSS), ENGINE_SCOPE);

            List<Boolean> safe = getSafe(doc);
            currentScriptContext.setAttribute("notSafeCVEsIndex", getNotSafeCVEsIndex(doc, safe), ENGINE_SCOPE);
            // The index of safe CVEs.
            currentScriptContext.setAttribute("safeCVEsIndex", getSafeCVEsIndex(doc, safe), ENGINE_SCOPE);
            currentScriptContext.setAttribute(EXTENSION_ID, buildExtensionId(doc), ENGINE_SCOPE);
            currentScriptContext.setAttribute("messages", mapToStrings(doc, IS_SAFE_EXPLANATIONS), ENGINE_SCOPE);

            return this.templateManager.renderNoException("extension/security/liveData/cveID.vm");
        }).orElse("");
    }

    private static List<Boolean> getSafe(SolrDocument doc)
    {
        // The list of safe CVEs.
        return Optional.ofNullable(doc.getFieldValues(IS_REVIEWED_SAFE))
            .map(values -> values.stream()
                .map(it -> (boolean) it)
                .collect(Collectors.toList()))
            .orElse(List.of());
    }

    private static List<Integer> getNotSafeCVEsIndex(SolrDocument doc, List<Boolean> safe)
    {
        // The index of non-safe CVEs.
        return IntStream.range(0, mapToStrings(doc, SECURITY_CVE_ID).size())
            .filter(((IntPredicate) safe::get).negate())
            .boxed()
            .collect(Collectors.toList());
    }

    private static List<Integer> getSafeCVEsIndex(SolrDocument doc, List<Boolean> safe)
    {
        return IntStream.range(0, mapToStrings(doc, SECURITY_CVE_ID).size())
            .filter(safe::get)
            .boxed()
            .collect(Collectors.toList());
    }

    private static List<String> mapToStrings(SolrDocument doc, String name)
    {
        Collection<Object> fieldValues = doc.getFieldValues(name);
        if (fieldValues == null) {
            return List.of();
        }
        return fieldValues.stream().map(String::valueOf).collect(Collectors.toList());
    }

    private String buildAdvice(SolrDocument doc)
    {
        return withNewExecutionContext(() -> {
            ExtensionId extensionId = this.extensionIndexStore.getExtensionId(doc);
            ScriptContext currentScriptContext = this.scriptContextManager.getCurrentScriptContext();
            currentScriptContext.setAttribute(EXTENSION_ID, extensionId.getId(), ENGINE_SCOPE);
            currentScriptContext.setAttribute(EXTENSION_VERSION, extensionId.getVersion(), ENGINE_SCOPE);
            String adviceId = this.solrUtils.get(SECURITY_ADVICE, doc);
            currentScriptContext.setAttribute("adviceId", adviceId, ENGINE_SCOPE);
            if (Objects.equals(adviceId, TRANSITIVE_DEPENDENCY_ADVICE.getTranslationId())) {
                currentScriptContext.setAttribute("backwardDependencies",
                    this.backwardDependenciesResolver.getExplicitlyInstalledBackwardDependencies(extensionId),
                    ENGINE_SCOPE);
            }

            currentScriptContext.setAttribute("extensionManagerUrl", getExtensionManagerLink(extensionId),
                ENGINE_SCOPE);

            // Provide a lambda to easily resolve a link to an extension in the extension manager by its id.
            currentScriptContext.setAttribute("extensionManagerLinkResolver",
                (Function<ExtensionId, String>) this::getExtensionManagerLink, ENGINE_SCOPE);
            return this.templateManager.renderNoException("extension/security/liveData/advice.vm");
        }).orElse("");
    }

    private String buildFixVersion(SolrDocument doc)
    {
        Object fixVersion = doc.get(SECURITY_FIX_VERSION);
        if (fixVersion == null) {
            return "";
        }
        return String.valueOf(fixVersion);
    }

    private Double buildMaxCVSS(SolrDocument doc)
    {
        return this.solrUtils.get(SECURITY_MAX_CVSS, doc);
    }

    private String buildExtensionId(SolrDocument doc)
    {
        return this.solrUtils.get(SOLR_FIELD_ID, doc);
    }

    private String buildExtensionName(SolrDocument doc)
    {
        String extensionName;
        // If the extension does not have a name and version indexed, we fall back to an extensionId parsing.
        // Note: this is not supposed to happen in practice.
        ExtensionId extensionId = this.extensionIndexStore.getExtensionId(doc);
        if (doc.get(FieldUtils.NAME) != null) {
            extensionName = this.solrUtils.get(FieldUtils.NAME, doc);
        } else {
            // Fallback to the id in case the name is empty.
            String[] versionId = this.solrUtils.getId(doc).split("/");
            extensionName = versionId[0];
        }
        String url = getExtensionManagerLink(extensionId);

        String extensionNameEscaped = escapeXml(String.valueOf(extensionName));
        String extensionIdEscaped = escapeXml(buildExtensionId(doc));
        return String.format("<a href='%s' title='%s'>%s</a><br/><span class='xHint' title='%s'>%s</span>",
            escapeXml(url),
            extensionNameEscaped,
            extensionNameEscaped,
            extensionIdEscaped,
            extensionIdEscaped
        );
    }

    private String buildWikis(SolrDocument doc)
    {
        List<Object> list = this.solrUtils.getList(InstalledExtension.FIELD_INSTALLED_NAMESPACES, doc);
        if (list == null) {
            return "";
        }
        return list.stream()
            .map(String::valueOf)
            .map(it -> it.replaceFirst("wiki:", ""))
            .filter(it -> !Objects.equals(it, "{root}"))
            .collect(joining(", "));
    }

    /**
     * Executes the given supplier in a new execution context.
     *
     * @param supplier the supplier to execute
     * @param <T> the type of the result
     * @return an {@link Optional} containing the result of the supplier execution, or an {@link Optional#empty()} if
     *     the execution context fails to initialize
     */
    private <T> Optional<T> withNewExecutionContext(Supplier<T> supplier)
    {
        try {
            this.contextManager.pushContext(new ExecutionContext(), true);
            try {
                return Optional.of(supplier.get());
            } finally {
                this.contextManager.popContext();
            }
        } catch (ExecutionContextException e) {
            this.logger.warn("Failed to initialize a new execution context. Cause: [{}]", getRootCauseMessage(e));
            return Optional.empty();
        }
    }

    /**
     * Returns the link to the extension manager for a given extension.
     *
     * @param extensionId the ID of the extension
     * @return the link to the extension in the extension manager
     */
    private String getExtensionManagerLink(ExtensionId extensionId)
    {
        List<BasicNameValuePair> parameters = List.of(
            new BasicNameValuePair("section", "XWiki.Extensions"),
            new BasicNameValuePair(SolrToLiveDataEntryMapper.EXTENSION_ID, extensionId.getId()),
            new BasicNameValuePair(EXTENSION_VERSION, extensionId.getVersion().getValue())
        );
        return this.documentAccessBridge.getDocumentURL(new LocalDocumentReference("XWiki", "XWikiPreferences"),
            "admin", URLEncodedUtils.format(parameters, Charset.defaultCharset()), null);
    }
}
