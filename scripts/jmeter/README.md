# Quarkus Superheroes Sample :: JMeter Scripts

## Load Testing with JMeter

[JMeter](https://jmeter.apache.org/) is used to do load testing.
Thanks to the [JMeter Maven plugin](https://github.com/jmeter-maven-plugin/jmeter-maven-plugin), you can execute the following commands:

* `./mvnw jmeter:configure jmeter:gui -DguiTestFile=src/test/jmeter/fight.jmx`: Executes the GUI so you can visualize the `src/test/jmeter/fight.jmx` file
* `./mvnw clean verify`: runs the load test

> **NOTE:** You need to make sure the [`rest-fights`](../../rest-fights) service is up and running.

The [`src/test/jmeter/fight.jmx`](src/test/jmeter/fight.jmx) script executes some load tests on the Fight API by choosing a few random fighters and performing a fight.
By default, the load test is done on http://localhost:8082.
You can configure it by updating the `fight.jmx` and setting the `FIGHT_PROTOCOL`, `FIGHT_URL` and `FIGHT_PORT` arguments (you can leave `FIGHT_PORT` empty if there is no specific port or set it to 443 if you are doing HTTPs).
