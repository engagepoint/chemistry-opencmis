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

package org.apache.chemistry.opencmis.jcr.query;

import org.antlr.runtime.tree.Tree;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.server.support.query.CmisQueryWalker;
import org.apache.chemistry.opencmis.server.support.query.QueryObject;
import org.apache.chemistry.opencmis.server.support.query.QueryObject.SortSpec;
import org.apache.chemistry.opencmis.server.support.query.QueryUtil;
import org.apache.jackrabbit.util.ISO9075;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for translating a CMIS query statement to a JCR XPath
 * query statement.
 * Overriding class need to implement methods for mapping CMIS ids to JCR paths,
 * CMIS property names to JCR property names, CMIS type names to JCR type name and
 * in addition a method for adding further constraints to a query based on a CMIS
 * type.
 */
public abstract class QueryTranslator {
    private final JcrTypeManager typeManager;
    private final EvaluatorXPath evaluator;
    private QueryObject queryObject;
    private static final String ABSOLUTE_PATH = "//";
    private static final String IN_TREE_SQL_CLAUSE = "IN_TREE";
    private static final String OR_SQL_CLAUSE = "OR";
    private static final String AND_SQL_CLAUSE = "AND";
    private static final Pattern IN_TREE_PATTERN = Pattern.compile("\\s?(OR|AND|or|and)?\\s?IN_TREE\\('\\S+'\\)\\s?(OR|AND|or|and)?");

    /**
     * Create a new query translator which uses the provided <code>typeManager</code>
     * to resolve CMIS type names to CMIS types.
     *
     * @param typeManager
     */
    protected QueryTranslator(JcrTypeManager typeManager) {
        this.typeManager = typeManager;
        evaluator = new EvaluatorXPath() {

            @Override
            protected String jcrPathFromId(String id) {
                return QueryTranslator.this.jcrPathFromId(id);
            }

            @Override
            protected String jcrPathFromCol(String name) {
                return QueryTranslator.this.jcrPathFromCol(queryObject.getMainFromName(), name);
            }
        };
    }

    /**
     * @return the {@link QueryObject} from the last translation performed through
     * {@link QueryTranslator#translateToXPath(String)}.
     */
    public QueryObject getQueryObject() {
        return queryObject;
    }

    private ParseState getParseResult(String statement) {
        QueryUtil queryUtil = new QueryUtil();
        queryObject = new QueryObject(typeManager);
        ParseTreeWalker<XPathBuilder> parseTreeWalker = new ParseTreeWalker<XPathBuilder>(evaluator);
        CmisQueryWalker walker = queryUtil.traverseStatementAndCatchExc(statement, queryObject, parseTreeWalker);
        walker.setDoFullTextParse(false);
        XPathBuilder parseResult = parseTreeWalker.getResult();
        return new ParseState(walker, parseResult);
    }

    private class ParseState {
        private CmisQueryWalker walker;
        private XPathBuilder parseResult;

        public ParseState(final CmisQueryWalker walker, final XPathBuilder parseResult) {
            this.walker = walker;
            this.parseResult = parseResult;
        }

        public CmisQueryWalker getWalker() {
            return walker;
        }

        public XPathBuilder getParseResult() {
            return parseResult;
        }
    }

    /**
     * Translate a CMIS query statement to a JCR XPath query statement.
     *
     * @param statement
     * @return
     */
    public String translateToXPath(String statement) {
        ParseState parseState = getParseResult(statement);
        TypeDefinition fromType = getFromName(queryObject);
        String elementTest = buildElementTest(fromType);
        String predicates = buildPredicates(fromType, getCondition(parseState.getParseResult()));
        String orderByClause = buildOrderByClause(fromType, queryObject.getOrderBys());

        Tree tree = parseState.getWalker().getWherePredicateTree();
        if (tree != null) {
            Tree parent = getRootTree(tree);

            boolean hasInTree = getTree(parent, IN_TREE_SQL_CLAUSE) != null;
            boolean hasInFolder = getTree(parent, "IN_FOLDER") != null;
            boolean hasChildrenExceptInTreeWithOr = hasChildrenExcept(parent, IN_TREE_SQL_CLAUSE, OR_SQL_CLAUSE);
            boolean hasChildrenExceptInTreeWithAnd = hasChildrenExcept(parent, IN_TREE_SQL_CLAUSE, AND_SQL_CLAUSE);
            boolean containsOnlyOr = getTree(parent, OR_SQL_CLAUSE) != null && getTree(parent, AND_SQL_CLAUSE) == null;
            boolean containsOnlyAnd = getTree(parent, AND_SQL_CLAUSE) != null && getTree(parent, OR_SQL_CLAUSE) == null;
            final String inTreeXPathSeparator = containsOnlyOr ? " or " : (containsOnlyAnd ? " and " : null);

            if (hasInTree && inTreeXPathSeparator != null && !hasInFolder) {
                return buildXPath(
                        inTreeXPathSeparator,
                        hasChildrenExceptInTreeWithOr || hasChildrenExceptInTreeWithAnd,
                        "jcr:like(@jcr:path, '%s%%')",
                        IN_TREE_PATTERN,
                        elementTest,
                        predicates,
                        statement,
                        fromType,
                        orderByClause,
                        parseState);
            }
        }
        String pathExpression = buildPathExpression(fromType, getFolderPredicate(parseState.getParseResult()));
        String pathExpressionEncoded = ISO9075.encodePath(pathExpression);
        return "/jcr:root" + pathExpressionEncoded + elementTest + predicates + orderByClause;
    }

    private boolean hasChildrenExcept(final Tree parent, final String except, final String predicateClause) {
        Tree orTree = getTree(parent, predicateClause);
        int count = 0;
        if (orTree != null) {
            for (int i = 0; i < orTree.getChildCount(); i++) {
                String childTokenName = orTree.getChild(i).getText();
                if (!except.equalsIgnoreCase(childTokenName)) {
                    count++;
                }
            }
        }
        return count > 0;
    }

    private String buildXPath(final String predicateSeparator,
                              final boolean containsOtherClauses,
                              final String xPathPattern,
                              final Pattern removePattern,
                              final String elementTest,
                              final String predicates,
                              final String statement,
                              final TypeDefinition fromType,
                              final String orderByClause,
                              final ParseState parseState) {
        StringBuilder xPathBuilder = new StringBuilder();
        xPathBuilder.append(ABSOLUTE_PATH);
        xPathBuilder.append(elementTest);
        xPathBuilder.append("[");
        Iterator<XPathBuilder> folderPredicatesIterator = parseState.getParseResult().folderPredicates().iterator();
        while (folderPredicatesIterator.hasNext()) {
            XPathBuilder p = folderPredicatesIterator.next();
            String xPathPredicate = p.xPath();
            String folderPredicate =
                    String.format(xPathPattern, xPathPredicate.endsWith(ABSOLUTE_PATH) ?
                            xPathPredicate.substring(0, xPathPredicate.length() - 1) : xPathPredicate);
            xPathBuilder.append(folderPredicate);
            if (folderPredicatesIterator.hasNext()) {
                xPathBuilder.append(predicateSeparator);
            }
        }

        if ((predicates == null || predicates.isEmpty())) {
            if (containsOtherClauses) {
                String newStatement = removePatternFromStatement(statement, removePattern);
                ParseState parseState2 = getParseResult(newStatement);
                String predicates2 = buildPredicates(fromType, getCondition(parseState2.getParseResult()));
                if (predicates2 != null && !predicates2.isEmpty()) {
                    xPathBuilder.append(predicateSeparator).append(predicates2.substring(1, predicates2.length() - 1));
                }
            }
        } else {
            xPathBuilder.append(predicateSeparator).append(predicates.substring(1, predicates.length() - 1));
        }

        xPathBuilder.append("]").append(orderByClause);
        return xPathBuilder.toString();
    }

    private Tree getTree(final Tree parent, final String sqlClause) {
        if (parent.getText().equalsIgnoreCase(sqlClause)) {
            return parent;
        } else {
            Tree tree = null;
            for (int i = 0; i < parent.getChildCount(); i++) {
                Tree child = parent.getChild(i);
                tree = getTree(child, sqlClause);
                if (tree != null) {
                    break;
                }
            }
            return tree;
        }
    }

    private String removePatternFromStatement(final String statement, final Pattern pattern) {
        Matcher matcher = pattern.matcher(statement);
        String newStatement = statement;
        while (matcher.find()) {
            for (int i = 0; i < matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null) {
                    newStatement = newStatement.replace(group, "");
                }
            }
        }
        Pattern wherePattern = Pattern.compile("\\s?WHERE|where\\s?");
        Matcher whereMatcher = wherePattern.matcher(newStatement.trim());
        if (whereMatcher.find()) {
            if (whereMatcher.end() == newStatement.length()) {
                newStatement = newStatement.replace(whereMatcher.group(), "");
            }
        }
        return newStatement;
    }


    private Tree getRootTree(Tree tree) {
        return tree == null || tree.getParent() == null ? tree : getRootTree(tree.getParent());
    }

    //------------------------------------------< protected >---

    /**
     * Map a CMIS objectId to an absolute JCR path. This method is called to
     * resolve the folder if of folder predicates (i.e. IN_FOLDER, IN_TREE).
     *
     * @param id objectId
     * @return absolute JCR path corresponding to <code>id</code>.
     */
    protected abstract String jcrPathFromId(String id);

    /**
     * Map a column name in the CMIS query to the corresponding relative JCR path.
     * The path must be relative to the context node.
     *
     * @param fromType Type on which the CMIS query is performed
     * @param name     column name
     * @return relative JCR path
     */
    protected abstract String jcrPathFromCol(TypeDefinition fromType, String name);

    /**
     * Map a CMIS type to the corresponding JCR type name.
     *
     * @param fromType CMIS type
     * @return name of the JCR type corresponding to <code>fromType</code>
     * @see #jcrTypeCondition(TypeDefinition)
     */
    protected abstract String jcrTypeName(TypeDefinition fromType);

    /**
     * Create and additional condition in order for the query to only return nodes
     * of the right type. This condition and-ed to the condition determined by the
     * CMIS query's where clause.
     * <p/>
     * A CMIS query for non versionable documents should for example result in the
     * following XPath query:
     * <p/>
     * <pre>
     *   element(*, nt:file)[not(@jcr:mixinTypes = 'mix:simpleVersionable')]
     * </pre>
     * Here the element test is covered by {@link #jcrTypeName(TypeDefinition)}
     * while the predicate is covered by this method.
     *
     * @param fromType
     * @return Additional condition or <code>null</code> if none.
     * @see #jcrTypeName(TypeDefinition)
     */
    protected abstract String jcrTypeCondition(TypeDefinition fromType);

    /**
     * Build a XPath path expression for the CMIS type queried for and a folder predicate.
     *
     * @param fromType        CMIS type queried for
     * @param folderPredicate folder predicate
     * @return a valid XPath path expression or <code>null</code> if none.
     */
    protected String buildPathExpression(TypeDefinition fromType, String folderPredicate) {
        return folderPredicate == null ? "//" : folderPredicate;
    }

    /**
     * Build a XPath element test for the given CMIS type.
     *
     * @param fromType CMIS type queried for
     * @return a valid XPath element test.
     */
    protected String buildElementTest(TypeDefinition fromType) {
        return "element(*," + jcrTypeName(fromType) + ")";
    }

    /**
     * Build a XPath predicate for the given CMIS type and an additional condition.
     * The additional condition should be and-ed to the condition resulting from
     * evaluating <code>fromType</code>.
     *
     * @param fromType  CMIS type queried for
     * @param condition additional condition.
     * @return a valid XPath predicate or <code>null</code> if none.
     */
    protected String buildPredicates(TypeDefinition fromType, String condition) {
        String typeCondition = jcrTypeCondition(fromType);

        if (typeCondition == null) {
            return condition == null ? "" : "[" + condition + "]";
        } else if (condition == null) {
            return "[" + typeCondition + "]";
        } else {
            return "[" + typeCondition + " and " + condition + "]";
        }
    }

    /**
     * Build a XPath order by clause for the given CMIS type and a list of {@link SortSpec}s.
     *
     * @param fromType CMIS type queried for
     * @param orderBys <code>SortSpec</code>s
     * @return a valid XPath order by clause
     */
    protected String buildOrderByClause(TypeDefinition fromType, List<SortSpec> orderBys) {
        StringBuilder orderSpecs = new StringBuilder();

        for (SortSpec orderBy : orderBys) {
            String selector = jcrPathFromCol(fromType, orderBy.getSelector().getName());
            boolean ascending = orderBy.isAscending();

            if (orderSpecs.length() > 0) {
                orderSpecs.append(',');
            }

            orderSpecs
                    .append(selector)
                    .append(' ')
                    .append(ascending ? "ascending" : "descending");
        }

        return orderSpecs.length() > 0
                ? "order by " + orderSpecs
                : "";
    }

    //------------------------------------------< private >---

    private static String getFolderPredicate(XPathBuilder parseResult) {
        if (parseResult == null) {
            return null;
        }

        String folderPredicate = null;
        for (XPathBuilder p : parseResult.folderPredicates()) {
            if (folderPredicate == null) {
                folderPredicate = p.xPath();
            } else {
                throw new CmisInvalidArgumentException("Query may only contain a single folder predicate");
            }
        }

        // See the class comment on XPathBuilder for details on affirmative literals
        if (folderPredicate != null &&                    // IF has single folder predicate
                !Boolean.FALSE.equals(parseResult.eval(false))) {      // AND folder predicate is not affirmative
            throw new CmisInvalidArgumentException("Folder predicate " + folderPredicate + " is not affirmative.");
        }

        return folderPredicate;
    }

    private static TypeDefinition getFromName(QueryObject queryObject) {
        if (queryObject.getTypes().size() != 1) {
            throw new CmisInvalidArgumentException("From must contain one single reference");
        }
        return queryObject.getMainFromName();
    }

    private static String getCondition(XPathBuilder parseResult) {
        // No condition if either parseResult is null or when it evaluates to true under
        // the valuation which assigns true to the folder predicate.
        return parseResult == null || Boolean.TRUE.equals(parseResult.eval(true)) ? null : parseResult.xPath();
    }

}
