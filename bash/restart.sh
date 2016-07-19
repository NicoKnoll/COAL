#!/bin/bash

# Reset RabbitMQ 
# 	RabbitMQ offers no function to flush queues, so we need to reset
# 	and set users again
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl start_app
rabbitmqctl add_user coal coal
rabbitmqctl set_user_tags coal administrator
rabbitmqctl set_permissions -p / coal ".*" ".*" ".*"

# Build and start coal server
mvn clean install -DskipTests=true
mvn exec:java
