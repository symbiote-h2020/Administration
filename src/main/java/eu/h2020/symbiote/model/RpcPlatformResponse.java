package eu.h2020.symbiote.model;

/**
 * Class used as a response to RPC call requesting platform operation
 */
public class RpcPlatformResponse {
    private int status;
    private Platform platform;

    public RpcPlatformResponse() {
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    @Override
    public String toString() {
        return "RpcPlatformResponse{" +
                "status=" + status +
                ", platform=" + platform +
                '}';
    }
}
