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
package com.xpn.xwiki.plugin.query;

import org.apache.jackrabbit.core.query.QueryConstants;

public class XWikiQueryConstants implements QueryConstants {
	public static boolean isGeneralComparisonType(int op) {
		return op == OPERATION_EQ_GENERAL
		|| op == OPERATION_NE_GENERAL
		|| op == OPERATION_LT_GENERAL
		|| op == OPERATION_GT_GENERAL
		|| op == OPERATION_GE_GENERAL
		|| op == OPERATION_LE_GENERAL;
	}
	public static boolean isValueComparisonType(int op) {
		return op == OPERATION_EQ_VALUE
		|| op == OPERATION_NE_VALUE
		|| op == OPERATION_LT_VALUE
		|| op == OPERATION_GT_VALUE
		|| op == OPERATION_GE_VALUE
		|| op == OPERATION_LE_VALUE;
	}
	public static String getHqlOperation(int op) {
		switch (op) {
		case OPERATION_EQ_VALUE:	return "=";
		case OPERATION_EQ_GENERAL:	return "=";
		case OPERATION_NE_VALUE:	return "<>";
		case OPERATION_NE_GENERAL:	return "<>";
		case OPERATION_LT_VALUE:	return "<";
		case OPERATION_LT_GENERAL:	return "<";
		case OPERATION_GT_VALUE:	return ">";
		case OPERATION_GT_GENERAL:	return ">";
		case OPERATION_GE_VALUE:	return ">=";
		case OPERATION_GE_GENERAL:	return ">=";
		case OPERATION_LE_VALUE:	return "<=";
		case OPERATION_LE_GENERAL:	return "<=";
		case OPERATION_LIKE:		return " like ";
		case OPERATION_BETWEEN:		return " between ";	// not 1.0 standart
		case OPERATION_IN:			return " in ";		// not 1.0 standart
		case OPERATION_NULL:		return " is null ";
		case OPERATION_NOT_NULL:	return " is not null ";
		default: return null;
		}
	}
}
