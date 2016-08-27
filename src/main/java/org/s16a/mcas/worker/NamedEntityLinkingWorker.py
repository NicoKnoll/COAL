from PythonWorkerWrapper import PythonWorker
from rdflib import URIRef, Literal, Graph, BNode
from rdflib.namespace import XSD, RDF
import namespaces
import sys
import requests
import ast

PATH_TO_DIR = None
RESOURCE_URL = None
KEA_URL = 'http://141.89.225.50/kea-2.0.1/services/annotate'

def textFileNameForSegment(segment):
    return PATH_TO_DIR + str(segment[0]) + "-" + str(segment[1]) + "_text"

def named_entity_linking(original_model, text_filename, segment):

    with open(text_filename) as file:
        text = file.read()

    # build nif context for kea service
    nif = Graph()
    data_uri = URIRef(RESOURCE_URL + '#char=' + str(0) + ',' + str(len(text)))

    nif.add((data_uri, RDF.type, namespaces.nif.RFC5147String))
    nif.add((data_uri, RDF.type, namespaces.nif.String))
    nif.add((data_uri, RDF.type, namespaces.nif.Context))
    nif.add((data_uri, namespaces.nif.isString, Literal(text, datatype=XSD.String)))
    nif_text = nif.serialize(format='turtle')

    # request to kea service
    response = requests.post(KEA_URL, data=nif_text)

    # insert correct media fragment (workaround)
    corrected_response = response.content.replace("#char=", '#t=' + str(segment[0]) + ',' + str(segment[1]) + '&char=')

    # merge existing and new rdf graphs
    model = Graph()
    model.parse(original_model, format='turtle')
    model.parse(data=corrected_response, format='turtle')

    with open(original_model, "r+") as turtleFile:
            turtleFile.write(model.serialize(format='turtle') + "\n")
    turtleFile.close()

def func():
    global PATH_TO_DIR
    global RESOURCE_URL
    PATH_TO_DIR = sys.argv[1]
    RESOURCE_URL = sys.argv[2]
    segments = ast.literal_eval(open(PATH_TO_DIR + "/segments", "r").readline())
    speechSegments = segments[0]

    for segment in speechSegments:
        named_entity_linking(PATH_TO_DIR + "data.ttl", textFileNameForSegment(segment), segment)

worker = PythonWorker(func)
worker.run()
