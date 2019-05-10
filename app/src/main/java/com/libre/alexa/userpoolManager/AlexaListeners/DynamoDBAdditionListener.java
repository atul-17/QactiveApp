package com.libre.alexa.userpoolManager.AlexaListeners;

import android.os.Bundle;

/**
 * Created by bhargav on 22/6/17.
 */
public interface DynamoDBAdditionListener {
    public void onItemAdded(Bundle bundle);
    public void onItemAdditionFailed(Bundle bundle);
}
