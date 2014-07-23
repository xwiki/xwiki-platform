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
package org.xwiki.rendering.signature.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;

/**
 * Dump a {@link MacroBlock} or a {@link MacroMarkerBlock} into a binary stream.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("macro")
@Singleton
public class MacroBlockDumper implements BlockDumper
{
    /**
     * {@inheritDoc}
     *
     * The dump contains all the macro parameters and the content of the macro.
     * The source MetaData is also included if available.
     *
     */
    @Override
    public void dump(OutputStream out, Block block) throws IOException
    {
        if (block instanceof MacroBlock) {
            MacroBlock b = (MacroBlock) block;
            dump(out, b.getId(), b.getParameters(), b.getContent());
        } else if (block instanceof MacroMarkerBlock) {
            MacroMarkerBlock b = (MacroMarkerBlock) block;
            dump(out, b.getId(), b.getParameters(), b.getContent());
        } else {
            throw new IllegalArgumentException("Unsupported block [" + block.getClass().getName() + "].");
        }

        String source = getSourceReference(block);
        if (source != null) {
            out.write(toBytes(source));
        }
        out.write(0x00);
    }

    @Override
    public byte[] dump(Block block) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dump(out, block);
        return out.toByteArray();
    }

    private static void dump(OutputStream out, String macroId, Map<String, String> parameters, String content)
        throws IOException
    {
        out.write(toBytes(macroId));
        out.write(0x00);

        for (Map.Entry<String, String> param : parameters.entrySet()) {
            out.write(toBytes(param.getKey()));
            out.write(0x00);
            out.write(toBytes(param.getValue()));
            out.write(0x00);
        }
        out.write(0x00);

        if (content != null) {
            out.write(toBytes(content));
        }
        out.write(0x00);
    }

    private String getSourceReference(Block block)
    {
        MetaDataBlock metaDataBlock =
            block.getFirstBlock(new MetadataBlockMatcher(MetaData.SOURCE), Block.Axes.ANCESTOR);
        if (metaDataBlock != null) {
            return (String) metaDataBlock.getMetaData().getMetaData(MetaData.SOURCE);
        }
        return null;
    }

    private static byte[] toBytes(String s)
    {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            // Should never happen since UTF-8 is a requirement for any JVM.
        }
        return s.getBytes();
    }
}
