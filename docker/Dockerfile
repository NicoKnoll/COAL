FROM java:8
MAINTAINER boeckhoff

# Update and install dependencies
RUN apt-get update
RUN apt-get upgrade -y

# Install dependencies
RUN apt-get install -y git
RUN apt-get install -y wget
RUN apt-get install -y build-essential

# Install Java dependencies
RUN apt-get install -y maven

# Install Python dependencies
RUN apt-get install -y \
  python-pip \ 
  python-matplotlib \ 
  python-scipy \ 
  libgsl0-dev \
  python-eyed3

# Pip dependencies
RUN pip install numpy
RUN pip install --upgrade pip
RUN pip install scikit-learn==0.16.1
RUN pip install scikits.talkbox
RUN pip install simplejson
RUN pip install -Iv https://pypi.python.org/packages/source/S/SpeechRecognition/SpeechRecognition-2.1.3.tar.gz
RUN pip install mutagen
RUN pip install rdflib

RUN wget http://sourceforge.net/projects/mlpy/files/mlpy%203.5.0/mlpy-3.5.0.tar.gz 
RUN tar xvf mlpy-3.5.0.tar.gz

RUN cd mlpy-3.5.0 && python setup.py install

WORKDIR /knowmin

# RabbitMQ
RUN apt-get install -y rabbitmq-server
RUN rabbitmq-plugins enable rabbitmq_management
RUN service rabbitmq-server restart
ENV RABBIT_HOST $RABBIT_PORT_4369_TCP_ADDR

# Audio Converter
RUN apt-get install -y libav-tools

# Install pyAudioAnalysis
WORKDIR /knowmin
RUN git clone https://github.com/boeckhoff/pyAudioAnalysis.git
RUN export PYTHONPATH=$PYTHONPATH:$(pwd)

# Mediainfo
RUN apt-get install -y libmediainfo-dev

# Pythonpath
RUN echo "export PYTHONPATH=$PYTHONPATH:"/knowmin"" >> /root/.bashrc

# fpcalc
RUN wget https://bitbucket.org/acoustid/chromaprint/downloads/chromaprint-fpcalc-1.3.2-linux-x86_64.tar.gz
RUN tar xfv chromaprint-fpcalc-1.3.2-linux-x86_64.tar.gz
RUN echo "alias fpcalc='/knowmin/chromaprint-fpcalc-1.3.2-linux-x86_64/fpcalc'" >> /root/.bashrc

WORKDIR /knowmin
RUN git clone https://github.com/acrcloud-demo/audio-recognition-quick-trial.git
RUN mv audio-recognition-quick-trial/acrcloud /usr/local/lib/python2.7/dist-packages/

RUN mkdir COAL
WORKDIR /knowmin/COAL

# Expose ports.
EXPOSE 5672
EXPOSE 15672
EXPOSE 8080
