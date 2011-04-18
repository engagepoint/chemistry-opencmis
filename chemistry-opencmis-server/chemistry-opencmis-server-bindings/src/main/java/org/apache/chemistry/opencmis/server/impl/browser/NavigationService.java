/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.server.impl.browser;

import static org.apache.chemistry.opencmis.server.shared.HttpUtils.getBigIntegerParameter;
import static org.apache.chemistry.opencmis.server.shared.HttpUtils.getBooleanParameter;
import static org.apache.chemistry.opencmis.server.shared.HttpUtils.getEnumParameter;
import static org.apache.chemistry.opencmis.server.shared.HttpUtils.getStringParameter;

import java.math.BigInteger;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.Constants;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.impl.browser.json.JSONConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Navigation Service operations.
 */
public final class NavigationService {

    /**
     * getChildren.
     */
    public static void getChildren(CallContext context, CmisService service, String repositoryId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get parameters
        String folderId = (String) context.get(BrowserBindingUtils.CONTEXT_OBJECT_ID);
        String filter = getStringParameter(request, Constants.PARAM_FILTER);
        String orderBy = getStringParameter(request, Constants.PARAM_ORDER_BY);
        Boolean includeAllowableActions = getBooleanParameter(request, Constants.PARAM_ALLOWABLE_ACTIONS);
        IncludeRelationships includeRelationships = getEnumParameter(request, Constants.PARAM_RELATIONSHIPS,
                IncludeRelationships.class);
        String renditionFilter = getStringParameter(request, Constants.PARAM_RENDITION_FILTER);
        Boolean includePathSegment = getBooleanParameter(request, Constants.PARAM_PATH_SEGMENT);
        BigInteger maxItems = getBigIntegerParameter(request, Constants.PARAM_MAX_ITEMS);
        BigInteger skipCount = getBigIntegerParameter(request, Constants.PARAM_SKIP_COUNT);

        // execute
        ObjectInFolderList children = service.getChildren(repositoryId, folderId, filter, orderBy,
                includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, maxItems,
                skipCount, null);

        if (children == null) {
            throw new CmisRuntimeException("Children are null!");
        }

        TypeCache typeCache = new TypeCache(repositoryId, service);
        JSONObject jsonChildren = JSONConverter.convert(children, typeCache);

        response.setStatus(HttpServletResponse.SC_OK);
        BrowserBindingUtils.writeJSON(jsonChildren, request, response);
    }

    /**
     * getDescendants.
     */
    @SuppressWarnings("unchecked")
    public static void getDescendants(CallContext context, CmisService service, String repositoryId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get parameters
        String folderId = (String) context.get(BrowserBindingUtils.CONTEXT_OBJECT_ID);
        BigInteger depth = getBigIntegerParameter(request, Constants.PARAM_DEPTH);
        String filter = getStringParameter(request, Constants.PARAM_FILTER);
        Boolean includeAllowableActions = getBooleanParameter(request, Constants.PARAM_ALLOWABLE_ACTIONS);
        IncludeRelationships includeRelationships = getEnumParameter(request, Constants.PARAM_RELATIONSHIPS,
                IncludeRelationships.class);
        String renditionFilter = getStringParameter(request, Constants.PARAM_RENDITION_FILTER);
        Boolean includePathSegment = getBooleanParameter(request, Constants.PARAM_PATH_SEGMENT);

        // execute
        List<ObjectInFolderContainer> descendants = service.getDescendants(repositoryId, folderId, depth, filter,
                includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, null);

        if (descendants == null) {
            throw new CmisRuntimeException("Descendants are null!");
        }

        TypeCache typeCache = new TypeCache(repositoryId, service);
        JSONArray jsonDescendants = new JSONArray();
        for (ObjectInFolderContainer descendant : descendants) {
            jsonDescendants.add(JSONConverter.convert(descendant, typeCache));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        BrowserBindingUtils.writeJSON(jsonDescendants, request, response);
    }

    /**
     * getFolderTree.
     */
    @SuppressWarnings("unchecked")
    public static void getFolderTree(CallContext context, CmisService service, String repositoryId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get parameters
        String folderId = (String) context.get(BrowserBindingUtils.CONTEXT_OBJECT_ID);
        BigInteger depth = getBigIntegerParameter(request, Constants.PARAM_DEPTH);
        String filter = getStringParameter(request, Constants.PARAM_FILTER);
        Boolean includeAllowableActions = getBooleanParameter(request, Constants.PARAM_ALLOWABLE_ACTIONS);
        IncludeRelationships includeRelationships = getEnumParameter(request, Constants.PARAM_RELATIONSHIPS,
                IncludeRelationships.class);
        String renditionFilter = getStringParameter(request, Constants.PARAM_RENDITION_FILTER);
        Boolean includePathSegment = getBooleanParameter(request, Constants.PARAM_PATH_SEGMENT);

        // execute
        List<ObjectInFolderContainer> folderTree = service.getFolderTree(repositoryId, folderId, depth, filter,
                includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, null);

        if (folderTree == null) {
            throw new CmisRuntimeException("Folder Tree are null!");
        }

        TypeCache typeCache = new TypeCache(repositoryId, service);
        JSONArray jsonDescendants = new JSONArray();
        for (ObjectInFolderContainer descendant : folderTree) {
            jsonDescendants.add(JSONConverter.convert(descendant, typeCache));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        BrowserBindingUtils.writeJSON(jsonDescendants, request, response);
    }

    /**
     * getObjectParents.
     */
    @SuppressWarnings("unchecked")
    public static void getObjectParents(CallContext context, CmisService service, String repositoryId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get parameters
        String objectId = (String) context.get(BrowserBindingUtils.CONTEXT_OBJECT_ID);
        String filter = getStringParameter(request, Constants.PARAM_FILTER);
        Boolean includeAllowableActions = getBooleanParameter(request, Constants.PARAM_ALLOWABLE_ACTIONS);
        IncludeRelationships includeRelationships = getEnumParameter(request, Constants.PARAM_RELATIONSHIPS,
                IncludeRelationships.class);
        String renditionFilter = getStringParameter(request, Constants.PARAM_RENDITION_FILTER);
        Boolean includeRelativePathSegment = getBooleanParameter(request, Constants.PARAM_RELATIVE_PATH_SEGMENT);

        // execute
        List<ObjectParentData> parents = service.getObjectParents(repositoryId, objectId, filter,
                includeAllowableActions, includeRelationships, renditionFilter, includeRelativePathSegment, null);

        if (parents == null) {
            throw new CmisRuntimeException("Parents are null!");
        }

        TypeCache typeCache = new TypeCache(repositoryId, service);
        JSONArray jsonParents = new JSONArray();
        for (ObjectParentData parent : parents) {
            jsonParents.add(JSONConverter.convert(parent, typeCache));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        BrowserBindingUtils.writeJSON(jsonParents, request, response);
    }
}