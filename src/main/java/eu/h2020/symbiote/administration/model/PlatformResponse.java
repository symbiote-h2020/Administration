package eu.h2020.symbiote.administration.model;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import eu.h2020.symbiote.core.model.Platform;

/**
 * Class used as a response to RPC call requesting platform actions
 *
 * @author Mateusz Lukaszenko (PSNC)
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
public class PlatformResponse {

    /* -------- Properties -------- */

    private int status;
    private String message;
    private Platform platform;


    /* -------- Constructors -------- */

    /**
     * Empty constructor
     */
    public PlatformResponse() {
    }

    /**
     * Constructor with properties
     *
     * @param status    response status (HTTP)
     * @param message   response message
     * @param platform  response platform object
     */
    public PlatformResponse(int status, String message, Platform platform) {
        this.status = status;
        this.message = message;
        this.platform = platform;
    }


    /* -------- Getters & Setters -------- */

    /**
     * @return status
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
     * @return platform
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
     * @return message
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
