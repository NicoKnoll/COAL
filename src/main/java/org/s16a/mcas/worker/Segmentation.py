import sys, os
import wave
import contextlib

def getDuration(fileName):

    with contextlib.closing(wave.open(fileName,'r')) as f:
        frames = f.getnframes()
        rate = f.getframerate()
        duration = frames / float(rate)
        return int(duration*1000)


def sliceWav(infile, fileOutName, startMS, endMS):

    width = infile.getsampwidth()
    rate = infile.getframerate()
    fpms = rate / 1000 # frames per ms
    length = (endMS - startMS) * fpms
    start_index = startMS * fpms

    out = wave.open(fileOutName, "w")
    out.setparams((infile.getnchannels(), width, rate, length, infile.getcomptype(), infile.getcompname()))

    infile.rewind()
    anchor = infile.tell()
    infile.setpos(anchor + start_index)
    out.writeframes(infile.readframes(length))


def sliceSegmentIntoParts(pathToFile, partDurationMS = 5000):
    "Slices a segment into short parts better digestable by SpeechRecognition"

    slashPos = pathToFile.rfind("/")
    if slashPos != -1:
        pathToDir = pathToFile[:pathToFile.rfind("/")+1]
        fileInName = pathToFile[pathToFile.rfind("/")+1:].replace(".wav","").replace("_speech","")
    else:
        pathToDir = ""

    duration = getDuration(pathToFile)

    if duration < partDurationMS or duration < 1000:
        "Segment is too short"
        return []

    if partDurationMS < 1000:
        raise Exception("Duration of parts needs to be atleast a second")

    startMS = 0
    endMS = startMS + partDurationMS
    fileNames = []

    while endMS <= duration:
        fileOutName = pathToDir + fileInName + ":" + str(startMS) + "-" + str(endMS) + ".wav"
        sliceWav(wave.open(pathToFile, "r"), fileOutName, startMS, endMS)
        fileNames.append(fileOutName)
        startMS = endMS - 500 #Half a second of puffer for speechRecognition
        endMS += partDurationMS

    #Add segment < partDurationMS at the end
    if endMS != duration:
        endMS = duration
        fileOutName = pathToDir + str(startMS) + "-" + str(endMS) + ".wav"
        sliceWav(wave.open(pathToFile, "r"), fileOutName, startMS, endMS)
        fileNames.append(fileOutName)

    return fileNames
