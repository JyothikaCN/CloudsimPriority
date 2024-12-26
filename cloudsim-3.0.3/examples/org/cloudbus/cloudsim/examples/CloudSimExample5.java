package org.cloudbus.cloudsim.examples;

import java.awt.BorderLayout;
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
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class CloudSimExample5 {

	private static List<Cloudlet> cloudletList1;
	private static List<Cloudlet> cloudletList2;

	private static List<Vm> vmlist1;
	private static List<Vm> vmlist2;

	private static List<Double> responseTimes = new ArrayList<>();
	private static List<Double> waitTimes = new ArrayList<>();
	private static List<Double> resourceUtilization = new ArrayList<>();

	public static void main(String[] args) {

		Log.printLine("Starting CloudSimExample5...");

		try {
			int num_user = 2;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("Datacenter_1");

			DatacenterBroker broker1 = createBroker(1);
			int brokerId1 = broker1.getId();

			DatacenterBroker broker2 = createBroker(2);
			int brokerId2 = broker2.getId();

			vmlist1 = new ArrayList<Vm>();
			vmlist2 = new ArrayList<Vm>();

			int vmid = 0;
			int mips = 250;
			long size = 10000;
			int ram = 512;
			long bw = 1000;
			int pesNumber = 1;
			String vmm = "Xen";

			Vm vm1 = new Vm(vmid, brokerId1, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			Vm vm2 = new Vm(vmid, brokerId2, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

			vmlist1.add(vm1);
			vmlist2.add(vm2);

			broker1.submitVmList(vmlist1);
			broker2.submitVmList(vmlist2);

			cloudletList1 = new ArrayList<Cloudlet>();
			cloudletList2 = new ArrayList<Cloudlet>();

			int id = 0;
			long length = 40000;
			long fileSize = 300;
			long outputSize = 300;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			Cloudlet cloudlet1 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet1.setUserId(brokerId1);

			Cloudlet cloudlet2 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet2.setUserId(brokerId2);

			cloudletList1.add(cloudlet1);
			cloudletList2.add(cloudlet2);

			broker1.submitCloudletList(cloudletList1);
			broker2.submitCloudletList(cloudletList2);

			CloudSim.startSimulation();

			List<Cloudlet> newList1 = broker1.getCloudletReceivedList();
			List<Cloudlet> newList2 = broker2.getCloudletReceivedList();

			collectResults(broker1);
			collectResults(broker2);

			CloudSim.stopSimulation();

			Log.print("=============> User " + brokerId1 + "    ");
			printCloudletList(newList1);

			Log.print("=============> User " + brokerId2 + "    ");
			printCloudletList(newList2);

			Log.printLine("CloudSimExample5 finished!");

			// Creating the frames for the charts
			JFrame frame1 = createChartFrame("Response Time", createResponseTimeChart());
			JFrame frame2 = createChartFrame("Wait Time", createWaitTimeChart());
			JFrame frame3 = createChartFrame("Resource Utilization", createResourceUtilizationChart());

			frame1.setVisible(true);
			frame2.setVisible(true);
			frame3.setVisible(true);

		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static void collectResults(DatacenterBroker broker) {
		List<Cloudlet> cloudletList = broker.getCloudletReceivedList();
		DecimalFormat dft = new DecimalFormat("###.##");

		for (Cloudlet cloudlet : cloudletList) {
			double responseTime = cloudlet.getFinishTime() - cloudlet.getExecStartTime();
			responseTimes.add(responseTime);

			double waitTime = cloudlet.getExecStartTime() - cloudlet.getSubmissionTime();
			waitTimes.add(waitTime);

			double cpuTime = cloudlet.getActualCPUTime();
			double totalCPUTime = cloudlet.getFinishTime() - cloudlet.getSubmissionTime();
			double utilization = cpuTime / totalCPUTime;
			resourceUtilization.add(utilization);
		}
	}

	private static Datacenter createDatacenter(String name){
		List<Host> hostList = new ArrayList<Host>();
		List<Pe> peList = new ArrayList<Pe>();

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

	private static DatacenterBroker createBroker(int id){
		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker" + id);
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
				"Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");

				Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) +
						indent + indent + dft.format(cloudlet.getFinishTime()));
			}
		}
	}

	private static JFrame createChartFrame(String title, JFreeChart chart) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new ChartPanel(chart), BorderLayout.CENTER);
		frame.pack();
		return frame;
	}

	private static JFreeChart createResponseTimeChart() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int i = 0; i < responseTimes.size(); i++) {
			dataset.addValue(responseTimes.get(i), "Response Time", "Cloudlet " + (i + 1));
		}
		return ChartFactory.createBarChart("Response Time", "Cloudlet", "Time (s)", dataset);
	}

	private static JFreeChart createWaitTimeChart() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int i = 0; i < waitTimes.size(); i++) {
			dataset.addValue(waitTimes.get(i), "Wait Time", "Cloudlet " + (i + 1));
		}
		return ChartFactory.createBarChart("Wait Time", "Cloudlet", "Time (s)", dataset);
	}

	private static JFreeChart createResourceUtilizationChart() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int i = 0; i < resourceUtilization.size(); i++) {
			dataset.addValue(resourceUtilization.get(i), "Utilization", "Cloudlet " + (i + 1));
		}
		return ChartFactory.createBarChart("Resource Utilization", "Cloudlet", "Utilization (%)", dataset);
	}
}
