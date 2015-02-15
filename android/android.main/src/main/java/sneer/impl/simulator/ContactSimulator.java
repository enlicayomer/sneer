package sneer.impl.simulator;

import sneer.Contact;
import sneer.Party;
import sneer.rx.Observed;
import sneer.rx.ObservedSubject;

public class ContactSimulator implements Contact {

	private final Party party;
	private final ObservedSubject<String> nickname;

	
	ContactSimulator(String nickname, Party party) {
		this.nickname = ObservedSubject.create(nickname);
		this.party = party;
	}


	@Override
	public Party party() {
		return party;
	}


	@Override
	public Observed<String> nickname() {
		return nickname.observed();
	}


	@Override
	public void setNickname(String newNickname) {
		nickname.onNext(newNickname);
	}

}