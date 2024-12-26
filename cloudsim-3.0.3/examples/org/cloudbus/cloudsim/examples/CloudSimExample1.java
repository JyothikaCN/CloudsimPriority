package org.cloudbus.cloudsim.examples;

/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

// Add your existing imports here (org.cloudbus.cloudsim.*)

public class CloudSimExample1 {

	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;

	public static void main(String[] args) {

		Log.printLine("Starting CloudSimExample1...");

		try {
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			vmlist = new ArrayList<>();
			Vm vm = new Vm(0, brokerId, 1000, 1, 512, 1000, 10000, "Xen", new CloudletSchedulerTimeShared());
			vmlist.add(vm);
			broker.submitVmList(vmlist);

			cloudletList = new ArrayList<>();
			Cloudlet cloudlet = new Cloudlet(0, 400000, 1, 300, 300,
					new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
			cloudlet.setUserId(brokerId);
			cloudlet.setVmId(vm.getId());
			cloudletList.add(cloudlet);

			broker.submitCloudletList(cloudletList);

			CloudSim.startSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();

			printCloudletList(newList);

			// Generate Graphs
			generateResourceUtilizationGraph();
			generateResponseTimeGraph(newList);
			generateWaitingTimeGraph(newList);

			Log.printLine("CloudSimExample1 finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	private static void generateResourceUtilizationGraph() {
		XYSeries series = new XYSeries("CPU Utilization");
		series.add(1, 50);  // Replace with actual data points
		series.add(2, 70);
		series.add(3, 30);

		XYSeriesCollection dataset = new XYSeriesCollection(series);

		JFreeChart chart = ChartFactory.createXYLineChart(
				"Resource Utilization",
				"Time",
				"Utilization (%)",
				dataset
		);

		displayChart(chart);
	}

	private static void generateResponseTimeGraph(List<Cloudlet> cloudlets) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (Cloudlet cloudlet : cloudlets) {
			dataset.addValue(cloudlet.getFinishTime() - cloudlet.getExecStartTime(), "Response Time", "Cloudlet " + cloudlet.getCloudletId());
		}

		JFreeChart barChart = ChartFactory.createBarChart(
				"Response Time of Each Task",
				"Cloudlet",
				"Response Time (s)",
				dataset
		);

		displayChart(barChart);
	}

	private static void generateWaitingTimeGraph(List<Cloudlet> cloudlets) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (Cloudlet cloudlet : cloudlets) {
			double waitingTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
			dataset.addValue(waitingTime, "Waiting Time", "Cloudlet " + cloudlet.getCloudletId());
		}

		JFreeChart barChart = ChartFactory.createBarChart(
				"Waiting Time of Each Task",
				"Cloudlet",
				"Waiting Time (s)",
				dataset
		);

		displayChart(barChart);
	}

	private static void displayChart(JFreeChart chart) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		ChartPanel chartPanel = new ChartPanel(chart);
		frame.add(chartPanel, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}

	private static Datacenter createDatacenter(String name) {
		List<Host> hostList = new ArrayList<>();
		List<Pe> peList = new ArrayList<>();
		peList.add(new Pe(0, new PeProvisionerSimple(1000)));
		hostList.add(new Host(0, new RamProvisionerSimple(2048), new BwProvisionerSimple(10000),
				1000000, peList, new VmSchedulerTimeShared(peList)));

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
		}
		return broker;
	}

	private static void printCloudletList(List<Cloudlet> list) {
		DecimalFormat dft = new DecimalFormat("###.##");
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID    STATUS    Data center ID    VM ID    Time    Start Time    Finish Time");

		for (Cloudlet cloudlet : list) {
			Log.print(cloudlet.getCloudletId() + "    ");
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS    ");
				Log.print(cloudlet.getResourceId() + "    ");
				Log.print(cloudlet.getVmId() + "    ");
				Log.print(dft.format(cloudlet.getActualCPUTime()) + "    ");
				Log.print(dft.format(cloudlet.getExecStartTime()) + "    ");
				Log.print(dft.format(cloudlet.getFinishTime()));
				Log.printLine();
			}
		}
	}
}
