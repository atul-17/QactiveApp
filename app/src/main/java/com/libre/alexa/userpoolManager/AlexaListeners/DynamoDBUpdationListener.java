package com.libre.alexa.userpoolManager.AlexaListeners;

import android.os.Bundle;

/**
 * Created by bhargav on 28/6/17.
 */

public interface DynamoDBUpdationListener {
    public void itemUpdated(Bundle bundle);
    public void itemUpdationFailed(Bundle bundle);
}
