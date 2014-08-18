package sneer.core.tests;

import org.junit.*;

import rx.subjects.*;
import sneer.*;
import sneer.admin.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class ConversationsAPITestsBase {

//	private final TuplesFactoryInProcess world = new TuplesFactoryInProcess();
	private final Object network = Glue.newNetwork();
	
	protected final SneerAdmin adminA = newSneerAdmin();
	protected final SneerAdmin adminB = newSneerAdmin();
	protected final SneerAdmin adminC = newSneerAdmin();

	protected final PublicKey userA = adminA.sneer().self().publicKey().current();
	protected final PublicKey userB = adminB.sneer().self().publicKey().current();
	protected final PublicKey userC = adminC.sneer().self().publicKey().current();
	
	protected final Sneer sneerA = adminA.sneer();
	protected final Sneer sneerB = adminB.sneer();
	protected final Sneer sneerC = adminC.sneer();

	protected PrivateKey newPrivateKey() {
		return Keys.createPrivateKey();
	}
	
	@After
	public void closeNetwork() {
		Glue.tearDownNetwork(network);
	}

	private SneerAdmin newSneerAdmin() {
		PrivateKey prik = Keys.createPrivateKey();
		TupleSpace tupleSpace = Glue.newTupleSpace(prik.publicKey(), PublishSubject.<PublicKey>create(), network);
//		TupleSpace tupleSpace = world.newTupleSpace(prik);
//		SneerAdminImpl admin = new SneerAdminImpl(tupleSpace, prik);
		return (SneerAdmin) Glue.var("sneer.admin", "new-sneer-admin").invoke(tupleSpace, prik);
	}

}