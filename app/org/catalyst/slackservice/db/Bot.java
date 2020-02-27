package org.catalyst.slackservice.db;

public class Bot {
    public String token;
    public String userId;
    public String teamName;

    public Bot() {

    }

    public Bot(String token, String userId, String teamName) {
        this.token = token;
        this.userId = userId;
        this.teamName = teamName;
    }
}
