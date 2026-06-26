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

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Merge JSON nodes.
 * 
 * @version $Id$
 * @since 12.10
 */
public class JSONMerge
{
    private static final String ID = "id";

    /**
     * Deep merge the given objects.
     * Note that the right element always have priority over the left element in case of conflict when merging. Also
     * the merge of object nodes happens only if both objects have an {@code id} key containing the same value. If
     * it's not the case the object will be treated as simple elements without performing deep analysis.
     * 
     * @param objects the objects to merge
     * @param <T> the type of objects to merge
     * @return the result of the merge
     * @throws IOException if the merge fails
     */
    public <T> T merge(@SuppressWarnings("unchecked") T... objects)
    {
        if (objects == null) {
            return null;
        }

        return Stream.of(objects).reduce(null, this::merge);
    }

    private <T> T merge(T left, T right)
    {
        if (left == null && right == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode leftNode = objectMapper.valueToTree(left);
        JsonNode rightNode = objectMapper.valueToTree(right);
        Class<?> type = right != null ? right.getClass() : left.getClass();
        try {
            return objectMapper.readerFor(type).readValue(merge(leftNode, rightNode));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode merge(JsonNode left, JsonNode right)
    {
        if (right == null || right.isNull()) {
            // Nothing to do.
            return left;
        } else if (left == null || !(left.isObject() || left.isArray())
            || !Objects.equals(left.getNodeType(), right.getNodeType())) {
            // Simply copy the right node because either the left node is null or we cannot merge.
            return right.deepCopy();
        } else if (left.isObject()) {
            // Merge object nodes.
            return merge((ObjectNode) left, (ObjectNode) right);
        } else {
            // Merge array nodes.
            return merge((ArrayNode) left, (ArrayNode) right);
        }
    }

    private ObjectNode merge(ObjectNode left, ObjectNode right)
    {
        // Don't merge if the right object has an identifier different than the one of the left object.
        if (right.has(ID) && !Objects.equals(left.get(ID), right.get(ID))) {
            return right.deepCopy();
        }

        Iterator<String> fieldNames = right.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode leftField = left.get(fieldName);
            JsonNode rightField = right.get(fieldName);
            left.set(fieldName, merge(leftField, rightField));
        }

        return left;
    }

    private ArrayNode merge(ArrayNode left, ArrayNode right)
    {
        ArrayNode result = left.arrayNode();
        // Add the items from the right, merging them with the corresponding item from the left.
        for (JsonNode rightItem : right) {
            // Find the corresponding left item to merge with.
            JsonNode leftItem = findById(rightItem.path(ID), left);
            result.add(merge(leftItem, rightItem));
        }
        // Add the items from the left that are identifiable and that are not present on the right.
        for (JsonNode leftItem : left) {
            if (leftItem.hasNonNull(ID) && findById(leftItem.get(ID), right) == null) {
                result.add(leftItem.deepCopy());
            }
        }
        return result;
    }

    private JsonNode findById(JsonNode id, ArrayNode array)
    {
        if (id != null && !id.isNull() && !id.isMissingNode()) {
            for (JsonNode item : array) {
                if (id.equals(item.path(ID))) {
                    return item;
                }
            }
        }
        return null;
    }
}
