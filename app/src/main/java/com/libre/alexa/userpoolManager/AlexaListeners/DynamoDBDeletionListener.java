package com.libre.alexa.userpoolManager.AlexaListeners;

import android.os.Bundle;

/**
 * Created by bhargav on 22/6/17.
 */
public interface DynamoDBDeletionListener {
    public void itemDeleted(Bundle bundle);
    public void itemDeletionFailed(Bundle bundle);
}
