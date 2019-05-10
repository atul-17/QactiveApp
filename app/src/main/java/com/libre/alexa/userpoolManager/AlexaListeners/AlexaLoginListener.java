package com.libre.alexa.userpoolManager.AlexaListeners;

/**
 * Created by bhargav on 20/6/17.
 */
public interface AlexaLoginListener {
    public void loginSuccessful(String username);
    public void loginFailed(String Error);
}
