# Rationale
We selected [Thingsboard](https://thingsboard.io/) as the back-end platform for the Ledger project.
It quickly became apparent that a middleware is needed in order to

- Manage the lifecycle of the Thingsboard entities
- Overcome some of the Thingsboard's API limitations
- Expose a custom API to mobile clients


# Getting started

## Required tools
This project is written in java and uses maven as the build tool.
Be sure that you have both [Java 8](https://thingsboard.io/docs/user-guide/install/linux#java) and [maven](https://maven.apache.org/download.cgi#) installed in your system. 

## Thingsboard server
A running instance of a Thinsgboard server is needed. There are a lot of [options](https://thingsboard.io/docs/installation/)
on how to get it up and running, but we will use the [docker](https://thingsboard.io/docs/user-guide/install/docker/) variation in this example.

Make sure to run the server with a valid, at least 512 bit long, JWT secret key like so
`sudo docker run -it -p 9090:9090 -p 1883:1883 -p 5683:5683/udp -v ~/.mytb-data:/data -v ~/.mytb-logs:/var/log/thingsboard --name mytb --restart always --env JWT_TOKEN_SIGNING_KEY=A_LONG_BASE64_ENCODED_SECRET_KEY thingsboard/tb-postgres`

## TB-proxy
1. First you have to update the configuration. There are two approaches depending on your preferences 
    1. Edit the configuration file at `src/main/resources/application.yml` and update the `tb` section of the file with the correct values
    2. Define the required environment variables `TB_URL`, `TB_JWTKEY`, `TB_TENANT`, `TB_PASSWORD` in your system
2. Build application with `mvn install`
3. Lastly run it with `java -jar target/tb-proxy-VERSION.jar`

You can find more info about the exposed endpoints at `http://localhost:8080/swagger-ui.html`


# High level description
There are 7 main objects in this application logic that need to be modeled as Thingsboard entities:

- Farmer
- Agronomist
- Device
- Forecast
- Field
- Crop
- Log

The straightforward way of modeling these is to represent Farmers and Agronomists as `Customers`, Devices, Forecasts and Fields as Customer's `Devices` and lastly Crops and Logs as `Telemetry`. 
The problem with this approach is that there is no way to share data between Customers due to Thingsboard restrictions. To overcome this, an `Entity View` is created for each of the Customer's Fields 
and then is assigned to him. This mechanism is also used for Forecasts and Devices in order to keep the managing of these entities available for the `Tenant`. The user can later "attach" to the 
entities that he chooses, gaining access to the data they provide. This approach has the following benefits:

- The managing of all entities is the `Tenant`'s responsibility
- The `Tenant` controls which `Devices` and `Forecasts` are available
- Each user has an isolated read-only view of the data he is interested at
- It is possible to share data between `Customers` 

The application's API abstracts most of the complexity away from its user and provides an easy way to manage his entities without having to
worry about all of the above. So in essence, what this application offers is a way of interacting with the Thingsboard server in a strict way, 
by following a different set of rules depending on who is using the API.