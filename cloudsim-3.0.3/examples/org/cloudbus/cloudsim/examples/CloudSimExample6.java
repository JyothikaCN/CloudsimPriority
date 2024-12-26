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
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.examples.CloudSimExample6;
import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.text.DecimalFormat;
import java.util.*;

public class CloudSimExample6 extends ApplicationFrame {

	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;

	public CloudSimExample6(JFreeChart chart, String title) {
		super(title);
		setContentPane(new ChartPanel(chart));
	}

	private static List<Vm> createVM(int userId, int vms) {
		LinkedList<Vm> list = new LinkedList<Vm>();
		long size = 10000; // image size (MB)
		int ram = 512; // vm memory (MB)
		int mips = 1000;
		long bw = 1000;
		int pesNumber = 1; // number of CPUs
		String vmm = "Xen"; // VMM name

		Vm[] vm = new Vm[vms];
		for (int i = 0; i < vms; i++) {
			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			list.add(vm[i]);
		}
		return list;
	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets) {
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();
		long length = 1000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();
		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}
		return list;
	}

	public static void main(String[] args) {
		Log.printLine("Starting CloudSimExample6...");

		try {
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Create Datacenters
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			Datacenter datacenter1 = createDatacenter("Datacenter_1");

			// Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			// Create VMs and Cloudlets and send them to broker
			vmlist = createVM(brokerId, 20); // creating 20 vms
			cloudletList = createCloudlet(brokerId, 40); // creating 40 cloudlets

			broker.submitVmList(vmlist);
			broker.submitCloudletList(cloudletList);

			// Start the simulation
			CloudSim.startSimulation();

			// Stop the simulation
			CloudSim.stopSimulation();

			// Print results
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			printCloudletList(newList);

			// Plot the graphs
			plotGraphs(newList);

			Log.printLine("CloudSimExample6 finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static Datacenter createDatacenter(String name) {
		List<Host> hostList = new ArrayList<Host>();
		List<Pe> peList1 = new ArrayList<Pe>();
		int mips = 1000;

		peList1.add(new Pe(0, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mips)));

		List<Pe> peList2 = new ArrayList<Pe>();
		peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
		peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

		int hostId = 0;
		int ram = 2048;
		long storage = 1000000;
		int bw = 10000;

		hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList1, new VmSchedulerTimeShared(peList1)));
		hostId++;

		hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList2, new VmSchedulerTimeShared(peList2)));

		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.1;
		double costPerBw = 0.1;
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
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
				"Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");

				Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getFinishTime()));
			}
		}
	}

	private static void plotGraphs(List<Cloudlet> cloudletList) {
		DefaultCategoryDataset responseTimeDataset = new DefaultCategoryDataset();
		DefaultCategoryDataset waitingTimeDataset = new DefaultCategoryDataset();
		DefaultCategoryDataset resourceUtilizationDataset = new DefaultCategoryDataset();

		DecimalFormat df = new DecimalFormat("###.##");

		for (Cloudlet cloudlet : cloudletList) {
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				// Response Time (Finish time - Start time)
				double responseTime = cloudlet.getFinishTime() - cloudlet.getExecStartTime();
				responseTimeDataset.addValue(responseTime, "Response Time", "Cloudlet " + cloudlet.getCloudletId());

				// Waiting Time (Start time - Submit time)
				double waitingTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
				waitingTimeDataset.addValue(waitingTime, "Waiting Time", "Cloudlet " + cloudlet.getCloudletId());

				// Resource Utilization (CPU Time used)
				double cpuTime = cloudlet.getActualCPUTime();
				resourceUtilizationDataset.addValue(cpuTime, "Resource Utilization", "Cloudlet " + cloudlet.getCloudletId());
			}
		}

		JFreeChart responseTimeChart = ChartFactory.createBarChart(
				"Response Time", "Cloudlet", "Time (seconds)", responseTimeDataset, PlotOrientation.VERTICAL, true, true, false);

		JFreeChart waitingTimeChart = ChartFactory.createBarChart(
				"Waiting Time", "Cloudlet", "Time (seconds)", waitingTimeDataset, PlotOrientation.VERTICAL, true, true, false);

		JFreeChart resourceUtilizationChart = ChartFactory.createBarChart(
				"Resource Utilization", "Cloudlet", "CPU Time (seconds)", resourceUtilizationDataset, PlotOrientation.VERTICAL, true, true, false);

		// Create and display charts in a separate frame
		CloudSimExample6 responseTimeChartFrame = new CloudSimExample6(responseTimeChart, "Response Time Graph");
		responseTimeChartFrame.pack();
		responseTimeChartFrame.setVisible(true);

		CloudSimExample6 waitingTimeChartFrame = new CloudSimExample6(waitingTimeChart, "Waiting Time Graph");
		waitingTimeChartFrame.pack();
		waitingTimeChartFrame.setVisible(true);

		CloudSimExample6 resourceUtilizationChartFrame = new CloudSimExample6(resourceUtilizationChart, "Resource Utilization Graph");
		resourceUtilizationChartFrame.pack();
		resourceUtilizationChartFrame.setVisible(true);
	}
}
