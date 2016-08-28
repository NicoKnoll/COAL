import os, sys
import ast
from PythonWorkerWrapper import PythonWorker
from acrcloud.recognizer import ACRCloudRecognizer
from ACRCloudConfig import *
from rdflib import URIRef, Literal, Graph, BNode
from rdflib.namespace import XSD, RDF, DC
import namespaces
import json

re = None

PATH_TO_DIR = sys.argv[1]
PATH_TO_WAV = PATH_TO_DIR + "data.wav"
RESOURCE_URL = sys.argv[2]

def musicFileNameForSegment(segment):
    return PATH_TO_DIR + "/" + str(segment[0]) + "-" + str(segment[1]) + "_music"

def fileNameForSegment(segment):
    return PATH_TO_DIR + "/" + str(segment[0]) + "-" + str(segment[1]) + "_music.wav"

def recognizeForSegment(segment):
    fileName = fileNameForSegment(segment)

    result = re.recognize_by_file(fileName, 0) #0 for skipping zero seconds at beginning
    createModelForSegment(segment, result)
    with open(musicFileNameForSegment(segment), "a") as textFile:
        textFile.write(str(result))
    textFile.close()

def createModelForSegment(segment, result):
    if len(result) == 0:
        return -1
    data = json.loads(result)
    title = data['metadata']['music'][0]['title']
    artist = data['metadata']['music'][0]['artists'][0]['name']
    album = data['metadata']['music'][0]['album']['name']
    label = data['metadata']['music'][0]['label']

    music = Graph()
    data_uri = URIRef(RESOURCE_URL + '#t=' + str(segment[0]) + ',' + str(segment[1]))
    music.bind("dc", DC)
    music.bind("mo", namespaces.mo)
    music.bind("nif", namespaces.nif)
    music.add((data_uri, RDF.type, namespaces.nif.RFC5147String))
    music.add((data_uri, RDF.type, namespaces.mo.Track))
    music.add((data_uri, DC.title, Literal(title, datatype=XSD.String)))
    music.add((data_uri, namespaces.mo.artist, Literal(artist, datatype=XSD.String)))
    music.add((data_uri, namespaces.mo.album, Literal(album, datatype=XSD.String)))
    music.add((data_uri, namespaces.mo.label, Literal(label, datatype=XSD.String)))
    mergeModels(PATH_TO_DIR + "data.ttl", music)

def mergeModels(original_model, music_model):
    model = Graph()
    model.parse(original_model, format='turtle')
    model.parse(data=music_model.serialize(format='turtle'), format='turtle')

    with open(original_model, "r+") as turtleFile:
        turtleFile.write(model.serialize(format='turtle') + "\n")
    turtleFile.close()

def func():

    if not config:
        print "ERROR: Cannot find ACRCloudConfig"
        return

    global re
    re = ACRCloudRecognizer(config)

    segments = ast.literal_eval(open(PATH_TO_DIR + "/segments", "r").readline())
    musicSegments = segments[1]

    for s in musicSegments:
        recognizeForSegment(s)

worker = PythonWorker(func)
worker.run()
