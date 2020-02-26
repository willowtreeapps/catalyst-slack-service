package org.catalyst.slackservice.services;

import org.catalyst.slackservice.domain.Action;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(Theories.class)
public class AnalyticsEventTest {

    @Test
    public void testBiasMatchCreateMessageEvent() {
        var key = new AnalyticsKey("trackingId", "channelId", "userId");
        var trigger = "trigger";

        var expected = new HashMap<String, String>() {{
            put("v", "1");
            put("t", "event");
            put("tid", "trackingId");
            put("cid", "userId");
            put("ec", "channelId");
            put("ea", "Message - Bias Match");
            put("el", "");
        }};

        var result = AnalyticsEvent.createMessageEvent(key, trigger).getMap();

        Assert.assertEquals(expected, result);
    }

    @DataPoints
    public static String[] invalidTriggers = {null, ""};

    @Test
    @Theory
    public void testNoActionCreateMessageEvent(String trigger) {
        var key = new AnalyticsKey("trackingId", "channelId", "userId");

        var expected = new HashMap<String, String>() {{
            put("v", "1");
            put("t", "event");
            put("tid", "trackingId");
            put("cid", "userId");
            put("ec", "channelId");
            put("ea", "Message - No Action");
            put("el", "");
        }};

        var result = AnalyticsEvent.createMessageEvent(key, trigger).getMap();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testYesCreateMessageActionEvent() {
        var key = new AnalyticsKey("trackingId", "channelId", "userId");
        var action = new Action();
        action.value = Action.YES.toString();

        var expected = new HashMap<String, String>() {{
            put("v", "1");
            put("t", "event");
            put("tid", "trackingId");
            put("cid", "userId");
            put("ec", "channelId");
            put("ea", "User - Applied Suggestion");
            put("el", "");
        }};

        var result = AnalyticsEvent.createMessageActionEvent(key, action).getMap();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testNoCreateMessageActionEvent() {
        var key = new AnalyticsKey("trackingId", "channelId", "userId");;
        var action = new Action();
        action.value = Action.NO.toString();

        var expected = new HashMap<String, String>() {{
            put("v", "1");
            put("t", "event");
            put("tid", "trackingId");
            put("cid", "userId");
            put("ec", "channelId");
            put("ea", "User - Rejected Suggestion");
            put("el", "");
        }};

        var result = AnalyticsEvent.createMessageActionEvent(key, action).getMap();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testLearnMoreCreateMessageActionEvent() {
        var key = new AnalyticsKey("trackingId", "channelId", "userId");
        var action = new Action();
        action.value = Action.LEARN_MORE.toString();

        var expected = new HashMap<String, String>() {{
            put("v", "1");
            put("t", "event");
            put("tid", "trackingId");
            put("cid", "userId");
            put("ec", "channelId");
            put("ea", "User - Clicked Learn More");
            put("el", "");
        }};

        var result = AnalyticsEvent.createMessageActionEvent(key, action).getMap();

        Assert.assertEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCreateMessageActionEvent() {
        var key = new AnalyticsKey("trackingId", "channelId", "userId");
        var action = new Action(); // specify no value

        var expected = new HashMap<String, String>() {{
            put("v", "1");
            put("t", "event");
            put("tid", "trackingId");
            put("cid", "userId");
            put("ec", "channelId");
            put("ea", "User - Clicked Learn More");
            put("el", "");
        }};

        var result = AnalyticsEvent.createMessageActionEvent(key, action);

        // should never reach this statement because createMessageAction should throw an exception
        Assert.assertEquals(true, false);
    }
}
