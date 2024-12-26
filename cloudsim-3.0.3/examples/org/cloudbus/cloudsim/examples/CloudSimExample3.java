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


/**
 * A simple example showing how to create
 * a datacenter with two hosts and run two
 * cloudlets on it. The cloudlets run in
 * VMs with different MIPS requirements.
 * The cloudlets will take different time
 * to complete the execution depending on
 * the requested VM performance.
 */
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.text.DecimalFormat;
import java.util.*;

public class CloudSimExample3 {

	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;

	private static List<Double> responseTimes = new ArrayList<>();
	private static List<Double> waitTimes = new ArrayList<>();
	private static List<Double> utilizationRates = new ArrayList<>();

	public static void main(String[] args) {
		Log.printLine("Starting CloudSimExampleWithGraphs...");

		try {
			// Step 1: Initialize the CloudSim package
			int num_user = 1;   // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			// Step 2: Create Datacenter
			Datacenter datacenter0 = createDatacenter("Datacenter_0");

			// Step 3: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			// Step 4: Create VMs
			vmlist = new ArrayList<>();

			// VM properties
			int vmid = 0;
			int mips = 250;
			long size = 10000; // image size (MB)
			int ram = 2048;    // vm memory (MB)
			long bw = 1000;
			int pesNumber = 1; // number of CPUs
			String vmm = "Xen";

			// Create two VMs
			Vm vm1 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			Vm vm2 = new Vm(++vmid, brokerId, mips * 2, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

			vmlist.add(vm1);
			vmlist.add(vm2);

			broker.submitVmList(vmlist);

			// Step 5: Create Cloudlets
			cloudletList = new ArrayList<>();

			// Cloudlet properties
			int id = 0;
			long length = 40000;
			long fileSize = 300;
			long outputSize = 300;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			Cloudlet cloudlet1 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			Cloudlet cloudlet2 = new Cloudlet(++id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);

			cloudlet1.setUserId(brokerId);
			cloudlet2.setUserId(brokerId);

			cloudletList.add(cloudlet1);
			cloudletList.add(cloudlet2);

			broker.submitCloudletList(cloudletList);

			// Bind cloudlets to specific VMs
			broker.bindCloudletToVm(cloudlet1.getCloudletId(), vm1.getId());
			broker.bindCloudletToVm(cloudlet2.getCloudletId(), vm2.getId());

			// Step 6: Start simulation
			CloudSim.startSimulation();

			// Step 7: Retrieve results
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			CloudSim.stopSimulation();

			// Step 8: Print and plot results
			printCloudletList(newList);
			collectDataForGraphs(newList);
			plotGraphs();

			Log.printLine("CloudSimExampleWithGraphs finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Simulation terminated due to an unexpected error.");
		}
	}

	private static Datacenter createDatacenter(String name) {
		List<Host> hostList = new ArrayList<>();

		int mips = 1000;
		List<Pe> peList1 = new ArrayList<>();
		peList1.add(new Pe(0, new PeProvisionerSimple(mips)));

		List<Pe> peList2 = new ArrayList<>();
		peList2.add(new Pe(0, new PeProvisionerSimple(mips)));

		int ram = 2048; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;

		hostList.add(new Host(0, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw),
				storage, peList1, new VmSchedulerTimeShared(peList1)));

		hostList.add(new Host(1, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw),
				storage, peList2, new VmSchedulerTimeShared(peList2)));

		String arch = "x86";      // system architecture
		String os = "Linux";      // operating system
		String vmm = "Xen";
		double time_zone = 10.0;  // time zone this resource located
		double cost = 3.0;        // cost per CPU
		double costPerMem = 0.05; // cost per memory
		double costPerStorage = 0.001; // cost per storage
		double costPerBw = 0.0;   // cost per bandwidth

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList,
				time_zone, cost, costPerMem, costPerStorage, costPerBw);

		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList),
					new LinkedList<>(), 0);
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

	private static void printCloudletList(List<Cloudlet> list) {
		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent +
				"Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (Cloudlet cloudlet : list) {
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");

				Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + cloudlet.getVmId() +
						indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + dft.format(cloudlet.getExecStartTime()) +
						indent + indent + dft.format(cloudlet.getFinishTime()));
			}
		}
	}

	private static void collectDataForGraphs(List<Cloudlet> cloudlets) {
		for (Cloudlet cloudlet : cloudlets) {
			double responseTime = cloudlet.getFinishTime() - cloudlet.getExecStartTime();
			double waitTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
			double utilizationRate = (cloudlet.getActualCPUTime() / responseTime) * 100;

			responseTimes.add(responseTime);
			waitTimes.add(waitTime);
			utilizationRates.add(utilizationRate);
		}
	}

	private static void plotGraphs() {
		plotGraph("Response Time", responseTimes, "Time (s)");
		plotGraph("Wait Time", waitTimes, "Time (s)");
		plotGraph("Resource Utilization", utilizationRates, "Utilization (%)");
	}

	private static void plotGraph(String title, List<Double> data, String yAxisLabel) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (int i = 0; i < data.size(); i++) {
			dataset.addValue(data.get(i), title, "Cloudlet " + i);
		}

		JFreeChart chart = ChartFactory.createLineChart(
				title,
				"Cloudlets",
				yAxisLabel,
				dataset
		);

		JFrame frame = new JFrame(title);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
		frame.setContentPane(chartPanel);
		frame.pack();
		frame.setVisible(true);
	}
}
