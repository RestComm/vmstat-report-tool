How to run:

1 - Compile with 'mvn install'

2 - Copy the file 'target/vmstat-report-0.0.1-SNAPSHOT-with-dependencies.jar' to the location of the statistics files produced by vmstat

3 - Run with 'java -jar' and check the available options with '-h' flag

4 - Have fun



How to make vmstat produce statistics files

1 - Start vmstat using the following command 'vmstat -n -a -t 1 > vmstat.csv'

2 - This will produce a .csv file that can be used by this tool