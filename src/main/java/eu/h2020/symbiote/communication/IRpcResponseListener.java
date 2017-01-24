package eu.h2020.symbiote.communication;

import eu.h2020.symbiote.model.RpcPlatformResponse;

/**
 * Interface used as a response listener for RPC Rabbit communication
 */
public interface IRpcResponseListener {
    void onRpcResponseReceive(RpcPlatformResponse rpcPlatformResponse);
}
