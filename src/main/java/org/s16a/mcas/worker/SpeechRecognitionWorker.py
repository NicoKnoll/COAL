from PythonWorkerWrapper import PythonWorker
from Segmentation import *
import speech_recognition as sr
import sys
import os
import ast
from threading import Thread

PATH_TO_DIR = sys.argv[1]
threads = []
r = sr.Recognizer()


def fileNameForSegment(segment):
    return PATH_TO_DIR + "/" + str(segment[0]) + "-" + str(segment[1]) + "_speech.wav"

def textFileNameForSegment(segment):
    return PATH_TO_DIR + "/" + str(segment[0]) + "-" + str(segment[1]) + "_text"

def recognizeForPart(fileName, segment, previousThread):
    with sr.WavFile(fileName) as source:
        audio = r.record(source)

        try:
            text = r.recognize(audio)
            print(text)
        except LookupError:
            pass

        if previousThread != None:
            while previousThread.isAlive():
                pass

        try:
            with open(textFileNameForSegment(segment), "a") as textFile: 
                textFile.write(text + "\n")
            textFile.close()
        except:
            pass

    os.remove(fileName)

def recognizeForSegment(segment):
    previousThread = None
    parts = sliceSegmentIntoParts(fileNameForSegment(segment))
    for fileName in parts:
        try:
            t = Thread(target=recognizeForPart, args=(fileName, segment, previousThread))
            threads.append(t)
            previousThread = t
            t.start()
        except:
            raise Exception("Error: unable to start thread")

    for t in threads:
        t.join()


def func():
    segments = ast.literal_eval(open(PATH_TO_DIR + "/segments", "r").readline())
    speechSegments = segments[0]

    for s in speechSegments:
        recognizeForSegment(s)

worker = PythonWorker(func)
worker.run()
