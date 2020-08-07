FROM rdegiovanni/strix_image:docker_runner
# Bundle app source
ARG formula
ARG input
ARG output
RUN echo "FORMULA: $formula"
RUN echo "IN: $input"
RUN echo "out: $output"
#COPY $filename Spec.tlsf
#COPY run-strix.sh run-strix.sh
#COPY syfco syfco
#RUN ./run-strix.sh "$formula" "$input" "$output"
RUN ./bin/strix -r -f "$formula" --ins "$input" --outs "$output"