package sneer.core.tests;

import static sneer.ClojureUtils.var;

public class PubSubOverLocalServerNetwork extends PubSubTest {

	@Override
	protected Object newNetwork() {
		return var("sneer.core.tests.local-server-network", "start").invoke();
	}
}
