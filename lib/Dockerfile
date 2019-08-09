FROM vcatechnology/linux-mint

# Update aptitude with new repo
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y git

#install latest cmake
ADD cmake-3.15.2-Linux-x86_64.sh /cmake-3.15.2-Linux-x86_64.sh
RUN mkdir /opt/cmake
RUN sh /cmake-3.15.2-Linux-x86_64.sh --prefix=/opt/cmake --skip-license
RUN ln -s /opt/cmake/bin/cmake /usr/local/bin/cmake
RUN cmake --version

#install g++7
RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:ubuntu-toolchain-r/test
RUN apt update
RUN apt install g++-7 -y
RUN update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-7 60 --slave /usr/bin/g++ g++ /usr/bin/g++-7
RUN update-alternatives --config gcc

RUN apt -y install make libboost-dev libboost-program-options-dev libboost-filesystem-dev libboost-iostreams-dev zlib1g-dev unzip
RUN apt -y install python-apt
RUN add-apt-repository -y ppa:openjdk-r/ppa
RUN apt-get update
RUN apt-cache policy openjdk-11-jdk
RUN apt-get -y install openjdk-11-jdk

RUN git clone https://gitlab.lrz.de/i7/strix.git
WORKDIR strix
RUN git submodule init
RUN git submodule update
RUN /usr/bin/printf '\xfe\xed\xfe\xed\x00\x00\x00\x02\x00\x00\x00\x00\xe2\x68\x6e\x45\xfb\x43\xdf\xa4\xd9\x92\xdd\x41\xce\xb6\xb2\x1c\x63\x30\xd7\x92' > /etc/ssl/certs/java/cacerts
RUN /var/lib/dpkg/info/ca-certificates-java.postinst configure
RUN make