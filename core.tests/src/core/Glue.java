package core;

import sneer.admin.*;
import clojure.java.api.*;
import clojure.lang.*;

class Glue {

	public static SneerAdmin newSneerAdmin(Object network) {
		try {
			return (SneerAdmin) sneerCoreVar("new-sneer-admin").invoke(network);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static Object newNetwork() {
		try {
			return var("core.network-simulator", "new-network").call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static void tearDownNetwork(Object network) {
		var("core.network-simulator", "stop-network").invoke(network);
	}
	
	private static IFn sneerCoreVar(String simpleName) {
		return var("sneer.core", simpleName);
	}

	private static IFn var(String ns, String simpleName) {
		Clojure.var("clojure.core/require").invoke(Clojure.read(ns));
		return Clojure.var(ns + "/" + simpleName);
	}	
}
