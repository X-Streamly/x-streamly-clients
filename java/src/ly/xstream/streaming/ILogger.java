package ly.xstream.streaming;

public interface ILogger {
	void log(String s);
	void handleError(String message,Exception e);
}
