package ee.sk.siddemo.services;

public interface AsyncCallback<T> {

    void onComplete(T result, Throwable error);
}
