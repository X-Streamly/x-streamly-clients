package ly.xstream.streaming;

public enum ConnectionStates {
	connecting,
	awaitingSecurityClearence,
	connected,
	disconnected,
	failed,
	securityCheckFailed,
}
