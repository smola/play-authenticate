import java.lang.reflect.Method;
import java.util.Arrays;

import models.SecurityRole;
import play.Application;
import play.GlobalSettings;
import play.mvc.Action;
import play.mvc.Call;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.PlayAuthenticate.Resolver;
import com.feth.play.module.pa.exceptions.AccessDeniedException;
import com.feth.play.module.pa.exceptions.AuthException;
import com.feth.play.module.pa.providers.cookie.CookieAuthProvider;
import com.feth.play.module.pa.user.AuthUser;

import controllers.routes;

public class Global extends GlobalSettings {

	public void onStart(Application app) {
		PlayAuthenticate.setResolver(new Resolver() {

			@Override
			public Call login() {
				// Your login page
				return routes.Application.login();
			}

			@Override
			public Call afterAuth() {
				// The user will be redirected to this page after authentication
				// if no original URL was saved
				return routes.Application.index();
			}

			@Override
			public Call afterLogout() {
				return routes.Application.index();
			}

			@Override
			public Call auth(final String provider) {
				// You can provide your own authentication implementation,
				// however the default should be sufficient for most cases
				return com.feth.play.module.pa.controllers.routes.Authenticate
						.authenticate(provider);
			}

			@Override
			public Call askMerge() {
				return routes.Account.askMerge();
			}

			@Override
			public Call askLink() {
				return routes.Account.askLink();
			}

			@Override
			public void onAuthSuccess(final Context ctx, final AuthUser authUser) {
				// Comment this if you want to disable Remember Me cookies.
				if (CookieAuthProvider.getCookieProvider() != null) {
					CookieAuthProvider.getCookieProvider().remember(ctx, authUser);
				}
				
			}
			
			@Override
			public Call onException(final AuthException e) {
				if (e instanceof AccessDeniedException) {
					return routes.Signup
							.oAuthDenied(((AccessDeniedException) e)
									.getProviderKey());
				}

				// more custom problem handling here...
				return super.onException(e);
			}
		});

		initialData();
	}
	
	// Comment this if you want to disable Remember Me cookies
	@SuppressWarnings("rawtypes")
	@Override    
	public Action onRequest(Request request, Method actionMethod) {
		return new Action.Simple() {
			public Result call(Context ctx) throws Throwable {
				if (!PlayAuthenticate.isLoggedIn(ctx.session())) {
					CookieAuthProvider.getCookieProvider().authenticate(ctx);
				}
				return delegate.call(ctx);
			}
		};
	}


	private void initialData() {
		if (SecurityRole.find.findRowCount() == 0) {
			for (final String roleName : Arrays
					.asList(controllers.Application.USER_ROLE)) {
				final SecurityRole role = new SecurityRole();
				role.roleName = roleName;
				role.save();
			}
		}
	}
}
