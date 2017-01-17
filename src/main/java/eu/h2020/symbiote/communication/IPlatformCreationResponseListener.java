package eu.h2020.symbiote.communication;

import eu.h2020.symbiote.model.PlatformCreationResponse;

/**
 * Interface used as a response listener for RPC Rabbit communication
 */
public interface IPlatformCreationResponseListener {
    void onPlatformCreationResponseReceive(PlatformCreationResponse platformCreationResponse);
}
