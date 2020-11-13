# start from the most recent opensource Java Development Kit.
FROM ubuntu:latest

# Install the firewall, some helpful networking tools, and NodeJS for a simple
# web server
RUN apt-get update
RUN apt-get -y install iptables net-tools nodejs openjdk-11-jdk

# Copy everything to /app and make that our working directory
#COPY . /app
WORKDIR /app

# Compile everything in the directory
#RUN javac *.java

# ENTRYPOINT ["/app/run/run.sh"]

CMD ["/bin/bash"]
