Compile:
    javac *.java

Run:
    docker run -it --rm --network nodenet --cap-add=NET_ADMIN --ip 172.18.0.21 -v /home/ascalva/Documents/masters/y1s1/computer_networks/project_3/:/app/ project_3
