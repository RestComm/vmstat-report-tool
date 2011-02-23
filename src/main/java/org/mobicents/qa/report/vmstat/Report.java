package org.mobicents.qa.report.vmstat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.ImageEncoder;
import org.jfree.chart.encoders.KeypointPNGEncoderAdapter;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

public class Report {

    private static Logger logger = Logger.getLogger(Report.class.getName());

    private enum FieldType {
        DOUBLE, INVALID
    };

    private static Set<String> selectedCategories = new HashSet<String>();
    private static Set<String> capacityCategories = new HashSet<String>();
    private static Set<String> counterCategories = new HashSet<String>();

    private static Map<String, String> categoriesTranslator = new HashMap<String, String>();

    static {
        String[] selected_vmstat = new String[] { "r", "b", "swpd", "bi", "bo", "in", "cs", "us", "sy", "id", "wa", "st" };
        String[] selected_jstat = new String[] { "Timestamp", "S0", "S1", "E", "O", "P", "EC", "OC", "PC", "EU", "OU", "PU", "YGC", "YGCT", "FGC", "FGCT",
                "GCT", "YGC#d/dt", "YGCT#d/dt", "FGC#d/dt", "FGCT#d/dt", "GCT#d/dt" };

        String[] jstat_capacity = new String[] { "S0C", "S1C", "EC", "OC", "PC" };
        String[] jstat_counter = new String[] { "YGC", "YGCT", "FGC", "FGCT", "GCT" };

        for (String category : selected_vmstat) {
            selectedCategories.add(category);
        }
        for (String category : selected_jstat) {
            selectedCategories.add(category);
        }
        for (String category : jstat_capacity) {
            capacityCategories.add(category);
        }
        for (String category : jstat_counter) {
            counterCategories.add(category);
        }

        categoriesTranslator.put("r", "Processes waiting for runtime");
        categoriesTranslator.put("b", "Processes in uninterruptible sleep");
        categoriesTranslator.put("swpd", "Virtual memory used");
        categoriesTranslator.put("free", "Idle memory");
        categoriesTranslator.put("buff", "Memory used as buffers");
        categoriesTranslator.put("cache", "Memory used as cache");
        categoriesTranslator.put("inact", "Inactive memory");
        categoriesTranslator.put("active", "Active memory");
        categoriesTranslator.put("si", "Memory swapped in from disk (/s)");
        categoriesTranslator.put("so", "Memory swapped to disk (/s)");
        categoriesTranslator.put("bi", "IO: Blocks received (/s)");
        categoriesTranslator.put("bo", "IO: Blocks sent (/s)");
        categoriesTranslator.put("in", "Interrupts, including the clock (/s)");
        categoriesTranslator.put("cs", "Context switches (/s)");
        categoriesTranslator.put("us", "User time (%)");
        categoriesTranslator.put("sy", "System time (%)");
        categoriesTranslator.put("id", "Idle time (%)");
        categoriesTranslator.put("wa", "Waiting for IO time (%)");
        categoriesTranslator.put("st", "Time stolen from a virtual machine (%)");

        categoriesTranslator.put("Timestamp", "Timestamp");

        categoriesTranslator.put("Loaded", "Number of classes loaded");
        categoriesTranslator.put("Unloaded", "Number of classes unloaded");
        categoriesTranslator.put("Bytes", "Number of Kbytes loaded / unloaded (?)");
        categoriesTranslator.put("Time", "Time spent in the tasks");

        categoriesTranslator.put("Compiled", "Number of compilation tasks performed");
        categoriesTranslator.put("Failed", "Number of compilation tasks that failed");
        categoriesTranslator.put("Invalid", "Number of compilation tasks that were invalidated");
        categoriesTranslator.put("FailedType", "Compile type of the last failed compilation");
        categoriesTranslator.put("FailedMethod", "Class name and method for the last failed compilation");

        categoriesTranslator.put("NGCMN", "Minimum new generation capacity (KB)");
        categoriesTranslator.put("NGCMX", "Maximum new generation capacity (KB)");
        categoriesTranslator.put("NGC", "New generation capacity (KB)");
        categoriesTranslator.put("NGU", "New generation usage (KB)");
        categoriesTranslator.put("S0CMX", "Maximum survivor space 0 capacity (KB)");
        categoriesTranslator.put("S0C", "Survivor space 0 capacity (KB)");
        categoriesTranslator.put("S1CMX", "Maximum survivor space 1 capacity (KB)");
        categoriesTranslator.put("S1C", "Survivor space 0 capacity (KB)");
        categoriesTranslator.put("S0U", "Survivor space 0 usage (KB)");
        categoriesTranslator.put("S1U", "Survivor space 1 usage (KB)");
        categoriesTranslator.put("S0", "Survivor space 0 usage (%)");
        categoriesTranslator.put("S1", "Survivor space 1 usage (%)");
        categoriesTranslator.put("ECMX", "Maximum eden space capacity (KB)");
        categoriesTranslator.put("EC", "Eden space capacity (KB)");
        categoriesTranslator.put("EU", "Eden space usage (KB)");
        categoriesTranslator.put("E", "Eden space usage (%)");
        categoriesTranslator.put("TT", "Tenuring threshold");
        categoriesTranslator.put("MTT", "Maximum tenuring threshold");
        categoriesTranslator.put("DSS", "Desired survivor size (KB)");

        categoriesTranslator.put("OGCMN", "Minimum old generation capacity (KB)");
        categoriesTranslator.put("OGCMX", "Maximum old generation capacity (KB)");
        categoriesTranslator.put("OGC", "Old generation capacity (KB)");
        categoriesTranslator.put("OC", "Old space capacity (KB)");
        categoriesTranslator.put("OU", "Old space usage (KB)");
        categoriesTranslator.put("O", "Old space usage (%)");

        categoriesTranslator.put("PGCMN", "Minimum permanent generation capacity (KB)");
        categoriesTranslator.put("PGCMX", "Maximum permanent generation capacity (KB)");
        categoriesTranslator.put("PGC", "Permanent generation capacity (KB)");
        categoriesTranslator.put("PC", "Permanent space capacity (KB)");
        categoriesTranslator.put("PU", "Permanent space usage (KB)");
        categoriesTranslator.put("P", "Permanent space usage (%)");

        categoriesTranslator.put("YGC", "Number of young generation GC Events");
        categoriesTranslator.put("YGCT", "Young garbage collection total time");
        categoriesTranslator.put("FGC", "Number of full GC Events");
        categoriesTranslator.put("FGCT", "Full garbage collection total time");
        categoriesTranslator.put("GCT", "Total garbage collection total time");

        categoriesTranslator.put("YGC#d/dt", "Young generation GC Events per second");
        categoriesTranslator.put("YGCT#d/dt", "Young garbage collection time per second");
        categoriesTranslator.put("FGC#d/dt", "Full GC Events per second");
        categoriesTranslator.put("FGCT#d/dt", "Full garbage collection time per second");
        categoriesTranslator.put("GCT#d/dt", "Total garbage collection time per second");

        categoriesTranslator.put("Size", "Number of bytes of bytecode for the method");
        categoriesTranslator.put("Type", "Compilation type");
        categoriesTranslator.put("Method", "Method name is the method within the given class");
    }

    private static double period = 1;

    private static boolean allCharts = false;

    private static boolean singleFile = false;

    private static boolean printCharts = false;

    private static boolean bigCharts = false;

    private static boolean statsFile = false;

    private static String defaultOutputFileName = "vmstat-report.pdf";

    private static void printInfo() {
        logger.info("Usage: java -jar 'thisFile' [options] [file1 ... fileN]");
        logger.info("Usage: If no files are specified, all .csv files in current directory are used");
        logger.info("Option: -Tn- TIME n - Set the period according to vmstat delay");
        logger.info("Option: -a - ALL    - Generates charts for all the categories");
        logger.info("Option: -b - BIG    - Generates charts with 1 pixel for each elapsed second");
        logger.info("Option: -d - DEBUG  - Extra information during program execution");
        logger.info("Option: -h - HELP   - Shows this info and exits");
        logger.info("Option: -p - PRINT  - Print chart images in a subfolder");
        logger.info("Option: -o - OUTPUT - Chooses the filename of the output (in single file mode only)");
        logger.info("Option: -s - STATS  - Writes a txt file with the statistical properties of the categories");
    }

    public static void main(String[] args) {

        // Setup Log4j
        Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("%c %-5p %m%n")));
        logger.setLevel(Level.INFO);
        logger.info("VMStat Report Tool starting ... ");

        // Search for -d flag
        for (String string : args) {
            if ("-d".equals(string)) {
                logger.setLevel(Level.DEBUG);
                logger.debug("Debug level set");
                break;
            }
        }

        // Print arguments
        if (logger.isDebugEnabled()) {
            logger.debug(Arrays.toString(selectedCategories.toArray()));

            String s = "Arguments:_";
            for (String string : args) {
                s += string + "_|_";
            }
            logger.debug(s);
        }

        // Get filenames
        Set<String> filenames = new HashSet<String>();
        boolean inOutput = false;
        for (String string : args) {
            if (inOutput) {
                defaultOutputFileName = string;
                logger.debug("Output file name set: " + string);
                inOutput = false;
                continue;
            }
            if (string.charAt(0) != '-') {
                filenames.add(string);
                logger.debug("File to open: " + string);
            } else {
                if ("-a".equals(string)) {
                    allCharts = true;
                    logger.info("All charts set");
                    continue;
                }
                if ("-p".equals(string)) {
                    printCharts = true;
                    logger.info("Print chart images set");
                    continue;
                }
                if ("-b".equals(string)) {
                    bigCharts = true;
                    logger.info("Big chart set");
                    continue;
                }
                if ("-o".equals(string)) {
                    inOutput = true;
                    logger.info("Using alternate filename");
                    continue;
                }
                if ("-s".equals(string)) {
                    statsFile = true;
                    logger.info("Writing stat file");
                    continue;
                }
                if ("-t".equals(string.substring(0, 2))) {
                    try {
                        period = Double.parseDouble(string.substring(2));
                        logger.info("Period set to " + period + " seconds");
                    } catch (RuntimeException e) {
                        logger.warn("Could not set period to: " + string.substring(2));
                    }
                    continue;
                }
                if (("-h".equals(string)) || ("--help".equals(string))) {
                    printInfo();
                    return;
                }
            }
        }
        if (inOutput) {
            printInfo();
            return;
        }

        if (filenames.isEmpty()) {
            try {
                URL url = Report.class.getProtectionDomain().getCodeSource().getLocation();
                File myDir = new File(URLDecoder.decode(url.getFile(), "UTF-8")).getAbsoluteFile().getParentFile();
                File[] files = myDir.listFiles(new FilenameFilter() {

                    public boolean accept(File file, String s) {
                        if (s.endsWith(".csv")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

                for (File file : files) {
                    filenames.add(file.getAbsolutePath());
                }

            } catch (UnsupportedEncodingException e) { // should not happen
                logger.warn("Could not get current dir");
            }
        }

        if (filenames.isEmpty()) {
            printInfo();
        } else {
            if (filenames.size() == 1) {
                singleFile = true;
                logger.debug("Single file mode - set");
            }

            // Create the report
            for (String string : filenames) {
                createReports(string);
            }
            logger.info("Done. Oh yeah!");
        }
    }

    public static void createReports(String filename) {
        try {

            // Create a CSV reader
            OpenCsvReader csv = new OpenCsvReader(new FileReader(filename), ' ', '\"');

            // Get categories
            String[] categories;
            String[] firstLine = csv.readNext();

            if (firstLine[0].startsWith("procs")) {
                // vmstat prints an extra header line that starts with "procs". Skip it.
                categories = csv.readNext();
            } else {
                categories = firstLine;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Categories read from CSV: " + Arrays.toString(categories));
            }

            // Get values
            List<String[]> values = csv.readAll();
            int rows = values.size();

            csv.close();
            csv = null;

            // Reference categories
            double[] referenceData = new double[rows];
            for (int n = 0; n < rows; n++) {
                referenceData[n] = period * n;
            }

            // convert to categories
            Map<String, XYDataset> categoryValues = new LinkedHashMap<String, XYDataset>();
            Map<String, FieldType> categoryTypes = new LinkedHashMap<String, FieldType>();

            FileOutputStream statsFOS = null;
            if (statsFile) {
                String statsFileName = singleFile ? defaultOutputFileName.replaceAll(".pdf", ".txt") : filename.replaceAll(".csv", ".txt");
                statsFOS = new FileOutputStream(statsFileName);
                logger.info("Writting stats file '" + statsFileName + "'  ...");
            }

            for (int i = 0; i < categories.length; i++) {
                if (!selectedCategories.contains(categories[i]) && !allCharts) {
                    logger.debug("Category " + categories[i] + " does not belong to selected categories. Dropping.");
                    continue;
                }

                if (capacityCategories.contains(categories[i])) {
                    logger.debug("Category " + categories[i] + " is capacity category. Skipping.");
                    continue;
                }

                // check for usable columns
                try {
                    Double.parseDouble(values.get(0)[i]);
                    categoryTypes.put(categories[i], FieldType.DOUBLE);
                    logger.debug("Category " + categories[i] + " is of Double type (" + values.get(0)[i] + ")");
                } catch (NumberFormatException nfe) {
                    categoryTypes.put(categories[i], FieldType.INVALID);
                    logger.warn("Column " + categories[i] + " is not in numeric format (" + values.get(0)[i] + "): " + nfe.getMessage());
                    continue;
                }

                // convert values
                DefaultXYDataset dataset = new DefaultXYDataset();

                double[] valueData = Report.getDoubleFromValues(values, rows, i);
                if (statsFile) {
                    String unit = categories[i].endsWith("C") ? "collections" : "unit";
                    unit = categories[i].endsWith("U") ? "kbytes" : categories[i].endsWith("T") ? "sec" : unit;
                    writeStatsToFile(categories[i], unit, valueData, statsFOS);
                }

                // Check if a capacity category exists
                String correspondingCapacityCategory = categories[i].substring(0, categories[i].length() - 1) + "C";
                if (capacityCategories.contains(correspondingCapacityCategory)) {
                    for (int j = 0; j < categories.length; j++) {
                        if (correspondingCapacityCategory.equals(categories[j])) {

                            logger.debug("Category " + categories[i] + " has corresponding capacity catagory. Adding to graph.");

                            dataset.addSeries(categories[i] + " capacity", new double[][] { referenceData, Report.getDoubleFromValues(values, rows, j) });
                            break;
                        }
                    }
                }

                dataset.addSeries(categories[i] + " average", new double[][] { referenceData, Report.runningAverage(valueData) });
                dataset.addSeries(categories[i], new double[][] { referenceData, valueData });
                categoryValues.put(categories[i], dataset);

                if (counterCategories.contains(categories[i])) {
                    DefaultXYDataset diffDataset = new DefaultXYDataset();
                    double[] diffValueData = diffOperator(valueData);

                    if (statsFile) {
                        writeStatsToFile(categories[i] + "#d/dt", categories[i].endsWith("T") ? "sec/sec" : "collections/sec", diffValueData, statsFOS);
                    }

                    diffDataset.addSeries(categories[i] + " average", new double[][] { referenceData, Report.runningAverage(diffValueData) });
                    diffDataset.addSeries(categories[i], new double[][] { referenceData, diffValueData });
                    categoryValues.put(categories[i] + "#d/dt", diffDataset);
                }
            }
            values = null;

            if (categoryValues.isEmpty()) {
                logger.warn("No categories to be written to file.");
                return;
            }

            // print available columns
            if (logger.isDebugEnabled()) {
                logger.debug("Writting categories: " + Arrays.toString(categoryValues.keySet().toArray()));
            }
            logger.info("Writting report '" + (singleFile ? defaultOutputFileName : filename.replaceAll(".csv", ".pdf")) + "'  ...");

            // Write files (1600 is the default value because it looks prettier in my display)
            int referenceSize = bigCharts ? new Double(referenceData[referenceData.length - 1]).intValue() : 1600;
            int imageSizeX = referenceSize;
            int imageSizeY = 800;

            Document document = new Document();
            document.setPageSize(new Rectangle(imageSizeX, imageSizeY));
            document.setMargins(0, 0, 0, 0);
            PdfWriter.getInstance(document, new FileOutputStream(singleFile ? defaultOutputFileName : filename.replaceAll(".csv", ".pdf")));
            document.open();

            for (String category : categoryValues.keySet()) {
                String title = categoriesTranslator.get(category);
                String xLabel = "seconds";
                String yLabel = category;

                JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, categoryValues.get(category), PlotOrientation.VERTICAL, false, false,
                        false);
                BufferedImage image = chart.createBufferedImage(imageSizeX, imageSizeY);

                document.add(Image.getInstance(image, null));
                logger.debug("Wrote category chart " + category);

                if (printCharts) {
                    String newDirName = singleFile ? "vmstat-charts" : filename.replaceAll(".csv", "-charts");
                    ImageEncoder encoder = new KeypointPNGEncoderAdapter();
                    new File(newDirName).mkdir();
                    String chartFile = newDirName + File.separator + category + "Chart.png";
                    encoder.encode(image, new FileOutputStream(chartFile));
                    logger.debug("Wrote category chart to file " + category);
                }
            }

            document.close();

        } catch (FileNotFoundException e) {
            logger.error("Unable to open file: " + filename, e);
        } catch (IOException e) {
            logger.warn("IOException in csv file: " + filename, e);
        } catch (DocumentException e) {
            logger.warn("DocumentException: " + e.getMessage(), e);
        }
    }

    private static double[] getDoubleFromValues(List<String[]> values, int rows, int i) {
        int n = 0;
        double[] valueData = new double[rows];

        for (String[] value : values) {
            valueData[n++] = Double.parseDouble(value[i]);
        }
        return valueData;
    }

    private static double[] diffOperator(double[] valueData) {
        if (valueData.length <= 0) { return valueData; }
        double[] diffResult = new double[valueData.length];
        diffResult[0] = 0;

        for (int n = 1; n < valueData.length; n++) {
            diffResult[n] = valueData[n] - valueData[n - 1];
        }

        return diffResult;
    }

    private static double[] runningAverage(double[] valueData) {
        if (valueData.length <= 0) { throw new IllegalArgumentException("At leat one element is required to calculate an average."); }

        double[] runningAverage = new double[valueData.length];
        runningAverage[0] = valueData[0];

        for (int n = 1; n < valueData.length; n++) {
            runningAverage[n] = runningAverage[n - 1] + (valueData[n] - runningAverage[n - 1]) / (n + 1);
        }

        return runningAverage;
    }

    private static void writeStatsToFile(String category, String unit, double[] values, FileOutputStream statsFOS) {
        if (values.length == 0) {
            logger.warn("Cannot write stat file: No values.");
            return;
        }
        if (statsFOS == null) {
            logger.warn("Cannot write stat file: No file to write to.");
            return;
        }

        double min = values[0];
        double max = values[0];
        double average = values[0];
        double stdev = 0;
        double sum = 0;
        int samples = 1;

        for (double d; samples < values.length; samples++) {
            if ((d = values[samples]) == Double.NaN) {
                continue;
            }
            if (d < min) {
                min = d;
            }
            if (d > max) {
                max = d;
            }
            sum += d;
            double oldAverage = average;
            average = (oldAverage * (samples - 1) + d) / samples;
            // average = oldAverage + (d - oldAverage) / (samples + 1);

            double diffAvg = average - oldAverage;
            if (samples == 1) {
                stdev = 2 * diffAvg * diffAvg;
            } else {
                stdev = ((1.0 - 1.0 / (samples - 1)) * stdev) + samples * diffAvg * diffAvg;
            }
        }

        try {
            category = category.replaceAll("[^a-zA-Z0-9]", ""); // Remove non alphanum chars and invalid symbols
            DecimalFormatSymbols dfs = new DecimalFormatSymbols();
            dfs.setInfinity("0");
            dfs.setNaN("0");
            NumberFormat formatter = new DecimalFormat("#0.000", dfs); // Format doubles so there is no exponent

            StringBuilder sb = new StringBuilder();
            sb.append(category).append(" SAMPLES IS ").append(formatter.format(samples - 1)).append(" ").append("samples").append(";\n");
            statsFOS.write(sb.toString().getBytes());
            logger.debug("Wrote to stat file: \"" + sb.substring(0, sb.length() - 1) + "\"");

            sb = new StringBuilder();
            sb.append(category).append(" SUM IS ").append(formatter.format(sum)).append(" ").append(unit).append(";\n");
            statsFOS.write(sb.toString().getBytes());
            logger.debug("Wrote to stat file: \"" + sb.substring(0, sb.length() - 1) + "\"");

            sb = new StringBuilder();
            sb.append(category).append(" MIN IS ").append(formatter.format(min)).append(" ").append(unit).append(";\n");
            statsFOS.write(sb.toString().getBytes());
            logger.debug("Wrote to stat file: \"" + sb.substring(0, sb.length() - 1) + "\"");

            sb = new StringBuilder();
            sb.append(category).append(" MAX IS ").append(formatter.format(max)).append(" ").append(unit).append(";\n");
            statsFOS.write(sb.toString().getBytes());
            logger.debug("Wrote to stat file: \"" + sb.substring(0, sb.length() - 1) + "\"");

            sb = new StringBuilder();
            sb.append(category).append(" AVG IS ").append(formatter.format(average)).append(" ").append(unit).append(";\n");
            statsFOS.write(sb.toString().getBytes());
            logger.debug("Wrote to stat file: \"" + sb.substring(0, sb.length() - 1) + "\"");

            sb = new StringBuilder();
            sb.append(category).append(" STD IS ").append(formatter.format(stdev)).append(" ").append(unit).append(";\n");
            statsFOS.write(sb.toString().getBytes());
            logger.debug("Wrote to stat file: \"" + sb.substring(0, sb.length() - 1) + "\"");

            statsFOS.flush();
        } catch (IOException e) {
            logger.warn("Cannot write stat file: IO Excetion while writing to file: " + e.getMessage());
        }
    }
}