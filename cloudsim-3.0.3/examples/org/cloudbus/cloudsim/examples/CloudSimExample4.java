/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

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
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


/**
 * A simple example showing how to create
 * two datacenters with one host each and
 * run two cloudlets on them.
 */
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class CloudSimExample4 {

	// Cloudlet, VM, and other necessary variables
	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;

	// Lists for storing data points for plotting
	private static List<Double> responseTimeData = new ArrayList<>();
	private static List<Double> waitTimeData = new ArrayList<>();
	private static List<Double> resourceUtilizationData = new ArrayList<>();

	public static void main(String[] args) {
		Log.printLine("Starting CloudSimExample4...");

		try {
			// Initialize CloudSim
			int num_user = 1;  // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;
			CloudSim.init(num_user, calendar, trace_flag);

			// Create Datacenters
			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("Datacenter_1");

			// Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			// Create VMs
			vmlist = new ArrayList<>();
			int vmid = 0;
			int mips = 250;
			long size = 10000; // image size (MB)
			int ram = 512; // VM memory (MB)
			long bw = 1000;
			int pesNumber = 1; // number of CPUs
			String vmm = "Xen";
			Vm vm1 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			vmid++;
			Vm vm2 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			vmlist.add(vm1);
			vmlist.add(vm2);
			broker.submitVmList(vmlist);

			// Create Cloudlets
			cloudletList = new ArrayList<>();
			int id = 0;
			long length = 40000;
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
			broker.submitCloudletList(cloudletList);

			// Bind Cloudlets to VMs
			broker.bindCloudletToVm(cloudlet1.getCloudletId(), vm1.getId());
			broker.bindCloudletToVm(cloudlet2.getCloudletId(), vm2.getId());

			// Start the simulation
			CloudSim.startSimulation();

			// Collect data for plotting
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			CloudSim.stopSimulation();

			// Collect response time, wait time, and resource utilization
			for (Cloudlet cloudlet : newList) {
				responseTimeData.add(cloudlet.getActualCPUTime());
				waitTimeData.add(cloudlet.getExecStartTime());
				resourceUtilizationData.add(cloudlet.getFinishTime() - cloudlet.getExecStartTime());
			}

			// Create and display graphs
			createAndShowGraph(responseTimeData, "Response Time", "Cloudlet ID", "Time (s)");
			createAndShowGraph(waitTimeData, "Wait Time", "Cloudlet ID", "Time (s)");
			createAndShowGraph(resourceUtilizationData, "Resource Utilization", "Cloudlet ID", "Utilization (%)");

			Log.printLine("CloudSimExample4 finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
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
		hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, new VmSchedulerSpaceShared(peList)));
		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.001;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<>();
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

	// Function to create and show a graph
	private static void createAndShowGraph(List<Double> data, String title, String xAxisLabel, String yAxisLabel) {
		// Create the series for the data
		XYSeries series = new XYSeries(title);
		for (int i = 0; i < data.size(); i++) {
			series.add(i, data.get(i));
		}

		// Create a dataset and add the series
		XYSeriesCollection dataset = new XYSeriesCollection(series);

		// Create the chart
		JFreeChart chart = ChartFactory.createXYLineChart(
				title,       // chart title
				xAxisLabel,  // x axis label
				yAxisLabel,  // y axis label
				dataset,     // dataset
				PlotOrientation.VERTICAL, // orientation
				true,        // include legend
				true,        // tooltips
				false        // URLs
		);

		// Create a panel to display the chart
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(800, 600));

		// Create a frame and add the panel
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(chartPanel, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}
}
