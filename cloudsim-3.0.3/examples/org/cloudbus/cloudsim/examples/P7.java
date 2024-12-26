package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.*;
import java.text.DecimalFormat;
import javax.swing.*;

public class P7 {

    public static void main(String[] args) {
        Log.printLine("Starting CloudSimPrioritySchedulingExample...");

        try {
            int numUsers = 1; // Number of users (cloudlets)
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false; // Disabling tracing for simplicity

            // Initialize CloudSim
            CloudSim.init(numUsers, calendar, traceFlag);

            // Create Datacenter
            Datacenter datacenter = createDatacenter("Datacenter_0");

            // Create a Broker (DatacenterBroker)
            PriorityBroker broker = new PriorityBroker("PriorityBroker");

            // Create VMs and Cloudlets
            List<Vm> vmList = createVMs(broker.getId(), 3); // 3 VMs
            List<Cloudlet> cloudletList = createPriorityCloudlets(broker.getId(), 6); // 6 Cloudlets with priorities

            // Submit VMs and Cloudlets to broker
            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            // Start the simulation
            CloudSim.startSimulation();

            // Retrieve the results
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            // Stop simulation
            CloudSim.stopSimulation();

            // Print results
            printCloudletList(newList);

            // Collect metrics
            List<Double> waitTimes = new ArrayList<>();
            List<Double> responseTimes = new ArrayList<>();
            List<Double> resourceUtilizations = new ArrayList<>();
            collectMetrics(newList, waitTimes, responseTimes, resourceUtilizations);

            // Plot graphs
            plotGraphs(waitTimes, responseTimes, resourceUtilizations);

            Log.printLine("CloudSimPrioritySchedulingExample finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();
        int mips = 1000; // MIPS (Million Instructions Per Second) per core

        // Create PEs (Processing Elements)
        for (int i = 0; i < 4; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(mips)));
        }

        // Create Host
        int hostId = 0;
        int ram = 8192; // RAM in MB
        long storage = 100000; // Storage in MB
        int bw = 10000; // Bandwidth
        hostList.add(new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList)
        ));

        // Create Datacenter Characteristics
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double timeZone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.1;
        double costPerBw = 0.1;
        LinkedList<Storage> storageList = new LinkedList<>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static List<Vm> createVMs(int brokerId, int vmCount) {
        List<Vm> vms = new ArrayList<>();
        int mips = 250;
        long size = 10000; // VM image size (in MB)
        int ram = 512;
        long bw = 1000;
        int pesNumber = 1;
        String vmm = "Xen";

        for (int i = 0; i < vmCount; i++) {
            Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vms.add(vm);
        }

        return vms;
    }

    private static List<Cloudlet> createPriorityCloudlets(int brokerId, int cloudletCount) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        long length = 40000; // Cloudlet length (instructions)
        long fileSize = 300; // Input file size (in MB)
        long outputSize = 300; // Output file size (in MB)
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        // Accept priority as an argument for each cloudlet
        for (int i = 0; i < cloudletCount; i++) {
            int priority = (i % 3) + 1; // Assign priority values 1, 2, 3 cyclically
            Cloudlet cloudlet = new PriorityCloudlet(i, length, pesNumber, fileSize, outputSize,
                    utilizationModel, utilizationModel, utilizationModel, priority);
            cloudlet.setUserId(brokerId);
            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }

    private static void collectMetrics(List<Cloudlet> cloudletList, List<Double> waitTimes,
                                       List<Double> responseTimes, List<Double> resourceUtilizations) {
        for (Cloudlet cloudlet : cloudletList) {
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                double waitTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
                double responseTime = cloudlet.getFinishTime() - cloudlet.getSubmissionTime();
                double resourceUtilization = cloudlet.getActualCPUTime() / cloudlet.getCloudletLength();

                waitTimes.add(waitTime);
                responseTimes.add(responseTime);
                resourceUtilizations.add(resourceUtilization);
            }
        }
    }

    private static void plotGraphs(List<Double> waitTimes, List<Double> responseTimes,
                                   List<Double> resourceUtilizations) {
        // Prepare the datasets for each metric
        DefaultCategoryDataset waitTimeDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset responseTimeDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset resourceUtilizationDataset = new DefaultCategoryDataset();

        // Add data to the datasets
        for (int i = 0; i < waitTimes.size(); i++) {
            waitTimeDataset.addValue(waitTimes.get(i), "Wait Time", "Cloudlet " + (i + 1));
            responseTimeDataset.addValue(responseTimes.get(i), "Response Time", "Cloudlet " + (i + 1));
            resourceUtilizationDataset.addValue(resourceUtilizations.get(i), "Resource Utilization", "Cloudlet " + (i + 1));
        }

        // Create the charts
        JFreeChart waitTimeChart = ChartFactory.createBarChart("Cloudlet Wait Time", "Cloudlet", "Time (seconds)", waitTimeDataset);
        JFreeChart responseTimeChart = ChartFactory.createBarChart("Cloudlet Response Time", "Cloudlet", "Time (seconds)", responseTimeDataset);
        JFreeChart resourceUtilizationChart = ChartFactory.createBarChart("Cloudlet Resource Utilization", "Cloudlet", "Utilization", resourceUtilizationDataset);

        // Display the charts
        JFrame frame = new JFrame("CloudSim Metrics");
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new ChartPanel(waitTimeChart));
        frame.add(new ChartPanel(responseTimeChart));
        frame.add(new ChartPanel(resourceUtilizationChart));

        frame.pack();
        frame.setVisible(true);
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (Cloudlet cloudlet : list) {
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + dft.format(cloudlet.getExecStartTime()) + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }

    // Custom class for PriorityCloudlet
    public static class PriorityCloudlet extends Cloudlet {
        private int priority;

        public PriorityCloudlet(int cloudletId, long cloudletLength, int pesNumber,
                                long cloudletFileSize, long cloudletOutputSize,
                                UtilizationModel utilizationCpu,
                                UtilizationModel utilizationMemory,
                                UtilizationModel utilizationBw,
                                int priority) {
            super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize,
                    utilizationCpu, utilizationMemory, utilizationBw);
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    // Custom Broker to handle priority scheduling
    public static class PriorityBroker extends DatacenterBroker {

        public PriorityBroker(String name) throws Exception {
            super(name);
        }

        @Override
        protected void submitCloudlets() {
            // Sort cloudlets by priority in descending order (higher priority first)
            getCloudletList().sort((c1, c2) -> {
                PriorityCloudlet pc1 = (PriorityCloudlet) c1;
                PriorityCloudlet pc2 = (PriorityCloudlet) c2;
                return Integer.compare(pc2.getPriority(), pc1.getPriority());
            });
            super.submitCloudlets();
        }
    }
}
