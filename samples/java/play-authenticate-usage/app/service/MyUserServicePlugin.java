package service;

import models.User;
import play.Application;

import com.avaje.ebean.Ebean;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.providers.cookie.CookieAuthUser;
import com.feth.play.module.pa.service.UserServicePlugin;

public class MyUserServicePlugin extends UserServicePlugin {

	public MyUserServicePlugin(final Application app) {
		super(app);
	}

	@Override
	public Object save(final AuthUser authUser) {
		final boolean isLinked = User.existsByAuthUserIdentity(authUser);
		if (!isLinked) {
			return User.create(authUser).id;
		} else {
			// we have this user already, so return null
			return null;
		}
	}

	@Override
	public Object getLocalIdentity(final AuthUserIdentity identity) {
		// For production: Caching might be a good idea here...
		// ...and dont forget to sync the cache when users get deactivated/deleted
		final User u = User.findByAuthUserIdentity(identity);
		if(u != null) {
			return u.id;
		} else {
			return null;
		}
	}

	@Override
	public AuthUser merge(final AuthUser newUser, final AuthUser oldUser) {
		if (!oldUser.equals(newUser)) {
			User.merge(oldUser, newUser);
		}
		return oldUser;
	}

	@Override
	public AuthUser link(final AuthUser oldUser, final AuthUser newUser) {
		User.addLinkedAccount(oldUser, newUser);
		return newUser;
	}
	
	@Override
	public void unlink(final AuthUser unlinkUser) {
		if (unlinkUser instanceof CookieAuthUser) {
			Ebean.createSqlUpdate("DELETE FROM linked_account WHERE provider_key = :provider AND provider_user_id = :id")
				.setParameter("provider", unlinkUser.getProvider())
				.setParameter("id", unlinkUser.getId())
				.execute();
			return;
		}
		throw new UnsupportedOperationException("unlink is not supported for provider: " + unlinkUser.getProvider());
	}
	
	@Override
	public AuthUser update(final AuthUser knownUser) {
		// User logged in again, bump last login date
		User.setLastLoginDate(knownUser);
		return knownUser;
	}

}
