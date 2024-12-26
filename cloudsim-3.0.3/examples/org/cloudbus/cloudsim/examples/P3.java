package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.renderer.category.BarRenderer;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.GridLayout;

public class P3 {
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;

    public static void main(String[] args) {
        Log.printLine("Starting CloudSim Visualization...");

        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);
            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Create VMs
            vmlist = new ArrayList<Vm>();
            int mips = 250;
            long size = 10000;
            int ram = 2048;
            long bw = 1000;
            int pesNumber = 1;
            String vmm = "Xen";

            // Create 5 VMs with different MIPS
            for (int i = 0; i < 5; i++) {
                Vm vm = new Vm(i, brokerId, mips * (i + 1), pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
                vmlist.add(vm);
            }

            broker.submitVmList(vmlist);

            // Create Cloudlets
            cloudletList = new ArrayList<Cloudlet>();
            long length = 40000;
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            // Create 10 cloudlets with different lengths
            for (int i = 0; i < 10; i++) {
                PriorityCloudlet cloudlet = new PriorityCloudlet(
                        i,
                        length * (1 + (i % 3)), // Vary length for different execution times
                        pesNumber,
                        fileSize,
                        outputSize,
                        utilizationModel,
                        i % 3 + 1  // Priority 1-3
                );
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            broker.submitCloudletList(cloudletList);

            // Bind cloudlets to VMs in a round-robin fashion
            for (int i = 0; i < cloudletList.size(); i++) {
                broker.bindCloudletToVm(i, i % vmlist.size());
            }

            CloudSim.startSimulation();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            printCloudletList(newList);
            visualizeMetrics(newList);

            Log.printLine("CloudSim Visualization finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static void visualizeMetrics(List<Cloudlet> list) {
        DefaultCategoryDataset responseTimeDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset utilizationDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset waitingTimeDataset = new DefaultCategoryDataset();

        DecimalFormat dft = new DecimalFormat("###.##");

        for (Cloudlet cloudlet : list) {
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                String cloudletId = "CL" + cloudlet.getCloudletId(); // Shorter label

                double waitingTime = cloudlet.getExecStartTime() - ((PriorityCloudlet) cloudlet).getArrivalTime();
                double responseTime = cloudlet.getFinishTime() - ((PriorityCloudlet) cloudlet).getArrivalTime();
                double resourceUtilization = cloudlet.getActualCPUTime() / (cloudlet.getFinishTime() - cloudlet.getExecStartTime());

                responseTimeDataset.addValue(responseTime, "Response Time", cloudletId);
                utilizationDataset.addValue(resourceUtilization, "Resource Utilization", cloudletId);
                waitingTimeDataset.addValue(waitingTime, "Waiting Time", cloudletId);
            }
        }

        // Create and customize charts
        JFreeChart[] charts = {
                createCustomizedChart("Response Time by Cloudlet", "Cloudlet ID", "Response Time (seconds)", responseTimeDataset),
                createCustomizedChart("Resource Utilization by Cloudlet", "Cloudlet ID", "Resource Utilization", utilizationDataset),
                createCustomizedChart("Waiting Time by Cloudlet", "Cloudlet ID", "Waiting Time (seconds)", waitingTimeDataset)
        };

        // Create and display frame
        JFrame frame = new JFrame("CloudSim Metrics Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(2, 2));
        frame.setSize(1200, 800);

        // Add charts to frame
        for (JFreeChart chart : charts) {
            ChartPanel panel = new ChartPanel(chart);
            frame.add(panel);
        }

        frame.setVisible(true);
    }

    private static JFreeChart createCustomizedChart(String title, String xLabel, String yLabel, DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                title,
                xLabel,
                yLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        // Set the width of the bars (0.1 means 10% of the available space)
        renderer.setMaximumBarWidth(0.1);

        // You can also adjust the gap between bars if needed
        renderer.setItemMargin(0.02);

        return chart;
    }

    // [Previous methods remain the same: createDatacenter, createBroker, printCloudletList, PriorityCloudlet class]

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<Host>();
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        int hostId = 0;
        int ram = 2048;
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
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
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

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;
        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent +
                "Time" + indent + "Start Time" + indent + "Finish Time" + indent +
                "Waiting Time" + indent + "Response Time" + indent + "Resource Utilization");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                double waitingTime = cloudlet.getExecStartTime() - ((PriorityCloudlet) cloudlet).getArrivalTime();
                double responseTime = cloudlet.getFinishTime() - ((PriorityCloudlet) cloudlet).getArrivalTime();
                double resourceUtilization = cloudlet.getActualCPUTime() / (cloudlet.getFinishTime() - cloudlet.getExecStartTime());

                Log.print(indent + cloudlet.getCloudletId() + indent + indent);
                Log.print("SUCCESS");
                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent +
                        dft.format(cloudlet.getExecStartTime()) + indent + indent +
                        dft.format(cloudlet.getFinishTime()) + indent + indent +
                        dft.format(waitingTime) + indent + indent +
                        dft.format(responseTime) + indent + indent +
                        dft.format(resourceUtilization));
            }
        }
    }

    static class PriorityCloudlet extends Cloudlet {
        private int priority;
        private double arrivalTime;

        public PriorityCloudlet(int id, long length, int pesNumber, long fileSize, long outputSize,
                                UtilizationModel utilizationModel, int priority) {
            super(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            this.priority = priority;
            this.arrivalTime = CloudSim.clock();
        }

        public int getPriority() {
            return priority;
        }

        public double getArrivalTime() {
            return arrivalTime;
        }
    }
}