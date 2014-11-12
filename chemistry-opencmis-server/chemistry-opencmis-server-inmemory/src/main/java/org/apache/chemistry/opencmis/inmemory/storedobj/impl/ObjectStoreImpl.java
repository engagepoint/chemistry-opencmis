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
package org.apache.chemistry.opencmis.inmemory.storedobj.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNameConstraintViolationException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Document;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.DocumentVersion;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Fileable;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Filing;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Folder;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.ObjectStore;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.ObjectStoreMultiFiling;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Relationship;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.StoredObject;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.VersionedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The object store is the central core of the in-memory repository. It is based
 * on huge HashMap map mapping ids to objects in memory. To allow access from
 * multiple threads a Java concurrent HashMap is used that allows parallel
 * access methods.
 * <p>
 * Certain methods in the in-memory repository must guarantee constraints. For
 * example a folder enforces that each child has a unique name. Therefore
 * certain operations must occur in an atomic manner. In the example it must be
 * guaranteed that no write access occurs to the map between acquiring the
 * iterator to find the children and finishing the add operation when no name
 * conflicts can occur. For this purpose this class has methods to lock an
 * unlock the state of the repository. It is very important that the caller
 * acquiring the lock enforces an unlock under all circumstances. Typical code
 * is:
 * <p>
 * 
 * <pre>
 * ObjectStoreImpl os = ... ;
 * try {
 *     os.lock();
 * } finally {
 *     os.unlock();
 * }
 * </pre>
 * 
 * The locking is very coarse-grained. Productive implementations would probably
 * implement finer grained locks on a folder or document rather than the
 * complete repository.
 */
public class ObjectStoreImpl implements ObjectStore, ObjectStoreMultiFiling {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectStoreImpl.class);

    /**
     * user id for administrator always having all rights
     */
    public static final String ADMIN_PRINCIPAL_ID = "Admin";

    /**
     * Simple id generator that uses just an integer
     */
    private static int NEXT_UNUSED_ID = 100;

    /**
     * a concurrent HashMap as core element to hold all objects in the
     * repository
     */
    private final Map<String, StoredObject> fStoredObjectMap = new ConcurrentHashMap<String, StoredObject>();

    /**
     * a concurrent HashMap to hold all Acls in the repository
     */
    private int nextUnusedAclId = 0;

    private final List<InMemoryAcl> fAcls = new ArrayList<InMemoryAcl>();

    private final Lock fLock = new ReentrantLock();

    final String fRepositoryId;
    FolderImpl fRootFolder = null;

    public ObjectStoreImpl(String repositoryId) {
        fRepositoryId = repositoryId;
        createRootFolder();
    }

    private static synchronized Integer getNextId() {
        return NEXT_UNUSED_ID++;
    }

    private synchronized Integer getNextAclId() {
        return nextUnusedAclId++;
    }

    private void lock() {
        fLock.lock();
    }

    private void unlock() {
        fLock.unlock();
    }

    @Override
    public Folder getRootFolder() {
        return fRootFolder;
    }

    @Override
    public StoredObject getObjectByPath(String path, String user) {
        StoredObject so = findObjectWithPathInDescendents(path, user, Folder.PATH_SEPARATOR, fRootFolder);
        return so;
    }

    private Fileable findObjectWithPathInDescendents(String path, String user, String prefix, Fileable fo) {
        if (path.equals(prefix)) {
            return fo;
        } else if (fo instanceof Folder) {
            List<Fileable> children = getChildren((Folder) fo);
            for (Fileable child : children) {
                String foundPath = prefix.length() == 1 ? prefix + child.getName() : prefix + Folder.PATH_SEPARATOR
                        + child.getName();
                if (path.startsWith(foundPath)) {
                    Fileable found = findObjectWithPathInDescendents(path, user, foundPath, child);
                    if (null != found)
                        return found;   // note that there can be multiple folders with the same prefix like folder1, folder10
                }
            }
        }
        return null;
    }

    @Override
    public StoredObject getObjectById(String objectId) {
        // we use path as id so we just can look it up in the map
        StoredObject so = fStoredObjectMap.get(objectId);
        return so;
    }

    @Override
    public void deleteObject(String objectId, Boolean allVersions, String user) {
        StoredObject obj = fStoredObjectMap.get(objectId);

        if (null == obj) {
            throw new RuntimeException("Cannot delete object with id  " + objectId + ". Object does not exist.");
        }

        if (obj instanceof FolderImpl) {
            deleteFolder(objectId, user);
        } else if (obj instanceof DocumentVersion) {
            DocumentVersion vers = (DocumentVersion) obj;
            VersionedDocument parentDoc = vers.getParentDocument();
            boolean otherVersionsExists;
            if (allVersions != null && allVersions) {
                otherVersionsExists = false;
                List<DocumentVersion> allVers = parentDoc.getAllVersions();
                for (DocumentVersion ver : allVers) {
                    fStoredObjectMap.remove(ver.getId());
                }
            } else {
                fStoredObjectMap.remove(objectId);
                otherVersionsExists = parentDoc.deleteVersion(vers);
            }

            if (!otherVersionsExists) {
                fStoredObjectMap.remove(parentDoc.getId());
            }
        } else {
            fStoredObjectMap.remove(objectId);
        }
    }

    public void removeVersion(DocumentVersion vers) {
        StoredObject found = fStoredObjectMap.remove(vers.getId());

        if (null == found) {
            throw new CmisInvalidArgumentException("Cannot delete object with id  " + vers.getId()
                    + ". Object does not exist.");
        }
    }

    public String storeObject(StoredObject so) {
        String id = so.getId();
        // check if update or create
        if (null == id) {
            id = getNextId().toString();
        }
        fStoredObjectMap.put(id, so);
        return id;
    }

    StoredObject getObject(String id) {
        return fStoredObjectMap.get(id);
    }

    void removeObject(String id) {
        fStoredObjectMap.remove(id);
    }

    public Set<String> getIds() {
        Set<String> entries = fStoredObjectMap.keySet();
        return entries;
    }

    /**
     * Clear repository and remove all data.
     */
    @Override
    public void clear() {
        lock();
        fStoredObjectMap.clear();
        storeObject(fRootFolder);
        unlock();
    }

    @Override
    public long getObjectCount() {
        return fStoredObjectMap.size();
    }

    // /////////////////////////////////////////
    // private helper methods

    private void createRootFolder() {
        FolderImpl rootFolder = new FolderImpl(this);
        rootFolder.setName("RootFolder");
        rootFolder.setParentId(null);
        rootFolder.setTypeId(BaseTypeId.CMIS_FOLDER.value());
        rootFolder.setCreatedBy("Admin");
        rootFolder.setModifiedBy("Admin");
        rootFolder.setModifiedAtNow();
        rootFolder.setRepositoryId(fRepositoryId);
        rootFolder.setAclId(addAcl(InMemoryAcl.getDefaultAcl()));
        rootFolder.persist();
        fRootFolder = rootFolder;
    }

    @Override
    public Document createDocument(String name, Map<String, PropertyData<?>> propMap, String user, Folder folder,
            List<String> policies, Acl addACEs, Acl removeACEs) {
        DocumentImpl doc = new DocumentImpl(this);
        doc.createSystemBasePropertiesWhenCreated(propMap, user);
        doc.setCustomProperties(propMap);
        doc.setRepositoryId(fRepositoryId);
        doc.setName(name);
        if (null != folder) {
            if (hasChild(folder, name))
                throw new CmisNameConstraintViolationException("Cannot create document an object with name " + name
                        + " already exists in folder " + folder.getPath());
            doc.addParentId(folder.getId());
        }
        int aclId = getAclId(((FolderImpl) folder), addACEs, removeACEs);
        doc.setAclId(aclId);
        if (null != policies)
            doc.setAppliedPolicies(policies);
        return doc;
    }

    @Override
    public StoredObject createItem(String name, Map<String, PropertyData<?>> propMap, String user, Folder folder,
            List<String> policies, Acl addACEs, Acl removeACEs) {
        ItemImpl item = new ItemImpl(this);
        item.createSystemBasePropertiesWhenCreated(propMap, user);
        item.setCustomProperties(propMap);
        item.setRepositoryId(fRepositoryId);
        item.setName(name);
        if (null != folder) {
            if (hasChild(folder, name))
                throw new CmisNameConstraintViolationException("Cannot create document an object with name " + name
                        + " already exists in folder " + folder.getPath());
            item.addParentId(folder.getId());
        }
        if (null != policies)
            item.setAppliedPolicies(policies);
        int aclId = getAclId(((FolderImpl) folder), addACEs, removeACEs);
        item.setAclId(aclId);
        return item;
    }

    @Override
    public DocumentVersion createVersionedDocument(String name, Map<String, PropertyData<?>> propMap, String user,
            Folder folder, List<String> policies, Acl addACEs, Acl removeACEs, ContentStream contentStream,
            VersioningState versioningState) {
        VersionedDocumentImpl doc = new VersionedDocumentImpl(this);
        doc.createSystemBasePropertiesWhenCreated(propMap, user);
        doc.setCustomProperties(propMap);
        doc.setRepositoryId(fRepositoryId);
        doc.setName(name);
        DocumentVersion version = doc.addVersion(contentStream, versioningState, user);
        version.createSystemBasePropertiesWhenCreated(propMap, user);
        version.setCustomProperties(propMap);
        if (null != folder) {
            if (hasChild(folder, name))
                throw new CmisNameConstraintViolationException("Cannot create document an object with name " + name
                        + " already exists in folder " + folder.getPath());
            doc.addParentId(folder.getId());
        }
        int aclId = getAclId(((FolderImpl) folder), addACEs, removeACEs);
        doc.setAclId(aclId);
        if (null != policies)
            doc.setAppliedPolicies(policies);
        doc.persist();
        return version;
    }

    @Override
    public Folder createFolder(String name, Map<String, PropertyData<?>> propMap, String user, Folder parent,
            List<String> policies, Acl addACEs, Acl removeACEs) {

        if (null != parent && hasChild(parent, name)) {
            throw new CmisNameConstraintViolationException("Cannot create folder, this name already exists in parent folder.");
        }
        FolderImpl folder = new FolderImpl(this, name, parent.getId());
        if (null != propMap) {
            folder.createSystemBasePropertiesWhenCreated(propMap, user);
            folder.setCustomProperties(propMap);
        }
        folder.setRepositoryId(fRepositoryId);

        int aclId = getAclId(((FolderImpl) parent), addACEs, removeACEs);
        folder.setAclId(aclId);
        if (null != policies)
            folder.setAppliedPolicies(policies);

        return folder;
    }

    public Folder createFolder(String name) {
        Folder folder = new FolderImpl(this, name, null);
        folder.setRepositoryId(fRepositoryId);
        return folder;
    }

    @Override
    public StoredObject createPolicy(String name, String policyText, Map<String, PropertyData<?>> propMap, String user) {
        PolicyImpl policy = new PolicyImpl(this);
        policy.createSystemBasePropertiesWhenCreated(propMap, user);
        policy.setCustomProperties(propMap);
        policy.setRepositoryId(fRepositoryId);
        policy.setName(name);
        policy.setPolicyText(policyText);
        policy.persist();
        return policy;
    }

    @Override
    public List<StoredObject> getCheckedOutDocuments(String orderBy, String user,
            IncludeRelationships includeRelationships) {
        List<StoredObject> res = new ArrayList<StoredObject>();

        for (StoredObject so : fStoredObjectMap.values()) {
            if (so instanceof VersionedDocument) {
                VersionedDocument verDoc = (VersionedDocument) so;
                if (verDoc.isCheckedOut() && hasReadAccess(user, verDoc)) {
                    res.add(verDoc.getPwc());
                }
            }
        }

        return res;
    }

    @Override
    public StoredObject createRelationship(String name, StoredObject sourceObject, StoredObject targetObject,
            Map<String, PropertyData<?>> propMap, String user, Acl addACEs, Acl removeACEs) {

        RelationshipImpl rel = new RelationshipImpl(this);
        rel.createSystemBasePropertiesWhenCreated(propMap, user);
        rel.setCustomProperties(propMap);
        rel.setRepositoryId(fRepositoryId);
        rel.setName(name);
        if (null != sourceObject)
            rel.setSource(sourceObject.getId());
        if (null != targetObject)
            rel.setTarget(targetObject.getId());
        int aclId = getAclId(null, addACEs, removeACEs);
        rel.setAclId(aclId);
        rel.persist();
        return rel;
    }

    @Override
    public List<StoredObject> getRelationships(String objectId, List<String> typeIds, RelationshipDirection direction) {

        List<StoredObject> res = new ArrayList<StoredObject>();

        if (typeIds != null && typeIds.size() > 0) {
            for (String typeId : typeIds) {
                for (StoredObject so : fStoredObjectMap.values()) {
                    if (so instanceof Relationship && so.getTypeId().equals(typeId)) {
                        Relationship ro = (Relationship) so;
                        if (ro.getSourceObjectId().equals(objectId)
                                && (RelationshipDirection.EITHER == direction || RelationshipDirection.SOURCE == direction)) {
                            res.add(so);
                        } else if (ro.getTargetObjectId().equals(objectId)
                                && (RelationshipDirection.EITHER == direction || RelationshipDirection.TARGET == direction)) {
                            res.add(so);
                        }
                    }
                }
            }
        } else
            res = getAllRelationships(objectId, direction);
        return res;
    }

    @Override
    public Acl applyAcl(StoredObject so, Acl addAces, Acl removeAces, AclPropagation aclPropagation, String principalId) {
        if (aclPropagation == AclPropagation.OBJECTONLY || !(so instanceof Folder)) {
            return applyAcl(so, addAces, removeAces);
        } else {
            return applyAclRecursive(((Folder) so), addAces, removeAces, principalId);
        }
    }

    @Override
    public Acl applyAcl(StoredObject so, Acl acl, AclPropagation aclPropagation, String principalId) {
        if (aclPropagation == AclPropagation.OBJECTONLY || !(so instanceof Folder)) {
            return applyAcl(so, acl);
        } else {
            return applyAclRecursive(((Folder) so), acl, principalId);
        }
    }

    public List<Integer> getAllAclsForUser(String principalId, Permission permission) {
        List<Integer> acls = new ArrayList<Integer>();
        for (InMemoryAcl acl : fAcls) {
            if (acl.hasPermission(principalId, permission))
                acls.add(acl.getId());
        }
        return acls;
    }

    public Acl getAcl(int aclId) {
        InMemoryAcl acl = getInMemoryAcl(aclId);
        return acl == null ? InMemoryAcl.getDefaultAcl().toCommonsAcl() : acl.toCommonsAcl();
    }

    public int getAclId(StoredObjectImpl so, Acl addACEs, Acl removeACEs) {
        InMemoryAcl newAcl;
        boolean removeDefaultAcl = false;
        int aclId = 0;

        if (so == null) {
            newAcl = new InMemoryAcl();
        } else {
            aclId = so.getAclId();
            newAcl = getInMemoryAcl(aclId);
            if (null == newAcl)
                newAcl = new InMemoryAcl();
            else
                // copy list so that we can safely change it without effecting
                // the original
                newAcl = new InMemoryAcl(newAcl.getAces());
        }

        if (newAcl.size() == 0 && addACEs == null && removeACEs == null)
            return 0;

        if (null != removeACEs)
            for (Ace ace : removeACEs.getAces()) {
                InMemoryAce inMemAce = new InMemoryAce(ace);
                if (inMemAce.equals(InMemoryAce.getDefaultAce()))
                    removeDefaultAcl = true;
            }

        if (so != null && 0 == aclId && !removeDefaultAcl)
            return 0; // if object grants full access to everyone and it will
                      // not be removed we do nothing

        // add ACEs
        if (null != addACEs)
            for (Ace ace : addACEs.getAces()) {
                InMemoryAce inMemAce = new InMemoryAce(ace);
                if (inMemAce.equals(InMemoryAce.getDefaultAce()))
                    return 0; // if everyone has full access there is no need to
                              // add additional ACLs.
                newAcl.addAce(inMemAce);
            }

        // remove ACEs
        if (null != removeACEs)
            for (Ace ace : removeACEs.getAces()) {
                InMemoryAce inMemAce = new InMemoryAce(ace);
                newAcl.removeAce(inMemAce);
            }

        if (newAcl.size() > 0)
            return addAcl(newAcl);
        else
            return 0;
    }

    private void deleteFolder(String folderId, String user) {
        StoredObject folder = fStoredObjectMap.get(folderId);
        if (folder == null) {
            throw new CmisInvalidArgumentException("Unknown object with id:  " + folderId);
        }

        if (!(folder instanceof FolderImpl)) {
            throw new CmisInvalidArgumentException("Cannot delete folder with id:  " + folderId
                    + ". Object exists but is not a folder.");
        }

        // check if children exist
        List<Fileable> children = getChildren((Folder) folder, -1, -1, user, true).getChildren();
        if (children != null && !children.isEmpty()) {
            throw new CmisConstraintException("Cannot delete folder with id:  " + folderId + ". Folder is not empty.");
        }

        fStoredObjectMap.remove(folderId);
    }

    @Override
    public ChildrenResult getChildren(Folder folder, int maxItems, int skipCount, String user, boolean usePwc) {
        List<Fileable> children = getChildren(folder, user, usePwc);
        sortFolderList(children);

        if (maxItems < 0) {
            maxItems = children.size();
        }
        if (skipCount < 0) {
            skipCount = 0;
        }

        int from = Math.min(skipCount, children.size());
        int to = Math.min(maxItems + from, children.size());
        int noItems = children.size();

        children = children.subList(from, to);
        return new ChildrenResult(children, noItems);
    }

    private List<Fileable> getChildren(Folder folder) {
        return getChildren(folder, null, true);
    }

    private List<Fileable> getChildren(Folder folder, String user, boolean usePwc) {
        List<Fileable> children = new ArrayList<Fileable>();
        for (String id : getIds()) {
            StoredObject obj = getObject(id);
            if (obj instanceof Fileable) {
                Fileable pathObj = (Fileable) obj;
                if ((null == user || hasReadAccess(user, obj)) && pathObj.getParents().contains(folder.getId())) {
                    if (pathObj instanceof VersionedDocument) {
                    	DocumentVersion ver;
                    	if (usePwc) {
                    		ver = ((VersionedDocument) pathObj).getPwc();
                    		if (null == ver)
                    			ver = ((VersionedDocument) pathObj).getLatestVersion(false);
                    	} else {
                    		ver = ((VersionedDocument) pathObj).getLatestVersion(false);
                    	}
                    	children.add(ver);
                    } else if (pathObj instanceof DocumentVersion) {
                        // ignore
                    } else {
                    	children.add(pathObj);
                    }
                    
                }
            }
        }
        return children;
    }

    @Override
    public ChildrenResult getFolderChildren(Folder folder, int maxItems, int skipCount, String user) {
        List<Fileable> folderChildren = new ArrayList<Fileable>();
        for (String id : getIds()) {
            StoredObject obj = getObject(id);
            if (hasReadAccess(user, obj) && obj instanceof Folder) {
                Folder childFolder = (Folder) obj;
                if (childFolder.getParents().contains(folder.getId()) ) {
                    folderChildren.add(childFolder);
                }
            }
        }
        sortFolderList(folderChildren);
        int from = Math.min(skipCount, folderChildren.size());
        int to = Math.min(maxItems + from, folderChildren.size());
        int noItems = folderChildren.size();

        folderChildren = folderChildren.subList(from, to);

        return new ChildrenResult(folderChildren, noItems);
    }

    @Override
    public void move(StoredObject so, Folder oldParent, Folder newParent) {
        try {
            if (hasChild(newParent, so.getName())) {
                throw new CmisInvalidArgumentException("Cannot move object " + so.getName() + " to folder "
                        + newParent.getPath() + ". A child with this name already exists.");
            }
            lock();
            if (so instanceof FilingMutable) {
                FilingMutable fi = (FilingMutable) so;
                addParentIntern(fi, newParent);
                removeParentIntern(fi, oldParent);
            } else if (so instanceof FolderImpl) {
                ((FolderImpl) so).setParentId(newParent.getId());
            }
        } finally {
            unlock();
        }
    }

    @Override
    public void rename(Fileable so, String newName) {
        try {
            lock();
            if (so.getId().equals(fRootFolder.getId())) {
                throw new CmisInvalidArgumentException("Root folder cannot be renamed.");
            }
            for (String folderId : so.getParents()) {
                Folder folder = (Folder) getObjectById(folderId);
                if (hasChild(folder, newName))
                    throw new CmisNameConstraintViolationException("Cannot rename object to " + newName
                            + ". This path already exists in parent " + folder.getPath() + ".");
            }
            so.setName(newName);
        } finally {
            unlock();
        }
    }

    private boolean hasChild(Folder folder, String name) {
        List<Fileable> children = getChildren(folder);
        for (Fileable child : children)
            if (child.getName().equals(name)) {
                return true;
            }
        return false;
    }

    @Override
    public List<String> getParentIds(Filing fileable, String user) {
        List<String> visibleParents = new ArrayList<String>(); 
        List<String> parents = fileable.getParents();
        for (String id: parents) {
            StoredObject so = getObjectById(id);
            if (hasReadAccess(user, so)) {
                visibleParents.add(id);
            }
        }
        return visibleParents;
    }

    public boolean hasReadAccess(String principalId, StoredObject so) {
        return hasAccess(principalId, so, Permission.READ);
    }

    public boolean hasWriteAccess(String principalId, StoredObject so) {
        return hasAccess(principalId, so, Permission.WRITE);
    }

    public boolean hasAllAccess(String principalId, StoredObject so) {
        return hasAccess(principalId, so, Permission.ALL);
    }

    public void checkReadAccess(String principalId, StoredObject so) {
        checkAccess(principalId, so, Permission.READ);
    }

    public void checkWriteAccess(String principalId, StoredObject so) {
        checkAccess(principalId, so, Permission.WRITE);
    }

    public void checkAllAccess(String principalId, StoredObject so) {
        checkAccess(principalId, so, Permission.ALL);
    }

    private void checkAccess(String principalId, StoredObject so, Permission permission) {
        if (!hasAccess(principalId, so, permission))
            throw new CmisPermissionDeniedException("Object with id " + so.getId() + " and name " + so.getName()
                    + " does not grant " + permission.toString() + " access to principal " + principalId);
    }

    private boolean hasAccess(String principalId, StoredObject so, Permission permission) {
        if (null != principalId && principalId.equals(ADMIN_PRINCIPAL_ID))
            return true;
        List<Integer> aclIds = getAllAclsForUser(principalId, permission);
        return aclIds.contains(((StoredObjectImpl) so).getAclId());
    }

    private InMemoryAcl getInMemoryAcl(int aclId) {

        for (InMemoryAcl acl : fAcls) {
            if (aclId == acl.getId())
                return acl;
        }
        return null;
    }

    private int setAcl(StoredObjectImpl so, Acl acl) {
        int aclId;
        if (null == acl || acl.getAces().isEmpty())
            aclId = 0;
        else {
            aclId = getAclId(null, acl, null);
        }
        so.setAclId(aclId);
        return aclId;
    }

    /**
     * check if an Acl is already known
     * 
     * @param acl
     *            acl to be checked
     * @return 0 if Acl is not known, id of Acl otherwise
     */
    private int hasAcl(InMemoryAcl acl) {
        for (InMemoryAcl acl2 : fAcls) {
            if (acl2.equals(acl))
                return acl2.getId();
        }
        return -1;
    }

    private int addAcl(InMemoryAcl acl) {
        int aclId = -1;

        if (null == acl)
            return 0;

        lock();
        try {
            aclId = hasAcl(acl);
            if (aclId < 0) {
                aclId = getNextAclId();
                acl.setId(aclId);
                fAcls.add(acl);
            }
        } finally {
            unlock();
        }
        return aclId;
    }

    private Acl applyAcl(StoredObject so, Acl acl) {
        int aclId = setAcl((StoredObjectImpl) so, acl);
        return getAcl(aclId);
    }

    private Acl applyAcl(StoredObject so, Acl addAces, Acl removeAces) {
        int aclId = getAclId((StoredObjectImpl) so, addAces, removeAces);
        ((StoredObjectImpl) so).setAclId(aclId);
        return getAcl(aclId);
    }

    private Acl applyAclRecursive(Folder folder, Acl addAces, Acl removeAces, String principalId) {
        List<Fileable> children = getChildren(folder, -1, -1, ADMIN_PRINCIPAL_ID, true).getChildren();
        Acl result = applyAcl(folder, addAces, removeAces);

        if (null == children) {
            return result;
        }

        for (Fileable child : children) {
            if (hasAllAccess(principalId, child)) {
                if (child instanceof Folder) {
                    applyAclRecursive((Folder) child, addAces, removeAces, principalId);
                } else {
                    applyAcl(child, addAces, removeAces);
                }
            }
        }

        return result;
    }

    private Acl applyAclRecursive(Folder folder, Acl acl, String principalId) {
        List<Fileable> children = getChildren(folder, -1, -1, ADMIN_PRINCIPAL_ID, true).getChildren();

        Acl result = applyAcl(folder, acl);

        if (null == children) {
            return result;
        }

        for (Fileable child : children) {
            if (hasAllAccess(principalId, child)) {
                if (child instanceof Folder) {
                    applyAclRecursive((Folder) child, acl, principalId);
                } else {
                    applyAcl(child, acl);
                }
            }
        }

        return result;
    }

    private List<StoredObject> getAllRelationships(String objectId, RelationshipDirection direction) {

        List<StoredObject> res = new ArrayList<StoredObject>();

        for (StoredObject so : fStoredObjectMap.values()) {
            if (so instanceof Relationship) {
                Relationship ro = (Relationship) so;
                if (ro.getSourceObjectId().equals(objectId)
                        && (RelationshipDirection.EITHER == direction || RelationshipDirection.SOURCE == direction)) {
                    res.add(so);
                } else if (ro.getTargetObjectId().equals(objectId)
                        && (RelationshipDirection.EITHER == direction || RelationshipDirection.TARGET == direction)) {
                    res.add(so);
                }
            }
        }
        return res;
    }

    @Override
    public boolean isTypeInUse(String typeId) {
        // iterate over all the objects and check for each if the type matches
        for (String objectId : getIds()) {
            StoredObject so = getObjectById(objectId);
            if (so.getTypeId().equals(typeId))
                return true;
        }
        return false;
    }

    @Override
    public void addParent(StoredObject so, Folder parent) {
        try {
            lock();
            if (hasChild(parent, so.getName())) {
                throw new IllegalArgumentException(
                        "Cannot assign new parent folder, this name already exists in target folder.");
            }
            FilingMutable fi;
            if (so instanceof FilingMutable) 
                fi = (FilingMutable) so;
            else
                throw new IllegalArgumentException("Object " + so.getId() + "is not fileable");
                
            addParentIntern(fi, parent);
        } finally {
            unlock();
        }
    }

    @Override
    public void removeParent(StoredObject so, Folder parent) {
        try {
            lock();
            FilingMutable fi;
            if (so instanceof FilingMutable) 
                fi = (FilingMutable) so;
            else
                throw new IllegalArgumentException("Object " + so.getId() + "is not fileable");

            removeParentIntern(fi, parent);
        } finally {
            unlock();
        }
    }

    private void addParentIntern(FilingMutable so, Folder parent) {
        so.addParentId(parent.getId());
    }

    private void removeParentIntern(FilingMutable so, Folder parent) {
        so.removeParentId(parent.getId());
    }

    private static void sortFolderList(List<? extends StoredObject> list) {
        // TODO evaluate orderBy, for now sort by path segment
        class FolderComparator implements Comparator<StoredObject> {

            @Override
            public int compare(StoredObject f1, StoredObject f2) {
                String segment1 = f1.getName();
                String segment2 = f2.getName();

                return segment1.compareTo(segment2);
            }
        }

        Collections.sort(list, new FolderComparator());
    }

}
