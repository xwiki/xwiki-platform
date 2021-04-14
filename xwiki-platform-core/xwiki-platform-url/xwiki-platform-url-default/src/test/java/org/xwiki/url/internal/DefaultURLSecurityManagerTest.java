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
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
    
    private ExecutionContext executionContext;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);
    
    @BeforeEach
    void setup()
    {
        this.executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.urlConfiguration.isTrustedDomainsEnabled()).thenReturn(true);
    }

    @Test
    void isDomainTrusted() throws Exception
    {
        when(urlConfiguration.getTrustedDomains()).thenReturn(Arrays.asList(
            "foo.acme.org",
            "com" // this should not be taken into account
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

        assertEquals("The domain [com] specified in the trusted domains configuration won't be taken into account "
                + "since it doesn't respect the documented format.",
            logCapture.getMessage(0));
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
}
