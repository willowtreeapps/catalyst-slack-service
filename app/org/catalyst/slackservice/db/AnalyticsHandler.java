package org.catalyst.slackservice.db;

public interface AnalyticsHandler {
    void incrementMessageCounts(AnalyticsKey key);
    void incrementIgnoredMessageCounts(AnalyticsKey key);
    void incrementLearnMoreMessageCounts(AnalyticsKey key);
    void incrementCorrectedMessageCounts(AnalyticsKey key);
    void setTeamName(String teamId, String teamName);
}
