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
package org.xwiki.livedata.internal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link JSONMerge}.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
class JSONMergeTest
{
    private JSONMerge jsonMerge = new JSONMerge();

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mergeNull() throws Exception
    {
        assertNull(this.jsonMerge.merge((Object[]) null));
        assertNull(this.jsonMerge.merge((Object) null));
        assertNull(this.jsonMerge.merge(null, null));

        assertMerge("{'age':21}", "{'age':21}", null);
        assertMerge("{'age':21}", null, "{'age':21}");
    }

    @Test
    void mergeNodesWithDifferentTypes() throws Exception
    {
        assertMerge("13", "true", "13");
        assertMerge("'eight'", "8", "'eight'");
        assertMerge("{'enabled':true}", "[17]", "{'enabled':true}");
        assertMerge("[17]", "{'enabled':true}", "[17]");
    }

    @Test
    void mergeNodesOfSameTypeButNotObjectOrArray() throws Exception
    {
        assertMerge("13", "27", "13");
        assertMerge("false", "true", "false");
        assertMerge("'foo'", "'bar'", "'foo'");
    }

    @Test
    void mergeObjects() throws Exception
    {
        assertMerge("{'name':'alice','married':true,'age':37}", "{'name':'alice','married':false}",
            "{'age':37,'married':true}");

        assertMerge("{'name':'bob','address':{'street':'Test','flat':968,'floor':3}}",
            "{'name':'alice','address':{'street':'Test','flat':570}}",
            "{'name':'bob','address':{'flat':968,'floor':3}}");

        assertMerge("{'id':'alice','age':21}", "{'id':'alice','age':37}", "{'age':21}");

        assertMerge("{'id':'alice','age':21}", "{'married':true}", "{'id':'alice','age':21}");

        assertMerge("{'id':'alice','age':21}", "{'id':'bob','married':true,'profession':'teacher'}",
            "{'id':'alice','age':21}");

        assertMerge("{'name':'alice','address':{'id':'test','flat':968,'floor':3}}",
            "{'name':'alice','address':{'street':'Test','flat':570}}",
            "{'address':{'id':'test','flat':968,'floor':3}}");
    }

    @Test
    void mergeArrays() throws Exception
    {
        assertMerge("[17]", "[true,'red']", "[17]");
        assertMerge("[true,'red']", "[17]", "[true,'red']");

        assertMerge(
            "[{'name':'bob','country':'fr'}," + "{'id':'alice','age':16,'birthday':1234567890,'graduated':false},"
                + "'test',{'id':'carol','age':29}]",
            "[{'id':'alice','age':17,'birthday':1234567890}," + "{'name':'bob','age':25}," + "{'id':'carol','age':29},"
                + "{'id':null,'country':'uk'}]",
            "[{'name':'bob','country':'fr'},{'id':'alice','age':16,'graduated':false},'test']");
    }

    private void assertMerge(String expected, String left, String right) throws Exception
    {
        Object leftObject = left == null ? null : this.objectMapper.readValue(left.replace('\'', '"'), Object.class);
        Object rightObject = right == null ? null : this.objectMapper.readValue(right.replace('\'', '"'), Object.class);
        Object mergeObject = this.jsonMerge.merge(leftObject, rightObject);
        assertEquals(expected == null ? null : expected.replace('\'', '"'),
            mergeObject == null ? null : this.objectMapper.writeValueAsString(mergeObject));
    }
}
