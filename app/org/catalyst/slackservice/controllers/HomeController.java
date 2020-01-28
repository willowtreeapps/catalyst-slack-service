package org.catalyst.slackservice.controllers;

import com.typesafe.config.Config;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Map;

/**
 * This controller handles the health check endpoint
 */
public class HomeController extends Controller {

    private Config _config;

    @Inject
    public HomeController(Config config) {
        _config = config;
    }

    public Result index() {
        var sanitizedConfigurationInformation = Map.of(
                "version", _config.getString("version"),
                "environment", _config.getString("environment")
        );

        var response = Map.of(
                _config.getString("service_name"), sanitizedConfigurationInformation
        );
        return ok(Json.toJson(response));
    }

}
