FROM rdegiovanni/spectra_image:docker_runner
# Bundle app source
ARG filename
RUN echo "FILE: $filename"
#install spectra
#ADD spectra-cli /spectra-cli
#WORKDIR /spectra-cli
COPY $filename /spectra-cli/models/Spec.spectra
RUN ./spectra.sh -i /spectra-cli/models/Spec.spectra