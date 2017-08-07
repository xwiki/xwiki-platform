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
package org.xwiki.notifications.sources.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.LikeNode;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.expression.NotEqualsNode;
import org.xwiki.notifications.filters.expression.OrNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.filters.expression.generics.AbstractBinaryOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.filters.expression.generics.AbstractUnaryOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;
import org.xwiki.text.StringUtils;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

/**
 * Parser used to transform {@link AbstractNode} based abstract syntax trees to HQL language.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class NFExpressionToHQLParser
{
    private Map<String, String> queryParameters;

    /**
     * Parse the given node. The query parameters corresponding to the result expression are available using
     * {@link NFExpressionToHQLParser#getQueryParameters()}.
     *
     * @param node the root node to parse
     * @return the generated query
     */
    public String parse(AbstractNode node)
    {
        queryParameters = new HashMap<>();
        return parseBlock(node);
    }

    /**
     * @return the parsed query parameters
     */
    public Map<String, String> getQueryParameters()
    {
        return queryParameters;
    }

    private String parseBlock(AbstractNode node)
    {
        if (node instanceof AbstractValueNode) {
            return parseValue((AbstractValueNode) node);
        } else if (node instanceof AbstractUnaryOperatorNode) {
            return parseUnaryOperator((AbstractUnaryOperatorNode) node);
        } else if (node instanceof AbstractBinaryOperatorNode) {
            return parseBinaryOperator((AbstractBinaryOperatorNode) node);
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String parseValue(AbstractValueNode value)
    {
        return parseValue(value, false);
    }

    private String parseValue(AbstractValueNode value, boolean escape)
    {
        String returnValue;

        if (value instanceof PropertyValueNode) {
            switch (((PropertyValueNode) value).getContent()) {
                case APPLICATION:
                    returnValue = "event.application";
                    break;
                case BODY:
                    returnValue = "event.body";
                    break;
                case EVENT_TYPE:
                    returnValue = "event.eventType";
                    break;
                case HIDDEN:
                    returnValue = "event.hidden";
                    break;
                case PAGE:
                    returnValue = "event.page";
                    break;
                case PRIORITY:
                    returnValue = "event.priority";
                    break;
                case SPACE:
                    returnValue = "event.space";
                    break;
                case TITLE:
                    returnValue = "event.title";
                    break;
                case USER:
                    returnValue = "event.user";
                    break;
                case WIKI:
                    returnValue = "event.wiki";
                    break;
                default: returnValue = StringUtils.EMPTY;
            }
        } else if (value instanceof StringValueNode) {
            // If we’re dealing with raw values, we have to put them in the queryParameters map
            StringValueNode valueNode = (StringValueNode) value;
            String nodeContent = (escape) ? escape(valueNode.getContent()) : valueNode.getContent();

            // In order to lower the probability of having collisions in the query parameters provided by other
            // parsers, we use a key based on the sha256 fingerprint of its value.
            String mapKey = String.format("value_%s", sha256Hex(valueNode.getContent()));

            queryParameters.put(mapKey, nodeContent);

            returnValue = String.format(":%s", mapKey);
        } else {
            returnValue = StringUtils.EMPTY;
        }

        return returnValue;
    }

    private String parseUnaryOperator(AbstractUnaryOperatorNode operator)
    {
        if (operator instanceof NotNode) {
            return String.format(" NOT (%s)", parseBlock(operator.getOperand()));
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String parseBinaryOperator(AbstractBinaryOperatorNode operator)
    {
        String returnValue;

        if (operator instanceof AndNode) {
            returnValue = String.format("(%s) AND (%s)", parseBlock(operator.getLeftOperand()),
                    parseBlock(operator.getRightOperand()));
        } else if (operator instanceof OrNode) {
            returnValue = String.format("(%s) OR (%s)", parseBlock(operator.getLeftOperand()),
                    parseBlock(operator.getRightOperand()));
        } else if (operator instanceof EqualsNode) {
            returnValue = String.format("%s = %s", parseValue((AbstractValueNode) operator.getLeftOperand()),
                    parseValue((AbstractValueNode) operator.getRightOperand()));
        } else if (operator instanceof NotEqualsNode) {
            returnValue = String.format("%s <> %s", parseValue((AbstractValueNode) operator.getLeftOperand()),
                    parseValue((AbstractValueNode) operator.getRightOperand()));
        } else if (operator instanceof LikeNode) {
            returnValue = String.format("%s LIKE %s ESCAPE '!'",
                    parseValue((AbstractValueNode) operator.getLeftOperand()),
                    parseValue((AbstractValueNode) operator.getRightOperand(), true));
        } else {
            returnValue = StringUtils.EMPTY;
        }

        return returnValue;
    }

    private String escape(String format)
    {
        // See EscapeLikeParametersQuery#convertParameters()
        return format.replaceAll("([%_!])", "!$1");
    }
}
