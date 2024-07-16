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
package org.xwiki.web;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.url.script.URLSecurityScriptService;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.internal.XWikiDateTool;
import org.xwiki.xml.html.script.HTMLScriptService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@code macros.vm}.
 *
 * @version $Id$
 */
@ComponentList({XWikiDateTool.class, ModelScriptService.class})
class MacrosTest extends PageTest
{
    private VelocityManager velocityManager;

    @BeforeEach
    void setup() throws Exception
    {
        this.velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
    }
    
    @Test
    void setVariableGlobal() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
			+ "#macro(helloMacro $return)\n"
			+ "  #set($return = $NULL)\n"
			+ "  #setVariable(\"$return\" \"my new value\")\n"
			+ "#end\n"
			+ "#helloMacro($myVarToSet)"));
        
        VelocityContext velocityContext = this.velocityManager.getVelocityContext();
        assertEquals("my new value", velocityContext.get("myVarToSet"));
    } 

    @Test
    void setVariableInMacroContext() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
			+ "#macro(childMacro $return)\n"
			+ "  #set($return = $NULL)\n"
			+ "  #setVariable(\"$return\" \"value from child macro\")\n"
			+ "#end\n"  		
			+ "#macro(parentMacro $return)\n"
			+ "  #childMacro($macro.childMacroResult)\n"
			+ "  #setVariable(\"$return\" \"${macro.childMacroResult}/parentValue\")\n"
			+ "#end\n"
			+ "#parentMacro($myVarToSet)"));
        
        VelocityContext velocityContext = this.velocityManager.getVelocityContext();
        assertEquals("value from child macro/parentValue", velocityContext.get("myVarToSet"));
    } 
    
    @Test
    void addLivetableLocationFilter() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox')\n"
            + "$whereQL $whereParams"));
        assertEquals("AND LOWER(doc.fullName) LIKE LOWER(?) ESCAPE '!' [%sandbox%]", writer.toString().trim());
    }

    @Test
    void addLivetableLocationFilterWhenFilteringWebHome() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', true)\n"
            + "$whereQL $whereParams"));
        assertEquals("AND ((doc.name = 'WebHome' AND LOWER(doc.space) LIKE LOWER(?) ESCAPE '!') OR "
            + "(doc.name <> 'WebHome' AND LOWER(doc.fullName) LIKE LOWER(?) ESCAPE '!')) [%sandbox%, %sandbox%]",
            writer.toString().trim());
    }

    @Test
    void addLivetableLocationFilterWhenCustomWhere() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', false, 'SOMETHING')\n"
            + "$whereQL $whereParams"));
        assertEquals("SOMETHING []", writer.toString().trim());
    }

    @Test
    void addLivetableLocationFilterWhenFilteringWebHomeAndCustomWhere() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', true, 'SOMETHING')\n"
            + "$whereQL $whereParams"));
        assertEquals("SOMETHING []", writer.toString().trim());
    }

    @Test
    void livetableFilterObfuscatedTotalGreaterThanReturnedRows() throws Exception
    {
        Map<Object, Object> mapParameter = new HashMap<>();
        mapParameter.put("totalrows", 10);
        mapParameter.put("returnedrows", 5);
        List<String> dummyRows = asList("a", "b", "c");
        mapParameter.put("rows", dummyRows);
        this.velocityManager.getVelocityContext().put("map", mapParameter);
        this.velocityManager.evaluate(new StringWriter(), "livetable",
            new StringReader("#livetable_filterObfuscated($map)"));
        Map<Object, Object> map = (Map<Object, Object>) this.velocityManager.getVelocityContext().get("map");
        assertEquals(10, map.get("totalrows"));
        assertEquals(5, map.get("returnedrows"));
        assertSame(dummyRows, map.get("rows"));
    }

    @Test
    void livetableFilterObfuscatedTotalLowerThanReturnedRows() throws Exception
    {
        HashMap<Object, Object> mapParameter = new HashMap<>();
        mapParameter.put("totalrows", 2);
        mapParameter.put("returnedrows", 2);
        List<Map<Object, Object>> dummyRows = asList(
            singletonMap("doc_viewable", true),
            singletonMap("doc_viewable", false)
        );
        mapParameter.put("rows", dummyRows);
        this.velocityManager.getVelocityContext().put("map", mapParameter);
        this.velocityManager.evaluate(new StringWriter(), "livetable",
            new StringReader("#livetable_filterObfuscated($map)"));
        Map<String, Object> map = (Map<String, Object>) this.velocityManager.getVelocityContext().get("map");
        assertEquals(1, map.get("totalrows"));
        assertEquals(1, map.get("returnedrows"));
        assertEquals(1, ((List<?>) map.get("rows")).size());
        assertTrue(((List<Map<String, Boolean>>) map.get("rows")).get(0).get("doc_viewable"));
    }

    @Test
    void livetableFilterObfuscatedTotalrowsWithOffset() throws Exception
    {
        Map<Object, Object> mapParameter = new HashMap<>();
        mapParameter.put("offset", "3");
        mapParameter.put("totalrows", 2);
        mapParameter.put("returnedrows", 2);
        List<Map<Object, Object>> dummyRows = asList(
            singletonMap("doc_viewable", true),
            singletonMap("doc_viewable", true)
        );
        mapParameter.put("rows", dummyRows);
        this.velocityManager.getVelocityContext().put("map", mapParameter);
        this.velocityManager.evaluate(new StringWriter(), "livetable",
            new StringReader("#livetable_filterObfuscated($map)"));
        Map<String, Object> map = (Map<String, Object>) this.velocityManager.getVelocityContext().get("map");
        assertEquals(4, map.get("totalrows"));
        assertEquals(2, map.get("returnedrows"));
        assertEquals(2, ((List<?>) map.get("rows")).size());
    }

    @Test
    void parseDateRangeAfter() throws Exception
    {
        this.velocityManager.getVelocityContext().put("dateRange", new HashMap<>());
        this.velocityManager.getVelocityContext().put("dateValue", "2021-09-22T00:00:00+02:00");

        String script = "#parseDateRange('after' $dateValue $dateRange)";
        StringWriter out = new StringWriter();
        this.velocityManager.evaluate(out, "parseDateRangeAfter", new StringReader(script));

        Map<Object, Object> dateRange =
            (Map<Object, Object>) this.velocityManager.getVelocityContext().get("dateRange");
        assertEquals(1632261600000L, ((Date) dateRange.get("start")).getTime());
        assertNull(dateRange.get("end"));
    }

    @Test
    void parseDateRangeBefore() throws Exception
    {
        String dateValue = "2021-09-22T23:59:59+02:00";

        this.velocityManager.getVelocityContext().put("dateRange", new HashMap<>());
        this.velocityManager.getVelocityContext().put("dateValue", dateValue);

        String script = "#parseDateRange('before' $dateValue $dateRange)";
        StringWriter out = new StringWriter();
        this.velocityManager.evaluate(out, "parseDateRangeAfter", new StringReader(script));

        Map<Object, Object> dateRange =
            (Map<Object, Object>) this.velocityManager.getVelocityContext().get("dateRange");
        assertNull(dateRange.get("start"));
        assertEquals(1632347999000L, ((Date) dateRange.get("end")).getTime());
    }

    @Test
    void parseDateRangeBetween() throws Exception
    {
        String dateValue = "2021-09-22T00:00:00+02:00/2021-09-22T23:59:59+02:00";

        this.velocityManager.getVelocityContext().put("dateRange", new HashMap<>());
        this.velocityManager.getVelocityContext().put("dateValue", dateValue);

        String script = "#parseDateRange('between' $dateValue $dateRange)";
        StringWriter out = new StringWriter();
        this.velocityManager.evaluate(out, "parseDateRangeAfter", new StringReader(script));

        Map<Object, Object> dateRange =
            (Map<Object, Object>) this.velocityManager.getVelocityContext().get("dateRange");
        assertEquals(1632261600000L, ((Date) dateRange.get("start")).getTime());
        assertEquals(1632347999000L, ((Date) dateRange.get("end")).getTime());
    }

    @Test
    void parseDateRangeTimestampRange() throws Exception
    {
        this.velocityManager.getVelocityContext().put("dateRange", new HashMap<>());
        this.velocityManager.getVelocityContext().put("dateValue", "1607295600000-1632347999999");

        String script = "#parseDateRange('after' $dateValue $dateRange)";
        StringWriter out = new StringWriter();
        this.velocityManager.evaluate(out, "parseDateRangeAfter", new StringReader(script));

        Map<Object, Object> dateRange =
            (Map<Object, Object>) this.velocityManager.getVelocityContext().get("dateRange");
        assertEquals(1607295600000L, ((Date) dateRange.get("start")).getTime());
        assertEquals(1632347999999L, ((Date) dateRange.get("end")).getTime());
    }

    @Test
    void displayUser() throws Exception
    {
        // displaying user without any arg, should display Guest user
        String script = "#displayUser()";
        StringWriter out = new StringWriter();
        this.velocityManager.evaluate(out, "displayUser", new StringReader(script));
        assertEquals("<div class=\"user\" data-reference=\"xwiki:XWiki.XWikiGuest\">"
            + "<img class=\"user-avatar\" src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" "
            + "alt=\"XWikiGuest\" />"
            + "<a class=\"user-name\" href=\"/xwiki/bin/view/XWiki/XWikiGuest\">XWikiGuest</a>"
            + "</div>", out.toString().trim());

        script = "#displayUser($NULL)";
        out = new StringWriter();
        this.velocityManager.evaluate(out, "displayUser", new StringReader(script));
        assertEquals("<div class=\"user\" data-reference=\"xwiki:XWiki.XWikiGuest\">"
            + "<img class=\"user-avatar\" src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" "
            + "alt=\"XWikiGuest\" />"
            + "<a class=\"user-name\" href=\"/xwiki/bin/view/XWiki/XWikiGuest\">XWikiGuest</a>"
            + "</div>", out.toString().trim());

        script = "#displayUser(\"\")";
        out = new StringWriter();
        this.velocityManager.evaluate(out, "displayUser", new StringReader(script));
        assertEquals("<div class=\"user\" data-reference=\"xwiki:XWiki.XWikiGuest\">"
            + "<img class=\"user-avatar\" src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" "
            + "alt=\"XWikiGuest\" />"
            + "<a class=\"user-name\" href=\"/xwiki/bin/view/XWiki/XWikiGuest\">XWikiGuest</a>"
            + "</div>", out.toString().trim());

        script = "#displayUser(\"XWiki.Foo\")";
        out = new StringWriter();
        this.velocityManager.evaluate(out, "displayUser", new StringReader(script));
        assertEquals("<div class=\"user\" data-reference=\"xwiki:XWiki.Foo\">"
            + "<img class=\"user-avatar\" src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" "
            + "alt=\"Foo\" />"
            + "<a class=\"user-name\" href=\"/xwiki/bin/view/XWiki/Foo\">Foo</a>"
            + "</div>", out.toString().trim());

        DocumentReference userFoo = new DocumentReference("xwiki", "XWiki", "Foo");
        this.velocityManager.getVelocityContext().put("userFoo", userFoo);

        script = "#displayUser($userFoo)";
        out = new StringWriter();
        this.velocityManager.evaluate(out, "displayUser", new StringReader(script));
        assertEquals("<div class=\"user\" data-reference=\"xwiki:XWiki.Foo\">"
            + "<img class=\"user-avatar\" src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" "
            + "alt=\"Foo\" />"
            + "<a class=\"user-name\" href=\"/xwiki/bin/view/XWiki/Foo\">Foo</a>"
            + "</div>", out.toString().trim());
    }

    @Test
    void getSanitizedURLAttributeValue() throws Exception
    {
        HTMLScriptService htmlScriptService = mock(HTMLScriptService.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "html", htmlScriptService);
        SecurityScriptService securityScriptService = mock(SecurityScriptService.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "security", securityScriptService);
        URLSecurityScriptService urlSecurityScriptService = mock(URLSecurityScriptService.class);
        when(securityScriptService.get("url")).thenReturn(urlSecurityScriptService);

        String htmlElement = "form";
        String attributeName = "action";
        String url = "javascript:alert('foobar')";
        String fallbackUrl = "xwiki/bin/view/Sandbox";
        String script = "#getSanitizedURLAttributeValue($myHtmlElement,$myAttributeName,$myUrl,$myFallback,"
            + "$sanitizedResult)";

        VelocityContext velocityContext = this.velocityManager.getVelocityContext();
        velocityContext.put("myHtmlElement", htmlElement);
        velocityContext.put("myAttributeName", attributeName);
        velocityContext.put("myUrl", url);
        velocityContext.put("myFallback", fallbackUrl);

        URI fallBackUri = new URI(fallbackUrl);
        when(urlSecurityScriptService.parseToSafeURI(url)).thenReturn(null);
        when(urlSecurityScriptService.parseToSafeURI(fallbackUrl)).thenReturn(fallBackUri);
        when(htmlScriptService.isAttributeSafe(htmlElement, attributeName, fallBackUri.toString())).thenReturn(true);

        // First check: given URL is not secure according to security.url, so we fallback and the HTMLScriptService
        // allows the fallback URL.
        StringWriter out = new StringWriter();
        this.velocityManager.evaluate(out, "getSanitizedURLAttributeValue", new StringReader(script));
        verify(urlSecurityScriptService).parseToSafeURI(url);

        assertEquals(fallbackUrl, velocityContext.get("sanitizedResult"));

        // Second check: given URL is not secure according to security.url, so we fallback but the HTMLScriptService
        // does not allow the fallback URL.
        when(htmlScriptService.isAttributeSafe(htmlElement, attributeName, fallBackUri.toString())).thenReturn(false);
        out = new StringWriter();
        this.velocityManager.evaluate(out, "getSanitizedURLAttributeValue", new StringReader(script));
        verify(htmlScriptService, times(2)).isAttributeSafe(htmlElement, attributeName, fallBackUri.toString());
        assertEquals("", velocityContext.get("sanitizedResult"));

        // Third check: both the given URL and the fallback URL are not secure according to security.url
        when(urlSecurityScriptService.parseToSafeURI(fallbackUrl)).thenReturn(null);
        out = new StringWriter();
        this.velocityManager.evaluate(out, "getSanitizedURLAttributeValue", new StringReader(script));
        assertEquals("", velocityContext.get("sanitizedResult"));
        // Since the URLSecurityScriptService returns false for both URLs the HTMLScriptService should not be called.
        verify(htmlScriptService, times(2)).isAttributeSafe(htmlElement, attributeName, fallBackUri.toString());

        // Fourth check: the given URL is valid and there's no fallback
        url = "http://xwiki.org/?query=foo&bar=%20space%4F#éâ";
        fallbackUrl = "";
        velocityContext.put("myUrl", url);
        velocityContext.put("myFallback", fallbackUrl);

        URI xwikiOrgURI = new URI(url);
        when(urlSecurityScriptService.parseToSafeURI(url)).thenReturn(xwikiOrgURI);
        when(htmlScriptService.isAttributeSafe(htmlElement, attributeName, xwikiOrgURI.toString())).thenReturn(true);

        out = new StringWriter();
        this.velocityManager.evaluate(out, "getSanitizedURLAttributeValue", new StringReader(script));
        assertEquals("http://xwiki.org/?query=foo&#38;bar=%20space%4F#éâ", velocityContext.get("sanitizedResult"));

        // Fifth check: the given URL is not valid and there's no fallback
        when(urlSecurityScriptService.parseToSafeURI(url)).thenReturn(null);
        out = new StringWriter();
        this.velocityManager.evaluate(out, "getSanitizedURLAttributeValue", new StringReader(script));
        assertEquals("", velocityContext.get("sanitizedResult"));
    }
}
