package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Example demonstrating priority-based scheduling in CloudSim.
 */
public class P5 {

    private static List<PriorityCloudlet> cloudletList1;
    private static List<PriorityCloudlet> cloudletList2;

    private static List<Vm> vmlist1;
    private static List<Vm> vmlist2;

    public static void main(String[] args) {
        Log.printLine("Starting PriorityExample...");

        try {
            int num_user = 2;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            Datacenter datacenter1 = createDatacenter("Datacenter_1");

            PriorityBasedBroker broker1 = createPriorityBroker(1);
            int brokerId1 = broker1.getId();

            PriorityBasedBroker broker2 = createPriorityBroker(2);
            int brokerId2 = broker2.getId();

            vmlist1 = new ArrayList<>();
            vmlist2 = new ArrayList<>();

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

            cloudletList1 = new ArrayList<>();
            cloudletList2 = new ArrayList<>();

            // Assign priorities (lower value = higher priority)
            PriorityCloudlet cloudlet1 = createPriorityCloudlet(0, 1, brokerId1);
            PriorityCloudlet cloudlet2 = createPriorityCloudlet(1, 2, brokerId2);

            cloudletList1.add(cloudlet1);
            cloudletList2.add(cloudlet2);

            broker1.submitCloudletList(cloudletList1);
            broker2.submitCloudletList(cloudletList2);

            CloudSim.startSimulation();

            List<Cloudlet> newList1 = broker1.getCloudletReceivedList();
            List<Cloudlet> newList2 = broker2.getCloudletReceivedList();

            CloudSim.stopSimulation();

            Log.print("=============> User " + brokerId1 + "    ");
            printCloudletList(newList1);

            Log.print("=============> User " + brokerId2 + "    ");
            printCloudletList(newList2);

            Log.printLine("PriorityExample finished!");
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

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerSpaceShared(peList)
                )
        );

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static PriorityBasedBroker createPriorityBroker(int id) {
        PriorityBasedBroker broker = null;
        try {
            broker = new PriorityBasedBroker("Broker" + id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return broker;
    }

    private static PriorityCloudlet createPriorityCloudlet(int id, int priority, int userId) {
        long length = 40000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        PriorityCloudlet cloudlet = new PriorityCloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, priority);
        cloudlet.setUserId(userId);
        return cloudlet;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "Priority" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (Cloudlet cloudlet : list) {
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + ((PriorityCloudlet) cloudlet).getPriority() + indent + cloudlet.getResourceId() +
                        indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }
}

/**
 * Custom DatacenterBroker for Priority Scheduling.
 */
class PriorityBasedBroker extends DatacenterBroker {

    public PriorityBasedBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void submitCloudlets() {
        // Sort cloudlets by priority (lower value = higher priority)
        getCloudletList().sort((cloudlet1, cloudlet2) -> {
            if (cloudlet1 instanceof PriorityCloudlet && cloudlet2 instanceof PriorityCloudlet) {
                return Integer.compare(((PriorityCloudlet) cloudlet1).getPriority(), ((PriorityCloudlet) cloudlet2).getPriority());
            }
            return 0; // Default behavior if not PriorityCloudlet
        });
        super.submitCloudlets();
    }
}
