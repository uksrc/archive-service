package org.uksrc.archive.utils.tools;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

/**
 * Bean encapsulator for the YAUAA analyzer utility.
 */
@ApplicationScoped
public class UserAgentAnalyzerProducer {

    @Produces
    @ApplicationScoped
    public UserAgentAnalyzer createUserAgentAnalyzer() {
        return UserAgentAnalyzer
                .newBuilder()
                .withCache(1000)
                .withField("AgentClass")
                .build();
    }
}
