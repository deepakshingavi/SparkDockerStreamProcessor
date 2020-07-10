# Spark docker

Docker images to:
* Setup a standalone [Apache Spark](https://spark.apache.org/) cluster running one Spark Master and multiple Spark workers
* Build Spark applications in Java, Scala or Python to run on a Spark cluster

Currently, supported versions:
* Spark 2.4.5 for Hadoop 2.7 with OpenJDK 8, Scala 2.11

### Pre-Requisites
* docker
* docker-compose
* Java 8
* Scala 2.x

#### Purpose of sub projects
1. base --> Used for customizing and building the Spark's base image
2. master --> Build the image for Spark-master
3. worker --> Build the image for Spark-worker
4. transaction-stream-processor --> Scala program to process the transaction logs from a Kafka topic(transaction-logs) and pushes the output to another(output-topic)

#### Purpose of file in base folder
1. build.sh --> Builds the image base,master and worker using docker
2. docker-compose.yml --> Docker composition file defining network and Services  

## Using Docker Compose

Add the following services to your `docker-compose.yml` to integrate a Spark master and Spark worker in [your BDE pipeline](https://github.com/deepakshingavi/SparkDockerStreamProcessor):


## Setting up Docker containers  
* Spark-master
* Spark-worker
* Kafka
* Zookeeper

```shell script
# Run it from project's base folder SparkDockerStreamProcessor to bring docker containers up or down  

docker-compose up
docker-compose down

```
This will setup a Spark standalone cluster with one master and a worker on every available node using the default namespace and resources. The master is reachable in the same namespace at `spark://spark-master:7077`.
It will also setup a headless service so spark clients can be reachable from the workers using hostname `spark-client`.

## Setting up Docker container
* transaction-stream-processor

```shell script
cd transaction-stream-processor/

#Build the scala program and tag it using docker
docker build --rm=true -t dipakpravin87/spark-streamer:latest .

#Run the docker image to start the Spark streaming job
docker run --name transaction-stream-processor --network stream-net -e ENABLE_INIT_DAEMON=false --link spark-master:spark-master -d dipakpravin87/spark-streamer:latest
```

## Push data to Kafka and verify the out stream
```shell script
#Get into Kafka Docker container
bash -c "clear && docker exec -it kafka sh"

#Copy sample file transaction-stream-processor/src/main/resources/sample_data.csv
docker cp transaction-stream-processor/src/main/resources/sample_data.csv kafka:/tmp/

#Push the sample data w/o headers
/opt/kafka_2.11-0.10.0.0/bin/kafka-console-producer.sh --broker-list kafka:9092 --topic transaction-logs < /tmp/sample_data.csv


#Check the output of transaction-streamer-processor in topic "output-topic" 
/opt/kafka_2.11-0.10.0.0/bin/kafka-console-consumer.sh --zookeeper zookeeper:2181 --from-beginning --topic output-topic

```   

Note : Spark streamer takes while to push the output topic 
