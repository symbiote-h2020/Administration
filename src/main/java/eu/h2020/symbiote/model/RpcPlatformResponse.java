package eu.h2020.symbiote.model;

/**
 * POJO used as a response to RPC call requesting platform operation.
 * It consists of Platform object and standard HTTP status code as an integer.
 */
public class RpcPlatformResponse {
    private int status;
    private String message;
    private Platform platform;

    /**
     * Default empty constructor.
     */
    public RpcPlatformResponse() {
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
                ", message=" + message +
                '}';
    }
}
