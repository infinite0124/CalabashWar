package sample;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 网络方法接口
 */
public class CalabashClient {
    private Main player;//从属的玩家
    private int UDP_PORT;//客户端的UDP端口号
    private String serverIP = Attributes.localServerIP;//服务器IP地址
    private int serverUDPPort;//服务器转发客户UDP包的UDP端口
    private DatagramSocket ds = null;//客户端的UDP套接字

    public void setUDP_PORT(int UDP_PORT) {
        this.UDP_PORT = UDP_PORT;
    }

    public CalabashClient(Main player) {
        this.player = player;
        this.serverIP = player.serverIP;
        try {
            this.UDP_PORT = getRandomUDPPort();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    /**
     * 与服务器进行TCP连接
     *
     * @param ip server IP
     */
    public boolean connect(String ip) {
        Socket s = null;
        try {
            ds = new DatagramSocket(UDP_PORT);//创建UDP套接字
            try {
                s = new Socket(ip, CalabashServer.TCP_PORT);//创建TCP套接字
            } catch (Exception e1) {
                ds.close();
                return false;
            }
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.writeInt(UDP_PORT);//向服务器发送自己的UDP端口号
            DataInputStream in = new DataInputStream(s.getInputStream());
            int id = in.readInt();//获得自己的id号
            player.myID = id;
            this.serverUDPPort = in.readInt();//获得服务器转发客户端消息的UDP端口号
            System.out.println("connect to server successfully...");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (s != null) s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new Thread(new UDPThread()).start();//开启客户端UDP线程, 向服务器发送或接收游戏数据
        return true;
    }

    /**
     * 客户端随机获取UDP端口号
     *
     * @return
     */
    private int getRandomUDPPort() {
        return 55558 + (int) (Math.random() * 9000);
    }

    public void send(Message message) {
        message.send(ds, serverIP, serverUDPPort);
    }

    public class UDPThread implements Runnable {

        byte[] buffer = new byte[1024];

        @Override
        public void run() {
            while (ds != null) {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                try {
                    ds.receive(dp);
                    parse(dp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void parse(DatagramPacket dp) {
            ByteArrayInputStream bin = new ByteArrayInputStream(buffer, 0, dp.getLength());
            DataInputStream in = new DataInputStream(bin);
            int messageType = 0;
            try {
                messageType = in.readInt();//获得消息类型
            } catch (IOException e) {
                e.printStackTrace();
            }
            Message message = null;
            switch (messageType) {//根据消息的类型调用对应消息的解析方法
                case Message.GAME_START:
                    //message = new GameStartMessage(player);
                    //message.parse(in);
                    break;
                case Message.ROLE_MOVE:
                    message = new RoleMoveMessage(player);
                    message.parse(in);
                    break;
                case Message.ATTACK:
                    message = new AttackMessage(player);
                    message.parse(in);
                    break;
                case Message.NEW_PLAYER:
                    message = new NewPlayerMessage(player);
                    message.parse(in);
                    break;
                case Message.INVITATION:
                    message = new InviteMessage(player);
                    message.parse(in);
                    break;
                case Message.REPLY:
                    message = new ReplyMessage(player);
                    message.parse(in);
                    break;
            }
        }
    }
}



