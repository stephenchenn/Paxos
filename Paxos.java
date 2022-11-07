import java.lang.String;
import java.net.ServerSocket;
import java.io.IOException;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Paxos {

    static Proposer createProposer (String proposerUID, int quorumSize, String proposerName) {
        Proposer proposer = new Proposer(proposerUID, quorumSize);
        SocketClient client = new SocketClient(proposer, proposerName);
        Messenger proposerMessenger = new Messenger(client);
        proposer.setMessenger(proposerMessenger);
        proposer.setProposal(Integer.parseInt(proposerUID));
        return proposer;
    }

    static Learner createLearner (String l_ip, int l_port, int quorumSize, String learnerName, String[] ips, int[] ports) {
        Learner learner = new Learner(quorumSize);
        SocketServer l_server = new SocketServer(learner, l_port, learnerName, ips, ports);
        Messenger messenger = new Messenger(l_server);
        learner.setMessenger(messenger);
        return learner;
    }

    static Acceptor createAcceptor (String acceptorUID, int a_port, String l_ip, int l_port, String acceptorName) {
        Acceptor acceptor = new Acceptor(acceptorUID);
        SocketServer server = new SocketServer(acceptor, a_port, l_ip, l_port, acceptorName);
        Messenger acceptorMessenger1 = new Messenger(server);
        acceptor.setMessenger(acceptorMessenger1);
        return acceptor;
    }

    static AcceptRequest start (String[] args) throws IOException {

        PrintStream stdout = System.out;
        PrintStream out = new PrintStream(new FileOutputStream("output.txt"), true);
        System.setOut(out);

        // implemeting proposers as clients, acceptors and learners as servers

        // 9 members in the council
        int quorumSize = 5; // majority of 9 is 5

        // acceptor ips
        String ip1 = "127.0.0.1";
        String ip2 = "127.0.0.1";
        String ip3 = "127.0.0.1";
        String ip4 = "127.0.0.1";
        String ip5 = "127.0.0.1";
        String ip6 = "127.0.0.1";
        String ip7 = "127.0.0.1";
        String ip8 = "127.0.0.1";
        String ip9 = "127.0.0.1";
        String[] ips = {ip1, ip2, ip3, ip4, ip5, ip6, ip7, ip8, ip9};

        // M1-M3 will have both a proposer and an acceptor because they have a right to vote as well

        // proposers will propose their uid as the proposed value, as they want to be elected
        Proposer proposer1 = createProposer("1", quorumSize, "M1");
        Proposer proposer2 = createProposer("2", quorumSize, "M2");
        Proposer proposer3 = createProposer("3", quorumSize, "M3");
        
        // find 10 available ports
        int _ports[] = new int[10];
        try {
            for (int i=0; i<10 ; i++){
                ServerSocket s = new ServerSocket(0);
                _ports[i] = s.getLocalPort();
                // System.out.println("port: " + _ports[i]);
                s.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // learner
        int l_port = _ports[9];
        String l_ip = "127.0.0.1";
        Learner learner = createLearner(l_ip, l_port, quorumSize, "L1", ips, _ports);
        learner.start();

        // acceptors
        Acceptor acceptor1 = createAcceptor("1", _ports[0], l_ip, l_port, "M1");
        Acceptor acceptor2 = createAcceptor("2", _ports[1], l_ip, l_port, "M2");
        Acceptor acceptor3 = createAcceptor("3", _ports[2], l_ip, l_port, "M3");
        Acceptor acceptor4 = createAcceptor("4", _ports[3], l_ip, l_port, "M4");
        Acceptor acceptor5 = createAcceptor("5", _ports[4], l_ip, l_port, "M5");
        Acceptor acceptor6 = createAcceptor("6", _ports[5], l_ip, l_port, "M6");
        Acceptor acceptor7 = createAcceptor("7", _ports[6], l_ip, l_port, "M7");
        Acceptor acceptor8 = createAcceptor("8", _ports[7], l_ip, l_port, "M8");
        Acceptor acceptor9 = createAcceptor("9", _ports[8], l_ip, l_port, "M9");

        Acceptor [] acceptors = {acceptor1, acceptor2, acceptor3, acceptor4, acceptor5, acceptor6, acceptor7, acceptor8, acceptor9};

        // acceptors start listening for requests
        for (int i = 0; i < acceptors.length; i++) {
			acceptors[i].start();
		}
    
        // proposers send prepare
        int firstProposer = Integer.parseInt(args[1]);
        if (firstProposer == 1){
            proposer1.prepare(ips, _ports);
            proposer2.prepare(ips, _ports);
            proposer3.prepare(ips, _ports);
        } else if (firstProposer == 2){
            proposer2.prepare(ips, _ports);
            proposer1.prepare(ips, _ports);
            proposer3.prepare(ips, _ports);
        } else if (firstProposer == 3) {
            proposer3.prepare(ips, _ports);
            proposer1.prepare(ips, _ports);
            proposer2.prepare(ips, _ports);
        }
        
        while (true) {
            boolean allLearned = true;

            try{
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < acceptors.length; i++) {
                if (acceptors[i].getFinalValue() == -1){
                    allLearned = false;
                    break;
                }
            }
            
            if (allLearned) {
                System.setOut(stdout);
                if (args[0].equals("main")){
                    System.out.println("\n\n*** RESULT: M" + learner.getFinalValue() + " is elected president! ***\n\n");
                    System.out.println("\n\n*** find logs in output.txt! ***\n\n");
                    System.out.println("\n\n*** program has finished, press control+c to exit! ***\n\n");
                }
                break;
            }
        }

        // returning result for testing purposes
        AcceptRequest result = new AcceptRequest(learner.getFinalProposalID(), learner.getFinalValue());
        return result;
    }

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter who proposes first (1 for M1, 2 for M2, 3 for M3): ");
        // int firstProposer = Integer.parseInt(br.readLine());
        String firstProposer = br.readLine();
        while (!((firstProposer.equals("1"))||(firstProposer.equals("2"))||(firstProposer.equals("3")))){
            System.out.print("Invalid input\nEnter who proposes first (1 for M1, 2 for M2, 3 for M3): ");
            firstProposer = br.readLine();
        }
        String[] mode = {"main", firstProposer};
        start(mode);
    }
}