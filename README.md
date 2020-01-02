# catalyst-slack-service

A microservice that integrates Slack and Bias Correction Service. This service subscribes to message events on Slack and utilizes the Bias Correction Service for potential suggestions to replace biased terms.

## Setup

Download IntelliJ IDEA
Open this folder as a project in IDEA
Allow IDEA to download/configure the project as necessary
Navigate to the sbt shell pane and allow IDEA to configure it as necessary
When the shell is ready, type run
Navigate to http://localhost:4542 to verify that the service is running

## Packaging as a Docker container

sbt provides the infrastructure to easily build out a Docker image from our application. The current build.sbt file is configured with the options to use OpenJDK 12 on Alpine. This is intended to keep our shipping image as lean as possible. The application.conf file has also been updated to capture a few values from the environment when available. Finally, the build.sbt file has the option to configure the current version from an environment variable (which will be captured in the docker container tags).

In order to deploy on Docker, you will need to provide the `PLAY_SECRET_KEY`.  The service will throw an exception and quit at startup unless you do.  You can generate a new secret using `sbt playGenerateSecret` or from some other means such as using a static value or generating one from other tooling.  If you don't already have an sbt server running, using something like `head -c 30 /dev/random | base64` may produce a value more quickly.

Create a `prod.env` file containing all the slack environment variables for the service.

* `sbt docker:publishLocal`
* `docker run -p 8000:9000 -e PLAY_SECRET_KEY="$(head -c 30 /dev/random | base64)" --env-file prod.env catalyst-slack-service:1.0-SNAPSHOT`

In the above example, we expose the host/local port 8000 to the container's port 9000 (the default port for the Play framework).  We've also chosen to generate a random secret key and use the default Docker tag.  If we had specified an environment variable during the build phase (`VERSION_NUMBER=example sbt docker:publishLocal`), then our tag would be `catalyst-slack-service:example`.