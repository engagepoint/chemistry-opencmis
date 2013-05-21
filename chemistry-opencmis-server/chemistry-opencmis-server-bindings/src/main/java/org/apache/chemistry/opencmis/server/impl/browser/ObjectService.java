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

import static org.apache.chemistry.opencmis.commons.impl.Constants.MEDIATYPE_OCTETSTREAM;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_ACL;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_ALLOWABLE_ACTIONS;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_DOWNLOAD;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_FILTER;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_POLICY_IDS;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_RELATIONSHIPS;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_RENDITION_FILTER;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_RETURN_VERSION;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_SOURCE_FOLDER_ID;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_SOURCE_ID;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_STREAM_ID;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_TARGET_FOLDER_ID;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_TOKEN;
import static org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_VERSIONIG_STATE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.Constants;
import org.apache.chemistry.opencmis.commons.impl.JSONConverter;
import org.apache.chemistry.opencmis.commons.impl.MimeHelper;
import org.apache.chemistry.opencmis.commons.impl.ReturnVersion;
import org.apache.chemistry.opencmis.commons.impl.TypeCache;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BulkUpdateObjectIdAndChangeTokenImpl;
import org.apache.chemistry.opencmis.commons.impl.json.JSONArray;
import org.apache.chemistry.opencmis.commons.impl.json.JSONObject;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.spi.Holder;

/**
 * Object Service operations.
 */
public class ObjectService {

    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * Create document.
     */
    public static class CreateDocument extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String folderId = ((BrowserCallContextImpl) context).getObjectId();
            VersioningState versioningState = getEnumParameter(request, PARAM_VERSIONIG_STATE, VersioningState.class);
            String token = getStringParameter(request, PARAM_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            ControlParser cp = new ControlParser(request);
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);

            ContentStream contentStream = createContentStream(request);

            String newObjectId = null;
            try {
                newObjectId = service
                        .createDocument(repositoryId, createNewProperties(cp, typeCache), folderId, contentStream,
                                versioningState, createPolicies(cp), createAddAcl(cp), createRemoveAcl(cp), null);
            } finally {
                closeContentStream(contentStream);
            }

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("New document is null!");
            }

            // return object
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            setStatus(request, response, HttpServletResponse.SC_CREATED);
            setCookie(request, response, repositoryId, token,
                    createCookieValue(HttpServletResponse.SC_CREATED, object.getId(), null, null));

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * Create document from source.
     */
    public static class CreateDocumentFromSource extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String folderId = ((BrowserCallContextImpl) context).getObjectId();
            String sourceId = getStringParameter(request, PARAM_SOURCE_ID);
            VersioningState versioningState = getEnumParameter(request, PARAM_VERSIONIG_STATE, VersioningState.class);
            String token = getStringParameter(request, PARAM_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            ControlParser cp = new ControlParser(request);
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);

            ObjectData sourceDoc = getSimpleObject(service, repositoryId, sourceId);
            PropertyData<?> sourceTypeId = sourceDoc.getProperties().getProperties().get(PropertyIds.OBJECT_TYPE_ID);
            if (sourceTypeId == null || sourceTypeId.getFirstValue() == null) {
                throw new CmisRuntimeException("Source object has no type!?!");
            }

            String newObjectId = service.createDocumentFromSource(
                    repositoryId,
                    sourceId,
                    createUpdateProperties(cp, sourceTypeId.getFirstValue().toString(), null,
                            Collections.singletonList(sourceId), typeCache), folderId, versioningState,
                    createPolicies(cp), createAddAcl(cp), createRemoveAcl(cp), null);

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("New document is null!");
            }

            // return object
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            setStatus(request, response, HttpServletResponse.SC_CREATED);
            setCookie(request, response, repositoryId, token,
                    createCookieValue(HttpServletResponse.SC_CREATED, object.getId(), null, null));

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * Create folder.
     */
    public static class CreateFolder extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String folderId = ((BrowserCallContextImpl) context).getObjectId();
            String token = getStringParameter(request, PARAM_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            ControlParser cp = new ControlParser(request);
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);

            String newObjectId = service.createFolder(repositoryId, createNewProperties(cp, typeCache), folderId,
                    createPolicies(cp), createAddAcl(cp), createRemoveAcl(cp), null);

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("New folder is null!");
            }

            // return object
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            setStatus(request, response, HttpServletResponse.SC_CREATED);
            setCookie(request, response, repositoryId, token,
                    createCookieValue(HttpServletResponse.SC_CREATED, object.getId(), null, null));

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * Create policy.
     */
    public static class CreatePolicy extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String folderId = ((BrowserCallContextImpl) context).getObjectId();
            String token = getStringParameter(request, PARAM_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            ControlParser cp = new ControlParser(request);
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);

            String newObjectId = service.createPolicy(repositoryId, createNewProperties(cp, typeCache), folderId,
                    createPolicies(cp), createAddAcl(cp), createRemoveAcl(cp), null);

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("New policy is null!");
            }

            // return object
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            setStatus(request, response, HttpServletResponse.SC_CREATED);
            setCookie(request, response, repositoryId, token,
                    createCookieValue(HttpServletResponse.SC_CREATED, object.getId(), null, null));

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * Create Item.
     */
    public static class CreateItem extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String folderId = ((BrowserCallContextImpl) context).getObjectId();
            String token = getStringParameter(request, PARAM_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            ControlParser cp = new ControlParser(request);
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);

            String newObjectId = service.createItem(repositoryId, createNewProperties(cp, typeCache), folderId,
                    createPolicies(cp), createAddAcl(cp), createRemoveAcl(cp), null);

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("New item is null!");
            }

            // return object
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            setStatus(request, response, HttpServletResponse.SC_CREATED);
            setCookie(request, response, repositoryId, token,
                    createCookieValue(HttpServletResponse.SC_CREATED, object.getId(), null, null));

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * Create relationship.
     */
    public static class CreateRelationship extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String token = getStringParameter(request, PARAM_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            ControlParser cp = new ControlParser(request);
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);

            String newObjectId = service.createRelationship(repositoryId, createNewProperties(cp, typeCache),
                    createPolicies(cp), createAddAcl(cp), createRemoveAcl(cp), null);

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("New relationship is null!");
            }

            // return object
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            setStatus(request, response, HttpServletResponse.SC_CREATED);
            setCookie(request, response, repositoryId, token,
                    createCookieValue(HttpServletResponse.SC_CREATED, object.getId(), null, null));

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * updateProperties.
     */
    public static class UpdateProperties extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            String typeId = ((BrowserCallContextImpl) context).getTypeId();
            String changeToken = getStringParameter(request, Constants.CONTROL_CHANGE_TOKEN);
            String token = getStringParameter(request, PARAM_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            ControlParser cp = new ControlParser(request);
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);
            Holder<String> objectIdHolder = new Holder<String>(objectId);
            Holder<String> changeTokenHolder = (changeToken == null ? null : new Holder<String>(changeToken));

            service.updateProperties(repositoryId, objectIdHolder, changeTokenHolder,
                    createUpdateProperties(cp, typeId, null, Collections.singletonList(objectId), typeCache), null);

            String newObjectId = (objectIdHolder.getValue() == null ? objectId : objectIdHolder.getValue());

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("Object is null!");
            }

            // return object
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            int status = HttpServletResponse.SC_OK;
            if (!objectId.equals(newObjectId)) {
                status = HttpServletResponse.SC_CREATED;
            }

            setStatus(request, response, status);
            setCookie(request, response, repositoryId, token, createCookieValue(status, object.getId(), null, null));

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * bulkUpdateProperties.
     */
    public static class BulkUpdateProperties extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            ControlParser cp = new ControlParser(request);

            // get object ids and change tokens
            List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken = new ArrayList<BulkUpdateObjectIdAndChangeToken>();

            List<String> objectIds = cp.getValues(Constants.CONTROL_OBJECT_ID);
            List<String> changeTokens = cp.getValues(Constants.CONTROL_CHANGE_TOKEN);

            if (objectIds == null || objectIds.size() == 0) {
                throw new CmisInvalidArgumentException("No object ids provided!");
            }

            int n = objectIds.size();
            for (int i = 0; i < n; i++) {
                String id = objectIds.get(i);
                String changeToken = (changeTokens != null && changeTokens.size() > i ? changeTokens.get(i) : null);
                objectIdAndChangeToken.add(new BulkUpdateObjectIdAndChangeTokenImpl(id, changeToken));
            }

            // get secondary type ids
            List<String> addSecondaryTypes = cp.getValues(Constants.CONTROL_ADD_SECONDARY_TYPE);
            List<String> removeSecondaryTypes = cp.getValues(Constants.CONTROL_REMOVE_SECONDARY_TYPE);

            // compile properties
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);

            Properties properties = createUpdateProperties(cp, null, addSecondaryTypes, objectIds, typeCache);

            // execute
            List<BulkUpdateObjectIdAndChangeToken> result = service.bulkUpdateProperties(repositoryId,
                    objectIdAndChangeToken, properties, addSecondaryTypes, removeSecondaryTypes, null);

            // return result
            JSONArray jsonList = new JSONArray();
            if (result != null) {
                for (BulkUpdateObjectIdAndChangeToken oc : result) {
                    if (oc != null) {
                        jsonList.add(JSONConverter.convert(oc));
                    }
                }
            }

            response.setStatus(HttpServletResponse.SC_OK);
            writeJSON(jsonList, request, response);
        }
    }

    /**
     * getProperties.
     */
    public static class GetProperties extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            ReturnVersion returnVersion = getEnumParameter(request, PARAM_RETURN_VERSION, ReturnVersion.class);
            String filter = getStringParameter(request, PARAM_FILTER);
            boolean succinct = getBooleanParameter(request, Constants.PARAM_SUCCINCT, false);

            // execute
            Properties properties;

            if (returnVersion == ReturnVersion.LATEST || returnVersion == ReturnVersion.LASTESTMAJOR) {
                properties = service.getPropertiesOfLatestVersion(repositoryId, objectId, null,
                        returnVersion == ReturnVersion.LASTESTMAJOR, filter, null);
            } else {
                properties = service.getProperties(repositoryId, objectId, filter, null);
            }

            if (properties == null) {
                throw new CmisRuntimeException("Properties are null!");
            }

            // return object
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);
            JSONObject jsonObject = JSONConverter.convert(properties, objectId, typeCache,
                    JSONConverter.PropertyMode.OBJECT, succinct);

            response.setStatus(HttpServletResponse.SC_OK);
            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * getObject.
     */
    public static class GetObject extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            ReturnVersion returnVersion = getEnumParameter(request, PARAM_RETURN_VERSION, ReturnVersion.class);
            String filter = getStringParameter(request, PARAM_FILTER);
            Boolean includeAllowableActions = getBooleanParameter(request, PARAM_ALLOWABLE_ACTIONS);
            IncludeRelationships includeRelationships = getEnumParameter(request, PARAM_RELATIONSHIPS,
                    IncludeRelationships.class);
            String renditionFilter = getStringParameter(request, PARAM_RENDITION_FILTER);
            Boolean includePolicyIds = getBooleanParameter(request, PARAM_POLICY_IDS);
            Boolean includeAcl = getBooleanParameter(request, PARAM_ACL);
            boolean succinct = getBooleanParameter(request, Constants.PARAM_SUCCINCT, false);

            // execute
            ObjectData object;

            if (returnVersion == ReturnVersion.LATEST || returnVersion == ReturnVersion.LASTESTMAJOR) {
                object = service.getObjectOfLatestVersion(repositoryId, objectId, null,
                        returnVersion == ReturnVersion.LASTESTMAJOR, filter, includeAllowableActions,
                        includeRelationships, renditionFilter, includePolicyIds, includeAcl, null);
            } else {
                object = service.getObject(repositoryId, objectId, filter, includeAllowableActions,
                        includeRelationships, renditionFilter, includePolicyIds, includeAcl, null);
            }

            if (object == null) {
                throw new CmisRuntimeException("Object is null!");
            }

            // return object
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            response.setStatus(HttpServletResponse.SC_OK);
            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * getAllowableActions.
     */
    public static class GetAllowableActions extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();

            AllowableActions allowableActions = service.getAllowableActions(repositoryId, objectId, null);

            JSONObject jsonAllowableActions = JSONConverter.convert(allowableActions);

            response.setStatus(HttpServletResponse.SC_OK);
            writeJSON(jsonAllowableActions, request, response);
        }
    }

    /**
     * getRenditions.
     */
    public static class GetRenditions extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            String renditionFilter = getStringParameter(request, PARAM_RENDITION_FILTER);
            BigInteger maxItems = getBigIntegerParameter(request, Constants.PARAM_MAX_ITEMS);
            BigInteger skipCount = getBigIntegerParameter(request, Constants.PARAM_SKIP_COUNT);

            // execute
            List<RenditionData> renditions = service.getRenditions(repositoryId, objectId, renditionFilter, maxItems,
                    skipCount, null);

            JSONArray jsonRenditions = new JSONArray();
            if (renditions != null) {
                for (RenditionData rendition : renditions) {
                    jsonRenditions.add(JSONConverter.convert(rendition));
                }
            }

            response.setStatus(HttpServletResponse.SC_OK);
            writeJSON(jsonRenditions, request, response);
        }
    }

    /**
     * getContentStream.
     */
    public static class GetContentStream extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            String streamId = getStringParameter(request, PARAM_STREAM_ID);
            boolean download = "attachment".equalsIgnoreCase(getStringParameter(request, PARAM_DOWNLOAD));

            BigInteger offset = context.getOffset();
            BigInteger length = context.getLength();

            // execute
            ContentStream content = service.getContentStream(repositoryId, objectId, streamId, offset, length, null);

            if (content == null || content.getStream() == null) {
                throw new CmisRuntimeException("Content stream is null!");
            }

            // set HTTP headers, if requested by the server implementation
            if (sendContentStreamHeaders(content, request, response)) {
                return;
            }

            String contentType = content.getMimeType();
            if (contentType == null) {
                contentType = MEDIATYPE_OCTETSTREAM;
            }

            // set headers
            if ((offset == null || offset.signum() == 0) && length == null) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                setStatus(request, response, HttpServletResponse.SC_PARTIAL_CONTENT);
            }
            response.setContentType(contentType);

            String contentFilename = content.getFileName();
            if (contentFilename == null) {
                contentFilename = "content";
            }

            if (download) {
                response.setHeader(MimeHelper.CONTENT_DISPOSITION,
                        MimeHelper.encodeContentDisposition(MimeHelper.DISPOSITION_ATTACHMENT, contentFilename));
            } else {
                response.setHeader(MimeHelper.CONTENT_DISPOSITION,
                        MimeHelper.encodeContentDisposition(MimeHelper.DISPOSITION_INLINE, contentFilename));
            }

            // send content
            InputStream in = new BufferedInputStream(content.getStream(), BUFFER_SIZE);
            OutputStream out = new BufferedOutputStream(response.getOutputStream());

            byte[] buffer = new byte[BUFFER_SIZE];
            int b;
            while ((b = in.read(buffer)) > -1) {
                out.write(buffer, 0, b);
            }

            in.close();
            out.flush();
        }
    }

    /**
     * deleteObject.
     */
    public static class DeleteObject extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            Boolean allVersions = getBooleanParameter(request, Constants.PARAM_ALL_VERSIONS);

            service.deleteObject(repositoryId, objectId, allVersions, null);

            response.setStatus(HttpServletResponse.SC_OK);
            writeEmpty(request, response);
        }
    }

    /**
     * deleteTree.
     */
    public static class DeleteTree extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            Boolean allVersions = getBooleanParameter(request, Constants.PARAM_ALL_VERSIONS);
            UnfileObject unfileObjects = getEnumParameter(request, Constants.PARAM_UNFILE_OBJECTS, UnfileObject.class);
            Boolean continueOnFailure = getBooleanParameter(request, Constants.PARAM_CONTINUE_ON_FAILURE);

            // execute
            FailedToDeleteData ftd = service.deleteTree(repositoryId, objectId, allVersions, unfileObjects,
                    continueOnFailure, null);

            response.setStatus(HttpServletResponse.SC_OK);

            if ((ftd != null) && (ftd.getIds() != null) && (ftd.getIds().size() > 0)) {
                JSONObject jsonObject = JSONConverter.convert(ftd);
                writeJSON(jsonObject, request, response);
                return;
            }

            writeEmpty(request, response);
        }
    }

    /**
     * Delete content stream.
     */
    public static class DeleteContentStream extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            String changeToken = getStringParameter(request, Constants.PARAM_CHANGE_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            Holder<String> objectIdHolder = new Holder<String>(objectId);
            Holder<String> changeTokenHolder = (changeToken == null ? null : new Holder<String>(changeToken));
            service.deleteContentStream(repositoryId, objectIdHolder, changeTokenHolder, null);

            String newObjectId = (objectIdHolder.getValue() == null ? objectId : objectIdHolder.getValue());

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("Object is null!");
            }

            response.setStatus(HttpServletResponse.SC_OK);

            // return object
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * setContentStream.
     */
    public static class SetContentStream extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            String changeToken = getStringParameter(request, Constants.PARAM_CHANGE_TOKEN);
            Boolean overwriteFlag = getBooleanParameter(request, Constants.PARAM_OVERWRITE_FLAG);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            Holder<String> objectIdHolder = new Holder<String>(objectId);
            Holder<String> changeTokenHolder = (changeToken == null ? null : new Holder<String>(changeToken));
            ContentStream contentStream = createContentStream(request);

            try {
                service.setContentStream(repositoryId, objectIdHolder, overwriteFlag, changeTokenHolder, contentStream,
                        null);
            } finally {
                closeContentStream(contentStream);
            }

            String newObjectId = (objectIdHolder.getValue() == null ? objectId : objectIdHolder.getValue());

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("Object is null!");
            }

            // set headers
            setStatus(request, response, HttpServletResponse.SC_CREATED);
            response.setHeader("Location", compileObjectLocationUrl(request, repositoryId, newObjectId));

            // return object
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * appendContentStream.
     */
    public static class AppendContentStream extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            boolean isLastChunk = getBooleanParameter(request, Constants.CONTROL_IS_LAST_CHUNK, false);
            String changeToken = getStringParameter(request, Constants.PARAM_CHANGE_TOKEN);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            Holder<String> objectIdHolder = new Holder<String>(objectId);
            Holder<String> changeTokenHolder = (changeToken == null ? null : new Holder<String>(changeToken));
            ContentStream contentStream = createContentStream(request);

            try {
                service.appendContentStream(repositoryId, objectIdHolder, changeTokenHolder, contentStream,
                        isLastChunk, null);
            } finally {
                closeContentStream(contentStream);
            }

            String newObjectId = (objectIdHolder.getValue() == null ? objectId : objectIdHolder.getValue());

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("Object is null!");
            }

            // set headers
            setStatus(request, response, HttpServletResponse.SC_CREATED);
            response.setHeader("Location", compileObjectLocationUrl(request, repositoryId, newObjectId));

            // return object
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            writeJSON(jsonObject, request, response);
        }
    }

    /**
     * moveObject.
     */
    public static class MoveObject extends AbstractBrowserServiceCall {
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            // get parameters
            String objectId = ((BrowserCallContextImpl) context).getObjectId();
            String targetFolderId = getStringParameter(request, PARAM_TARGET_FOLDER_ID);
            String sourceFolderId = getStringParameter(request, PARAM_SOURCE_FOLDER_ID);
            boolean succinct = getBooleanParameter(request, Constants.CONTROL_SUCCINCT, false);

            // execute
            Holder<String> objectIdHolder = new Holder<String>(objectId);
            service.moveObject(repositoryId, objectIdHolder, targetFolderId, sourceFolderId, null);

            String newObjectId = (objectIdHolder.getValue() == null ? objectId : objectIdHolder.getValue());

            ObjectData object = getSimpleObject(service, repositoryId, newObjectId);
            if (object == null) {
                throw new CmisRuntimeException("Object is null!");
            }

            // set headers
            setStatus(request, response, HttpServletResponse.SC_CREATED);
            response.setHeader("Location", compileObjectLocationUrl(request, repositoryId, newObjectId));

            // return object
            TypeCache typeCache = new ServerTypeCacheImpl(repositoryId, service);
            JSONObject jsonObject = JSONConverter.convert(object, typeCache, JSONConverter.PropertyMode.OBJECT,
                    succinct);

            writeJSON(jsonObject, request, response);
        }
    }
}
