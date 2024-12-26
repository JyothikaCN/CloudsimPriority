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
	 * a datacenter with one host and run two
	 * cloudlets on it. The cloudlets run in
	 * VMs with the same MIPS requirements.
	 * The cloudlets will take the same time to
	 * complete the execution.
 */
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

import java.text.DecimalFormat;
import java.util.*;

public class CloudSimExample2 {

	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;

	public static void main(String[] args) {
		Log.printLine("Starting CloudSimExampleGraphs...");

		try {
			int num_user = 1;   // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			// Create Datacenter
			Datacenter datacenter0 = createDatacenter("Datacenter_0");

			// Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			// Create VMs
			vmlist = new ArrayList<>();
			int mips = 250;
			long size = 10000; // image size (MB)
			int ram = 512; // vm memory (MB)
			long bw = 1000;
			int pesNumber = 1; // number of CPUs
			String vmm = "Xen"; // VMM name

			Vm vm1 = new Vm(0, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			Vm vm2 = new Vm(1, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

			vmlist.add(vm1);
			vmlist.add(vm2);

			broker.submitVmList(vmlist);

			// Create Cloudlets
			cloudletList = new ArrayList<>();
			long length = 250000;
			long fileSize = 300;
			long outputSize = 300;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			Cloudlet cloudlet1 = new Cloudlet(0, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet1.setUserId(brokerId);
			Cloudlet cloudlet2 = new Cloudlet(1, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet2.setUserId(brokerId);

			cloudletList.add(cloudlet1);
			cloudletList.add(cloudlet2);

			broker.submitCloudletList(cloudletList);

			broker.bindCloudletToVm(cloudlet1.getCloudletId(), vm1.getId());
			broker.bindCloudletToVm(cloudlet2.getCloudletId(), vm2.getId());

			CloudSim.startSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();

			printCloudletList(newList);

			// Generate graphs
			plotResponseTime(newList);
			plotResourceUtilization(newList);
			plotWaitingTime(newList);

			Log.printLine("CloudSimExampleGraphs finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Simulation terminated due to an unexpected error");
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

		hostList.add(new Host(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerSimple(bw),
				storage,
				peList,
				new VmSchedulerTimeShared(peList)
		));

		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.001;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

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
		String indent = "    ";
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
				"Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (Cloudlet cloudlet : list) {
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");

				Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + cloudlet.getVmId() +
						indent + dft.format(cloudlet.getActualCPUTime()) + indent +
						dft.format(cloudlet.getExecStartTime()) + indent +
						dft.format(cloudlet.getFinishTime()));
			}
		}
	}

	private static void plotResponseTime(List<Cloudlet> cloudletList) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (Cloudlet cloudlet : cloudletList) {
			double responseTime = cloudlet.getFinishTime() - cloudlet.getExecStartTime();
			dataset.addValue(responseTime, "Response Time", "Cloudlet " + cloudlet.getCloudletId());
		}

		JFreeChart chart = ChartFactory.createBarChart(
				"Response Time",
				"Cloudlet ID",
				"Time (s)",
				dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				false
		);

		showChart(chart);
	}

	private static void plotResourceUtilization(List<Cloudlet> cloudletList) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (Cloudlet cloudlet : cloudletList) {
			double utilization = cloudlet.getUtilizationOfCpu(1.0); // Assuming 1.0 is the simulation time
			dataset.addValue(utilization, "Resource Utilization", "Cloudlet " + cloudlet.getCloudletId());
		}

		JFreeChart chart = ChartFactory.createBarChart(
				"Resource Utilization",
				"Cloudlet ID",
				"Utilization",
				dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				false
		);

		showChart(chart);
	}

	private static void plotWaitingTime(List<Cloudlet> cloudletList) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (Cloudlet cloudlet : cloudletList) {
			double waitingTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
			dataset.addValue(waitingTime, "Waiting Time", "Cloudlet " + cloudlet.getCloudletId());
		}

		JFreeChart chart = ChartFactory.createBarChart(
				"Waiting Time",
				"Cloudlet ID",
				"Time (s)",
				dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				false
		);

		showChart(chart);
	}

	private static void showChart(JFreeChart chart) {
		ChartPanel panel = new ChartPanel(chart);
		javax.swing.JFrame frame = new javax.swing.JFrame();
		frame.setContentPane(panel);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}
}
