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
package org.xwiki.rendering.internal.renderer.plain;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.rendering.listener.DocumentResourceReference;
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.listener.ResourceType;
import org.xwiki.rendering.renderer.link.LinkReferenceSerializer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link PlainTextChainingRenderer} that cannot easily be performed using the Rendering Test framework.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class PlainTextChainingRendererTest extends AbstractComponentTestCase
{
	private PlainTextRenderer renderer;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();

		// Force the link label generator to be null
		this.renderer = new PlainTextRenderer();
		ReflectionUtils.setFieldValue(this.renderer, "defaultLinkReferenceSerializer", 
		    getComponentManager().lookup(LinkReferenceSerializer.class, "xwiki/2.1"));
		this.renderer.initialize();
	}
	
    @Test
    public void testBeginLinkWhenLinkLabelGeneratorIsNull() throws Exception
    {
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.renderer.setPrinter(printer);

        DocumentResourceReference reference = new DocumentResourceReference("reference");
        reference.setAnchor("anchor");
        reference.setQueryString("param=value");

        this.renderer.beginLink(reference, false, Collections.<String, String> emptyMap());
        this.renderer.endLink(reference, false, Collections.<String, String> emptyMap());

        Assert.assertEquals("reference", printer.toString());
    }

    @Test
    public void testBeginLinkWhenExternalLink() throws Exception
    {
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.renderer.setPrinter(printer);

        ResourceReference reference = new ResourceReference("http://some/url", ResourceType.URL);
        this.renderer.beginLink(reference, false, Collections.<String, String> emptyMap());
        this.renderer.endLink(reference, false, Collections.<String, String> emptyMap());

        Assert.assertEquals("http://some/url", printer.toString());
    }
}
