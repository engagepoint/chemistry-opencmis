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
package org.apache.chemistry.opencmis.inmemory.server;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.RelationshipTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUpdateConflictException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BulkUpdateObjectIdAndChangeTokenImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.CmisExtensionElementImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.inmemory.FilterParser;
import org.apache.chemistry.opencmis.inmemory.NameValidator;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Content;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Document;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.DocumentVersion;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Fileable;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Filing;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Folder;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.ObjectStore;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.ObjectStoreFiling;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.ObjectStoreFiling.ChildrenResult;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.StoreManager;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.StoredObject;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.VersionedDocument;
import org.apache.chemistry.opencmis.inmemory.types.InMemoryDocumentTypeDefinition;
import org.apache.chemistry.opencmis.inmemory.types.InMemoryFolderTypeDefinition;
import org.apache.chemistry.opencmis.inmemory.types.InMemoryItemTypeDefinition;
import org.apache.chemistry.opencmis.inmemory.types.InMemoryPolicyTypeDefinition;
import org.apache.chemistry.opencmis.inmemory.types.InMemoryRelationshipTypeDefinition;
import org.apache.chemistry.opencmis.inmemory.types.PropertyCreationHelper;
import org.apache.chemistry.opencmis.server.support.TypeManager;
import org.apache.chemistry.opencmis.server.support.TypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryObjectServiceImpl extends InMemoryAbstractServiceImpl {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryServiceFactoryImpl.class.getName());

    public InMemoryObjectServiceImpl(StoreManager storeManager) {
        super(storeManager);
    }

    public String createDocument(CallContext context, String repositoryId, Properties properties, String folderId,
            ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {

        LOG.debug("start createDocument()");
        // Attach the CallContext to a thread local context that can be
        // accessed from everywhere

        StoredObject so = createDocumentIntern(context, repositoryId, properties, folderId, contentStream,
                versioningState, policies, addAces, removeAces, extension);
        LOG.debug("stop createDocument()");
        return so.getId();
    }

    public String createDocumentFromSource(CallContext context, String repositoryId, String sourceId,
            Properties properties, String folderId, VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {

        LOG.debug("start createDocumentFromSource()");
        StoredObject so = validator.createDocumentFromSource(context, repositoryId, sourceId, folderId, policies,
                extension);

        ContentStream content = getContentStream(context, repositoryId, sourceId, null, BigInteger.valueOf(-1),
                BigInteger.valueOf(-1), null);

        if (so == null) {
            throw new CmisObjectNotFoundException("Unknown object id: " + sourceId);
        }

        // build properties collection
        List<String> requestedIds = FilterParser.getRequestedIdsFromFilter("*");
        TypeManager tm = fStoreManager.getTypeManager(repositoryId);
        Properties existingProps = PropertyCreationHelper.getPropertiesFromObject(so, tm, requestedIds, true);

        PropertiesImpl newPD = new PropertiesImpl();
        // copy all existing properties
        for (PropertyData<?> prop : existingProps.getProperties().values()) {
            newPD.addProperty(prop);
        }

        if (null != properties)
            // overwrite all new properties
            for (PropertyData<?> prop : properties.getProperties().values()) {
                newPD.addProperty(prop);
            }

        String res = createDocument(context, repositoryId, newPD, folderId, content, versioningState, policies,
                addAces, removeAces, null);
        LOG.debug("stop createDocumentFromSource()");
        return res;
    }

    public String createFolder(CallContext context, String repositoryId, Properties properties, String folderId,
            List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        LOG.debug("start createFolder()");

        Folder folder = createFolderIntern(context, repositoryId, properties, folderId, policies, addAces, removeAces,
                extension);
        LOG.debug("stop createFolder()");
        return folder.getId();
    }

    public String createPolicy(CallContext context, String repositoryId, Properties properties, String folderId,
            List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {

        LOG.debug("start createPolicy()");
        StoredObject so = createPolicyIntern(context, repositoryId, properties, folderId, policies, addAces,
                removeAces, extension);
        LOG.debug("stop createPolicy()");
        return so == null ? null : so.getId();
    }

    public String createRelationship(CallContext context, String repositoryId, Properties properties,
            List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {

        LOG.debug("start createRelationship()");
        StoredObject so = createRelationshipIntern(context, repositoryId, properties, policies, addAces, removeAces,
                extension);
        LOG.debug("stop createRelationship()");
        return so == null ? null : so.getId();
    }

    // CMIS 1.1
    public String createItem(CallContext context, String repositoryId, Properties properties, String folderId,
            List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        StoredObject so = createItemIntern(context, repositoryId, properties, folderId, policies, addAces, removeAces,
                extension);
        return so.getId();
    }

    @SuppressWarnings("unchecked")
    public String create(CallContext context, String repositoryId, Properties properties, String folderId,
            ContentStream contentStream, VersioningState versioningState, List<String> policies,
            ExtensionsData extension, ObjectInfoHandler objectInfos) {

        if (null == properties || null == properties.getProperties()) {
            throw new CmisInvalidArgumentException("Cannot create object, without properties.");
        }

        // Find out what kind of object needs to be created
        PropertyData<String> pd = (PropertyData<String>) properties.getProperties().get(PropertyIds.OBJECT_TYPE_ID);
        String typeId = pd == null ? null : pd.getFirstValue();
        if (null == typeId) {
            throw new CmisInvalidArgumentException(
                    "Cannot create object, without a type (no property with id CMIS_OBJECT_TYPE_ID).");
        }

        TypeDefinitionContainer typeDefC = fStoreManager.getTypeById(repositoryId, typeId);
        if (typeDefC == null) {
            throw new CmisInvalidArgumentException("Cannot create object, a type with id " + typeId + " is unknown");
        }

        // check if the given type is a document type
        BaseTypeId typeBaseId = typeDefC.getTypeDefinition().getBaseTypeId();
        StoredObject so = null;
        if (typeBaseId.equals(InMemoryDocumentTypeDefinition.getRootDocumentType().getBaseTypeId())) {
            so = createDocumentIntern(context, repositoryId, properties, folderId, contentStream, versioningState,
                    null, null, null, null);
        } else if (typeBaseId.equals(InMemoryFolderTypeDefinition.getRootFolderType().getBaseTypeId())) {
            so = createFolderIntern(context, repositoryId, properties, folderId, null, null, null, null);
        } else if (typeBaseId.equals(InMemoryPolicyTypeDefinition.getRootPolicyType().getBaseTypeId())) {
            so = createPolicyIntern(context, repositoryId, properties, folderId, null, null, null, null);
        } else if (typeBaseId.equals(InMemoryRelationshipTypeDefinition.getRootRelationshipType().getBaseTypeId())) {
            so = createRelationshipIntern(context, repositoryId, properties, null, null, null, null);
        } else if (typeBaseId.equals(InMemoryItemTypeDefinition.getRootItemType().getBaseTypeId())) {
            so = createItemIntern(context, repositoryId, properties, folderId, null, null, null, null);
        } else {
            LOG.error("The type contains an unknown base object id, object can't be created");
        }

        // Make a call to getObject to convert the resulting id into an
        // ObjectData
        TypeManager tm = fStoreManager.getTypeManager(repositoryId);
        ObjectData od = PropertyCreationHelper.getObjectData(tm, so, null, context.getUsername(), false,
                IncludeRelationships.NONE, null, false, false, extension);

        if (context.isObjectInfoRequired()) {
            ObjectInfoImpl objectInfo = new ObjectInfoImpl();
            fAtomLinkProvider.fillInformationForAtomLinks(repositoryId, so, od, objectInfo);
            objectInfos.addObjectInfo(objectInfo);
        }
        return so != null ? so.getId() : null;
    }

    public void deleteContentStream(CallContext context, String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, ExtensionsData extension) {

        LOG.debug("start deleteContentStream()");
        StoredObject so = validator.deleteContentStream(context, repositoryId, objectId, extension);

        if (so == null) {
            throw new CmisObjectNotFoundException("Unknown object id: " + objectId);
        }

        if (so.getChangeToken() != null && (changeToken == null || !so.getChangeToken().equals(changeToken.getValue())))
            throw new CmisUpdateConflictException("deleteContentStream failed, ChangeToken does not match.");

        if (!(so instanceof Content)) {
            throw new CmisObjectNotFoundException("Id" + objectId
                    + " does not refer to a document, but only documents can have content");
        }

        ((Content) so).setContent(null, true);
        LOG.debug("stop deleteContentStream()");
    }

    public void deleteObject(CallContext context, String repositoryId, String objectId, Boolean allVersions,
            ExtensionsData extension) {

        LOG.debug("start deleteObject()");
        validator.deleteObject(context, repositoryId, objectId, allVersions, extension);
        ObjectStore objectStore = fStoreManager.getObjectStore(repositoryId);
        LOG.debug("delete object for id: " + objectId);

        // check if it is the root folder
        if (objectId.equals(objectStore.getRootFolder().getId())) {
            throw new CmisNotSupportedException("You can't delete a root folder");
        }

        objectStore.deleteObject(objectId, allVersions, context.getUsername());
        LOG.debug("stop deleteObject()");
    }

    public FailedToDeleteData deleteTree(CallContext context, String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure, ExtensionsData extension) {

        LOG.debug("start deleteTree()");
        StoredObject so = validator.deleteTree(context, repositoryId, folderId, allVersions, unfileObjects, extension);
        List<String> failedToDeleteIds = new ArrayList<String>();
        FailedToDeleteDataImpl result = new FailedToDeleteDataImpl();

        if (null == allVersions) {
            allVersions = true;
        }
        if (null == unfileObjects) {
            unfileObjects = UnfileObject.DELETE;
        }
        if (null == continueOnFailure) {
            continueOnFailure = false;
        }

        ObjectStore objectStore = fStoreManager.getObjectStore(repositoryId);

        if (null == so) {
            throw new CmisInvalidArgumentException("Cannot delete object with id  " + folderId
                    + ". Object does not exist.");
        }

        if (!(so instanceof Folder)) {
            throw new CmisInvalidArgumentException("deleteTree can only be invoked on a folder, but id " + folderId
                    + " does not refer to a folder");
        }

        if (unfileObjects == UnfileObject.UNFILE) {
            throw new CmisNotSupportedException("This repository does not support unfile operations.");
        }

        // check if it is the root folder
        if (folderId.equals(objectStore.getRootFolder().getId())) {
            throw new CmisNotSupportedException("You can't delete a root folder");
        }

        // recursively delete folder
        deleteRecursive(objectStore, (Folder) so, continueOnFailure, allVersions, failedToDeleteIds,
                context.getUsername());

        result.setIds(failedToDeleteIds);
        LOG.debug("stop deleteTree()");
        return result;
    }

    public AllowableActions getAllowableActions(CallContext context, String repositoryId, String objectId,
            ExtensionsData extension) {

        LOG.debug("start getAllowableActions()");
        StoredObject so = validator.getAllowableActions(context, repositoryId, objectId, extension);

        fStoreManager.getObjectStore(repositoryId);

        if (so == null) {
            throw new CmisObjectNotFoundException("Unknown object id: " + objectId);
        }

        String user = context.getUsername();
        // AllowableActions allowableActions =
        // DataObjectCreator.fillAllowableActions(so, user);
        AllowableActions allowableActions = so.getAllowableActions(user);
        LOG.debug("stop getAllowableActions()");
        return allowableActions;
    }

    public ContentStream getContentStream(CallContext context, String repositoryId, String objectId, String streamId,
            BigInteger offset, BigInteger length, ExtensionsData extension) {

        LOG.debug("start getContentStream()");
        StoredObject so = validator.getContentStream(context, repositoryId, objectId, streamId, extension);

        if (so == null) {
            throw new CmisObjectNotFoundException("Unknown object id: " + objectId);
        }

        if (!(so instanceof Content) && objectId.endsWith("-rendition")) {
            throw new CmisConstraintException("Id" + objectId
                    + " does not refer to a document or version, but only those can have content");
        }

        ContentStream csd = getContentStream(so, streamId, offset, length);

        if (null == csd) {
            throw new CmisConstraintException("Object " + so.getId() + " does not have content.");
        }

        LOG.debug("stop getContentStream()");
        return csd;
    }

    public ObjectData getObject(CallContext context, String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension, ObjectInfoHandler objectInfos) {

        LOG.debug("start getObject()");

        StoredObject so = validator.getObject(context, repositoryId, objectId, extension);

        if (so == null) {
            throw new CmisObjectNotFoundException("Unknown object id: " + objectId);
        }

        String user = context.getUsername();
        TypeManager tm = fStoreManager.getTypeManager(repositoryId);
        ObjectData od = PropertyCreationHelper.getObjectData(tm, so, filter, user, includeAllowableActions,
                includeRelationships, renditionFilter, includePolicyIds, includeAcl, extension);

        if (context.isObjectInfoRequired()) {
            ObjectInfoImpl objectInfo = new ObjectInfoImpl();
            fAtomLinkProvider.fillInformationForAtomLinks(repositoryId, so, objectInfo);
            objectInfos.addObjectInfo(objectInfo);
        }

        // fill an example extension
        String ns = "http://apache.org/opencmis/inmemory";
        List<CmisExtensionElement> extElements = new ArrayList<CmisExtensionElement>();

        Map<String, String> attr = new HashMap<String, String>();
        attr.put("type", so.getTypeId());

        extElements.add(new CmisExtensionElementImpl(ns, "objectId", attr, objectId));
        extElements.add(new CmisExtensionElementImpl(ns, "name", null, so.getName()));
        od.setExtensions(Collections.singletonList((CmisExtensionElement) new CmisExtensionElementImpl(ns,
                "exampleExtension", null, extElements)));

        LOG.debug("stop getObject()");

        return od;
    }

    public ObjectData getObjectByPath(CallContext context, String repositoryId, String path, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension, ObjectInfoHandler objectInfos) {

        LOG.debug("start getObjectByPath()");
        StoredObject so = validator.getObjectByPath(context, repositoryId, path, extension);
        if (so instanceof VersionedDocument) {
            VersionedDocument verDoc = (VersionedDocument) so;
            so = verDoc.getLatestVersion(false);
        }

        String user = context.getUsername();

        TypeManager tm = fStoreManager.getTypeManager(repositoryId);
        ObjectData od = PropertyCreationHelper.getObjectData(tm, so, filter, user, includeAllowableActions,
                includeRelationships, renditionFilter, includePolicyIds, includeAcl, extension);

        LOG.debug("stop getObjectByPath()");

        // To be able to provide all Atom links in the response we need
        // additional information:
        if (context.isObjectInfoRequired()) {
            ObjectInfoImpl objectInfo = new ObjectInfoImpl();
            fAtomLinkProvider.fillInformationForAtomLinks(repositoryId, so, objectInfo);
            objectInfos.addObjectInfo(objectInfo);
        }

        return od;
    }

    public Properties getProperties(CallContext context, String repositoryId, String objectId, String filter,
            ExtensionsData extension) {

        LOG.debug("start getProperties()");
        StoredObject so = validator.getProperties(context, repositoryId, objectId, extension);

        if (so == null) {
            throw new CmisObjectNotFoundException("Unknown object id: " + objectId);
        }

        // build properties collection
        List<String> requestedIds = FilterParser.getRequestedIdsFromFilter(filter);
        TypeManager tm = fStoreManager.getTypeManager(repositoryId);
        Properties props = PropertyCreationHelper.getPropertiesFromObject(so, tm, requestedIds, true);
        LOG.debug("stop getProperties()");
        return props;
    }

    public List<RenditionData> getRenditions(CallContext context, String repositoryId, String objectId,
            String renditionFilter, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

        LOG.debug("start getRenditions()");
        StoredObject so = validator.getRenditions(context, repositoryId, objectId, extension);

        if (so == null) {
            throw new CmisObjectNotFoundException("Unknown object id: " + objectId);
        }

        List<RenditionData> renditions = so.getRenditions(renditionFilter, maxItems == null ? 0 : maxItems.longValue(),
                skipCount == null ? 0 : skipCount.longValue());
        LOG.debug("stop getRenditions()");
        return renditions;
    }

    public ObjectData moveObject(CallContext context, String repositoryId, Holder<String> objectId,
            String targetFolderId, String sourceFolderId, ExtensionsData extension, ObjectInfoHandler objectInfos) {

        LOG.debug("start moveObject()");
        StoredObject[] sos = validator.moveObject(context, repositoryId, objectId, targetFolderId, sourceFolderId,
                extension);
        StoredObject so = sos[0];
        Folder targetFolder = null;
        Folder sourceFolder = null;
        ObjectStore objectStore = fStoreManager.getObjectStore(repositoryId);
        Filing spo = null;
        String user = context.getUsername();

        if (null == so) {
            throw new CmisObjectNotFoundException("Unknown object: " + objectId.getValue());
        } else if (so instanceof Filing) {
            spo = (Filing) so;
        } else {
            throw new CmisInvalidArgumentException("Object must be fileable: " + objectId.getValue());
        }

        StoredObject soTarget = objectStore.getObjectById(targetFolderId);
        if (null == soTarget) {
            throw new CmisObjectNotFoundException("Unknown target folder: " + targetFolderId);
        } else if (soTarget instanceof Folder) {
            targetFolder = (Folder) soTarget;
        } else {
            throw new CmisNotSupportedException("Destination " + targetFolderId
                    + " of a move operation must be a folder");
        }

        StoredObject soSource = objectStore.getObjectById(sourceFolderId);
        if (null == soSource) {
            throw new CmisObjectNotFoundException("Unknown source folder: " + sourceFolderId);
        } else if (soSource instanceof Folder) {
            sourceFolder = (Folder) soSource;
        } else {
            throw new CmisNotSupportedException("Source " + sourceFolderId + " of a move operation must be a folder");
        }

        boolean foundOldParent = false;
        for (String parentId : ((ObjectStoreFiling)objectStore).getParentIds(spo, user)) {
            if (parentId.equals(soSource.getId())) {
                foundOldParent = true;
                break;
            }
        }
        if (!foundOldParent) {
            throw new CmisNotSupportedException("Cannot move object, source folder " + sourceFolderId
                    + "is not a parent of object " + objectId.getValue());
        }

        if (so instanceof Folder && hasDescendant(context.getUsername(), objectStore, (Folder) so, targetFolder)) {
            throw new CmisNotSupportedException("Destination of a move cannot be a subfolder of the source");
        }

        if (objectStore instanceof ObjectStoreFiling) {
            ((ObjectStoreFiling) objectStore).move(so, sourceFolder, targetFolder);
        } else {
            throw new CmisInvalidArgumentException("Repository " + repositoryId + "does not support Filing");
        }
        objectId.setValue(so.getId());

        LOG.debug("stop moveObject()");

        TypeManager tm = fStoreManager.getTypeManager(repositoryId);
        ObjectData od = PropertyCreationHelper.getObjectData(tm, so, null, user, false, IncludeRelationships.NONE,
                null, false, false, extension);

        // To be able to provide all Atom links in the response we need
        // additional information:
        if (context.isObjectInfoRequired()) {
            ObjectInfoImpl objectInfo = new ObjectInfoImpl();
            fAtomLinkProvider.fillInformationForAtomLinks(repositoryId, so, od, objectInfo);
            objectInfos.addObjectInfo(objectInfo);
        }

        return od;
    }

    public void setContentStream(CallContext context, String repositoryId, Holder<String> objectId,
            Boolean overwriteFlag, Holder<String> changeToken, ContentStream contentStream, ExtensionsData extension) {

        LOG.debug("start setContentStream()");
        Content content;
        if (null == overwriteFlag) {
            overwriteFlag = Boolean.TRUE;
        }

        StoredObject so = validator.setContentStream(context, repositoryId, objectId, overwriteFlag, extension);

        if (changeToken != null && changeToken.getValue() != null
                && Long.valueOf(so.getChangeToken()) > Long.valueOf(changeToken.getValue())) {
            throw new CmisUpdateConflictException("updateProperties failed: changeToken does not match");
        }

        if (!(so instanceof Document || so instanceof VersionedDocument || so instanceof DocumentVersion)) {
            throw new CmisObjectNotFoundException("Id" + objectId
                    + " does not refer to a document, but only documents can have content");
        }

        // validate content allowed
        TypeDefinition typeDef = getTypeDefinition(repositoryId, so);
        if (!(typeDef instanceof DocumentTypeDefinition))
            throw new CmisInvalidArgumentException("Object does not refer to a document, can't set content");
        TypeValidator.validateContentAllowed((DocumentTypeDefinition) typeDef, null != contentStream);

        if (so instanceof Document) {
            content = ((Document) so);
        } else if (so instanceof DocumentVersion) {
            // something that is versionable check the proper status of the
            // object
            String user = context.getUsername();
            testHasProperCheckedOutStatus(so, user);
            content = (DocumentVersion) so;
        } else {
            throw new IllegalArgumentException("Content cannot be set on this object (must be document or version)");
        }

        if (!overwriteFlag && content.getContent(0, -1) != null) {
            throw new CmisContentAlreadyExistsException(
                    "cannot overwrite existing content if overwrite flag is not set");
        }

        content.setContent(contentStream, true);
        so.updateSystemBasePropertiesWhenModified(null, context.getUsername());

        LOG.debug("stop setContentStream()");
    }

    public void updateProperties(CallContext context, String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, Properties properties, Acl acl, ExtensionsData extension,
            ObjectInfoHandler objectInfos) {

        LOG.debug("start updateProperties()");
        StoredObject so = validator.updateProperties(context, repositoryId, objectId, extension);
        String user = context.getUsername();

        // Validation
        TypeDefinition typeDef = getTypeDefinition(repositoryId, so);
        boolean isCheckedOut = false;

        isCheckedOut = isCheckedOut(so, user);

        Map<String, PropertyData<?>> oldProperties = so.getProperties();

        // check properties for validity
        boolean cmis11 = context.getCmisVersion() != CmisVersion.CMIS_1_0;
        validateProperties(repositoryId, so, properties, false, cmis11);

        if (changeToken != null && changeToken.getValue() != null
                && Long.valueOf(so.getChangeToken()) > Long.valueOf(changeToken.getValue())) {
            throw new CmisUpdateConflictException("updateProperties failed: changeToken does not match");
        }

        // update properties
        boolean hasUpdatedProp = false;

        // Find secondary type definitions to consider for update
        List<String> existingSecondaryTypeIds = so.getSecondaryTypeIds();
        @SuppressWarnings("unchecked")
        PropertyData<String> pdSec = (PropertyData<String>) properties.getProperties().get(
                PropertyIds.SECONDARY_OBJECT_TYPE_IDS);
        List<String> newSecondaryTypeIds = pdSec == null ? null : pdSec.getValues();
        Set<String> secondaryTypeIds = new HashSet<String>();
        if (null != existingSecondaryTypeIds)
            secondaryTypeIds.addAll(existingSecondaryTypeIds);
        if (null != newSecondaryTypeIds)
            secondaryTypeIds.addAll(newSecondaryTypeIds);

        // Find secondary type definitions to delete (null means not set --> do
        // not change, empty --> remove all secondary types)
        if (null != newSecondaryTypeIds) {
            List<String> propertiesIdToDelete = getListOfPropertiesToDeleteFromRemovedSecondaryTypes(repositoryId, so,
                    newSecondaryTypeIds);
            for (String propIdToRemove : propertiesIdToDelete) {
                so.getProperties().remove(propIdToRemove);
            }
        }

        // update properties:
        if (properties != null) {
            for (String key : properties.getProperties().keySet()) {
                if (key.equals(PropertyIds.NAME)) {
                    continue; // ignore here
                }

                PropertyData<?> value = properties.getProperties().get(key);
                PropertyDefinition<?> propDef = typeDef.getPropertyDefinitions().get(key);
                if (null == propDef && cmis11) {
                    TypeDefinition typeDefSecondary = getSecondaryTypeDefinition(repositoryId, secondaryTypeIds, key);
                    if (null == typeDefSecondary)
                        throw new CmisInvalidArgumentException("Cannot update property " + key
                                + ": not contained in type");
                    propDef = typeDefSecondary.getPropertyDefinitions().get(key);
                }

                if (value.getValues() == null || value.getFirstValue() == null) {
                    // delete property
                    // check if a required a property
                    if (propDef.isRequired()) {
                        throw new CmisConstraintException(
                                "updateProperties failed, following property can't be deleted, because it is required: "
                                        + key);
                    }
                    oldProperties.remove(key);
                    hasUpdatedProp = true;
                } else {
                    if (propDef.getUpdatability().equals(Updatability.WHENCHECKEDOUT)) {
                        if (!isCheckedOut)
                            throw new CmisUpdateConflictException(
                                    "updateProperties failed, following property can't be updated, because it is not checked-out: "
                                            + key);
                    } else if (!propDef.getUpdatability().equals(Updatability.READWRITE)) {
                        throw new CmisConstraintException(
                                "updateProperties failed, following property can't be updated, because it is not writable: "
                                        + key);
                    }
                    oldProperties.put(key, value);
                    hasUpdatedProp = true;
                }
            }

            // get name from properties and perform special rename to check if
            // path already exists
            PropertyData<?> pd = properties.getProperties().get(PropertyIds.NAME);
            if (pd != null && so instanceof Filing) {
                String newName = (String) pd.getFirstValue();
                boolean hasParent = ((Filing) so).hasParent();
                if (so instanceof Folder && !hasParent) {
                    throw new CmisConstraintException("updateProperties failed, you cannot rename the root folder");
                }
                if (newName == null || newName.equals("")) {
                    throw new CmisConstraintException("updateProperties failed, name must not be empty.");
                }
                if (!NameValidator.isValidName(newName)) {
                    throw new CmisInvalidArgumentException(NameValidator.ERROR_ILLEGAL_NAME);
                }
                // Note: the test for duplicated name in folder is left to the object store
                ObjectStoreFiling objStore = (ObjectStoreFiling) fStoreManager.getObjectStore(repositoryId);
                objStore.rename((Fileable)so, (String) pd.getFirstValue()); 
                hasUpdatedProp = true;
            }
        }

        if (hasUpdatedProp) {
            // set user, creation date, etc.
            if (user == null) {
                user = "unknown";
            }
            so.updateSystemBasePropertiesWhenModified(properties.getProperties(), user);
            // set changeToken
            so.persist();
        }

        if (hasUpdatedProp) {
            objectId.setValue(so.getId()); // might have a new id
            if (null != changeToken) {
                String changeTokenVal = so.getChangeToken();
                LOG.debug("updateProperties(), new change token is: " + changeTokenVal);
                changeToken.setValue(changeTokenVal);
            }
        }

        if (null != acl) {
            // TODO
            LOG.warn("Setting ACLs is currently not supported by this implementation, acl is ignored");
            // if implemented add this call:
            // fAclService.appyAcl(context, repositoryId, acl, null,
            // AclPropagation.OBJECTONLY,
            // extension);
        }

        TypeManager tm = fStoreManager.getTypeManager(repositoryId);
        ObjectData od = PropertyCreationHelper.getObjectData(tm, so, null, user, false, IncludeRelationships.NONE,
                null, false, false, extension);

        // To be able to provide all Atom links in the response we need
        // additional information:
        if (context.isObjectInfoRequired()) {
            ObjectInfoImpl objectInfo = new ObjectInfoImpl();
            fAtomLinkProvider.fillInformationForAtomLinks(repositoryId, so, od, objectInfo);
            objectInfos.addObjectInfo(objectInfo);
        }

        LOG.debug("stop updateProperties()");
    }

    // CMIS 1.1
    public void appendContentStream(CallContext context, String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, ContentStream contentStream, ExtensionsData extension) {

        Content content;

        LOG.debug("start appendContentStream()");
        StoredObject so = validator.appendContentStream(context, repositoryId, objectId, extension);

        if (changeToken != null && changeToken.getValue() != null
                && Long.valueOf(so.getChangeToken()) > Long.valueOf(changeToken.getValue())) {
            throw new CmisUpdateConflictException("updateProperties failed: changeToken does not match");
        }

        if (!(so instanceof Document || so instanceof VersionedDocument || so instanceof DocumentVersion)) {
            throw new CmisObjectNotFoundException("Id" + objectId
                    + " does not refer to a document, but only documents can have content");
        }

        // validate content allowed
        TypeDefinition typeDef = getTypeDefinition(repositoryId, so);
        if (!(typeDef instanceof DocumentTypeDefinition))
            throw new CmisInvalidArgumentException("Object does not refer to a document, can't set content");
        TypeValidator.validateContentAllowed((DocumentTypeDefinition) typeDef, null != contentStream);

        if (so instanceof Document) {
            content = ((Document) so);
        } else if (so instanceof DocumentVersion) {
            // something that is versionable check the proper status of the
            // object
            String user = context.getUsername();
            testHasProperCheckedOutStatus(so, user);
            content = (DocumentVersion) so;
        } else {
            throw new IllegalArgumentException("Content cannot be set on this object (must be document or version)");
        }

        content.appendContent(contentStream);
        so.updateSystemBasePropertiesWhenModified(null, context.getUsername());
    }

    public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(CallContext context, String repositoryId,
            List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken, Properties properties,
            List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds, ExtensionsData extension,
            ObjectInfoHandler objectInfos) {

        List<BulkUpdateObjectIdAndChangeToken> result = new ArrayList<BulkUpdateObjectIdAndChangeToken>();
        for (BulkUpdateObjectIdAndChangeToken obj : objectIdAndChangeToken) {
            Holder<String> objId = new Holder<String>(obj.getId());
            Holder<String> changeToken = new Holder<String>(obj.getChangeToken());
            try {
                updateProperties(context, repositoryId, objId, changeToken, properties, null, null, objectInfos);
                result.add(new BulkUpdateObjectIdAndChangeTokenImpl(obj.getId(), changeToken.getValue()));
            } catch (Exception e) {
                LOG.error("updating properties in bulk upadate failed for object" + obj.getId() + ": ", e);
            }
        }
        return result;
    }

    // ///////////////////////////////////////////////////////
    // private helper methods

    private StoredObject createDocumentIntern(CallContext context, String repositoryId, Properties properties,
            String folderId, ContentStream contentStream, VersioningState versioningState, List<String> policies,
            Acl addACEs, Acl removeACEs, ExtensionsData extension) {

        addACEs = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(), addACEs);
        removeACEs = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(),
                removeACEs);

        validator.createDocument(context, repositoryId, folderId, policies, extension);

        // Validation stuff
        TypeValidator.validateRequiredSystemProperties(properties);

        String user = context.getUsername();
        TypeDefinition typeDef = getTypeDefinition(repositoryId, properties);

        ObjectStore objectStore = fStoreManager.getObjectStore(repositoryId);
        Map<String, PropertyData<?>> propMap = properties.getProperties();
        // get name from properties
        PropertyData<?> pd = propMap.get(PropertyIds.NAME);
        String name = (String) pd.getFirstValue();

        // validate ACL
        TypeValidator.validateAcl(typeDef, addACEs, removeACEs);

        Folder folder = null;
        if (null != folderId) {
            StoredObject so = objectStore.getObjectById(folderId);

            if (null == so) {
                throw new CmisInvalidArgumentException(" Cannot create document, folderId: " + folderId + " is invalid");
            }

            if (so instanceof Folder) {
                folder = (Folder) so;
            } else {
                throw new CmisInvalidArgumentException("Can't creat document, folderId does not refer to a folder: "
                        + folderId);
            }

            TypeValidator.validateAllowedChildObjectTypes(typeDef, folder.getAllowedChildObjectTypeIds());
        }

        // check if the given type is a document type
        if (!typeDef.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
            throw new CmisInvalidArgumentException("Cannot create a document, with a non-document type: "
                    + typeDef.getId());
        }

        // check name syntax
        if (!NameValidator.isValidName(name)) {
            throw new CmisInvalidArgumentException(NameValidator.ERROR_ILLEGAL_NAME + " Name is: " + name);
        }

        // validate content allowed
        TypeValidator.validateContentAllowed((DocumentTypeDefinition) typeDef, null != contentStream);

        TypeValidator.validateVersionStateForCreate((DocumentTypeDefinition) typeDef, versioningState);

        // set properties that are not set but have a default:
        Map<String, PropertyData<?>> propMapNew = setDefaultProperties(typeDef, propMap);
        if (propMapNew != propMap) {
            properties = new PropertiesImpl(propMapNew.values());
            propMap = propMapNew;
        }

        boolean cmis11 = context.getCmisVersion() != CmisVersion.CMIS_1_0;
        validateProperties(repositoryId, null, properties, false, cmis11);

        // set user, creation date, etc.
        if (user == null) {
            user = "unknown";
        }

        StoredObject so = null;

        // check if content stream parameters are set and if not set some
        // defaults
        if (null != contentStream
                && (contentStream.getFileName() == null || contentStream.getFileName().length() == 0
                        || contentStream.getMimeType() == null || contentStream.getMimeType().length() == 0)) {
            ContentStreamImpl cs = new ContentStreamImpl();
            cs.setStream(contentStream.getStream());
            if (contentStream.getFileName() == null || contentStream.getFileName().length() == 0) {
                cs.setFileName(name);
            } else {
                cs.setFileName(contentStream.getFileName());
            }
            cs.setLength(contentStream.getBigLength());
            if (contentStream.getMimeType() == null || contentStream.getMimeType().length() == 0) {
                cs.setMimeType("application/octet-stream");
            } else {
                cs.setMimeType(contentStream.getMimeType());
            }
            cs.setExtensions(contentStream.getExtensions());
            contentStream = cs;
        }

        // Now we are sure to have document type definition:
        if (((DocumentTypeDefinition) typeDef).isVersionable()) {
            DocumentVersion version = objectStore.createVersionedDocument(name, propMap, user, folder, policies,
                    addACEs, removeACEs, contentStream, versioningState);
            version.persist();
            so = version; // return the version and not the version series to
                          // caller
        } else {
            Document doc = objectStore.createDocument(name, propMap, user, folder, policies, addACEs, removeACEs);
            doc.setContent(contentStream, false);
            doc.persist();
            so = doc;
        }

        return so;
    }

    private Folder createFolderIntern(CallContext context, String repositoryId, Properties properties, String folderId,
            List<String> policies, Acl addACEs, Acl removeACEs, ExtensionsData extension) {

        addACEs = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(), addACEs);
        removeACEs = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(),
                removeACEs);

        validator.createFolder(context, repositoryId, folderId, policies, extension);
        TypeValidator.validateRequiredSystemProperties(properties);
        String user = context.getUsername();

        ObjectStore fs = fStoreManager.getObjectStore(repositoryId);
        Folder parent = null;

        // get required properties
        PropertyData<?> pd = properties.getProperties().get(PropertyIds.NAME);
        String folderName = (String) pd.getFirstValue();
        if (null == folderName || folderName.length() == 0) {
            throw new CmisInvalidArgumentException("Cannot create a folder without a name.");
        }

        // check name syntax
        if (!NameValidator.isValidName(folderName)) {
            throw new CmisInvalidArgumentException(NameValidator.ERROR_ILLEGAL_NAME + " Name is: " + folderName);
        }

        TypeDefinition typeDef = getTypeDefinition(repositoryId, properties);

        // check if the given type is a folder type
        if (!typeDef.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
            throw new CmisInvalidArgumentException("Cannot create a folder, with a non-folder type: " + typeDef.getId());
        }

        Map<String, PropertyData<?>> propMap = properties.getProperties();
        Map<String, PropertyData<?>> propMapNew = setDefaultProperties(typeDef, propMap);
        if (propMapNew != propMap) {
            properties = new PropertiesImpl(propMapNew.values());
        }

        boolean cmis11 = context.getCmisVersion() != CmisVersion.CMIS_1_0;
        validateProperties(repositoryId, null, properties, false, cmis11);

        // validate ACL
        TypeValidator.validateAcl(typeDef, addACEs, removeACEs);

        StoredObject so = null;
        // create folder
        try {
            LOG.debug("get folder for id: " + folderId);
            so = fs.getObjectById(folderId);
        } catch (Exception e) {
            throw new CmisObjectNotFoundException("Failed to retrieve folder.", e);
        }

        if (so instanceof Folder) {
            parent = (Folder) so;
        } else {
            throw new CmisInvalidArgumentException("Can't create folder, folderId does not refer to a folder: "
                    + folderId);
        }

        if (user == null) {
            user = "unknown";
        }

        ObjectStore objStore = fStoreManager.getObjectStore(repositoryId);
        Folder newFolder = objStore.createFolder(folderName, properties.getProperties(), user, parent, policies,
                addACEs, removeACEs);
        LOG.debug("stop createFolder()");
        newFolder.persist();
        return newFolder;
    }

    private StoredObject createPolicyIntern(CallContext context, String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {

        validator.createPolicy(context, repositoryId, folderId, addAces, removeAces, policies, extension);

        addAces = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(), addAces);
        removeAces = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(),
                removeAces);

        String user = context.getUsername();
        Map<String, PropertyData<?>> propMap = properties.getProperties();
        // get name from properties
        PropertyData<?> pd = propMap.get(PropertyIds.NAME);
        String name = (String) pd.getFirstValue();
        pd = propMap.get(PropertyIds.POLICY_TEXT);
        String policyText = (String) pd.getFirstValue();

        ObjectStore objStore = fStoreManager.getObjectStore(repositoryId);
        StoredObject storedObject = objStore.createPolicy(name, policyText, propMap, user);

        return storedObject;
    }

    private StoredObject createRelationshipIntern(CallContext context, String repositoryId, Properties properties,
            List<String> policies, Acl addACEs, Acl removeACEs, ExtensionsData extension) {

        TypeValidator.validateRequiredSystemProperties(properties);

        String user = context.getUsername();

        addACEs = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(), addACEs);
        removeACEs = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(),
                removeACEs);

        // get required properties
        PropertyData<?> pd = properties.getProperties().get(PropertyIds.SOURCE_ID);
        String sourceId = (String) pd.getFirstValue();
        if (null == sourceId || sourceId.length() == 0)
            throw new CmisInvalidArgumentException("Cannot create a relationship without a sourceId.");

        pd = properties.getProperties().get(PropertyIds.TARGET_ID);
        String targetId = (String) pd.getFirstValue();
        if (null == targetId || targetId.length() == 0)
            throw new CmisInvalidArgumentException("Cannot create a relationship without a targetId.");

        TypeDefinition typeDef = getTypeDefinition(repositoryId, properties);

        // check if the given type is a relationship type
        if (!typeDef.getBaseTypeId().equals(BaseTypeId.CMIS_RELATIONSHIP))
            throw new CmisInvalidArgumentException("Cannot create a relationship, with a non-relationship type: "
                    + typeDef.getId());

        StoredObject[] relationObjects = validator.createRelationship(context, repositoryId, sourceId, targetId,
                policies, extension);

        // set default properties
        Map<String, PropertyData<?>> propMap = properties.getProperties();
        Map<String, PropertyData<?>> propMapNew = setDefaultProperties(typeDef, propMap);
        if (propMapNew != propMap) {
            properties = new PropertiesImpl(propMapNew.values());
        }

        boolean cmis11 = context.getCmisVersion() != CmisVersion.CMIS_1_0;
        validateProperties(repositoryId, null, properties, false, cmis11);

        // validate ACL
        TypeValidator.validateAcl(typeDef, addACEs, removeACEs);

        // validate the allowed types of the relationship
        ObjectStore objStore = fStoreManager.getObjectStore(repositoryId);

        TypeDefinition sourceTypeDef = fStoreManager.getTypeById(repositoryId,
                objStore.getObjectById(sourceId).getTypeId()).getTypeDefinition();
        TypeDefinition targetTypeDef = fStoreManager.getTypeById(repositoryId,
                objStore.getObjectById(targetId).getTypeId()).getTypeDefinition();
        TypeValidator.validateAllowedRelationshipTypes((RelationshipTypeDefinition) typeDef, sourceTypeDef,
                targetTypeDef);

        // get name from properties
        pd = propMap.get(PropertyIds.NAME);
        String name = (String) pd.getFirstValue();

        // StoredObject storedObject = objStore.createRelationship(
        // relationObjects[0], relationObjects[1],
        // propMap, user, addACEs, removeACEs);
        StoredObject storedObject = objStore.createRelationship(name, relationObjects[0], relationObjects[1], propMap,
                user, addACEs, removeACEs);
        return storedObject;
    }

    private StoredObject createItemIntern(CallContext context, String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addACEs, Acl removeACEs, ExtensionsData extension) {

        validator.createItem(context, repositoryId, properties, folderId, policies, addACEs, removeACEs, extension);

        addACEs = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(), addACEs);
        removeACEs = org.apache.chemistry.opencmis.inmemory.TypeValidator.expandAclMakros(context.getUsername(),
                removeACEs);

        validator.createDocument(context, repositoryId, folderId, policies, extension);

        // Validation stuff
        TypeValidator.validateRequiredSystemProperties(properties);

        String user = context.getUsername();
        TypeDefinition typeDef = getTypeDefinition(repositoryId, properties);

        ObjectStore objectStore = fStoreManager.getObjectStore(repositoryId);
        Map<String, PropertyData<?>> propMap = properties.getProperties();
        // get name from properties
        PropertyData<?> pd = propMap.get(PropertyIds.NAME);
        String name = (String) pd.getFirstValue();

        // validate ACL
        TypeValidator.validateAcl(typeDef, addACEs, removeACEs);

        Folder folder = null;
        if (null != folderId) {
            StoredObject so = objectStore.getObjectById(folderId);

            if (null == so) {
                throw new CmisInvalidArgumentException(" Cannot create item, folderId: " + folderId + " is invalid");
            }

            if (so instanceof Folder) {
                folder = (Folder) so;
            } else {
                throw new CmisInvalidArgumentException("Can't create item, folderId does not refer to a folder: "
                        + folderId);
            }

            TypeValidator.validateAllowedChildObjectTypes(typeDef, folder.getAllowedChildObjectTypeIds());
        }

        // check if the given type is an item type
        if (!typeDef.getBaseTypeId().equals(BaseTypeId.CMIS_ITEM)) {
            throw new CmisInvalidArgumentException("Cannot create an item, with a non-item type: " + typeDef.getId());
        }

        // check name syntax
        if (!NameValidator.isValidName(name)) {
            throw new CmisInvalidArgumentException(NameValidator.ERROR_ILLEGAL_NAME + " Name is: " + name);
        }

        // set properties that are not set but have a default:
        Map<String, PropertyData<?>> propMapNew = setDefaultProperties(typeDef, propMap);
        if (propMapNew != propMap) {
            properties = new PropertiesImpl(propMapNew.values());
            propMap = propMapNew;
        }

        boolean cmis11 = context.getCmisVersion() != CmisVersion.CMIS_1_0;
        validateProperties(repositoryId, null, properties, false, cmis11);

        // set user, creation date, etc.
        if (user == null) {
            user = "unknown";
        }

        StoredObject so = null;

        // Now we are sure to have document type definition:
        so = objectStore.createItem(name, propMap, user, folder, policies, addACEs, removeACEs);
        so.persist();

        return so;

    }

    private boolean hasDescendant(String user, ObjectStore objStore, Folder sourceFolder, Folder targetFolder) {
        String sourceId = sourceFolder.getId();
        String targetId = targetFolder.getId();
        
        while (targetId != null) {
            if (targetId.equals(sourceId)) {
                return true;
            }
            List<String>parentIds = ((ObjectStoreFiling)objStore).getParentIds(targetFolder, user);
            targetId = parentIds == null || parentIds.isEmpty() ? null : parentIds.get(0);    
            if (null != targetId) {
                targetFolder = (Folder) objStore.getObjectById(targetId);
            }
        }
        return false;
    }

    /**
     * Recursively delete a tree by traversing it and first deleting all
     * children and then the object itself
     * 
     * @param objStore
     * @param parentFolder
     * @param continueOnFailure
     * @param allVersions
     * @param failedToDeleteIds
     * @return returns true if operation should continue, false if it should
     *         stop
     */
    private boolean deleteRecursive(ObjectStore objStore, Folder parentFolder, boolean continueOnFailure,
            boolean allVersions, List<String> failedToDeleteIds, String user) {
        
        ObjectStoreFiling filingStore = (ObjectStoreFiling) objStore;
        ChildrenResult childrenResult = filingStore.getChildren(parentFolder, -1, -1, "Admin", true);
        List<Fileable> children = childrenResult.getChildren();

        if (null == children) {
            return true;
        }

        for (Fileable child : children) {
            if (child instanceof Folder) {
                boolean mustContinue = deleteRecursive(objStore, (Folder) child, continueOnFailure, allVersions,
                        failedToDeleteIds, user);
                if (!mustContinue && !continueOnFailure) {
                    return false; // stop further deletions
                }
            } else {
                try {
                    objStore.deleteObject(child.getId(), allVersions, user);
                } catch (Exception e) {
                    failedToDeleteIds.add(child.getId());
                }
            }
        }
        objStore.deleteObject(parentFolder.getId(), allVersions, user);
        return true;
    }

    private static ContentStream getContentStream(StoredObject so, String streamId, BigInteger offset, BigInteger length) {
        ContentStream csd = null;
        long lOffset = offset == null ? 0 : offset.longValue();
        long lLength = length == null ? -1 : length.longValue();

        if (streamId == null) {
            csd = ((Content) so).getContent(lOffset, lLength);
            return csd;
        } else if (streamId.endsWith("-rendition")) {
            csd = so.getRenditionContent(streamId, lOffset, lLength);
        }

        return csd;
    }

    private Map<String, PropertyData<?>> setDefaultProperties(TypeDefinition typeDef,
            Map<String, PropertyData<?>> properties) {
        Map<String, PropertyDefinition<?>> propDefs = typeDef.getPropertyDefinitions();
        boolean hasCopied = false;

        for (PropertyDefinition<?> propDef : propDefs.values()) {
            String propId = propDef.getId();
            List<?> defaultVal = propDef.getDefaultValue();
            if (defaultVal != null && null == properties.get(propId)) {
                if (!hasCopied) {
                    properties = new HashMap<String, PropertyData<?>>(properties); // copy
                                                                                   // because
                                                                                   // it
                                                                                   // is
                                                                                   // an
                                                                                   // unmodified
                                                                                   // collection
                    hasCopied = true;
                }
                Object value = propDef.getCardinality() == Cardinality.SINGLE ? defaultVal.get(0) : defaultVal;
                PropertyData<?> pd = fStoreManager.getObjectFactory().createPropertyData(propDef, value);
                // set property:
                properties.put(propId, pd);
            }
        }
        return properties;
    }

    private void validateProperties(String repositoryId, StoredObject so, Properties properties,
            boolean checkMandatory, boolean cmis11) {
        TypeDefinition typeDef;

        if (null != so)
            typeDef = getTypeDefinition(repositoryId, so);
        else
            typeDef = getTypeDefinition(repositoryId, properties);

        // check properties for validity
        if (!cmis11) {
            TypeValidator.validateProperties(typeDef, properties, checkMandatory, cmis11);
            return;
        }

        // CMIS 1.1 secondary types
        PropertyData<?> pd = properties.getProperties().get(PropertyIds.SECONDARY_OBJECT_TYPE_IDS);

        @SuppressWarnings("unchecked")
        List<String> secondaryTypeIds = (List<String>) (pd == null ? null : pd.getValues());
        // if no secondary types are passed use the existing ones:
        if (null != so && (null == secondaryTypeIds || secondaryTypeIds.size() == 0)) {
            secondaryTypeIds = so.getSecondaryTypeIds();
        }

        if (null != secondaryTypeIds && secondaryTypeIds.size() != 0) {
            List<String> allTypeIds = new ArrayList<String>(secondaryTypeIds);
            allTypeIds.add(typeDef.getId());
            List<TypeDefinition> typeDefs = getTypeDefinition(repositoryId, allTypeIds);
            TypeValidator.validateProperties(typeDefs, properties, checkMandatory);
        } else {
            TypeValidator.validateProperties(typeDef, properties, checkMandatory, true);
        }
    }

    private TypeDefinition getSecondaryTypeDefinition(String repositoryId, Set<String> secondaryTypeIds,
            String propertyId) {
        if (null == secondaryTypeIds || secondaryTypeIds.isEmpty())
            return null;

        for (String typeId : secondaryTypeIds) {
            TypeDefinitionContainer typeDefC = fStoreManager.getTypeById(repositoryId, typeId);
            TypeDefinition typeDef = typeDefC.getTypeDefinition();

            if (TypeValidator.typeContainsProperty(typeDef, propertyId)) {
                return typeDef;
            }
        }

        return null;
    }

    private List<String> getListOfPropertiesToDeleteFromRemovedSecondaryTypes(String repositoryId, StoredObject so,
            List<String> newSecondaryTypeIds) {

        List<String> propertiesToDelete = new ArrayList<String>(); // properties
                                                                   // id to be
                                                                   // removed

        // calculate delta to be removed
        List<String> existingSecondaryTypeIds = so.getSecondaryTypeIds();
        List<String> delta = new ArrayList<String>(existingSecondaryTypeIds);
        delta.removeAll(newSecondaryTypeIds);
        for (String typeDefId : delta) {
            TypeDefinitionContainer typeDefC = fStoreManager.getTypeById(repositoryId, typeDefId);
            TypeDefinition typeDef = typeDefC.getTypeDefinition();
            propertiesToDelete.addAll(typeDef.getPropertyDefinitions().keySet());
        }

        // TODO: the list may contain too many properties, if the same property
        // is also in a type not to be removed
        return propertiesToDelete;
    }

}
