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
package org.xwiki.rendering.wikimodel.renderer;

import java.io.PrintWriter;
import java.io.Writer;

import org.wikimodel.wem.tex.TexSerializer;
import org.xwiki.rendering.listener.ListenerDelegate;
import org.xwiki.rendering.renderer.Renderer;
import org.xwiki.rendering.wikimodel.internal.WikiModelGeneratorListener;
import org.xwiki.rendering.wikimodel.internal.XWikiPrinter;

public class TexRenderer extends ListenerDelegate implements Renderer
{
    private PrintWriter writer;

	public TexRenderer(Writer writer)
	{
		this.writer = new PrintWriter(writer);
		setWrappedListener(new WikiModelGeneratorListener(
				new TexSerializer(new XWikiPrinter(this.writer))));
	}
}
