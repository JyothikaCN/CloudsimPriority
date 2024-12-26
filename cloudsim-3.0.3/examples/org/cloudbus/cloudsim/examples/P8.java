package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;

public class P8 {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;

    private static List<Vm> createVM(int userId, int vms, int idShift) {
        LinkedList<Vm> list = new LinkedList<Vm>();

        long size = 10000; //image size (MB)
        int ram = 512; //vm memory (MB)
        int mips = 250;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        Vm[] vm = new Vm[vms];

        for(int i = 0; i < vms; i++){
            vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            list.add(vm[i]);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift){
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        //cloudlet parameters
        long length = 40000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        PriorityCloudlet[] cloudlet = new PriorityCloudlet[cloudlets];

        for(int i = 0; i < cloudlets; i++){
            // Assign priorities cyclically (1, 2, 3, 1, 2, 3, ...)
            int priority = (i % 3) + 1;
            cloudlet[i] = new PriorityCloudlet(idShift + i, length, pesNumber, fileSize, outputSize,
                    utilizationModel, utilizationModel, utilizationModel, priority);
            cloudlet[i].setUserId(userId);
            list.add(cloudlet[i]);
        }

        return list;
    }

    public static void main(String[] args) {
        Log.printLine("Starting CloudSimPriorityExample...");

        try {
            int num_user = 2;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            GlobalBroker globalBroker = new GlobalBroker("GlobalBroker");

            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            Datacenter datacenter1 = createDatacenter("Datacenter_1");

            PriorityDatacenterBroker broker = createBroker("Broker_0");
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

            Log.printLine("CloudSimPriorityExample finished!");
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static Datacenter createDatacenter(String name){
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

        int hostId=0;
        int ram = 16384;
        long storage = 1000000;
        int bw = 10000;

        hostList.add(new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList1,
                new VmSchedulerTimeShared(peList1)
        ));

        hostId++;

        hostList.add(new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList2,
                new VmSchedulerTimeShared(peList2)
        ));

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.1;
        double costPerBw = 0.1;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<Storage>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static PriorityDatacenterBroker createBroker(String name){
        PriorityDatacenterBroker broker = null;
        try {
            broker = new PriorityDatacenterBroker(name);
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
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Priority" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent +
                "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
                Log.print("SUCCESS");
                int priority = ((PriorityCloudlet)cloudlet).getPriority();
                Log.printLine(indent + indent + priority + indent + indent +
                        cloudlet.getResourceId() + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }

    // Custom Cloudlet class with priority
    public static class PriorityCloudlet extends Cloudlet {
        private int priority;

        public PriorityCloudlet(int cloudletId, long cloudletLength, int pesNumber,
                                long cloudletFileSize, long cloudletOutputSize,
                                UtilizationModel utilizationCpu,
                                UtilizationModel utilizationMemory,
                                UtilizationModel utilizationBw,
                                int priority) {
            super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize,
                    utilizationCpu, utilizationMemory, utilizationBw);
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    // Custom Broker class that implements priority scheduling
    public static class PriorityDatacenterBroker extends DatacenterBroker {

        public PriorityDatacenterBroker(String name) throws Exception {
            super(name);
        }

        @Override
        protected void submitCloudlets() {
            // Sort cloudlets by priority in descending order (higher priority first)
            getCloudletList().sort((c1, c2) -> {
                PriorityCloudlet pc1 = (PriorityCloudlet) c1;
                PriorityCloudlet pc2 = (PriorityCloudlet) c2;
                return Integer.compare(pc2.getPriority(), pc1.getPriority());
            });

            super.submitCloudlets();
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
                    setBroker(createBroker(super.getName()+"_"));

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
            Log.printLine(super.getName()+" is starting...");
            schedule(getId(), 200, CREATE_BROKER);
        }

        @Override
        public void shutdownEntity() {}

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