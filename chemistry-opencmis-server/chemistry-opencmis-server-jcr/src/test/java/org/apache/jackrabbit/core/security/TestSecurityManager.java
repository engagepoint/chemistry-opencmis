package org.apache.jackrabbit.core.security;

import java.lang.reflect.Field;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.simple.SimpleSecurityManager;
import org.apache.jackrabbit.core.security.user.UserImpl;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;

/**
 * Provides access to the observation journal. It represents current user as administrator.  
 * The reason to use one is SimpleSecurityManager doesn't support UserManager of a session.
 * 
 * @author sergey.nikolaew@engagepoint.com
 * @date Oct 11, 2012
 */
public class TestSecurityManager extends SimpleSecurityManager {

	private class MockAdmin extends UserImpl {
		
		protected MockAdmin(UserManagerImpl userManager) {
			super(null, userManager);
		}
		
		public boolean isGroup() {
			return false;
		}
		
		public boolean isDisabled() throws RepositoryException {
			return false;
		}
		
		public boolean isAdmin() {
			return true;
		}
		
	};
	
	private class MockUserManager extends UserManagerImpl {

		public MockUserManager() throws RepositoryException {
			super((SessionImpl) getSystemSession(), "admin");
		}
		
		public Authorizable getAuthorizable(String id) throws RepositoryException {
			return new MockAdmin(this);
		}
	}

	@Override
	public UserManager getUserManager(Session session) throws RepositoryException {
		return new MockUserManager();
	}
	
	protected Session getSystemSession() {
		try {
			Field sessionField;
			sessionField = SimpleSecurityManager.class.getDeclaredField("systemSession");
			sessionField.setAccessible(true);
			return (Session) sessionField.get(this);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		} 
	}
}
