/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.jcr.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Miscellaneous utility functions
 */
public final class Util {
    public static final String CMIS_QUERY_PATTERN_STR ="[sS][eE][lL][eE][cC][tT]\\s+(.*)\\s+[fF][rR][oO][mM].*";
    private static final Pattern CMIS_QUERY_PATTERN = Pattern.compile(CMIS_QUERY_PATTERN_STR);
    private static final String CMIS_QUERY_REQUIRED_COLUMNS[] = {"cmis:objectId", "cmis:objectTypeId"};
    private static final String CMIS_QUERY_ALL_COLUMNS = "*";

    private Util() {}

    /**
     * Convert from <code>Calendar</code> to a <code>GregorianCalendar</code>.
     * 
     * @param date
     * @return  <code>date</code> if it is an instance of <code>GregorianCalendar</code>.
     *   Otherwise a new <code>GregorianCalendar</code> instance for <code>date</code>.
     */
    public static GregorianCalendar toCalendar(Calendar date) {
        if (date instanceof GregorianCalendar) {
            return (GregorianCalendar) date;
        }
        else {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeZone(date.getTimeZone());
            calendar.setTimeInMillis(date.getTimeInMillis());
            return calendar;
        }
    }

    /**
     * Replace every occurrence of <code>target</code> in <code>string</code> with
     * <code>replacement</code>.
     *
     * @param string  string to do replacement on
     * @param target  string to search for
     * @param replacement  string to replace with
     * @return  string with replacing done
     */
    public static String replace(String string, String target, String replacement) {
        if ("".equals(target)) {
            throw new IllegalArgumentException("target string must not be empty");
        }
        if ("".equals(replacement) || target.equals(replacement)) {
            return string;
        }

        StringBuilder result = new StringBuilder();
        int d = target.length();
        int k = 0;
        int j;
        do {
            j = string.indexOf(target, k);
            if (j < 0) {
                result.append(string.substring(k));
            }
            else {
                result.append(string.substring(k, j)).append(replacement);
            }

            k = j + d;
        } while (j >= 0);

        return result.toString();
    }

    /**
     * Escapes a JCR path such that it can be used in a XPath query
     * @param path
     * @return  escaped path
     */
    public static String escape(String path) {
        return replace(path, " ", "_x0020_"); // fixme do more thorough escaping of path
    }
    
    /**
     * The repository's support option definition.
     * @param node JCR node
     * @param option requested repository's option
     * @return true if repository supports option, otherwise false.
     * @throws RepositoryException thrown when any error with repository was occurred.
     */
    public static boolean supportOption(Node node, String option) throws RepositoryException {
    	return Boolean.valueOf(node.getSession().getRepository().getDescriptor(option));
    }

    /**
     * Parse statement and get set of columns
     * @param statement query statetment string
     * @return Set of columns
     */
    public static Set<String> getColumnsFromQueryStatement(final String statement) {
        Matcher queryMatcher = CMIS_QUERY_PATTERN.matcher(statement.trim());
        Set<String> res = new HashSet<String>(10);
        if (queryMatcher.matches()) {
            final String columnsStr = queryMatcher.group(1);

            if (!CMIS_QUERY_ALL_COLUMNS.equals(columnsStr)) {
                final String[] columns = columnsStr.split(",");
                for(String col : columns) {
                    String item = col.trim();
                    if (!CMIS_QUERY_ALL_COLUMNS.equals(item)){
                        res.add(item);
                    }
                }
                for(String item : CMIS_QUERY_REQUIRED_COLUMNS){
                    if (!res.contains(item)) {
                        res.add(item);
                    }
                }
            }
        }
        if (res.isEmpty()) {
            res.add(CMIS_QUERY_ALL_COLUMNS);
        }
        return res;
    }
}
