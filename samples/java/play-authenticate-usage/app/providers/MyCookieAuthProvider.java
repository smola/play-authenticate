package providers;

import java.util.Date;

import models.LinkedAccount;
import play.Application;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.feth.play.module.pa.providers.cookie.CookieAuthProvider;
import com.feth.play.module.pa.providers.cookie.CookieAuthUser;

public class MyCookieAuthProvider extends CookieAuthProvider {

	public MyCookieAuthProvider(final Application app) {
		super(app);
	}
	
	@Override
	protected CheckResult check(final CookieAuthUser cookieAuthUser) {
		final LinkedAccount la = LinkedAccount.find.where(Expr.and(
				Expr.eq("provider_key", PROVIDER_KEY),
				Expr.eq("provider_user_id", cookieAuthUser.getSeries())
				)).setMaxRows(1).findUnique();
		
		if (la == null) {
			return CheckResult.MISSING_SERIES;
		}
		
		if (!la.token.equals(cookieAuthUser.getToken())) {
			return CheckResult.INVALID_TOKEN;
		}
		
		if ((new Date().getTime() - la.created.getTime()) > getTimeout()*1000L) {
			return CheckResult.EXPIRED;
		}
		
		return CheckResult.SUCCESS;
	}

	@Override
	protected void renew(final CookieAuthUser cookieAuthUser, final String newToken) {
		Ebean.createSqlUpdate("UPDATE linked_account SET token = :new_token WHERE provider_key = :provider_key AND provider_user_id = :series")
			.setParameter("provider_key", CookieAuthProvider.PROVIDER_KEY)
			.setParameter("series", cookieAuthUser.getSeries())
			.setParameter("new_token", newToken)
			.execute();
	}
		
}
