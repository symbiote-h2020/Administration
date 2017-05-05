package eu.h2020.symbiote.model;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import eu.h2020.symbiote.core.model.Platform;

/**
 * Class used as a response to RPC call requesting platform actions
 *
 * Created by mateuszl
 */
public class PlatformResponse {
    private int status;
    private String message;
    private Platform platform;

    public PlatformResponse() {
    }

    public PlatformResponse(int status, String message, Platform platform) {
        this.status = status;
        this.message = message;
        this.platform = platform;
    }

    /**
     * @return
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * @param platform
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    /**
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
