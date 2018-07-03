package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class ChangePermissions {

    @NotNull
    private final boolean analyticsAndResearchConsent;

    @JsonCreator
    public ChangePermissions(@JsonProperty("analyticsAndResearchConsent") boolean analyticsAndResearchConsent) {
        this.analyticsAndResearchConsent = analyticsAndResearchConsent;
    }

    public boolean isAnalyticsAndResearchConsent() { return analyticsAndResearchConsent; }
}
