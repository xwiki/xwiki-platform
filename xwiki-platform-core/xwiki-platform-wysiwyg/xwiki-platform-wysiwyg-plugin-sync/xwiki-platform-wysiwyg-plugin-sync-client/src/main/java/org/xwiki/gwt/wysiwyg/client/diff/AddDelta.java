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
package org.xwiki.gwt.wysiwyg.client.diff;

import java.util.List;

/**
 * Holds an add-delta between to revisions of a text.
 * 
 * @version $Id$
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * @see Delta
 * @see Diff
 * @see Chunk
 */
public class AddDelta extends Delta
{

    public AddDelta()
    {
        super();
    }

    public AddDelta(int origpos, Chunk rev)
    {
        init(new Chunk(origpos, 0), rev);
    }

    public void verify(List target) throws PatchFailedException
    {
        if (original.first() > target.size())
        {
            throw new PatchFailedException("original.first() > target.size()");
        }
    }

    public void applyTo(List target)
    {
        revised.applyAdd(original.first(), target);
    }

    public void toString(StringBuffer s)
    {
        s.append(original.anchor());
        s.append("a");
        s.append(revised.rangeString());
        s.append(Diff.NL);
        revised.toString(s, "> ", Diff.NL);
    }

    public void toRCSString(StringBuffer s, String EOL)
    {
        s.append("a");
        s.append(original.anchor());
        s.append(" ");
        s.append(revised.size());
        s.append(EOL);
        revised.toString(s, "", EOL);
    }

    public void Accept(RevisionVisitor visitor)
    {
        visitor.visit(this);
    }

    public void accept(RevisionVisitor visitor)
    {
        visitor.visit(this);
    }
}
