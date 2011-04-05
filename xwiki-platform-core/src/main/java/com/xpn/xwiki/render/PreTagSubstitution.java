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
 *
 */

package com.xpn.xwiki.render;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;

public class PreTagSubstitution extends WikiSubstitution
{
    private int counter = 0;

    private List<String> list = new ArrayList<String>();

    private boolean removePre = false;

    public PreTagSubstitution(com.xpn.xwiki.util.Util util, boolean removepre)
    {
        super(util);
        setPattern("{pre}.*?{/pre}", Perl5Compiler.CASE_INSENSITIVE_MASK | Perl5Compiler.SINGLELINE_MASK);
        setRemovePre(removepre);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.render.WikiSubstitution#appendSubstitution(java.lang.StringBuffer, org.apache.oro.text.regex.MatchResult, int, org.apache.oro.text.regex.PatternMatcherInput, org.apache.oro.text.regex.PatternMatcher, org.apache.oro.text.regex.Pattern)
     */
    @Override
    public void appendSubstitution(StringBuffer stringBuffer, MatchResult matchResult, int i,
        PatternMatcherInput minput, PatternMatcher patternMatcher, Pattern pattern)
    {
        String content = matchResult.group(0);
        if (isRemovePre()) {
            content = getUtil().substitute("s/{pre}//ig", content);
            content = getUtil().substitute("s/{\\/pre}//ig", content);
        }
        getList().add(content);
        stringBuffer.append("%_" + this.counter + "_%");
        this.counter++;
    }

    public List<String> getList()
    {
        return this.list;
    }

    public void setList(List<String> list)
    {
        this.list = list;
    }

    public String insertNonWikiText(String content)
    {
        for (int i = 0; i < this.list.size(); i++) {
            content = StringUtils.replace(content, "%_" + i + "_%", this.list.get(i));
        }
        return content;
    }

    public boolean isRemovePre()
    {
        return this.removePre;
    }

    public void setRemovePre(boolean remove)
    {
        this.removePre = remove;
    }
}
