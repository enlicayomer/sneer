package sneer.convos;

import java.util.List;

import rx.Observable;
import sneer.flux.Action;
import sneer.rx.Timeline;

import static sneer.flux.Action.action;

public interface Sessions {

	Timeline<SessionMessage> messages(long sessionId);

	class Actions {
		public static Action sendMessage(long sessionId, Object payload) {
			return action("send-session-message", "session-id", sessionId, "payload", payload);
		}
	}
}
