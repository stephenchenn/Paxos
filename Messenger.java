public class Messenger{

    protected SocketServer server;
    protected SocketClient client;

    public Messenger () {
        // default constructor
    }

    public Messenger (SocketServer server) {
        this.server = server;
    }

    public Messenger (SocketClient client) {
        this.client = client;
    }

    public void startListening(){
        server.startServer();
    }

    public void sendPrepare(Proposer proposer, ProposalID proposalID, String ip, int port){
        client.startClient(proposalID, ip, port);
    }

}
