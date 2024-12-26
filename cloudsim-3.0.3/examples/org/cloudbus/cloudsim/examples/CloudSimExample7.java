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

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;


public class CloudSimExample7 {

	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;

	private static List<Vm> createVM(int userId, int vms, int idShift) {
		LinkedList<Vm> list = new LinkedList<Vm>();
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		int mips = 250;
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		Vm[] vm = new Vm[vms];

		for (int i = 0; i < vms; i++) {
			vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			list.add(vm[i]);
		}
		return list;
	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();
		long length = 40000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}

	public static void main(String[] args) {
		Log.printLine("Starting CloudSimExample7...");
		try {
			int num_user = 2;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("Datacenter_1");

			DatacenterBroker broker = createBroker("Broker_0");
			int brokerId = broker.getId();

			vmlist = createVM(brokerId, 5, 0);
			cloudletList = createCloudlet(brokerId, 10, 0);

			broker.submitVmList(vmlist);
			broker.submitCloudletList(cloudletList);

			CloudSim.startSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();
			CloudSim.stopSimulation();

			printCloudletList(newList);

			// Create the dataset for line charts
			XYSeries responseTimeSeries = new XYSeries("Response Time");
			XYSeries waitingTimeSeries = new XYSeries("Waiting Time");
			XYSeries resourceUtilizationSeries = new XYSeries("Resource Utilization");

			DecimalFormat dft = new DecimalFormat("###.##");

			for (Cloudlet cloudlet : newList) {
				if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
					double responseTime = cloudlet.getFinishTime() - cloudlet.getExecStartTime();
					double waitingTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
					double resourceUtilization = cloudlet.getActualCPUTime() / cloudlet.getExecStartTime(); // simplistic approach

					responseTimeSeries.add(cloudlet.getCloudletId(), responseTime);
					waitingTimeSeries.add(cloudlet.getCloudletId(), waitingTime);
					resourceUtilizationSeries.add(cloudlet.getCloudletId(), resourceUtilization);
				}
			}

			// Create a dataset for the line chart
			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(responseTimeSeries);
			dataset.addSeries(waitingTimeSeries);
			dataset.addSeries(resourceUtilizationSeries);

			// Create a line chart using JFreeChart
			JFreeChart chart = ChartFactory.createXYLineChart(
					"CloudSim - Cloudlet Statistics",
					"Cloudlet ID",
					"Value",
					dataset,
					PlotOrientation.VERTICAL,
					true, true, false
			);

			// Show the chart in a window
			ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
			javax.swing.JFrame frame = new javax.swing.JFrame();
			frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(chartPanel);
			frame.pack();
			frame.setVisible(true);

			Log.printLine("CloudSimExample7 finished!");
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
		int ram = 16384;
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

	private static DatacenterBroker createBroker(String name) {
		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker(name);
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
}
