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
package org.xwiki.url.internal;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.URLConfiguration;
import org.xwiki.url.URLSecurityManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultURLSecurityManager}.
 *
 * @version $Id$
 * @since 13.3RC1
 * @since 12.10.7
 */
@ComponentTest
class DefaultURLSecurityManagerTest
{
    @InjectMockComponents
    private DefaultURLSecurityManager urlSecurityManager;

    @MockComponent
    private URLConfiguration urlConfiguration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Execution execution;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext xWikiContext;
    
    private ExecutionContext executionContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);
    
    @BeforeEach
    void setup()
    {
        this.executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.urlConfiguration.isTrustedDomainsEnabled()).thenReturn(true);
        this.xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.xWikiContext);
    }

    @Test
    void isDomainTrusted() throws Exception
    {
        when(urlConfiguration.getTrustedDomains()).thenReturn(Arrays.asList(
            "foo.acme.org",
            "localdomain"
        ));

        WikiDescriptor wikiDescriptor1 = mock(WikiDescriptor.class);
        when(wikiDescriptor1.getAliases()).thenReturn(Arrays.asList(
            "www.xwiki.org",
            "something.bar.com"
        ));

        WikiDescriptor wikiDescriptor2 = mock(WikiDescriptor.class);
        when(wikiDescriptor2.getAliases()).thenReturn(Collections.singletonList(
            "enterprise.eu"
        ));

        XWikiRequest request = mock(XWikiRequest.class);
        when(this.xWikiContext.getRequest()).thenReturn(request);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(request.getHttpServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/xwiki/bin/register/"));
        when(this.wikiDescriptorManager.getAll()).thenReturn(Arrays.asList(wikiDescriptor1, wikiDescriptor2));

        assertThat("www.xwiki.org is trusted", this.urlSecurityManager
            .isDomainTrusted(new URL("http://www.xwiki.org/xwiki/bin/view/XWiki/Login")));
        assertThat("www.xwiki.org is trusted", this.urlSecurityManager
            .isDomainTrusted(new URL("https://www.xwiki.org/xwiki/bin/view/XWiki/Login")));
        assertThat("www.xwiki.com is not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://www.xwiki.com/xwiki/bin/view/XWiki/Login")));
        assertThat("xwiki.org is not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://xwiki.org/xwiki/bin/view/XWiki/Login")));
        assertThat("foo.acme.org is trusted", this.urlSecurityManager
            .isDomainTrusted(new URL("https://foo.acme.org/something/else")));
        assertThat("bar.foo.acme.org is trusted since foo.acme.org is", this.urlSecurityManager
            .isDomainTrusted(new URL("https://bar.foo.acme.org/something/else")));
        assertThat("buz.bar.foo.acme.org is trusted since foo.acme.org is", this.urlSecurityManager
            .isDomainTrusted(new URL("https://buz.bar.foo.acme.org/something/else")));
        assertThat("acme.org is not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://acme.org/something/else")));
        assertThat("www.acme.org is not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://www.acme.org/something/else")));
        assertThat("something.bar.thing.com is not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://something.bar.thing.com")));
        assertThat("bar.thing.com is not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://bar.thing.com")));
        assertThat("something.bar.com is tristed", this.urlSecurityManager
            .isDomainTrusted(new URL("https://something.bar.com")));
        assertThat("enterprise.eu is trusted", this.urlSecurityManager
            .isDomainTrusted(new URL("https://enterprise.eu/xwiki/")));
        assertThat("enterprise.eu. is not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://enterprise.eu./xwiki/")));
        assertThat("current domain is trusted", this.urlSecurityManager
            .isDomainTrusted(new URL("https://localhost:8080/xwiki/bin/view/XWiki/Page")));
        assertThat("local domain is trusted", this.urlSecurityManager
            .isDomainTrusted(new URL("https://localhost.localdomain:8080/xwiki/bin/view/XWiki/Page")));
    }

    @Test
    void invalidateCache() throws Exception
    {
        when(urlConfiguration.getTrustedDomains()).thenReturn(Collections.singletonList(
            "xwiki.org"
        ));
        assertThat("www.xwiki.org is trusted", this.urlSecurityManager
            .isDomainTrusted(new URL("http://www.xwiki.org")));
        assertThat("foo.acme.org is not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://foo.acme.org/something/else")));

        when(urlConfiguration.getTrustedDomains()).thenReturn(Collections.singletonList(
            "foo.acme.org"
        ));

        // the asserts are still the same because we rely on cached values
        assertThat("www.xwiki.org is still trusted", this.urlSecurityManager
            .isDomainTrusted(new URL("http://www.xwiki.org")));
        assertThat("foo.acme.org is still not trusted", !this.urlSecurityManager
            .isDomainTrusted(new URL("https://foo.acme.org/something/else")));

        // after invalidation the cache has been recomputed.
        this.urlSecurityManager.invalidateCache();
        assertThat("www.xwiki.org is not trusted anymore", !this.urlSecurityManager
            .isDomainTrusted(new URL("http://www.xwiki.org")));
        assertThat("foo.acme.org is trusted now", this.urlSecurityManager
            .isDomainTrusted(new URL("https://foo.acme.org/something/else")));
    }

    @Test
    void isDomainTrustedWhenCheckSkipped() throws MalformedURLException
    {
        when(urlConfiguration.getTrustedDomains()).thenReturn(Collections.singletonList(
            "foo.acme.org"
        ));
        when(urlConfiguration.isTrustedDomainsEnabled()).thenReturn(false);
        assertThat("Any domain can be trusted when check is skipped: check with www.xwiki.org",
            this.urlSecurityManager.isDomainTrusted(new URL("http://www.xwiki.org")));
        assertThat("Any domain can be trusted when check is skipped: check with www.bar.eu",
            this.urlSecurityManager.isDomainTrusted(new URL("http://www.bar.eu")));
        assertThat("Any domain can be trusted when check is skipped: check with foo.acme.org",
            this.urlSecurityManager.isDomainTrusted(new URL("http://foo.acme.org")));

        when(urlConfiguration.isTrustedDomainsEnabled()).thenReturn(true);
        assertThat("www.xwiki.org should not be trusted",
            !this.urlSecurityManager.isDomainTrusted(new URL("http://www.xwiki.org")));
        assertThat("www.bar.eu should not be trusted",
            !this.urlSecurityManager.isDomainTrusted(new URL("http://www.bar.eu")));
        assertThat("foo.acme.org should be trusted",
            this.urlSecurityManager.isDomainTrusted(new URL("http://foo.acme.org")));

        when(this.executionContext.getProperty(URLSecurityManager.BYPASS_DOMAIN_SECURITY_CHECK_CONTEXT_PROPERTY))
            .thenReturn(true);
        assertThat("www.xwiki.org should be trusted when check is bypassed",
            this.urlSecurityManager.isDomainTrusted(new URL("http://www.xwiki.org")));

        assertEquals("Domain of URL [http://www.xwiki.org] does not belong to the list of trusted domains but "
                + "it's considered as trusted since the check has been bypassed.",
            logCapture.getMessage(0));
    }

    @Test
    void isURITrusted() throws URISyntaxException
    {
        URI uri = new URI("");
        assertTrue(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("//xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("/xwiki/somepage");
        assertTrue(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("http://xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("http:xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("ftp://xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("mailto:foo@xwiki.org");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("market://launch?id=somePackageName");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        verify(this.urlConfiguration, times(3)).getTrustedSchemes();

        // Check that using a list of trusted schemes is not enough here.
        when(this.urlConfiguration.getTrustedSchemes()).thenReturn(List.of("http", "ftp", "mailto", "market"));

        uri = new URI("http://xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("http:xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        // See https://claroty.com/team82/research/exploiting-url-parsing-confusion
        uri = new URI("http:///xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("ftp://xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("mailto:foo@xwiki.org");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("market://launch?id=somePackageName");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));
        assertEquals(1, this.logCapture.size());
        assertEquals("Error while transforming URI [market://launch?id=somePackageName] to URL: "
            + "[MalformedURLException: unknown protocol: market]", this.logCapture.getMessage(0));

        // invalidate cache so that we can call inject other trustedDomains
        this.urlSecurityManager.invalidateCache();
        when(urlConfiguration.getTrustedDomains()).thenReturn(Collections.singletonList(
            "xwiki.org"
        ));
        when(this.urlConfiguration.getTrustedSchemes()).thenReturn(List.of("http", "ftp", "sftp"));
        uri = new URI("");
        assertTrue(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("http://xwiki.org/xwiki/something/");
        assertTrue(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("ftp://xwiki.org/xwiki/something/");
        assertTrue(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("//xwiki.org/xwiki/something/");
        assertTrue(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("http:xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        // See https://claroty.com/team82/research/exploiting-url-parsing-confusion
        uri = new URI("http:///xwiki.org/xwiki/something/");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("http://floo");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        // https not among accepted schemes.
        uri = new URI("https://xwiki.org/something");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("/xwiki/something/");
        assertTrue(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("mailto:foo@xwiki.org");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        uri = new URI("sftp://xwiki.org/something");
        assertFalse(this.urlSecurityManager.isURITrusted(uri));

        assertEquals(2, this.logCapture.size());
        assertEquals("Error while transforming URI [sftp://xwiki.org/something] to URL: "
            + "[MalformedURLException: unknown protocol: sftp]", this.logCapture.getMessage(1));
    }

    private void assertParseToSafeThrowSecurity(String location, String expectedExceptionLocation)
    {
        SecurityException securityException =
            assertThrows(SecurityException.class, () -> this.urlSecurityManager.parseToSafeURI(location));
        assertEquals(String.format("The given URI [%s] is not safe on this server.", expectedExceptionLocation),
            securityException.getMessage());
    }

    @Test
    void parseToSafeURI() throws URISyntaxException
    {
        String location = "";
        URI uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals(location, uri.toString());

        location = "//xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "/xwiki/somepage";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals(location, uri.toString());

        location = "http://xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "http:xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "ftp://xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "mailto:foo@xwiki.org";
        assertParseToSafeThrowSecurity(location, location);

        location = "market://launch?id=somePackageName";
        assertParseToSafeThrowSecurity(location, location);

        verify(this.urlConfiguration, times(3)).getTrustedSchemes();

        // Check that using a list of trusted schemes is not enough here.
        when(this.urlConfiguration.getTrustedSchemes()).thenReturn(List.of("http", "ftp", "mailto", "market"));

        location = "http:xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "http:xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        // See https://claroty.com/team82/research/exploiting-url-parsing-confusion
        location = "http:///xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "ftp://xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "mailto:foo@xwiki.org";
        assertParseToSafeThrowSecurity(location, location);

        location = "market://launch?id=somePackageName";
        assertParseToSafeThrowSecurity(location, location);
        assertEquals(1, this.logCapture.size());
        assertEquals("Error while transforming URI [market://launch?id=somePackageName] to URL: "
            + "[MalformedURLException: unknown protocol: market]", this.logCapture.getMessage(0));

        // invalidate cache so that we can call inject other trustedDomains
        this.urlSecurityManager.invalidateCache();
        when(urlConfiguration.getTrustedDomains()).thenReturn(Collections.singletonList(
            "xwiki.org"
        ));
        when(this.urlConfiguration.getTrustedSchemes()).thenReturn(List.of("http", "ftp", "sftp"));

        location = "";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals(location, uri.toString());

        location = "http://xwiki.org/xwiki/something/";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals(location, uri.toString());

        location = "ftp://xwiki.org/xwiki/something/";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals(location, uri.toString());

        location = "//xwiki.org/xwiki/something/";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals(location, uri.toString());

        location = "http:xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        // See https://claroty.com/team82/research/exploiting-url-parsing-confusion
        location = "http:///xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "http://floo";
        assertParseToSafeThrowSecurity(location, location);

        // https not among accepted schemes.
        location = "https://xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        location = "/xwiki/something/";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals(location, uri.toString());

        location = "mailto:foo@xwiki.org";
        assertParseToSafeThrowSecurity(location, location);

        location = "sftp://xwiki.org/xwiki/something/";
        assertParseToSafeThrowSecurity(location, location);

        assertEquals(2, this.logCapture.size());
        assertEquals("Error while transforming URI [sftp://xwiki.org/xwiki/something/] to URL: "
            + "[MalformedURLException: unknown protocol: sftp]", this.logCapture.getMessage(1));

        // Check behaviour with |
        location = "http://extensions.xwiki.org/xwiki/bin/view/Main/WebHome#|t=recommendedextensions&p=1&l=15&s=doc"
            + ".creationDate&d=desc&name=replication";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("http://extensions.xwiki.org/xwiki/bin/view/Main/WebHome#%7Ct=recommendedextensions&p=1&l=15&s"
            + "=doc.creationDate&d=desc&name=replication", uri.toString());

        location = "http://xwiki.org\\@anotherserver.com";
        assertParseToSafeThrowSecurity(location, "http://xwiki.org%5C@anotherserver.com");

        // invalidate cache so that we can call inject other trustedDomains
        this.urlSecurityManager.invalidateCache();
        when(urlConfiguration.getTrustedDomains()).thenReturn(Collections.singletonList(
            "anotherserver.com"
        ));
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("http://xwiki.org%5C@anotherserver.com", uri.toString());

        location = "/xwiki/\n/something/";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("/xwiki/%0A/something/", uri.toString());

        location = "/Space Test/?parameter=with space#fragment space";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("/Space%20Test/?parameter=with%20space#fragment%20space", uri.toString());

        location = "foo://www.x wiki.org/";
        assertParseToSafeThrowSecurity(location, "foo://www.x%20wiki.org/");

        location = "/xwiki/bin/edit/Some%20Space/03%20f%C3%A9vrier%202023";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals(location, uri.toString());

        location = "/xwiki/bin/edit/Some Space/03%20f%C3%A9vrier%202023";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("/xwiki/bin/edit/Some%20Space/03%20f%C3%A9vrier%202023", uri.toString());

        location = "/xwiki/bin/edit/Some Space/%%%%";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("/xwiki/bin/edit/Some%20Space/%25%25%25%25", uri.toString());

        location = "/xwiki/bin/edit/Some Space/%C3";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("/xwiki/bin/edit/Some%20Space/%C3", uri.toString());

        location = "/xwiki/bin/edit/Some Space/%C";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("/xwiki/bin/edit/Some%20Space/%25C", uri.toString());

        location = "/xwiki/bin/edit/Some Space/%?query=03%20f%C3%A9vrier%202023%C#anchor%25C%A9%e";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("/xwiki/bin/edit/Some%20Space/%25"
            + "?query=03%20f%C3%A9vrier%202023%25C#anchor%25C%A9%25e", uri.toString());

        // invalidate cache so that we can call inject other trustedDomains
        this.urlSecurityManager.invalidateCache();
        when(urlConfiguration.getTrustedDomains()).thenReturn(List.of(
            "faß.example",
            "fa%C3%9F%25.example"
        ));
        location = "http://faß.example/Some%20Space/%C";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("http://faß.example/Some%20Space/%25C", uri.toString());

        location = "http://fa%C3%9F%.example/Some Space/Test#anchor%202";
        uri = this.urlSecurityManager.parseToSafeURI(location);
        assertEquals("http://fa%C3%9F%25.example/Some%20Space/Test#anchor%202", uri.toString());

        String locationWithForbiddenToken = "//xwiki.org/ xwiki/__XWIKI_URL_SECURITY_PERCENT__/something/";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
            () -> this.urlSecurityManager.parseToSafeURI(locationWithForbiddenToken));
        assertEquals("The given uri [//xwiki.org/ xwiki/__XWIKI_URL_SECURITY_PERCENT__/something/] contains the string "
            + "[__XWIKI_URL_SECURITY_PERCENT__] which is used internally for performing escaping operations "
            + "when trying to 'repair' a URI which cannot be parsed. Check the original error for repairing the URI "
            + "or try to use a different marker.", illegalArgumentException.getMessage());
        assertTrue(illegalArgumentException.getCause() instanceof URISyntaxException);
    }
}
