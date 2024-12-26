package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.util.List;
import java.util.Calendar;
import java.util.Collections;
import java.util.ArrayList;

public class P2 {
    // Time series for each graph
    private static TimeSeries responseTimeSeries = new TimeSeries("Response Time");
    private static TimeSeries resourceUtilizationSeries = new TimeSeries("Resource Utilization");
    private static TimeSeries waitTimeSeries = new TimeSeries("Wait Time");

    public static void main(String[] args) {
        Log.printLine("Starting CloudSimExample...");

        try {
            // Initialize CloudSim
            int num_user = 1;   // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events
            CloudSim.init(num_user, calendar, trace_flag);

            // Create Datacenter
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            // Create Broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Create Virtual Machines (VMs)
            List<Vm> vmlist = new ArrayList<Vm>();

            int vmid = 0;
            int mips = 250;
            long size = 10000; // VM image size (MB)
            int ram = 512; // VM memory (MB)
            long bw = 1000;
            int pesNumber = 1; // number of CPUs
            String vmm = "Xen"; // VMM name

            Vm vm1 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vmid++;
            Vm vm2 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

            vmlist.add(vm1);
            vmlist.add(vm2);

            broker.submitVmList(vmlist);

            // Create Cloudlets
            List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();

            int id = 0;
            long length = 250000;
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            Cloudlet cloudlet1 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet1.setUserId(brokerId);
            id++;
            Cloudlet cloudlet2 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet2.setUserId(brokerId);

            cloudletList.add(cloudlet1);
            cloudletList.add(cloudlet2);

            // Sort cloudlets by priority (for demonstration)
            Collections.sort(cloudletList, (c1, c2) -> Integer.compare(c2.getCloudletId(), c1.getCloudletId()));

            broker.submitCloudletList(cloudletList);

            // Bind cloudlets to VMs
            broker.bindCloudletToVm(cloudletList.get(0).getCloudletId(), vm1.getId());  // First cloudlet to VM1
            broker.bindCloudletToVm(cloudletList.get(1).getCloudletId(), vm2.getId());  // Second cloudlet to VM2

            // Start the simulation
            CloudSim.startSimulation();

            // Collect results after simulation finishes
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            // Collect response time, resource utilization, and wait time data
            for (Cloudlet cloudlet : newList) {
                // Response time calculation: Finish Time - Submission Time
                double responseTime = cloudlet.getFinishTime() - cloudlet.getSubmissionTime();
                responseTimeSeries.addOrUpdate(new Second(), responseTime);

                // Resource Utilization: Ratio of actual CPU time used over the cloudlet length (as a proxy for CPU usage)
                double resourceUtilization = (cloudlet.getFinishTime() - cloudlet.getExecStartTime()) / (double) cloudlet.getCloudletLength(); // More accurate CPU utilization calculation
                resourceUtilizationSeries.addOrUpdate(new Second(), resourceUtilization);

                // Wait Time: Time from submission to start of execution
                double waitTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
                waitTimeSeries.addOrUpdate(new Second(), waitTime);
            }

            // Display graphs
            showGraphs();

            // Print Cloudlet Execution Results
            printCloudletList(newList);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }

    }

    private static Datacenter createDatacenter(String name) {
        // Datacenter creation logic (same as before)
        // You should configure the Datacenter with proper hosts and resources
        // For this simple example, we'll return null
        return null;
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

    private static void showGraphs() {
        // Create a JFrame for each of the three graphs
        JFrame responseTimeFrame = createChartFrame("Response Time", responseTimeSeries);
        JFrame resourceUtilizationFrame = createChartFrame("Resource Utilization", resourceUtilizationSeries);
        JFrame waitTimeFrame = createChartFrame("Wait Time", waitTimeSeries);

        // Show the frames
        responseTimeFrame.setVisible(true);
        resourceUtilizationFrame.setVisible(true);
        waitTimeFrame.setVisible(true);
    }

    private static JFrame createChartFrame(String title, TimeSeries timeSeries) {
        // Create a time series collection for the dataset
        TimeSeriesCollection dataset = new TimeSeriesCollection(timeSeries);

        // Create the chart
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,          // Title
                "Time",         // X-Axis Label
                "Value",        // Y-Axis Label
                dataset,        // Dataset
                false,          // Include legend
                true,           // Tooltips
                false           // URLs
        );

        // Create a panel to hold the chart
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        // Create the frame and add the chart panel
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(chartPanel);
        frame.pack();

        return frame;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;
        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.printLine("Cloudlet ID: " + cloudlet.getCloudletId());
            Log.printLine(" Status: " + cloudlet.getStatus());
            Log.printLine(" Data center ID: " + cloudlet.getResourceId());
            Log.printLine(" VM ID: " + cloudlet.getVmId());
            Log.printLine(" Finish Time: " + cloudlet.getFinishTime());
            double cost = cloudlet.getFinishTime() * 0.1;  // Example cost calculation
            Log.printLine(" Cost: " + cost);
            Log.printLine("===========================");
        }
    }
}
