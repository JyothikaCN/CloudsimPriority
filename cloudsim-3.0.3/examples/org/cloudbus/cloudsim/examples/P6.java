package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class P6 {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;

    public static void main(String[] args) {
        Log.printLine("Starting CloudSimMetricsExample...");

        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            vmlist = createVM(brokerId, 5);
            cloudletList = createCloudlet(brokerId, 10);

            broker.submitVmList(vmlist);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList(newList);

            // Plot all metrics in one window
            displayAllCharts(newList);

            Log.printLine("CloudSimMetricsExample finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error.");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();

        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        int hostId = 0;
        int ram = 2048;
        long storage = 1000000;
        int bw = 10000;

        hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
                new VmSchedulerTimeShared(peList)));

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.1;
        double costPerBw = 0.1;
        LinkedList<Storage> storageList = new LinkedList<>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
                cost, costPerMem, costPerStorage, costPerBw);

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
        }
        return broker;
    }

    private static List<Vm> createVM(int userId, int vms) {
        LinkedList<Vm> list = new LinkedList<>();

        long size = 10000;
        int ram = 512;
        int mips = 1000;
        long bw = 1000;
        int pesNumber = 1;
        String vmm = "Xen";

        for (int i = 0; i < vms; i++) {
            list.add(new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared()));
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets) {
        LinkedList<Cloudlet> list = new LinkedList<>();

        long length = 1000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < cloudlets; i++) {
            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(userId);
            list.add(cloudlet);
        }

        return list;
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
                        indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }

    private static void displayAllCharts(List<Cloudlet> cloudlets) {
        DefaultCategoryDataset responseTimeDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset waitTimeDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset utilizationDataset = new DefaultCategoryDataset();

        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                double responseTime = cloudlet.getFinishTime() - cloudlet.getExecStartTime();
                double waitTime = cloudlet.getExecStartTime();
                double utilization = cloudlet.getActualCPUTime() / responseTime * 100;

                responseTimeDataset.addValue(responseTime, "Response Time", "Cloudlet " + cloudlet.getCloudletId());
                waitTimeDataset.addValue(waitTime, "Wait Time", "Cloudlet " + cloudlet.getCloudletId());
                utilizationDataset.addValue(utilization, "Resource Utilization", "Cloudlet " + cloudlet.getCloudletId());
            }
        }

        JFreeChart responseTimeChart = ChartFactory.createBarChart(
                "Response Time", "Cloudlet", "Time (s)",
                responseTimeDataset, PlotOrientation.VERTICAL, true, true, false);

        JFreeChart waitTimeChart = ChartFactory.createBarChart(
                "Wait Time", "Cloudlet", "Time (s)",
                waitTimeDataset, PlotOrientation.VERTICAL, true, true, false);

        JFreeChart utilizationChart = ChartFactory.createBarChart(
                "Utilization", "Cloudlet", "Utilization (%)",
                utilizationDataset, PlotOrientation.VERTICAL, true, true, false);

        ChartPanel responseTimePanel = new ChartPanel(responseTimeChart);
        ChartPanel waitTimePanel = new ChartPanel(waitTimeChart);
        ChartPanel utilizationPanel = new ChartPanel(utilizationChart);

        JPanel mainPanel = new JPanel(new GridLayout(3, 1));
        mainPanel.add(responseTimePanel);
        mainPanel.add(waitTimePanel);
        mainPanel.add(utilizationPanel);

        ApplicationFrame frame = new ApplicationFrame("Cloudlet Metrics");
        frame.setContentPane(mainPanel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }
}
