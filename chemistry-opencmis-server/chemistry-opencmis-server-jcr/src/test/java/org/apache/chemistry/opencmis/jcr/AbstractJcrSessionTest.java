package org.apache.chemistry.opencmis.jcr;

import javax.jcr.AccessDeniedException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.jcr.impl.DefaultDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultFolderTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultUnversionedDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.Before;

/**
 * Abstract class for the test cases are covering JCR compliance.
 * The Jackrabbit's TransientRepository class uses as content repository.   
 * 
 * @author sergey.nikolaew@engagepoint.com
 * @date Sep 17, 2012
 */

public abstract class AbstractJcrSessionTest {

	private Repository transientRepository;
	private JcrRepository jcrRepository;
	private Session session;
	private JcrTypeManager typeManager;
	private static final String MOUNT_PATH = "/";
	
	
	@Before
	public void setUp() throws Exception {
		transientRepository = new TransientRepository();
		session = transientRepository.login(new SimpleCredentials("adminId", "admin".toCharArray()));
		typeManager = new JcrTypeManager();
		PathManager pathManger = new PathManager(MOUNT_PATH);
        JcrTypeHandlerManager typeHandlerManager = createTypeHandlerManager(pathManger, typeManager);
        jcrRepository = new JcrRepository(transientRepository, pathManger, typeManager, typeHandlerManager);
	}

	@After
	public void tearDown() throws Exception {
		((TransientRepository) transientRepository).shutdown();
	}
	
	private JcrTypeHandlerManager createTypeHandlerManager(PathManager pathManager, JcrTypeManager typeManager) {
        JcrTypeHandlerManager typeHandlerManager = new JcrTypeHandlerManager(pathManager, typeManager);
        typeHandlerManager.addHandler(new DefaultFolderTypeHandler());
        typeHandlerManager.addHandler(new DefaultDocumentTypeHandler());
        typeHandlerManager.addHandler(new DefaultUnversionedDocumentTypeHandler());
        return typeHandlerManager;
	}

	protected Session getSession() {
		try {
			//initialization of the UserManager
			((JackrabbitSession) session).getUserManager();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		} 
		return session;
	}

	protected JcrRepository getJcrRepository() {
		return jcrRepository;
	}
	
	protected ObjectData getRootFolder(){
		return getJcrRepository().getObjectByPath(getSession(),
				MOUNT_PATH, null, false, false, null, false);
	}
}
