import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.PrintStream;

// SocketServer can be an acceptor or a learner

public class SocketServer extends Thread {
    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;
    private Acceptor acceptor;
    private Learner learner = null;
    private String learner_ip;
    private int learner_port;
    private String name;
    private String[] ips;
    private int[] ports;

    // constructor for an acceptor
    // passing in acceptor to contact acceptor obj for paxos logic
    // passing in port to start the server on
    // passing in learner ip and port to contact learner server once a value has
    // been accepted
    public SocketServer(Acceptor acceptor, int port, String learner_ip, int learner_port, String acceptorName) {
        this.port = port;
        this.acceptor = acceptor;
        this.learner_ip = learner_ip;
        this.learner_port = learner_port;
        this.name = acceptorName;
    }

    // constructor for a learner
    public SocketServer(Learner learner, int port, String learnerName, String[] ips, int[] ports) {
        this.port = port;
        this.learner = learner;
        this.name = learnerName;
        this.ips = ips;
        this.ports = ports;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            this.start();
            System.out.println(name + " started on port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                // System.out.println("Listening for a connection");

                // Call accept() to receive the next connection
                Socket socket = serverSocket.accept();

                // if learner is not null, this server is for learner so it will be handled by a
                // LearnerHandler
                // otherwise it is for an acceptor and will be handled by a AcceptorHandler
                if (learner != null) {
                    Runnable r = new MyRunnable(this, socket, 1);
                    new Thread(r).start();
                } else {
                    // Pass the socket to the RequestHandler thread for processing
                    Runnable r = new MyRunnable(this, socket, 0);
                    new Thread(r).start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class MyRunnable implements Runnable {
        private Socket socket;
        private SocketServer server;
        private int type; // indicates what type of request handler to run, 0 for acceptor and 1 for learner

        public MyRunnable(SocketServer server, Socket socket, int type) {
            this.socket = socket;
            this.server = server;
            this.type = type;
        }

        public void run() {
            try {
                if (type == 0){
                    server.AcceptorHandler(socket);
                }else{ // type=1
                    server.LearnerHandler(socket);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void AcceptorHandler(Socket socket) throws InterruptedException {
        try {
            // System.out.println("Server received a connection");

            // Get input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());




            // RECEIVE PREPARE
            String first = in.readLine();

            if (first.equals("resolution")){
                int id = Integer.parseInt(in.readLine());
                String uid = in.readLine();
                int value = Integer.parseInt(in.readLine());
                synchronized (this) {
                    this.acceptor.resolution(id, uid, value);
                }
                System.out.println(name + ": learned (id:" + id + " uid:" + uid + " value:" + value + ")");
            } else {
                System.out.println(name + ": received prepare");
                int id = Integer.parseInt(first);
                String uid = in.readLine();

                ProposalID proposalID = new ProposalID(id, uid);
                Promise promise;
    
                synchronized (this) {
                    promise = this.acceptor.receivePrepare(uid, proposalID);
                }
    
                if (promise != null) {
                    String p_acceptorUID = promise.acceptorUID;
                    String p_proposal_number = String.valueOf(promise.proposalID.getNumber());
                    String p_proposal_uid = promise.proposalID.getUID();
                    String p_previous_number = null;
                    String p_previous_uid = null;
                    if (promise.previousID != null) {
                        p_previous_number = String.valueOf(promise.previousID.getNumber());
                        p_previous_uid = promise.previousID.getUID();
                    }
                    String p_acceptedValue = String.valueOf(promise.acceptedValue);
    
                    // SEND PROMISE
                    System.out.println(name + ": sending promise");
                    out.println(p_acceptorUID);
                    out.println(p_proposal_number);
                    out.println(p_proposal_uid);
                    out.println(p_previous_number);
                    out.println(p_previous_uid);
                    out.println(p_acceptedValue);
                    out.flush();
                    //
    
    
    
                } else {
                    System.out.println(name + ": prepare not accepted");
                }
    
    
                // RECEIVE ACCEPT REQUEST
                System.out.println(name + ": received accept request");
                String num = in.readLine();
                String a_proposal_uid = in.readLine();
                String val = in.readLine();
                int a_value = Integer.parseInt(val);
                int a_proposal_number = Integer.parseInt(num);
                ProposalID a_proposalID = new ProposalID(a_proposal_number, a_proposal_uid);
                //
    
    
                AcceptRequest accepted;
                synchronized (this) {
                    accepted = acceptor.receiveAcceptRequest(a_proposal_uid, a_proposalID, a_value);
                }
    
                Socket l_socket;
                if (accepted != null) {
                    // create a socket to inform learner of the accepted value
                    l_socket = new Socket(learner_ip, learner_port);
                    // Create input and output streams to read from and write to the server
                    PrintStream l_out = new PrintStream(l_socket.getOutputStream());
                    // BufferedReader l_in = new BufferedReader(new
                    // InputStreamReader(l_socket.getInputStream()));
    
                    System.out.println(name + ": sending accepted");
                    l_out.println(acceptor.getAcceptorUID());
                    l_out.println(accepted.proposalID.getNumber());
                    l_out.println(accepted.proposalID.getUID());
                    l_out.println(accepted.proposedValue);
                    l_out.flush();
    
                    l_out.close();
                    l_socket.close();
    
                } else {
                    System.out.println(name + ": accept request not accepted");
                }    
            }
            //

            // Close our connection
            in.close();
            out.close();
            socket.close();

            // System.out.println("Connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void LearnerHandler(Socket socket) throws InterruptedException {
        try {
            // System.out.println("Server received a connection");

            // Get input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            System.out.println(name + ": received accepted");
            String acceptor_uid = in.readLine();
            int number = Integer.parseInt(in.readLine());
            String uid = in.readLine();
            int acceptedValue = Integer.parseInt(in.readLine());

            ProposalID acceptedProposalID = new ProposalID(number, uid);

            AcceptRequest resolution;
            synchronized (this) {
                resolution = learner.receiveAccepted(acceptor_uid, acceptedProposalID, acceptedValue);
            }

            if (resolution != null) {
                System.out.println("\n" + name + ": RESOLUTION ID: " + resolution.proposalID.getNumber() + "\n");
                System.out.println("\n" + name + ": RESOLUTION UID: " + resolution.proposalID.getUID() + "\n");
                System.out.println("\n" + name + ": RESOLUTION VALUE:" + resolution.proposedValue + "\n");

                for (int i = 0; i < ips.length; i++) {
                    // inform all acceptors of the decision
                    Socket re_socket = new Socket(ips[i], ports[i]);
                    PrintStream re_out = new PrintStream(re_socket.getOutputStream());
                    // BufferedReader l_in = new BufferedReader(new
                    // InputStreamReader(l_socket.getInputStream()));
    
                    System.out.println(name + ": informing acceptor" + i);
                    re_out.println("resolution");
                    re_out.println(resolution.proposalID.getNumber());
                    re_out.println(resolution.proposalID.getUID());
                    re_out.println(resolution.proposedValue);
                    re_out.flush();

                    re_socket.close();
                    re_out.close();
                }

            } else {
                System.out.println(name + ": majority not reached");
            }

            // Close our connection
            in.close();
            out.close();
            socket.close();

            // System.out.println("Connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}