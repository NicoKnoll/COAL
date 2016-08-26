import os, sys
import ast
from PythonWorkerWrapper import PythonWorker
from acrcloud.recognizer import ACRCloudRecognizer
from ACRCloudConfig import *

re = None

PATH_TO_DIR = sys.argv[1]
PATH_TO_WAV = PATH_TO_DIR + "data.wav"

def musicFileNameForSegment(segment):
    return PATH_TO_DIR + "/" + str(segment[0]) + "-" + str(segment[1]) + "_music"

def fileNameForSegment(segment):
    return PATH_TO_DIR + "/" + str(segment[0]) + "-" + str(segment[1]) + "_music.wav"

def recognizeForSegment(segment):
    fileName = fileNameForSegment(segment)

    result = re.recognize_by_file(fileName, 0) #0 for skipping zero seconds at beginning

    with open(musicFileNameForSegment(segment), "a") as textFile:
        textFile.write(str(result))
    textFile.close()

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
