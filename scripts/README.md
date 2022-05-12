# Quarkus Superheroes Sample ::  Script

## Shell Scripts

This directory contains a few shell scripts that:

* `deploy-to-azure-containerapps.sh`: deploys the entire infrastructure and application to Azure Container Apps
* `generate-docker-compose-resources.sh`: generates the Docker Compose files located in the `deploy` directory
* `generate-k8s-resources.sh`: generates the Kubernetes files located in the `deploy` directory

## Load Testing with JMeter

JMeter is used to do load testing.
Thanks to the Maven plugin, you can execute the following commands:

* `mvn jmeter:configure jmeter:gui -DguiTestFile=src/test/jmeter/fight.jmx`: executes the GUI so you can visualize the `src/test/jmeter/fight.jmx` file
* `mvn jmeter:jmeter`: runs the load test

The `src/test/jmeter/fight.jmx` script executes some load tests on the Fight API by choosing a few random fighters and performing a fight.
By default, the load test is done on http://localhost:8082.
You can configure it by updating the `fight.jmx` and setting the `FIGHT_PROTOCOL`, `FIGHT_URL` and `FIGHT_PORT` arguments.
