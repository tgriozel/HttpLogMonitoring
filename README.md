# What is this?

This is a simple http log monitoring daemon. Currently, the log supported format is the
[Common Log Format](https://en.wikipedia.org/wiki/Common_Log_Format).  
You can configure the path to the log file to observe as well as many other parameters (polling and alerts frequency,
thresholds, etc) in the [configuration file](src/main/resources/application.conf).

# Building and running

You will need sbt for that:  

- To build and run:  
`sbt assembly`  
`sbt run`  

- To run the tests only:  
`sbt test`
