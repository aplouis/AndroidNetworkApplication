package com.fogoa.networkapplication.network.listeners;


import com.fogoa.networkapplication.network.models.NetworkRequestResult;

public interface AsyncNetworkThread {
    void OnRequestComplete(NetworkRequestResult resp);
}
