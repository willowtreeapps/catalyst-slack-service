# catalyst-slack-service

A microservice that integrates Slack and [Bias Correction Service](https://github.com/willowtreeapps/catalyst-bias-correct-service). This service subscribes to message events on Slack and utilizes the Bias Correction Service for potential suggestions to replace biased terms.

## Prerequisites
* [JDK 12](https://www.oracle.com/java/technologies/javase/jdk12-archive-downloads.html)
* [sbt](https://www.scala-sbt.org/download.html)
* [docker](https://hub.docker.com/editions/community/docker-ce-desktop-mac)
* [redis](https://redis.io/topics/quickstart) 
    - ```docker run --name my-redis -p 6379:6379 --restart always --detach redis```
* TODO - Slack Permissions

## Run

You can run this project using IntelliJ IDEA or Docker. You will need the same configuration variables for both.

```
REDIS_HOST=localhost
REDIS_PORT=6379
PLAY_SECRET_KEY={PLAY_SECRET_KEY}
BIAS_CORRECT_URL={BIAS_CORRECT_URL}
SLACK_TOKEN={SLACK_TOKEN}
SLACK_SIGNING_SECRET={SIGNING_SECRET}
SLACK_CLIENT_SECRET={CLIENT_SECRET}
SLACK_CLIENT_ID={CLIENT_ID}
TRACKING_ID={TRACKING_ID}
```

`PLAY_SECRET_KEY`: You can generate a new secret using `sbt playGenerateSecret` or if you don't already have an sbt server running `head -c 30 /dev/random | base64`.

`BIAS_CORRECT_URL`: URL for the Bias Correction Service Endpoint (ex. http://localhost:8000/corrector/correct).

`SLACK_TOKEN` && `SIGNING_SECRET` && `CLIENT_SECRET` && `CLIENT_ID`: Navigate to api.slack.com/app. Select your app and view the `Basic Information` section.

`TRACKING_ID`: Google Analytics Tracking ID (ex. UA-XXXX-Y).

### [IntelliJ IDEA](https://www.jetbrains.com/idea/)
Open Project
1. Open Project wizard, select **Import Project**.
1. In the window that opens, select this project.
1. On the next page of the wizard, select **Import project from external model** option, choose **sbt project**.
1. On the next page of the wizard, ensure that the **Project JDK** is set to **JDK 12**.
1. Enable Auto-Import for **sbt projects**.

Setup Configuration
1. Add configuration template **sbt Task**.
2. In the window that opens, enter "run" into the **Tasks** input.
3. Uncheck the **Use sbt shell** option.
4. Add the above configuration as environment variables.

Press Run

### [Docker](https://hub.docker.com/editions/community/docker-ce-desktop-mac)
sbt provides the infrastructure to easily build out a Docker image from our application. The current build.sbt file is configured with the options to use OpenJDK 12 on Alpine. This is intended to keep our shipping image as lean as possible. The application.conf file has also been updated to capture a few values from the environment when available. Finally, the build.sbt file has the option to configure the current version from an environment variable (which will be captured in the docker container tags).

Create a `prod.env` file containing all configuration outlined above. Then publish your project locally and run the the container.

```
sbt docker:publishLocal

docker run -p 8000:9000 -e PLAY_SECRET_KEY="$(head -c 30 /dev/random | base64)" --env-file prod.env catalyst-slack-service:1.0-SNAPSHOT
```

In the above example, we expose the host/local port 8000 to the container's port 9000 (the default port for the Play framework).  We've also chosen to generate a random secret key and use the default Docker tag.  If we had specified an environment variable during the build phase (`VERSION_NUMBER=example sbt docker:publishLocal`), then our tag would be `catalyst-slack-service:example`.

## Configure Slack App

After getting your local service running you will need to use [ngrok](https://dashboard.ngrok.com/get-started) to expose your local server. 

Setup and connect your ngrok account then expose your local service.

`./ngrok http 4542`

Take note of the forwarding https url. Next you need to update your Slack App with this forwarding url. 

Navigate to api.slack.com/apps.

1. Update **Interactive Components** with the **Request URL** `{FORWARDING_URL}/bias-correct/v2/slack/actions`.
2. Update **OAuth & Permissions** with the **Redirect URL** `{FORWARDING_URL}/bias-correct/v2/slack/auth/redirect`.
3. Update **Event Subscriptions** with the **Request URL** `{FORWARDING_URL}/bias-correct/v2/slack/events`.
4. Update **Slash Commands** with the **Request URL** `{FORWARDING_URL}/bias-correct/v2/slack/help`.
