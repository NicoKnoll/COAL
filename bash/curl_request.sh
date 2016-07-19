#!/bin/bash

rm -r cache/*

#curl -v -H "accept:text/turtle" "http://192.168.99.100:8080/coal/resource?url=http://static.nico.is/coal4.mp3"
curl -v -H "accept:text/turtle" "http://localhost:8080/coal/resource?url=http://static.nico.is/coal4.mp3"
