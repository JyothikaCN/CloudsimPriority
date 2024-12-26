package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Comparator;
import javax.swing.*;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import java.awt.Color;
import java.util.LinkedList;

public class PriorityCloudSim {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;
    private static List<Double> waitTimeList = new ArrayList<>();
    private static List<Double> responseTimeList = new ArrayList<>();
    private static List<Double> resourceUtilizationList = new ArrayList<>();

    public static class PriorityCloudlet extends Cloudlet {
        private int priority;

        public PriorityCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                                UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
                                UtilizationModel utilizationModelBw, int priority) {
            super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    public static void main(String[] args) {
        Log.printLine("Starting CloudSim with Priority Scheduling...");

        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            vmlist = new ArrayList<>();

            int vmid = 0;
            int mips = 2000;
            long size = 10000;
            int ram = 2048;
            long bw = 1000;
            int pesNumber = 2;
            String vmm = "Xen";

            Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vmlist.add(vm);
            broker.submitVmList(vmlist);

            cloudletList = new ArrayList<>();
            int id = 0;
            long length = 40000;
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            PriorityCloudlet cloudlet1 = new PriorityCloudlet(id++, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, 1);
            cloudlet1.setUserId(brokerId);
            cloudlet1.setVmId(vmid);
            cloudletList.add(cloudlet1);

            PriorityCloudlet cloudlet2 = new PriorityCloudlet(id++, length * 2, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, 5);
            cloudlet2.setUserId(brokerId);
            cloudlet2.setVmId(vmid);
            cloudletList.add(cloudlet2);

            PriorityCloudlet cloudlet3 = new PriorityCloudlet(id++, length * 3, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, 9);
            cloudlet3.setUserId(brokerId);
            cloudlet3.setVmId(vmid);
            cloudletList.add(cloudlet3);

            cloudletList.sort(Comparator.comparingInt(cl -> ((PriorityCloudlet) cl).getPriority()));

            submitCloudletsWithPriority(broker, cloudletList);

            // Start simulation
            CloudSim.startSimulation();

            // After simulation, collect metrics and process the results
            CloudSim.stopSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            collectMetrics(newList);
            printCloudletList(newList);
            plotGraphs();

            Log.printLine("CloudSim finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();

        int mips = 2000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList.add(new Pe(1, new PeProvisionerSimple(mips)));

        int hostId = 0;
        int ram = 4096;
        long storage = 1000000;
        int bw = 10000;

        hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, new VmSchedulerTimeShared(peList)));

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        List<Storage> storageList = new ArrayList<>();  // Changed to ArrayList

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            // Fixed Datacenter constructor issue by passing the correct VmAllocationPolicy
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static void submitCloudletsWithPriority(DatacenterBroker broker, List<Cloudlet> cloudletList) {
        // Fixed delay value for each cloudlet (in milliseconds)
        double delay = 100.0;
        double cumulativeDelay = 0;

        for (Cloudlet cloudlet : cloudletList) {
            // Set the submission time with cumulative delay for each cloudlet
            cumulativeDelay += delay;
            cloudlet.setSubmissionTime(cumulativeDelay);
            Log.printLine("Cloudlet ID: " + cloudlet.getCloudletId() + " Submission Time: " + cloudlet.getSubmissionTime());
        }

        broker.submitCloudletList(cloudletList);
    }

    private static void collectMetrics(List<Cloudlet> newList) {
        for (Cloudlet cloudlet : newList) {
            // Ensure the simulation has started to correctly calculate the execution start time
            double waitTime =  cloudlet.getFinishTime()-cloudlet.getSubmissionTime();
            waitTimeList.add(waitTime);

            double responseTime = cloudlet.getFinishTime() - cloudlet.getSubmissionTime();
            responseTimeList.add(responseTime);

            double resourceUtilization = cloudlet.getActualCPUTime() / cloudlet.getCloudletLength();
            resourceUtilizationList.add(resourceUtilization);
        }
    }

    private static void plotGraphs() {
        DefaultCategoryDataset waitTimeDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset responseTimeDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset resourceUtilizationDataset = new DefaultCategoryDataset();

        for (int i = 0; i < waitTimeList.size(); i++) {
            String cloudletLabel = "Priority " + ((PriorityCloudlet)cloudletList.get(i)).getPriority();
            waitTimeDataset.addValue(waitTimeList.get(i), "Wait Time", cloudletLabel);
            responseTimeDataset.addValue(responseTimeList.get(i), "Response Time", cloudletLabel);
            resourceUtilizationDataset.addValue(resourceUtilizationList.get(i), "Resource Utilization", cloudletLabel);
        }

        JFreeChart waitTimeChart = createCustomChart("Cloudlet Wait Time", "Priority Level", "Wait Time (ms)", waitTimeDataset);
        JFreeChart responseTimeChart = createCustomChart("Cloudlet Response Time", "Priority Level", "Response Time (ms)", responseTimeDataset);
        JFreeChart resourceUtilizationChart = createCustomChart("Cloudlet Resource Utilization", "Priority Level", "Resource Utilization", resourceUtilizationDataset);

        JFrame frame = new JFrame("CloudSim Metrics");
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new ChartPanel(waitTimeChart));
        frame.add(new ChartPanel(responseTimeChart));
        frame.add(new ChartPanel(resourceUtilizationChart));

        frame.pack();
        frame.setVisible(true);
    }

    private static JFreeChart createCustomChart(String title, String categoryAxisLabel, String valueAxisLabel, DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                title,
                categoryAxisLabel,
                valueAxisLabel,
                dataset
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        renderer.setSeriesPaint(0, new Color(41, 128, 185));

        chart.setBackgroundPaint(Color.white);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        return chart;
    }

    private static void printCloudletList(List<Cloudlet> cloudletList) {
        String indent = "    ";
        Log.printLine();
        Log.printLine("Cloudlet ID" + indent + "Submission Time" + indent + "Start Time" + indent + "Finish Time" + indent + "Wait Time");

        DecimalFormat dft = new DecimalFormat("###.##");

        for (Cloudlet cloudlet : cloudletList) {
            Log.print(cloudlet.getCloudletId() + indent);
            Log.print(dft.format(cloudlet.getSubmissionTime()) + indent);
            Log.print(dft.format(cloudlet.getExecStartTime()) + indent);
            Log.print(dft.format(cloudlet.getFinishTime()) + indent);
            Log.printLine(dft.format(cloudlet.getExecStartTime() - cloudlet.getSubmissionTime()));
        }
    }
}
