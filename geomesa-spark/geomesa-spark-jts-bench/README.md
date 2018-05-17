# Spark JTS Benchmarking

To run build the JMH benchmarks:

    mvn clean package -T 1C -pl :geomesa-spark-jts-bench_2.11 -am -Dmaven.test.skip=true -offline
    
To run the packaged benchmarks:
    
    java -jar geomesa-spark/geomesa-spark-jts-bench/target/jmh-benchmarks.jar -rf json 
    
The output will be written to `jmh-results.json`


To run a specific benchmark, provide a regex argument. e.g.:

    java -jar geomesa-spark/geomesa-spark-jts-bench/target/jmh-benchmarks.jar -rf json '.*WKBLengthBench'
    

Additional commandline options are available when running the benchmarks, useful for overriding 
annotation defaults:

    java -jar geomesa-spark/geomesa-spark-jts-bench/target/jmh-benchmarks.jar -h
    

Note: http://jmh.morethan.io/ is a nice online tool for visualizing JSON-formatted JMH results, 
especially when comparing runs.
