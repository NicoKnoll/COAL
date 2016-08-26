from PythonWorkerWrapper import PythonWorker
from rdflib import URIRef, Literal, Graph, BNode
from rdflib.namespace import XSD, RDF
import namespaces
import sys
import os
import requests
import ast

PATH_TO_DIR = sys.argv[1]
KEA_URL = 'http://141.89.225.50/kea-2.0.1/services/annotate'

def textFileNameForSegment(segment):
    return PATH_TO_DIR + str(segment[0]) + "-" + str(segment[1]) + "_text"

def process_data(original_model, text_filename):

    with open(text_filename) as file:
        text = file.read()

    nif = Graph()
    data_uri = URIRef(PATH_TO_DIR)

    nif.add((data_uri, RDF.type, namespaces.nif.RFC5147String))
    nif.add((data_uri, RDF.type, namespaces.nif.String))
    nif.add((data_uri, RDF.type, namespaces.nif.Context))

    nif.add((data_uri, namespaces.nif.isString, Literal(text, datatype=XSD.String)))

    nif_text = nif.serialize(format='turtle')
    response = requests.post(KEA_URL, data=nif_text)

    model = Graph()
    model.parse(original_model, format='turtle')
    model.parse(data=response.content, format='turtle')

    with open(original_model, "r+") as turtleFile:
            turtleFile.write(model.serialize(format='turtle') + "\n")
    turtleFile.close()

def func():
    segments = ast.literal_eval(open(PATH_TO_DIR + "/segments", "r").readline())
    speechSegments = segments[0]

    for segment in speechSegments:
        process_data(PATH_TO_DIR + "data.ttl", textFileNameForSegment(segment))

worker = PythonWorker(func)
worker.run()