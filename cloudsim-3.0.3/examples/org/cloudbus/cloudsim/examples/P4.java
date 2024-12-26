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

public class P4 {

    /** The cloudlet list. */
    private static List<PriorityCloudlet> cloudletList;

    /** The vmlist. */
    private static List<Vm> vmlist;

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {

        Log.printLine("Starting CloudSimExample4 with Priority...");

        try {
            // First step: Initialize the CloudSim package.
            int num_user = 1;   // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            // Second step: Create Datacenters
            @SuppressWarnings("unused")
            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            @SuppressWarnings("unused")
            Datacenter datacenter1 = createDatacenter("Datacenter_1");

            // Third step: Create Broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Fourth step: Create Virtual Machines
            vmlist = new ArrayList<Vm>();

            // VM description
            int vmid = 0;
            int mips = 250;
            long size = 10000; // image size (MB)
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

            // Fifth step: Create Cloudlets with priority
            cloudletList = new ArrayList<PriorityCloudlet>();

            int id = 0;
            long length = 40000;
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            // PriorityCloudlet with priority 2
            PriorityCloudlet cloudlet1 = new PriorityCloudlet(id, length, pesNumber, fileSize, outputSize,
                    utilizationModel, utilizationModel, utilizationModel, 2); // Priority 2
            cloudlet1.setUserId(brokerId);
            id++;

            // PriorityCloudlet with priority 1
            PriorityCloudlet cloudlet2 = new PriorityCloudlet(id, length, pesNumber, fileSize, outputSize,
                    utilizationModel, utilizationModel, utilizationModel, 1); // Priority 1
            cloudlet2.setUserId(brokerId);

            cloudletList.add(cloudlet1);
            cloudletList.add(cloudlet2);

            // Sort cloudlets based on priority (higher priority first)
            cloudletList.sort((c1, c2) -> Integer.compare(c2.getPriority(), c1.getPriority()));

            broker.submitCloudletList(cloudletList);

            // Bind the cloudlets to the VMs
            broker.bindCloudletToVm(cloudlet1.getCloudletId(), vm1.getId());
            broker.bindCloudletToVm(cloudlet2.getCloudletId(), vm2.getId());

            // Sixth step: Start the simulation
            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList(newList);

            Log.printLine("CloudSimExample4 finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<Host>();

        List<Pe> peList = new ArrayList<Pe>();
        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // Create a PE

        int hostId = 0;
        int ram = 2048; // Host memory (MB)
        long storage = 1000000; // Host storage
        int bw = 10000;

        hostList.add(new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerSpaceShared(peList)
        ));

        String arch = "x86"; // System architecture
        String os = "Linux"; // Operating system
        String vmm = "Xen";
        double time_zone = 10.0; // Time zone this resource located
        double cost = 3.0; // Cost of using processing
        double costPerMem = 0.05; // Cost of using memory
        double costPerStorage = 0.001; // Cost of using storage
        double costPerBw = 0.0; // Cost of using bandwidth
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
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }
}

class PriorityCloudlet extends Cloudlet {
    private int priority;

    public PriorityCloudlet(int cloudletId, long length, int pesNumber, long fileSize, long outputSize,
                            UtilizationModel utilizationModel, UtilizationModel utilizationModel2,
                            UtilizationModel utilizationModel3, int priority) {
        super(cloudletId, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel2, utilizationModel3);
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
