package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
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
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class CloudSimExample8 {

	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmList;

	private static List<Vm> createVM(int userId, int vms, int idShift) {
		LinkedList<Vm> list = new LinkedList<Vm>();
		long size = 10000;
		int ram = 512;
		int mips = 250;
		long bw = 1000;
		int pesNumber = 1;
		String vmm = "Xen";
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
		Log.printLine("Starting CloudSimExample8...");

		try {
			int num_user = 2;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			GlobalBroker globalBroker = new GlobalBroker("GlobalBroker");

			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("Datacenter_1");

			DatacenterBroker broker = createBroker("Broker_0");
			int brokerId = broker.getId();

			vmList = createVM(brokerId, 5, 0);
			cloudletList = createCloudlet(brokerId, 10, 0);

			broker.submitVmList(vmList);
			broker.submitCloudletList(cloudletList);

			CloudSim.startSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();
			newList.addAll(globalBroker.getBroker().getCloudletReceivedList());

			CloudSim.stopSimulation();

			printCloudletList(newList);

			// Generate Bar Graphs for Waiting Time, Response Time, and Resource Utilization
			generateBarGraph(newList);

			Log.printLine("CloudSimExample8 finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static void generateBarGraph(List<Cloudlet> cloudletList) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// Gather data for Waiting Time, Response Time, and Resource Utilization
		for (Cloudlet cloudlet : cloudletList) {
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				double waitingTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
				double responseTime = cloudlet.getFinishTime() - cloudlet.getSubmissionTime();
				double resourceUtilization = cloudlet.getActualCPUTime() / (cloudlet.getFinishTime() - cloudlet.getSubmissionTime());

				dataset.addValue(waitingTime, "Waiting Time", "Cloudlet " + cloudlet.getCloudletId());
				dataset.addValue(responseTime, "Response Time", "Cloudlet " + cloudlet.getCloudletId());
				dataset.addValue(resourceUtilization, "Resource Utilization", "Cloudlet " + cloudlet.getCloudletId());
			}
		}

		// Create a bar chart from the dataset
		JFreeChart chart = ChartFactory.createBarChart(
				"CloudSim Performance Metrics", // Chart title
				"Cloudlet ID", // X-axis label
				"Time/Utilization", // Y-axis label
				dataset
		);

		// Display the chart in a panel
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));

		// Create a frame to display the chart
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(chartPanel);
		frame.pack();
		frame.setVisible(true);
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

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

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

	public static class GlobalBroker extends SimEntity {

		private static final int CREATE_BROKER = 0;
		private List<Vm> vmList;
		private List<Cloudlet> cloudletList;
		private DatacenterBroker broker;

		public GlobalBroker(String name) {
			super(name);
		}

		@Override
		public void processEvent(SimEvent ev) {
			switch (ev.getTag()) {
				case CREATE_BROKER:
					setBroker(createBroker(super.getName() + "_"));

					setVmList(createVM(getBroker().getId(), 5, 100));
					setCloudletList(createCloudlet(getBroker().getId(), 10, 100));

					broker.submitVmList(getVmList());
					broker.submitCloudletList(getCloudletList());

					CloudSim.resumeSimulation();

					break;

				default:
					Log.printLine(getName() + ": unknown event type");
					break;
			}
		}

		@Override
		public void startEntity() {
			Log.printLine(super.getName() + " is starting...");
			schedule(getId(), 200, CREATE_BROKER);
		}

		@Override
		public void shutdownEntity() {
		}

		public List<Vm> getVmList() {
			return vmList;
		}

		protected void setVmList(List<Vm> vmList) {
			this.vmList = vmList;
		}

		public List<Cloudlet> getCloudletList() {
			return cloudletList;
		}

		protected void setCloudletList(List<Cloudlet> cloudletList) {
			this.cloudletList = cloudletList;
		}

		public DatacenterBroker getBroker() {
			return broker;
		}

		protected void setBroker(DatacenterBroker broker) {
			this.broker = broker;
		}
	}
}
