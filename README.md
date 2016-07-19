# COAL
web media content analysis framework

### Requirements
- [Docker](https://docs.docker.com/engine/installation/)

### How to run

#### run docker-machine
- only necessary for mac
```
docker-machine start
docker-machine env
eval "$(docker-machine env default)"
```

#### build docker-image
- you need to be in /COAL/docker directory
```
docker build -t boeckhoff/knowmin .
```

#### run docker-image
- you need to be in /COAL directory
```
docker run -v $(pwd):/knowmin/COAL -t -i -p 8080:8080 boeckhoff/knowmin ./bash/startup.sh
```
- or run with port fowarding for RabbitMQ
```
docker run -v $(pwd):/knowmin/COAL -t -i -p 8080:8080 -p 15672:15672 boeckhoff/knowmin ./bash/startup.sh
```
- or run with accelerated maven build (use volume with local maven repo) 
```
docker run -v ~/.m2:/root/.m2 -v $(pwd):/knowmin/COAL -t -i -p 8080:8080 boeckhoff/knowmin ./bash/startup.sh
```

### How to use
- [mac] call the following commands in your terminal (not in docker container)
```
docker-machine ip
curl -v -H "accept:text/turtle" "http://DOCKER-MACHINE-IP:8080/coal/resource?url=RESOURCE-URI"
```
- [else] call the following command in your terminal (not in docker container)
```
curl -v -H "accept:text/turtle" "http://localhost:8080/coal/resource?url=RESOURCE-URI"
```